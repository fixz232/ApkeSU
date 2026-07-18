use anyhow::{Context, Result, bail};
use log::{info, warn};
use std::ffi::CString;
use std::process::Command;

use crate::module::{handle_updated_modules, prune_modules};
use crate::{assets, builtin_mount, defs, init_event, ksucalls, metamodule, restorecon, utils};

fn dump_process_info(label: &str) {
    use rustix::process::{getgid, getgroups, getpid, getuid};

    let pid = getpid().as_raw_nonzero();
    let uid = getuid().as_raw();
    let gid = getgid().as_raw();
    let groups: Vec<String> = getgroups()
        .unwrap_or_default()
        .iter()
        .map(|g| g.as_raw().to_string())
        .collect();
    let selinux = std::fs::read_to_string("/proc/self/attr/current")
        .unwrap_or_else(|_| "unknown".to_string());
    let seccomp = std::fs::read_to_string("/proc/self/status")
        .ok()
        .and_then(|s| {
            s.lines()
                .find(|l| l.starts_with("Seccomp:"))
                .map(|l| l.trim().to_string())
        })
        .unwrap_or_else(|| "unknown".to_string());

    info!(
        "[{label}] pid={pid}, uid={uid}, gid={gid}, groups=[{}], selinux={}, {seccomp}",
        groups.join(","),
        selinux.trim(),
    );
}

const PER_USER_RANGE: u32 = 100_000;
const FIRST_APPLICATION_APPID: u32 = 10_000;
const FIRST_ISOLATED_APPID: u32 = 90_000;

const fn normalize_appid(uid: u32) -> u32 {
    uid % PER_USER_RANGE
}

fn is_normal_appid(appid: u32) -> bool {
    (FIRST_APPLICATION_APPID..FIRST_ISOLATED_APPID).contains(&appid)
}

fn get_pkg_appid_from_stat(pkg: &str) -> Result<u32> {
    let paths = [
        format!("/data/data/{pkg}"),
        format!("/data/user/0/{pkg}"),
        format!("/data/user_de/0/{pkg}"),
    ];
    let mut errors = Vec::new();

    for path in paths {
        match rustix::fs::stat(path.as_str()) {
            Ok(stat) => return Ok(normalize_appid(stat.st_uid)),
            Err(e) => errors.push(format!("{path}: {e}")),
        }
    }

    bail!("stat manager data dirs failed: {}", errors.join("; "))
}

fn get_pkg_appid_from_packages_list(pkg: &str) -> Result<u32> {
    const PACKAGES_LIST: &str = "/data/system/packages.list";
    let contents =
        std::fs::read_to_string(PACKAGES_LIST).with_context(|| format!("read {PACKAGES_LIST}"))?;

    for line in contents.lines() {
        let mut fields = line.split_whitespace();
        let Some(package) = fields.next() else {
            continue;
        };
        let Some(uid) = fields.next() else {
            continue;
        };
        if package == pkg {
            let uid = uid
                .parse::<u32>()
                .with_context(|| format!("parse uid for {pkg} from {PACKAGES_LIST}"))?;
            return Ok(normalize_appid(uid));
        }
    }

    bail!("{pkg} not found in {PACKAGES_LIST}")
}

fn get_pkg_appid(pkg: &str) -> Result<u32> {
    get_pkg_appid_from_stat(pkg).or_else(|stat_err| {
        get_pkg_appid_from_packages_list(pkg)
            .with_context(|| format!("fallback after data dir lookup failed: {stat_err:#}"))
    })
}

fn ensure_this_manager_package(package_name: &str) -> Result<()> {
    anyhow::ensure!(
        package_name == defs::DEFAULT_MANAGER_PACKAGE,
        "refusing to register manager appid for package {package_name}; this build only trusts {}",
        defs::DEFAULT_MANAGER_PACKAGE
    );
    Ok(())
}

fn resolve_manager_appid(package_name: &str, manager_uid: Option<u32>) -> Option<u32> {
    let package_appid = match get_pkg_appid(package_name) {
        Ok(appid) => {
            info!("Using manager appid {appid} for package {package_name}");
            Some(appid)
        }
        Err(e) => {
            warn!("get manager appid failed for {package_name}: {e:#}");
            None
        }
    };

    let supplied_appid = manager_uid.and_then(|uid| {
        let appid = normalize_appid(uid);
        if is_normal_appid(appid) {
            Some(appid)
        } else {
            warn!("Ignoring suspicious manager uid {uid} (appid {appid})");
            None
        }
    });

    if let Some(uid) = manager_uid {
        let appid = normalize_appid(uid);
        if package_appid == Some(appid) {
            info!("Verified manager appid {appid} from uid {uid}");
        } else if let Some(expected_appid) = package_appid {
            warn!(
                "Ignoring mismatched manager uid {uid} (appid {appid}); package {package_name} owns appid {expected_appid}"
            );
        } else if supplied_appid.is_some() {
            info!("Using manager appid {appid} from zygote preload uid fallback");
        }
    }

    package_appid.or(supplied_appid).filter(|appid| {
        if is_normal_appid(*appid) {
            true
        } else {
            warn!("Ignoring suspicious package appid {appid} for {package_name}");
            false
        }
    })
}

