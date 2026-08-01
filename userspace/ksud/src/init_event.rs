use crate::module::{handle_updated_modules, prune_modules};
use crate::utils::{is_safe_mode, switch_mnt_ns};
use crate::{
    assets, builtin_mount, defs, ksucalls, metamodule, restorecon,
    utils::{self},
};
use anyhow::{Context, Result};
use libc::_exit;
use log::{info, warn};
use prop_rs_android::resetprop::ResetProp;
use prop_rs_android::sys_prop;
use rustix::process::chdir;
use std::path::Path;
use std::process::Command;

pub fn on_post_data_fs() -> Result<()> {
    if ksucalls::is_uapi_version_mismatch() {
        warn!(
            "Kernel and userspace uapi version mismatch; continue post-fs-data with compatible paths"
        );
    }

    ksucalls::report_post_fs_data();
    crate::late_load::register_default_manager_appid();

    utils::umask(0);

    crate::rescue::check_on_post_fs_data();
    let rescue_skip_modules = crate::rescue::take_skip_modules_once();

    // Clear all temporary module configs early
    if let Err(e) = crate::module_config::clear_all_temp_configs() {
        warn!("clear temp configs failed: {e}");
    }

    #[cfg(unix)]
    let _ = catch_bootlog("logcat", &["logcat", "-b", "all"]);
    #[cfg(unix)]
    let _ = catch_bootlog("dmesg", &["dmesg", "-w", "-r"]);

    if rescue_skip_modules {
        warn!("rescue requested temporary module skip; skip post-fs-data module stages");
        match crate::cpu_spoof::disable_for_recovery() {
            Ok(true) => warn!("disabled CPU spoof during rescue startup"),
            Ok(false) => {}
            Err(e) => warn!("failed to disable CPU spoof during rescue startup: {e:#}"),
        }
        if let Err(e) = assets::ensure_binaries(true) {
            warn!("failed to extract bin assets during rescue module skip: {e}");
        }
        return Ok(());
    }

    if utils::has_magisk() {
        warn!("Magisk detected, skip post-fs-data!");
        return Ok(());
    }

    let safe_mode = crate::utils::is_safe_mode();

    if safe_mode {
        // we should still ensure module directory exists in safe mode
        // because we may need to operate the module dir in safe mode
        warn!("safe mode, skip common post-fs-data.d scripts");
    } else {
        // Then exec common post-fs-data scripts
        if let Err(e) = crate::module::exec_common_scripts("post-fs-data.d", true) {
            warn!("exec common post-fs-data scripts failed: {e}");
        }
    }

    let module_dir = defs::MODULE_DIR;

    assets::ensure_binaries(true).with_context(|| "Failed to extract bin assets")?;
    if let Err(e) = utils::ensure_magisk_module_compat() {
        warn!("ensure magisk module compat failed: {e}");
    }

    // if we are in safe mode, we should disable all modules
    if safe_mode {
        warn!("safe mode, skip post-fs-data scripts and disable all modules!");
        match crate::cpu_spoof::disable_for_recovery() {
            Ok(true) => warn!("disabled CPU spoof during safe-mode startup"),
            Ok(false) => {}
            Err(e) => warn!("failed to disable CPU spoof during safe-mode startup: {e:#}"),
        }
        if let Err(e) = crate::module::disable_all_modules() {
            warn!("disable all modules failed: {e}");
        }
        return Ok(());
    }

    if let Err(e) = handle_updated_modules() {
        warn!("handle updated modules failed: {e}");
    }

    if let Err(e) = prune_modules() {
        warn!("prune modules failed: {e}");
    }

    if let Err(e) = builtin_mount::ensure_active_compat_entry() {
        warn!("ensure builtin mount compat entry failed: {e}");
    }

    // Refresh /metadata/watchdog/ksu/modules.rc so the next boot's kernel hook sees the
    // current module set. Acts as a safety net when state was changed outside
    // of ksud's normal mutation commands.
    if let Err(e) = crate::module::regenerate_preinit_rc() {
        warn!("regenerate preinit rc failed: {e}");
    }

    if let Err(e) = restorecon::restorecon() {
        warn!("restorecon failed: {e}");
    }

    // load sepolicy.rule
    if crate::module::load_sepolicy_rule().is_err() {
        warn!("load sepolicy.rule failed");
    }

    if let Err(e) = crate::profile::apply_sepolies() {
        warn!("apply root profile sepolicy failed: {e}");
    }

    // load feature config
    if is_safe_mode() {
        warn!("safe mode, skip load feature config");
    } else if let Err(e) = crate::feature::init_features() {
        warn!("init features failed: {e}");
    }

    crate::pathmask::apply_if_configured();

    // execute metamodule post-fs-data script first (priority)
    if let Err(e) = metamodule::exec_stage_script("post-fs-data", true) {
        warn!("exec metamodule post-fs-data script failed: {e}");
    }

    // exec modules post-fs-data scripts
    // TODO: Add timeout
    if let Err(e) = crate::module::exec_stage_script("post-fs-data", true) {
        warn!("exec post-fs-data scripts failed: {e}");
    }

    // load system.prop
    if let Err(e) = crate::module::load_system_prop() {
        warn!("load system.prop failed: {e}");
    }

    // Apply property hiding before services and applications can inspect boot state.
    crate::epkesu_hide::apply_if_enabled();

    // execute metamodule mount script
    if let Err(e) = metamodule::exec_mount_script(module_dir) {
        warn!("execute metamodule mount failed: {e}");
    }

    run_stage("post-mount", true);

    std::env::set_current_dir("/").with_context(|| "failed to chdir to /")?;

    Ok(())
}