fn register_manager_appid(appid: Option<u32>) {
    if let Some(appid) = appid {
        match ksucalls::set_manager_appid(appid) {
            Ok(()) => info!("Registered manager appid {appid}"),
            Err(e) => warn!("set manager appid failed: {e}"),
        }
    }
}

pub fn register_default_manager_appid() {
    register_manager_appid(resolve_manager_appid(defs::DEFAULT_MANAGER_PACKAGE, None));
}

pub fn register_manager(package_name: &str, manager_uid: Option<u32>) -> Result<()> {
    ensure_this_manager_package(package_name)?;
    let appid = resolve_manager_appid(package_name, manager_uid)
        .context("unable to resolve the ApkeSU manager appid")?;
    ksucalls::set_manager_appid(appid)
        .with_context(|| format!("failed to register manager appid {appid}"))?;
    println!("{appid}");
    Ok(())
}

pub fn run(
    package_name: &str,
    manager_uid: Option<u32>,
    kmi: Option<String>,
    allow_shell: bool,
) -> Result<()> {
    ensure_this_manager_package(package_name)?;

    utils::daemonize(false)?;
    info!("late-load command triggered!");
    dump_process_info("late-load start");
    let manager_appid = resolve_manager_appid(package_name, manager_uid);

    // 1. Check if KernelSU is already loaded
    if ksuinit::has_kernelsu() {
        info!("KernelSU already loaded, skip loading ko");
    } else {
        // 2. Detect current KMI version
        let kmi = kmi.map_or_else(
            || crate::boot_patch::get_current_kmi().context("Failed to detect current KMI version"),
            Ok,
        )?;
        info!("Detected KMI: {kmi}");

        // 3. Get kernelsu.ko from embedded assets
        let ko_name = format!("{kmi}_kernelsu.ko");
        let ko_data = assets::get_asset_data(&ko_name)
            .with_context(|| format!("Failed to get {ko_name} from assets"))?;

        // 4. Load kernelsu.ko from memory with manual relocation
        info!("Loading kernelsu.ko for KMI {kmi}...");
        let mut module_params = Vec::new();
        if allow_shell {
            module_params.push("allow_shell=1".to_string());
        }
        if let Some(appid) = manager_appid {
            module_params.push(format!("manager_appid={appid}"));
        }
        let params = CString::new(module_params.join(" ")).context("build module params")?;
        ksuinit::load_module(&ko_data, &params).context("Failed to load kernelsu.ko")?;
        info!("kernelsu.ko loaded successfully!");
        dump_process_info("after load_module");
    }

    register_manager_appid(manager_appid);

    // We need to reset stdin/stdout/stderr; otherwise, sending file descriptors via cmd transactions
    // will be blocked by SELinux because its fsec->sid is still u:r:su:s0 instead of u:r:ksu:s0.
    utils::reset_std()?;

    utils::umask(0);

    if let Err(e) = crate::module_config::clear_all_temp_configs() {
        warn!("clear temp configs failed: {e}");
    }

    utils::install(None).context("Failed to install ksud")?;

    // 5. Handle module updates
    if let Err(e) = handle_updated_modules() {
        warn!("handle updated modules failed: {e}");
    }

    if let Err(e) = prune_modules() {
        warn!("prune modules failed: {e}");
    }

    if let Err(e) = builtin_mount::ensure_active_compat_entry() {
        warn!("ensure builtin mount compat entry failed: {e}");
    }

    if let Err(e) = restorecon::restorecon() {
        warn!("restorecon failed: {e}");
    }

    // 6. Load SELinux rules
    if crate::module::load_sepolicy_rule().is_err() {
        warn!("load sepolicy.rule failed");
    }

    if let Err(e) = crate::profile::apply_sepolies() {
        warn!("apply root profile sepolicy failed: {e}");
    }

    // 7. Initialize features
    if let Err(e) = crate::feature::init_features() {
        warn!("init features failed: {e}");
    }

    // 8. Execute late-load stage scripts (blocking)
    init_event::run_stage("late-load", true);

    // 9. Load system.prop
    if let Err(e) = crate::module::load_system_prop() {
        warn!("load system.prop failed: {e}");
    }

    // 10. Execute metamodule mount script (OverlayFS)
    if let Err(e) = metamodule::exec_mount_script(defs::MODULE_DIR) {
        warn!("execute metamodule mount failed: {e}");
    }

    // 11. Execute post-mount stage scripts (blocking)
    init_event::run_stage("post-mount", true);

    // 12. Execute service stage scripts (non-blocking)
    init_event::run_stage("service", false);

    // 13. Execute boot-completed stage scripts (non-blocking)
    init_event::run_stage("boot-completed", false);

    // Late-load runs after Android is already up, so property spoofing cannot
    // interfere with SystemUI or launcher initialization.
    crate::cpu_spoof::apply_if_enabled_after_boot();

    // 14. Restart Manager so it gets a fresh ksu fd from the newly loaded kernel module
    info!("Restarting KernelSU Manager {package_name}...");
    let _ = Command::new("am")
        .args(["force-stop", package_name])
        .status();
    let _ = Command::new("am")
        .args([
            "start",
            "-n",
            &format!("{package_name}/me.weishu.kernelsu.ui.MainActivity"),
        ])
        .status();

    Ok(())
}