pub fn run_stage(stage: &str, block: bool) {
    utils::umask(0);

    if crate::rescue::should_skip_modules_this_boot() {
        warn!("rescue requested temporary module skip; skip {stage} scripts");
        return;
    }

    if utils::has_magisk() {
        warn!("Magisk detected, skip {stage}");
        return;
    }

    if crate::utils::is_safe_mode() {
        warn!("safe mode, skip {stage} scripts");
        return;
    }

    if let Err(e) = utils::ensure_magisk_module_compat() {
        warn!("ensure magisk module compat failed before {stage}: {e}");
    }

    // post-fs-data is the earliest load window, but targets on late-mounted
    // storage may not exist yet. Retry Pathmask during the later Android
    // service milestones; apply_if_configured is idempotent once it is loaded.
    if matches!(stage, "service" | "boot-completed") {
        crate::pathmask::apply_if_configured();
    }

    if let Err(e) = crate::module::exec_common_scripts(&format!("{stage}.d"), block) {
        warn!("Failed to exec common {stage} scripts: {e}");
    }

    // execute metamodule stage script first (priority)
    if let Err(e) = metamodule::exec_stage_script(stage, block) {
        warn!("Failed to exec metamodule {stage} script: {e}");
    }

    // execute regular modules stage scripts
    if let Err(e) = crate::module::exec_stage_script(stage, block) {
        warn!("Failed to exec {stage} scripts: {e}");
    }
}

pub fn on_services() {
    if ksucalls::is_uapi_version_mismatch() {
        warn!(
            "Kernel and userspace uapi version mismatch; continue services with compatible paths"
        );
    }

    info!("on_services triggered!");
    if let Err(e) = utils::daemonize(true) {
        warn!("failed to daemonize services runner: {e}");
    }
    run_stage("service", false);
}

pub fn on_boot_completed() {
    if ksucalls::is_uapi_version_mismatch() {
        warn!(
            "Kernel and userspace uapi version mismatch; continue boot-completed with compatible paths"
        );
    }

    ksucalls::report_boot_complete();
    info!("on_boot_completed triggered!");

    crate::rescue::mark_boot_completed();

    run_stage("boot-completed", false);
    // post-fs-data is the preferred early window. Retry here for devices whose
    // property service was not ready during that stage.
    crate::epkesu_hide::apply_if_enabled();

    // Changing ro.soc.model before SystemUI and vendor services initialize can
    // break launcher startup on some ROMs. Apply only after Android reports a
    // completed boot; safe mode and rescue startup disable the saved setting.
    crate::cpu_spoof::apply_if_enabled_after_boot();
}

const fn resetprop() -> ResetProp {
    ResetProp {
        skip_svc: true,
        persistent: false,
        persist_only: false,
        verbose: false,
        show_context: false,
        rebuild: false,
    }
}

fn reset_boot_completed() -> Result<()> {
    sys_prop::init().context("Failed to initialize system property API")?;
    let rp = resetprop();
    // Set prop value to 0 in advance to ensure resetprop -w works
    info!("reset boot complete prop to 0");
    rp.set("sys.boot_completed", "0")
        .context("Failed to set sys.boot_completed to 0")?;
    Ok(())
}

fn wait_for_boot_completed() -> Result<()> {
    sys_prop::init().context("Failed to initialize system property API")?;
    let rp = resetprop();
    info!("waiting for boot complete");
    rp.wait("sys.boot_completed", Some("0"), None)
        .context("wait for sys.boot_completed failed")?;
    Ok(())
}

#[cfg(unix)]
fn catch_bootlog(logname: &str, command: &[&str]) -> Result<()> {
    use std::os::unix::process::CommandExt;
    use std::process::Stdio;

    let logdir = Path::new(defs::LOG_DIR);
    utils::ensure_dir_exists(logdir)?;
    let bootlog = logdir.join(format!("{logname}.log"));
    let oldbootlog = logdir.join(format!("{logname}.old.log"));

    if bootlog.exists() {
        std::fs::rename(&bootlog, oldbootlog)?;
    }

    let bootlog = std::fs::File::create(bootlog)?;

    let mut args = vec!["-s", "9", "30s"];
    args.extend_from_slice(command);
    // timeout -s 9 30s logcat > boot.log
    let result = unsafe {
        std::process::Command::new("timeout")
            .process_group(0)
            .pre_exec(|| {
                utils::switch_cgroups();
                Ok(())
            })
            .args(args)
            .stdout(Stdio::from(bootlog))
            .spawn()
    };

    if let Err(e) = result {
        warn!("Failed to start logcat: {e:#}");
    }

    Ok(())
}

pub fn soft_reboot() -> Result<()> {
    // check it avoid user click "soft_reboot" in manager when version mismatch
    if ksucalls::is_uapi_version_mismatch() {
        warn!(
            "Kernel and userspace uapi version mismatch; continue soft_reboot with compatible paths"
        );
    }

    utils::daemonize_with(true, || -> Result<()> {
        switch_mnt_ns(1)?;
        chdir("/")?;
        Ok(())
    })?;

    info!("emulating soft_reboot!");
    if let Err(e) = reset_boot_completed() {
        warn!("reset boot completed failed: {e}");
    }
    run_stage("emulated-soft-reboot", true);
    info!("stop");
    let status = Command::new("stop").status().context("stop failed")?;
    if !status.success() {
        warn!("stop exited with status: {status}");
    }
    info!("post-fs-data");
    on_post_data_fs()?;
    info!("start");
    let status = Command::new("start").status().context("start failed")?;
    if !status.success() {
        warn!("start exited with status: {status}");
    }
    info!("services");
    on_services();
    if let Err(e) = wait_for_boot_completed() {
        warn!("wait for boot completed failed: {e}");
    }
    on_boot_completed();

    unsafe {
        _exit(0);
    }
}
