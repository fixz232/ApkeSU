use anyhow::{Context, Result, anyhow, bail};
use chrono::Local;
use const_format::concatcp;
use serde_json::{Value, json};
use std::{
    collections::{BTreeMap, BTreeSet},
    ffi::CString,
    fs::{self, File, OpenOptions},
    io::{self, Write},
    os::fd::AsRawFd,
    path::{Component, Path},
    process::Command,
    thread,
    time::Duration,
};

use crate::{assets, boot_patch, defs, ksucalls, utils};

const PATHMASK_DIR: &str = concatcp!(defs::WORKING_DIR, "pathmask/");
const CONFIG_PATH: &str = concatcp!(PATHMASK_DIR, "config.json");
const CANDIDATE_CONFIG_PATH: &str = concatcp!(PATHMASK_DIR, "candidate.json");
const LAST_GOOD_CONFIG_PATH: &str = concatcp!(PATHMASK_DIR, "last_good.json");
const RUNTIME_STATE_PATH: &str = concatcp!(PATHMASK_DIR, "runtime_state.json");
const OPERATION_LOCK_PATH: &str = concatcp!(PATHMASK_DIR, "operation.lock");
const UNLOAD_BOOT_ID_PATH: &str = concatcp!(PATHMASK_DIR, "unload_boot_id");
const AUTO_LOAD_SCHEDULE_LOCK_PATH: &str = concatcp!(PATHMASK_DIR, "auto_load_schedule.lock");
const LOG_PATH: &str = concatcp!(PATHMASK_DIR, "pathmask.log");
const BOOT_ID_PATH: &str = "/proc/sys/kernel/random/boot_id";
const UPTIME_PATH: &str = "/proc/uptime";
const PATHMASK_MODULE_NAME: &str = "pathmask";
const PACKAGES_LIST_PATH: &str = "/data/system/packages.list";
const USER_DATA_ROOTS: &[&str] = &["/data/user", "/data/user_de"];
const MAX_TARGET_PATHS: usize = 64;
const MAX_APP_PACKAGES: usize = 256;
const MAX_TARGET_PATHS_LEN: usize = 1900;
const MAX_DENY_UIDS_LEN: usize = 1900;
const MAX_MODULE_PARAMS_LEN: usize = 3900;
const MAX_AUTO_LOAD_DELAY_SECONDS: u64 = 300;
const TARGET_WAIT_SECONDS: u64 = 5;
const TARGET_WAIT_STEP_MS: u64 = 200;
const RESOLVED_COUNT_READ_ATTEMPTS: usize = 5;
const RESOLVED_COUNT_READ_STEP_MS: u64 = 100;
const MODULE_UNLOAD_WAIT_MS: u64 = 2000;
const MODULE_UNLOAD_STEP_MS: u64 = 100;
const LOG_MAX_BYTES: u64 = 512 * 1024;
const LOG_ROTATION_COUNT: usize = 3;
const ERROR_PREFIX: &str = "APKESU_ERROR";
const MANAGED_ROOT_PATHS: &[&str] = &[
    "/data/adb/modules",
    "/data/adb/modules_update",
    "/data/adb/ksu",
    "/data/adb/ap",
];

#[derive(Clone, Debug, PartialEq, Eq)]
#[allow(clippy::struct_excessive_bools)]
struct PathmaskConfig {
    target_paths: Vec<String>,
    app_packages: Vec<String>,
    use_app_scope: bool,
    hide_dirents: bool,
    hide_isolated: bool,
    auto_load_enabled: bool,
    auto_load_delay_seconds: u64,
}

#[derive(Default)]
struct DenyUidResolution {
    uids: BTreeSet<u32>,
    unresolved_packages: Vec<String>,
}

impl Default for PathmaskConfig {
    fn default() -> Self {
        Self {
            target_paths: Vec::new(),
            app_packages: Vec::new(),
            use_app_scope: true,
            hide_dirents: true,
            hide_isolated: true,
            auto_load_enabled: true,
            auto_load_delay_seconds: 0,
        }
    }
}

#[derive(Clone, Debug, Default, PartialEq, Eq)]
struct PathmaskRuntimeObservation {
    config_key: String,
    configured_target_paths: Vec<String>,
    available_target_paths: Vec<String>,
    resolved_target_paths: Vec<String>,
    resolved_count: usize,
}

#[derive(Clone, Debug, Default, PartialEq, Eq)]
struct PathmaskRuntimeState {
    phase: String,
    error_code: String,
    error_message: String,
    requires_reboot: bool,
    updated_at: String,
    observation: PathmaskRuntimeObservation,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum ReconcileAction {
    Disabled,
    WaitForTargets,
    Load,
    Reload,
    Noop,
}

struct OperationLock(File);

impl Drop for OperationLock {
    fn drop(&mut self) {
        unsafe {
            libc::flock(self.0.as_raw_fd(), libc::LOCK_UN);
        }
    }
}

fn coded_error(code: &str, message: impl AsRef<str>) -> anyhow::Error {
    anyhow!("{ERROR_PREFIX}:{code}:{}", message.as_ref())
}

pub fn print_status() {
    let config = match read_config() {
        Ok(config) => config,
        Err(error) => {
            append_log(format!("status failed: {error:#}"));
            println!(
                "{}",
                json!({
                    "errorCode": "pathmask.config_read_failed",
                    "error": format!("failed to read pathmask config: {error:#}"),
                })
            );
            return;
        }
    };
    let current_kmi = boot_patch::get_current_kmi().unwrap_or_default();
    let loaded = is_module_loaded();
    let resolved_count = if loaded { read_resolved_count() } else { 0 };
    let active_target_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let available_paths = existing_target_paths(&config);
    let missing_target_paths = missing_target_paths(&config);
    let runtime_state = read_runtime_state().unwrap_or_default();
    let active_config_matches =
        active_target_paths_match(&config, &available_paths, &active_target_paths);
    let reload_required = loaded
        && (!active_config_matches
            || runtime_requires_reload(
                &config,
                &available_paths,
                &active_target_paths,
                resolved_count,
                &runtime_state,
            ));
    let requires_reload = matches!(
        reconcile_action(
            config.auto_load_enabled,
            loaded,
            available_paths.len(),
            resolved_count,
            reload_required,
        ),
        ReconcileAction::Reload
    );
    let unresolved_target_paths = unresolved_target_paths(
        &config,
        &available_paths,
        &active_target_paths,
        loaded,
        resolved_count,
    );
    let unresolved_target_count = available_paths.len().saturating_sub(resolved_count);
    let auto_load_remaining_seconds = if config.auto_load_enabled {
        boot_auto_load_remaining_seconds(config.auto_load_delay_seconds)
    } else {
        0
    };
    let manually_unloaded = !loaded && is_unload_suppressed_this_boot();
    let phase = if manually_unloaded {
        "unloaded_this_boot"
    } else if !runtime_state.requires_reboot
        && runtime_state.error_code.is_empty()
        && !loaded
        && !config.target_paths.is_empty()
        && auto_load_remaining_seconds > 0
    {
        "waiting_delay"
    } else {
        derive_phase(
            &config,
            loaded,
            available_paths.len(),
            resolved_count,
            requires_reload,
            runtime_state.requires_reboot,
        )
    };
    let last_log = tail_file(LOG_PATH, 40).unwrap_or_default();
    let deny_uid_resolution = if config.use_app_scope {
        resolve_deny_uids(&config.app_packages)
    } else {
        DenyUidResolution::default()
    };

    println!(
        "{}",
        json!({
            "targetPaths": config.target_paths,
            "appPackages": config.app_packages,
            "useAppScope": config.use_app_scope,
            "hideDirents": config.hide_dirents,
            "hideIsolated": config.hide_isolated,
            "autoLoadEnabled": config.auto_load_enabled,
            "autoLoadDelaySeconds": config.auto_load_delay_seconds,
            "autoLoadRemainingSeconds": auto_load_remaining_seconds,
            "loaded": loaded,
            "currentKmi": current_kmi,
            "phase": phase,
            "savedCount": config.target_paths.len(),
            "availableCount": available_paths.len(),
            "activeCount": resolved_count,
            "resolvedCount": resolved_count,
            "activeTargetPaths": active_target_paths,
            "availableTargetPaths": available_paths,
            "missingTargetPaths": missing_target_paths,
            "unresolvedTargetPaths": unresolved_target_paths,
            "unresolvedTargetCount": unresolved_target_count,
            "requiresReload": requires_reload,
            "requiresReboot": runtime_state.requires_reboot,
            "hasPendingCandidate": Path::new(CANDIDATE_CONFIG_PATH).exists(),
            "lastErrorCode": runtime_state.error_code,
            "lastErrorMessage": runtime_state.error_message,
            "lastPhase": runtime_state.phase,
            "stateUpdatedAt": runtime_state.updated_at,
            "lastLog": last_log,
            "resolvedAppUids": deny_uid_resolution.uids,
            "unresolvedAppPackages": deny_uid_resolution.unresolved_packages,
        })
    );
}

pub fn test_visibility(uid: u32, path: &str) -> Result<()> {
    let path = sanitize_target_path(path).context("invalid visibility probe path")?;
    let root_exists = fs::symlink_metadata(&path).is_ok();
    let module_loaded = is_module_loaded();
    let resolved_count = read_sysfs_param("resolved_count").unwrap_or_default();

    let (status, visible, error) = if root_exists {
        match probe_path_as_uid(uid, &path) {
            Ok(()) => ("visible", true, String::new()),
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                ("not_visible", false, String::new())
            }
            Err(error) => ("probe_failed", false, error.to_string()),
        }
    } else {
        ("missing", false, String::new())
    };

    append_log(format!(
        "visibility probe: uid={uid}, path={path}, status={status}, module_loaded={module_loaded}"
    ));
    println!(
        "{}",
        json!({
            "uid": uid,
            "path": path,
            "status": status,
            "visible": visible,
            "rootExists": root_exists,
            "moduleLoaded": module_loaded,
            "resolvedCount": resolved_count,
            "error": error,
        })
    );
    Ok(())
}

fn probe_path_as_uid(uid: u32, path: &str) -> io::Result<()> {
    // This command exits immediately after probing, so dropping all credentials
    // in-process cannot leak into another ksud operation.
    unsafe {
        if libc::setgroups(0, std::ptr::null()) != 0 {
            return Err(io::Error::last_os_error());
        }
        if libc::setresgid(uid, uid, uid) != 0 {
            return Err(io::Error::last_os_error());
        }
        if libc::setresuid(uid, uid, uid) != 0 {
            return Err(io::Error::last_os_error());
        }
    }
    fs::symlink_metadata(path).map(|_| ())
}

pub fn import_config(file: &Path) -> Result<()> {
    let result = import_config_inner(file);
    if let Err(err) = &result {
        append_log(format!(
            "import config failed from {}: {err:#}",
            file.display()
        ));
    }
    result
}

pub fn import_config_text(content: &str) -> Result<()> {
    let result = import_config_text_inner(content, "inline JSON");
    if let Err(err) = &result {
        append_log(format!("import inline config failed: {err:#}"));
    }
    result
}

fn import_config_inner(file: &Path) -> Result<()> {
    let content =
        fs::read_to_string(file).with_context(|| format!("failed to read {}", file.display()))?;
    import_config_text_inner(&content, &file.display().to_string())
}

fn import_config_text_inner(content: &str, source: &str) -> Result<()> {
    let config = parse_config(content)?;
    validate_config(&config)?;
    write_config(CANDIDATE_CONFIG_PATH, &config)?;
    append_log(format!("staged candidate config from {source}"));
    Ok(())
}

pub fn apply() -> Result<()> {
    let result = acquire_operation_lock().and_then(|_lock| apply_inner(true));
    if let Err(err) = &result {
        append_log(format!("apply failed: {err:#}"));
        record_runtime_error(err, error_requires_reboot(err));
    }
    result
}

pub fn apply_config_text(content: &str) -> Result<()> {
    let result = acquire_operation_lock().and_then(|_lock| {
        let config = parse_config(content)?;
        validate_config(&config)?;
        write_config(CANDIDATE_CONFIG_PATH, &config)?;
        apply_inner(true)
    });
    if let Err(err) = &result {
        append_log(format!("atomic apply failed: {err:#}"));
        record_runtime_error(err, error_requires_reboot(err));
    }
    result
}

fn apply_inner(clear_boot_suppression: bool) -> Result<()> {
    ensure_pathmask_runtime_supported()?;

    let has_candidate = Path::new(CANDIDATE_CONFIG_PATH).exists();
    let config = if has_candidate {
        read_config_from_path(CANDIDATE_CONFIG_PATH)?
    } else {
        read_config()?
    };
    validate_config(&config)?;

    if clear_boot_suppression {
        clear_boot_unload_suppression();
    }

    if has_candidate && config.auto_load_enabled && is_module_loaded() {
        let committed = read_config()?;
        if differs_only_by_auto_load_delay(&committed, &config)
            && active_runtime_matches(&committed)
        {
            promote_candidate_config()?;
            write_config(LAST_GOOD_CONFIG_PATH, &config)?;
            let available_count = count_existing_target_paths(&config);
            let phase = loaded_phase(&config, available_count, read_resolved_count());
            write_runtime_state(phase, "", "", false);
            append_log(format!(
                "updated boot auto-load delay to {}s without reloading pathmask",
                config.auto_load_delay_seconds
            ));
            return Ok(());
        }
    }

    if !config.auto_load_enabled {
        if has_candidate {
            promote_candidate_config()?;
        }
        if is_module_loaded()
            && let Err(error) = unload_loaded_pathmask()
        {
            let message = format!(
                "auto-load is disabled, but the active module is busy; reboot is required: {error:#}"
            );
            write_runtime_state(
                "disabled_runtime_active",
                "pathmask.reboot_required",
                &message,
                true,
            );
            append_log(&message);
            return Ok(());
        }
        write_runtime_state("disabled", "", "", false);
        append_log("pathmask auto-load disabled");
        return Ok(());
    }

    let module_params = build_module_params(&config)?;
    let kmi = boot_patch::get_current_kmi().context("failed to detect current KMI")?;
    let ko_name = format!("{kmi}_pathmask.ko");
    let ko_data =
        assets::get_asset_data(&ko_name).with_context(|| format!("failed to load {ko_name}"))?;

    append_log(format!("applying pathmask for KMI {kmi}: {module_params}"));

    let previous_loaded = is_module_loaded();
    if previous_loaded {
        append_log("pathmask already loaded; reload is required for new target paths");
        unload_loaded_pathmask().map_err(|error| {
            coded_error(
                "pathmask.module_busy",
                format!(
                    "active pathmask cannot be unloaded safely; reboot before applying: {error:#}"
                ),
            )
        })?;
        append_log("unloaded old pathmask module");
    }

    let existing_count = match wait_for_any_target_path(&config) {
        Ok(count) => count,
        Err(error) => {
            return Err(error_after_rollback(error, previous_loaded, &kmi, &ko_data));
        }
    };
    append_log(format!(
        "target path precheck passed: {existing_count}/{} currently exists",
        config.target_paths.len()
    ));

    if let Err(err) = load_pathmask(&ko_data, &module_params) {
        append_log(format!("load failed: {err:#}"));
        let error = coded_error(
            "pathmask.load_failed",
            format!("failed to load pathmask with candidate config: {err:#}"),
        );
        return Err(error_after_rollback(error, previous_loaded, &kmi, &ko_data));
    }

    let (resolved_count, active_target_paths) = match read_loaded_target_state() {
        Ok(state) => state,
        Err(error) => {
            append_log(format!(
                "pathmask loaded but runtime state could not be read: {error:#}"
            ));
            return Err(error_after_rollback(error, previous_loaded, &kmi, &ko_data));
        }
    };
    let candidate = has_candidate.then(|| Path::new(CANDIDATE_CONFIG_PATH));
    if let Err(error) = commit_candidate_after_verification(
        candidate,
        Path::new(CONFIG_PATH),
        verify_loaded_target_count(resolved_count, existing_count),
    ) {
        append_log(format!(
            "candidate verification or commit failed: {error:#}; rolling back"
        ));
        return Err(error_after_rollback(error, previous_loaded, &kmi, &ko_data));
    }

    if let Err(error) = write_config(LAST_GOOD_CONFIG_PATH, &config) {
        append_log(format!(
            "active config committed, but last-good snapshot update failed: {error:#}"
        ));
    }
    let available_paths = existing_target_paths(&config);
    let phase = loaded_phase(&config, available_paths.len(), resolved_count);
    let observation = runtime_observation(
        &config,
        &available_paths,
        &active_target_paths,
        resolved_count,
    );
    write_runtime_state_with_observation(phase, "", "", false, Some(&observation));
    if resolved_count < existing_count {
        append_log(format!(
            "pathmask loaded with partial resolution: resolved_count={resolved_count}, available_before_load={existing_count}"
        ));
    }
    append_log(format!(
        "pathmask loaded successfully, resolved_count={resolved_count}, available={}, configured={}",
        available_paths.len(),
        config.target_paths.len()
    ));
    Ok(())
}

pub fn apply_if_configured() {
    if !Path::new(CONFIG_PATH).exists() {
        return;
    }

    let config = match read_config() {
        Ok(config) => config,
        Err(error) => {
            append_log(format!(
                "skip boot auto-load: failed to read config: {error:#}"
            ));
            return;
        }
    };
    if !config.auto_load_enabled || config.auto_load_delay_seconds == 0 {
        apply_configured_now();
        return;
    }

    if let Err(error) = ensure_pathmask_runtime_supported() {
        append_log(format!("skip boot auto-load: {error:#}"));
        return;
    }

    if boot_auto_load_remaining_seconds(config.auto_load_delay_seconds) == 0 {
        apply_configured_now();
    } else {
        schedule_delayed_auto_load();
    }
}

fn schedule_delayed_auto_load() {
    let schedule_lock = match acquire_auto_load_schedule_lock() {
        Ok(lock) => lock,
        Err(error) if error_code(&error) == "pathmask.auto_load_already_scheduled" => {
            append_log("delayed auto-load is already scheduled");
            write_runtime_state("waiting_delay", "", "", false);
            return;
        }
        Err(error) => {
            append_log(format!(
                "failed to acquire delayed auto-load lock: {error:#}"
            ));
            record_runtime_error(&error, false);
            return;
        }
    };

    let config = match read_config() {
        Ok(config) => config,
        Err(error) => {
            append_log(format!(
                "skip boot auto-load: failed to read config: {error:#}"
            ));
            return;
        }
    };
    if !config.auto_load_enabled || config.auto_load_delay_seconds == 0 {
        drop(schedule_lock);
        apply_configured_now();
        return;
    }
    let remaining_seconds = boot_auto_load_remaining_seconds(config.auto_load_delay_seconds);
    if remaining_seconds == 0 {
        drop(schedule_lock);
        apply_configured_now();
        return;
    }
    if is_unload_suppressed_this_boot() {
        write_runtime_state("unloaded_this_boot", "", "", false);
        append_log("skip boot auto-load: manually unloaded for this boot");
        return;
    }
    write_runtime_state("waiting_delay", "", "", false);

    // Keep this descriptor open across the daemon double-fork so only one timer
    // can exist; the inherited open file description releases it on exit.
    match utils::create_daemon(true) {
        Ok(true) => {
            let _schedule_lock = schedule_lock;
            run_delayed_auto_load();
            unsafe {
                libc::_exit(0);
            }
        }
        Ok(false) => append_log(format!(
            "scheduled pathmask auto-load in {remaining_seconds}s"
        )),
        Err(error) => {
            let error = coded_error(
                "pathmask.auto_load_schedule_failed",
                format!("failed to schedule delayed auto-load: {error:#}"),
            );
            append_log(format!("{error:#}"));
            record_runtime_error(&error, false);
        }
    }
}

fn run_delayed_auto_load() {
    loop {
        let Ok(config) = read_config() else {
            append_log("delayed auto-load stopped: failed to re-read config");
            return;
        };
        if !config.auto_load_enabled || is_unload_suppressed_this_boot() {
            append_log("delayed auto-load cancelled by current configuration");
            return;
        }
        let remaining_seconds = boot_auto_load_remaining_seconds(config.auto_load_delay_seconds);
        if remaining_seconds == 0 {
            break;
        }
        thread::sleep(Duration::from_secs(remaining_seconds.min(1)));
    }

    append_log("delayed auto-load timer elapsed");
    apply_configured_now();
}

fn apply_configured_now() {
    if !Path::new(CONFIG_PATH).exists() {
        return;
    }

    for attempt in 0..3 {
        match acquire_operation_lock() {
            Ok(lock) => {
                apply_configured_now_locked(lock);
                return;
            }
            Err(error) if error_code(&error) == "pathmask.operation_busy" && attempt < 2 => {
                thread::sleep(Duration::from_millis(200));
            }
            Err(error) => {
                append_log(format!("skip boot auto-load: {error:#}"));
                return;
            }
        }
    }
}

fn apply_configured_now_locked(_lock: OperationLock) {
    let config = match read_config() {
        Ok(config) => config,
        Err(error) => {
            append_log(format!(
                "skip boot auto-load: failed to read config: {error:#}"
            ));
            return;
        }
    };
    if !config.auto_load_enabled {
        write_runtime_state("disabled", "", "", is_module_loaded());
        return;
    }
    if is_unload_suppressed_this_boot() {
        write_runtime_state("unloaded_this_boot", "", "", false);
        append_log("skip boot auto-load: manually unloaded for this boot");
        return;
    }

    if let Err(err) = ensure_pathmask_runtime_supported() {
        append_log(format!("skip boot auto-load: {err:#}"));
        return;
    }

    let loaded = is_module_loaded();
    let available_paths = existing_target_paths(&config);
    let available_count = available_paths.len();
    let resolved_count = read_resolved_count();
    let active_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let runtime_state = read_runtime_state().unwrap_or_default();
    let active_matches = active_target_paths_match(&config, &available_paths, &active_paths);
    let reload_required = loaded
        && (!active_matches
            || runtime_requires_reload(
                &config,
                &available_paths,
                &active_paths,
                resolved_count,
                &runtime_state,
            ));
    let action = reconcile_action(
        config.auto_load_enabled,
        loaded,
        available_count,
        resolved_count,
        reload_required,
    );
    match action {
        ReconcileAction::Disabled | ReconcileAction::Noop => {
            let phase = loaded_phase(&config, available_count, resolved_count);
            write_runtime_state(phase, "", "", false);
            return;
        }
        ReconcileAction::WaitForTargets => {
            write_runtime_state("waiting_targets", "", "", false);
            append_log("defer auto-load: configured target paths are not mounted yet");
            return;
        }
        ReconcileAction::Load | ReconcileAction::Reload => {}
    }

    if let Err(err) = apply_inner(false) {
        record_runtime_error(&err, error_requires_reboot(&err));
        append_log(format!("boot auto-load failed: {err:#}"));
        log::warn!("pathmask: boot auto-load failed: {err:#}");
    }
}

fn ensure_pathmask_runtime_supported() -> Result<()> {
    if ksucalls::is_late_load() {
        return Err(coded_error(
            "pathmask.unsupported_late_load",
            "pathmask LKM is disabled in jailbreak mode",
        ));
    }
    if !ksucalls::is_lkm_mode() {
        return Err(coded_error(
            "pathmask.requires_lkm",
            "pathmask LKM is only available in LKM mode; use SUSFS path config in GKI mode",
        ));
    }
    Ok(())
}

pub fn unload() -> Result<()> {
    let result = acquire_operation_lock().and_then(|_lock| unload_inner());
    if let Err(err) = &result {
        append_log(format!("unload failed: {err:#}"));
        record_runtime_error(err, error_requires_reboot(err));
    }
    result
}

fn unload_inner() -> Result<()> {
    mark_unload_suppressed_this_boot()?;
    if !is_module_loaded() {
        write_runtime_state("unloaded_this_boot", "", "", false);
        append_log("pathmask unload skipped: module is not loaded");
        return Ok(());
    }

    unload_loaded_pathmask().map_err(|error| {
        coded_error(
            "pathmask.module_busy",
            format!("pathmask cannot be unloaded safely this boot: {error:#}"),
        )
    })?;
    write_runtime_state("unloaded_this_boot", "", "", false);
    append_log("pathmask unloaded for this boot; auto-load configuration is preserved");
    Ok(())
}

pub fn set_auto_load(enabled: bool, delay_seconds: Option<u64>) -> Result<()> {
    let result = acquire_operation_lock().and_then(|_lock| {
        let mut config = read_config()?;
        config.auto_load_enabled = enabled;
        if let Some(delay_seconds) = delay_seconds {
            config.auto_load_delay_seconds = delay_seconds;
        }
        validate_config(&config)?;
        write_config(CANDIDATE_CONFIG_PATH, &config)?;
        apply_inner(enabled)
    });
    if let Err(error) = &result {
        append_log(format!("set auto-load failed: {error:#}"));
        record_runtime_error(error, error_requires_reboot(error));
    }
    result
}

pub fn delete_config() -> Result<()> {
    let result = acquire_operation_lock().and_then(|_lock| {
        mark_unload_suppressed_this_boot()?;
        for path in [CONFIG_PATH, CANDIDATE_CONFIG_PATH, LAST_GOOD_CONFIG_PATH] {
            remove_file_if_exists(path)?;
        }
        if is_module_loaded()
            && let Err(error) = unload_loaded_pathmask()
        {
            let message = format!(
                "configuration was deleted, but the active module is busy; reboot is required: {error:#}"
            );
            write_runtime_state(
                "deleted_runtime_active",
                "pathmask.reboot_required",
                &message,
                true,
            );
            append_log(&message);
            return Ok(());
        }
        write_runtime_state("unconfigured", "", "", false);
        append_log("pathmask configuration deleted");
        Ok(())
    });
    if let Err(error) = &result {
        append_log(format!("delete config failed: {error:#}"));
        record_runtime_error(error, false);
    }
    result
}

pub fn print_logs() {
    let mut output = String::new();
    if let Ok(manager_log) = tail_file(LOG_PATH, 200)
        && !manager_log.is_empty()
    {
        output.push_str("=== manager pathmask log ===\n");
        output.push_str(&manager_log);
        if !output.ends_with('\n') {
            output.push('\n');
        }
    }

    let kernel_log = read_kernel_pathmask_log();
    if !kernel_log.is_empty() {
        output.push_str("=== kernel pathmask log ===\n");
        output.push_str(&kernel_log);
        if !output.ends_with('\n') {
            output.push('\n');
        }
    }

    if output.is_empty() {
        output.push_str("No pathmask logs yet.\n");
    }

    print!("{output}");
}

pub fn print_diagnostics() {
    println!("=== ApkeSU Pathmask diagnostic ===");
    println!("generatedAt={}", Local::now().to_rfc3339());
    println!("=== status ===");
    print_status();
    println!("=== logs ===");
    print_logs();
}

pub fn clear_logs() -> Result<()> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    fs::write(LOG_PATH, "").with_context(|| format!("failed to clear {LOG_PATH}"))?;
    for index in 1..=LOG_ROTATION_COUNT {
        remove_file_if_exists(&format!("{LOG_PATH}.{index}"))?;
    }
    append_log("cleared manager log; kernel dmesg history is not cleared");
    Ok(())
}

fn restore_committed_config(kmi: &str, ko_data: &[u8]) -> Result<()> {
    let config = read_config().or_else(|_| read_config_from_path(LAST_GOOD_CONFIG_PATH))?;
    if !config.auto_load_enabled || config.target_paths.is_empty() {
        return Ok(());
    }
    let module_params = build_module_params(&config)?;
    let available_count = wait_for_any_target_path(&config).map_err(|error| {
        coded_error(
            "pathmask.rollback_targets_not_mounted",
            format!("committed config cannot be restored: {error:#}"),
        )
    })?;
    append_log(format!("restoring committed pathmask config for KMI {kmi}"));
    load_pathmask(ko_data, &module_params)?;
    let (resolved_count, active_target_paths) = read_loaded_target_state().map_err(|error| {
        coded_error(
            "pathmask.rollback_incomplete",
            format!("rollback runtime state could not be read: {error:#}"),
        )
    })?;
    verify_loaded_target_count(resolved_count, available_count).map_err(|error| {
        coded_error(
            "pathmask.rollback_incomplete",
            format!("rollback did not resolve any mounted target path: {error:#}"),
        )
    })?;
    let available_paths = existing_target_paths(&config);
    let phase = loaded_phase(&config, available_paths.len(), resolved_count);
    let observation = runtime_observation(
        &config,
        &available_paths,
        &active_target_paths,
        resolved_count,
    );
    write_runtime_state_with_observation(phase, "", "", false, Some(&observation));
    if resolved_count < available_count {
        append_log(format!(
            "rollback restored with partial resolution: resolved_count={resolved_count}, available_before_load={available_count}"
        ));
    }
    Ok(())
}

fn rollback_loaded_candidate(previous_loaded: bool, kmi: &str, ko_data: &[u8]) -> Result<()> {
    if is_module_loaded() {
        unload_loaded_pathmask().map_err(|error| {
            coded_error(
                "pathmask.rollback_unload_failed",
                format!("failed to unload rejected candidate config: {error:#}"),
            )
        })?;
    }
    if previous_loaded {
        restore_committed_config(kmi, ko_data).map_err(|error| {
            coded_error(
                "pathmask.rollback_failed",
                format!("failed to restore committed config: {error:#}"),
            )
        })?;
    }
    Ok(())
}

fn error_after_rollback(
    operation_error: anyhow::Error,
    previous_loaded: bool,
    kmi: &str,
    ko_data: &[u8],
) -> anyhow::Error {
    match rollback_loaded_candidate(previous_loaded, kmi, ko_data) {
        Ok(()) => operation_error,
        Err(rollback_error) => {
            append_log(format!("rollback failed: {rollback_error:#}"));
            combine_operation_and_rollback_error(&operation_error, &rollback_error)
        }
    }
}

fn combine_operation_and_rollback_error(
    operation_error: &anyhow::Error,
    rollback_error: &anyhow::Error,
) -> anyhow::Error {
    coded_error(
        "pathmask.rollback_failed",
        format!(
            "pathmask operation failed: {operation_error:#}; rollback failed: {rollback_error:#}"
        ),
    )
}

fn load_pathmask(ko_data: &[u8], module_params: &str) -> Result<()> {
    let params = CString::new(module_params).context("module params contain NUL byte")?;
    ksuinit::load_module(ko_data, &params).context("init_module failed")
}

fn unload_loaded_pathmask() -> Result<()> {
    let graceful_flags = libc::O_NONBLOCK;
    match rustix::system::delete_module(c"pathmask", graceful_flags) {
        Ok(()) => wait_for_pathmask_unloaded(),
        Err(err) if is_busy_unload_error(err) => Err(coded_error(
            "pathmask.module_busy",
            format!("delete_module reported {err}; forced unload is intentionally disabled"),
        )),
        Err(err) => Err(err).context("delete_module failed"),
    }
}

const fn is_busy_unload_error(err: rustix::io::Errno) -> bool {
    let code = err.raw_os_error();
    code == libc::EWOULDBLOCK || code == libc::EBUSY
}

fn wait_for_pathmask_unloaded() -> Result<()> {
    let rounds = MODULE_UNLOAD_WAIT_MS / MODULE_UNLOAD_STEP_MS;
    for _ in 0..rounds {
        if !is_module_loaded() {
            return Ok(());
        }
        thread::sleep(Duration::from_millis(MODULE_UNLOAD_STEP_MS));
    }

    bail!("pathmask is still loaded after unload request");
}

fn wait_for_any_target_path(config: &PathmaskConfig) -> Result<usize> {
    let rounds = TARGET_WAIT_SECONDS * 1000 / TARGET_WAIT_STEP_MS;
    for _ in 0..=rounds {
        let existing = count_existing_target_paths(config);
        if existing > 0 {
            return Ok(existing);
        }
        thread::sleep(Duration::from_millis(TARGET_WAIT_STEP_MS));
    }

    Err(coded_error(
        "pathmask.targets_not_mounted",
        "no configured target path exists now; wait for storage mounts and retry",
    ))
}

fn count_existing_target_paths(config: &PathmaskConfig) -> usize {
    existing_target_paths(config).len()
}

fn existing_target_paths(config: &PathmaskConfig) -> Vec<String> {
    config
        .target_paths
        .iter()
        .filter(|path| Path::new(path.as_str()).exists())
        .cloned()
        .collect()
}

fn missing_target_paths(config: &PathmaskConfig) -> Vec<String> {
    config
        .target_paths
        .iter()
        .filter(|path| !Path::new(path.as_str()).exists())
        .cloned()
        .collect()
}

fn read_resolved_count() -> usize {
    read_sysfs_param("resolved_count")
        .and_then(|value| value.parse::<usize>().ok())
        .unwrap_or(0)
}

fn read_loaded_target_state() -> Result<(usize, String)> {
    let mut last_count = None;
    for attempt in 0..RESOLVED_COUNT_READ_ATTEMPTS {
        if let Some(raw_count) = read_sysfs_param("resolved_count")
            && let Ok(count) = raw_count.parse::<usize>()
        {
            last_count = Some(count);
            let active_paths = read_sysfs_param("target_paths").unwrap_or_default();
            if count > 0 || attempt + 1 == RESOLVED_COUNT_READ_ATTEMPTS {
                return Ok((count, active_paths));
            }
        }
        if attempt + 1 < RESOLVED_COUNT_READ_ATTEMPTS {
            thread::sleep(Duration::from_millis(RESOLVED_COUNT_READ_STEP_MS));
        }
    }

    if let Some(count) = last_count {
        return Ok((count, String::new()));
    }

    Err(coded_error(
        "pathmask.resolved_count_unavailable",
        "pathmask loaded but its resolved_count parameter could not be read",
    ))
}

fn parse_target_paths(value: &str) -> Vec<String> {
    value
        .split(',')
        .map(str::trim)
        .filter(|path| !path.is_empty())
        .collect::<BTreeSet<_>>()
        .into_iter()
        .map(ToOwned::to_owned)
        .collect()
}

fn target_path_set(paths: &[String]) -> BTreeSet<&str> {
    paths.iter().map(String::as_str).collect()
}

fn active_target_paths_match(
    config: &PathmaskConfig,
    available_paths: &[String],
    active_target_paths: &str,
) -> bool {
    if active_target_paths.is_empty() {
        return !is_module_loaded();
    }
    let active = parse_target_paths(active_target_paths)
        .into_iter()
        .collect::<BTreeSet<_>>();
    let expected = target_path_set(&config.target_paths);
    let available = target_path_set(available_paths);
    let active_refs = active.iter().map(String::as_str).collect::<BTreeSet<_>>();

    // Different pathmask builds expose either the full input list or only the
    // targets that were resolved. Both representations are valid while every
    // reported path still belongs to the current configuration.
    active_refs == expected
        || active_refs == available
        || (active_refs.iter().all(|path| expected.contains(path))
            && active_refs.iter().all(|path| available.contains(path)))
}

fn runtime_config_key(config: &PathmaskConfig) -> String {
    json!({
        "targetPaths": config.target_paths,
        "appPackages": config.app_packages,
        "useAppScope": config.use_app_scope,
        "hideDirents": config.hide_dirents,
        "hideIsolated": config.hide_isolated,
    })
    .to_string()
}

fn runtime_observation(
    config: &PathmaskConfig,
    available_paths: &[String],
    active_target_paths: &str,
    resolved_count: usize,
) -> PathmaskRuntimeObservation {
    PathmaskRuntimeObservation {
        config_key: runtime_config_key(config),
        configured_target_paths: config.target_paths.clone(),
        available_target_paths: available_paths.to_vec(),
        resolved_target_paths: parse_target_paths(active_target_paths),
        resolved_count,
    }
}

fn runtime_requires_reload(
    config: &PathmaskConfig,
    available_paths: &[String],
    active_target_paths: &str,
    resolved_count: usize,
    runtime_state: &PathmaskRuntimeState,
) -> bool {
    if resolved_count == 0 || active_target_paths.trim().is_empty() {
        return true;
    }

    let active = parse_target_paths(active_target_paths)
        .into_iter()
        .collect::<BTreeSet<_>>();
    let configured = target_path_set(&config.target_paths);
    if active
        .iter()
        .any(|path| !configured.contains(path.as_str()))
    {
        return true;
    }

    let observation = &runtime_state.observation;
    if observation.config_key.is_empty() {
        // State written by older builds has no observation. Allow a fully
        // resolved module to remain in place, but reload once for a partial
        // module so future boots can distinguish late mounts from skips.
        return resolved_count < available_paths.len();
    }
    if observation.config_key != runtime_config_key(config) {
        return true;
    }

    let current_available = target_path_set(available_paths);
    let observed_available = target_path_set(&observation.available_target_paths);
    current_available
        .iter()
        .any(|path| !observed_available.contains(path))
}

fn unresolved_target_paths(
    config: &PathmaskConfig,
    available_paths: &[String],
    active_target_paths: &str,
    loaded: bool,
    resolved_count: usize,
) -> Vec<String> {
    if resolved_count >= available_paths.len() {
        return Vec::new();
    }
    if !loaded {
        return available_paths.to_vec();
    }

    let active = parse_target_paths(active_target_paths)
        .into_iter()
        .collect::<BTreeSet<_>>();
    if active.is_empty() {
        return available_paths.to_vec();
    }

    let configured = target_path_set(&config.target_paths);
    if active.iter().map(String::as_str).collect::<BTreeSet<_>>() == configured {
        // Some pathmask builds expose the original input list even when a
        // target could not be resolved. The count is still authoritative, but
        // the exact missing entry cannot be named safely in that format.
        return Vec::new();
    }

    available_paths
        .iter()
        .filter(|path| !active.contains(path.as_str()))
        .cloned()
        .collect()
}

const fn loaded_phase(
    config: &PathmaskConfig,
    available_count: usize,
    resolved_count: usize,
) -> &'static str {
    if available_count < config.target_paths.len() || resolved_count < available_count {
        "partial"
    } else {
        "active"
    }
}

const fn reconcile_action(
    enabled: bool,
    loaded: bool,
    available_count: usize,
    resolved_count: usize,
    reload_required: bool,
) -> ReconcileAction {
    if !enabled {
        ReconcileAction::Disabled
    } else if available_count == 0 {
        ReconcileAction::WaitForTargets
    } else if !loaded {
        ReconcileAction::Load
    } else if reload_required || resolved_count == 0 {
        ReconcileAction::Reload
    } else {
        ReconcileAction::Noop
    }
}

fn differs_only_by_auto_load_delay(committed: &PathmaskConfig, candidate: &PathmaskConfig) -> bool {
    committed.auto_load_delay_seconds != candidate.auto_load_delay_seconds
        && committed.target_paths == candidate.target_paths
        && committed.app_packages == candidate.app_packages
        && committed.use_app_scope == candidate.use_app_scope
        && committed.hide_dirents == candidate.hide_dirents
        && committed.hide_isolated == candidate.hide_isolated
        && committed.auto_load_enabled == candidate.auto_load_enabled
}

fn active_runtime_matches(config: &PathmaskConfig) -> bool {
    let loaded = is_module_loaded();
    let available_paths = existing_target_paths(config);
    let available_count = available_paths.len();
    let resolved_count = read_resolved_count();
    let active_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let runtime_state = read_runtime_state().unwrap_or_default();
    let active_matches = active_target_paths_match(config, &available_paths, &active_paths);
    let reload_required = loaded
        && (!active_matches
            || runtime_requires_reload(
                config,
                &available_paths,
                &active_paths,
                resolved_count,
                &runtime_state,
            ));
    reconcile_action(
        config.auto_load_enabled,
        loaded,
        available_count,
        resolved_count,
        reload_required,
    ) == ReconcileAction::Noop
}

const fn derive_phase(
    config: &PathmaskConfig,
    loaded: bool,
    available_count: usize,
    resolved_count: usize,
    requires_reload: bool,
    requires_reboot: bool,
) -> &'static str {
    if config.target_paths.is_empty() {
        "unconfigured"
    } else if !config.auto_load_enabled {
        if loaded {
            "disabled_runtime_active"
        } else {
            "disabled"
        }
    } else if requires_reboot {
        "reboot_required"
    } else if available_count == 0 {
        "waiting_targets"
    } else if !loaded {
        "waiting_load"
    } else if requires_reload {
        "reload_required"
    } else if available_count < config.target_paths.len() || resolved_count < available_count {
        "partial"
    } else {
        "active"
    }
}

fn build_module_params(config: &PathmaskConfig) -> Result<String> {
    validate_config(config)?;
    ensure_safe_scope(config)?;

    let target_paths = config.target_paths.join(",");
    if target_paths.len() > MAX_TARGET_PATHS_LEN {
        bail!("target_paths is too long");
    }

    let mut params = vec![
        format!("target_paths={}", quote_module_param_value(&target_paths)),
        format!("hide_dirents={}", bool_param(config.hide_dirents)),
        format!("hide_isolated={}", bool_param(config.hide_isolated)),
    ];

    if config.use_app_scope {
        let resolution = resolve_deny_uids(&config.app_packages);
        if resolution.uids.is_empty() {
            let unresolved = resolution.unresolved_packages.join(", ");
            bail!(
                "application scope is enabled but no valid app UID was resolved; unresolved packages: {unresolved}"
            );
        }
        if !resolution.unresolved_packages.is_empty() {
            append_log(format!(
                "skipping unresolved packages while applying UID scope: {}",
                resolution.unresolved_packages.join(", ")
            ));
        }
        let deny_uids = resolution
            .uids
            .iter()
            .map(u32::to_string)
            .collect::<Vec<_>>()
            .join(",");
        if deny_uids.len() > MAX_DENY_UIDS_LEN {
            return Err(coded_error(
                "pathmask.uid_parameter_too_long",
                format!(
                    "resolved UID parameter is {} bytes; maximum is {MAX_DENY_UIDS_LEN}",
                    deny_uids.len()
                ),
            ));
        }
        params.push("scope_mode=deny".to_string());
        params.push(format!("deny_uids={deny_uids}"));
    } else {
        params.push("scope_mode=global".to_string());
    }

    let params = params.join(" ");
    if params.len() > MAX_MODULE_PARAMS_LEN {
        return Err(coded_error(
            "pathmask.module_parameter_too_long",
            format!(
                "module parameter string is {} bytes; maximum is {MAX_MODULE_PARAMS_LEN}",
                params.len()
            ),
        ));
    }
    Ok(params)
}

const fn bool_param(value: bool) -> &'static str {
    if value { "1" } else { "0" }
}

fn ensure_safe_scope(config: &PathmaskConfig) -> Result<()> {
    if config.use_app_scope {
        return Ok(());
    }

    if config.target_paths.iter().any(|path| {
        MANAGED_ROOT_PATHS
            .iter()
            .any(|managed| path == managed || path.starts_with(&format!("{managed}/")))
    }) {
        bail!(
            "managed /data/adb paths must use app UID scope; global scope can break module management"
        );
    }

    Ok(())
}

fn quote_module_param_value(value: &str) -> String {
    if value.chars().any(char::is_whitespace) {
        format!("\"{value}\"")
    } else {
        value.to_string()
    }
}

fn resolve_deny_uids(app_packages: &[String]) -> DenyUidResolution {
    let package_uids = read_package_uids();
    let mut resolution = DenyUidResolution::default();
    for app in app_packages {
        if let Ok(uid) = app.parse::<u32>() {
            resolution.uids.insert(uid);
            continue;
        }

        let base_uid = package_uids
            .get(app)
            .copied()
            .or_else(|| resolve_uid_from_data_dir(app));
        let package_uids = resolve_package_uids_for_all_users(app, base_uid);
        if package_uids.is_empty() {
            resolution.unresolved_packages.push(app.clone());
        } else {
            resolution.uids.extend(package_uids);
        }
    }
    resolution
}

fn resolve_package_uids_for_all_users(package: &str, base_uid: Option<u32>) -> BTreeSet<u32> {
    resolve_package_uids_from_roots(
        package,
        base_uid,
        USER_DATA_ROOTS.iter().map(Path::new),
        |package_path| rustix::fs::stat(package_path).ok().map(|stat| stat.st_uid),
    )
}

fn resolve_package_uids_from_roots<'a, I, F>(
    package: &str,
    base_uid: Option<u32>,
    roots: I,
    uid_for_path: F,
) -> BTreeSet<u32>
where
    I: IntoIterator<Item = &'a Path>,
    F: Fn(&Path) -> Option<u32>,
{
    let mut uids = BTreeSet::new();
    for root in roots {
        let Ok(users) = fs::read_dir(root) else {
            continue;
        };
        for user in users.flatten() {
            if user.file_name().to_string_lossy().parse::<u32>().is_err() {
                continue;
            }
            let package_path = user.path().join(package);
            if let Some(uid) = uid_for_path(&package_path) {
                uids.insert(uid);
            }
        }
    }
    if uids.is_empty()
        && let Some(uid) = base_uid
    {
        uids.insert(uid);
    }
    uids
}

fn read_package_uids() -> BTreeMap<String, u32> {
    fs::read_to_string(PACKAGES_LIST_PATH)
        .map(|contents| parse_package_uids(&contents))
        .unwrap_or_default()
}

fn parse_package_uids(contents: &str) -> BTreeMap<String, u32> {
    let mut package_uids = BTreeMap::new();
    for line in contents.lines() {
        let mut fields = line.split_whitespace();
        let Some(package) = fields.next() else {
            continue;
        };
        let Some(uid) = fields.next().and_then(|value| value.parse::<u32>().ok()) else {
            continue;
        };
        package_uids.entry(package.to_owned()).or_insert(uid);
    }
    package_uids
}

fn resolve_uid_from_data_dir(package: &str) -> Option<u32> {
    let package_path = format!("/data/data/{package}");
    rustix::fs::stat(package_path.as_str())
        .ok()
        .map(|stat| stat.st_uid)
}

fn validate_config(config: &PathmaskConfig) -> Result<()> {
    if config.target_paths.is_empty() {
        return Err(coded_error(
            "pathmask.no_targets",
            "no target path is configured",
        ));
    }
    if config.target_paths.len() > MAX_TARGET_PATHS {
        return Err(coded_error(
            "pathmask.too_many_targets",
            format!(
                "{} target paths exceed the limit of {MAX_TARGET_PATHS}",
                config.target_paths.len()
            ),
        ));
    }
    if config.app_packages.len() > MAX_APP_PACKAGES {
        return Err(coded_error(
            "pathmask.too_many_apps",
            format!(
                "{} app entries exceed the limit of {MAX_APP_PACKAGES}",
                config.app_packages.len()
            ),
        ));
    }
    if config.auto_load_delay_seconds > MAX_AUTO_LOAD_DELAY_SECONDS {
        return Err(coded_error(
            "pathmask.invalid_auto_load_delay",
            format!(
                "auto-load delay {}s exceeds the limit of {MAX_AUTO_LOAD_DELAY_SECONDS}s",
                config.auto_load_delay_seconds
            ),
        ));
    }
    ensure_no_duplicates(
        &config.target_paths,
        "pathmask.duplicate_path",
        "target path",
    )?;
    ensure_no_duplicates(
        &config.app_packages,
        "pathmask.duplicate_app",
        "application entry",
    )?;
    if config.use_app_scope && config.app_packages.is_empty() {
        return Err(coded_error(
            "pathmask.no_apps",
            "application scope is enabled but no application is configured",
        ));
    }
    Ok(())
}

fn ensure_no_duplicates(values: &[String], code: &str, label: &str) -> Result<()> {
    let mut seen = BTreeSet::new();
    if let Some(duplicate) = values.iter().find(|value| !seen.insert(value.as_str())) {
        return Err(coded_error(code, format!("duplicate {label}: {duplicate}")));
    }
    Ok(())
}

fn read_config() -> Result<PathmaskConfig> {
    read_config_from_path(CONFIG_PATH)
}

fn read_config_from_path(path: &str) -> Result<PathmaskConfig> {
    if !Path::new(path).exists() {
        return Ok(PathmaskConfig::default());
    }

    let content = fs::read_to_string(path).with_context(|| format!("failed to read {path}"))?;
    parse_config(&content)
}

fn parse_config(content: &str) -> Result<PathmaskConfig> {
    let value: Value = serde_json::from_str(content).map_err(|error| {
        coded_error(
            "pathmask.invalid_json",
            format!("invalid pathmask config JSON: {error}"),
        )
    })?;
    let target_paths = string_array(value.get("targetPaths"))
        .into_iter()
        .map(|path| {
            sanitize_target_path(&path).ok_or_else(|| {
                coded_error(
                    "pathmask.invalid_target",
                    format!("invalid target path: {path}"),
                )
            })
        })
        .collect::<Result<Vec<_>>>()?;
    let app_packages = string_array(value.get("appPackages"))
        .into_iter()
        .map(|package| {
            sanitize_app_entry(&package).ok_or_else(|| {
                coded_error(
                    "pathmask.invalid_app",
                    format!("invalid application entry: {package}"),
                )
            })
        })
        .collect::<Result<Vec<_>>>()?;

    let config = PathmaskConfig {
        target_paths,
        app_packages,
        use_app_scope: value
            .get("useAppScope")
            .and_then(Value::as_bool)
            .unwrap_or(true),
        hide_dirents: value
            .get("hideDirents")
            .and_then(Value::as_bool)
            .unwrap_or(true),
        hide_isolated: value
            .get("hideIsolated")
            .and_then(Value::as_bool)
            .unwrap_or(true),
        auto_load_enabled: value
            .get("autoLoadEnabled")
            .and_then(Value::as_bool)
            .unwrap_or(true),
        auto_load_delay_seconds: parse_auto_load_delay_seconds(value.get("autoLoadDelaySeconds"))?,
    };
    validate_config(&config)?;
    Ok(config)
}

fn parse_auto_load_delay_seconds(value: Option<&Value>) -> Result<u64> {
    let Some(value) = value else {
        return Ok(0);
    };
    let seconds = value.as_u64().ok_or_else(|| {
        coded_error(
            "pathmask.invalid_auto_load_delay",
            "auto-load delay must be a non-negative integer",
        )
    })?;
    if seconds > MAX_AUTO_LOAD_DELAY_SECONDS {
        return Err(coded_error(
            "pathmask.invalid_auto_load_delay",
            format!(
                "auto-load delay {seconds}s exceeds the limit of {MAX_AUTO_LOAD_DELAY_SECONDS}s"
            ),
        ));
    }
    Ok(seconds)
}

fn string_array(value: Option<&Value>) -> Vec<String> {
    value
        .and_then(Value::as_array)
        .map(|array| {
            array
                .iter()
                .filter_map(Value::as_str)
                .map(str::trim)
                .filter(|item| !item.is_empty())
                .map(ToOwned::to_owned)
                .collect()
        })
        .unwrap_or_default()
}

fn sanitize_target_path(path: &str) -> Option<String> {
    let path = normalize_target_path(path);
    let candidate = Path::new(&path);
    if path != "/"
        && candidate.is_absolute()
        && candidate
            .components()
            .all(|component| matches!(component, Component::RootDir | Component::Normal(_)))
        && !path.contains(',')
        && !path.contains('\0')
        && !path.contains('"')
        && !path.contains('\\')
        && !path.chars().any(char::is_control)
    {
        Some(path)
    } else {
        append_log(format!("ignored invalid target path: {path}"));
        None
    }
}

fn normalize_target_path(path: &str) -> String {
    let path = path.trim();
    if path.len() > 1 {
        path.trim_end_matches('/').to_owned()
    } else {
        path.to_owned()
    }
}

fn sanitize_app_entry(app: &str) -> Option<String> {
    let app = app.trim();
    if app
        .chars()
        .all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '_' || c == '-' || c == ':')
    {
        Some(app.to_owned())
    } else {
        append_log(format!("ignored invalid app entry: {app}"));
        None
    }
}

fn write_config(path: &str, config: &PathmaskConfig) -> Result<()> {
    let content = json!({
        "schemaVersion": 3,
        "targetPaths": config.target_paths,
        "appPackages": config.app_packages,
        "useAppScope": config.use_app_scope,
        "hideDirents": config.hide_dirents,
        "hideIsolated": config.hide_isolated,
        "autoLoadEnabled": config.auto_load_enabled,
        "autoLoadDelaySeconds": config.auto_load_delay_seconds,
    })
    .to_string();
    write_atomic(path, content.as_bytes())
}

fn write_atomic(path: &str, content: &[u8]) -> Result<()> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    let temporary_path = format!("{path}.tmp.{}", std::process::id());
    let result = (|| {
        let mut file = File::create(&temporary_path)
            .with_context(|| format!("failed to create {temporary_path}"))?;
        file.write_all(content)
            .with_context(|| format!("failed to write {temporary_path}"))?;
        file.sync_all()
            .with_context(|| format!("failed to sync {temporary_path}"))?;
        fs::rename(&temporary_path, path).with_context(|| format!("failed to replace {path}"))?;
        Ok(())
    })();
    if result.is_err() {
        let _ = fs::remove_file(&temporary_path);
    }
    result
}

fn promote_candidate_config() -> Result<()> {
    promote_config_files(Path::new(CANDIDATE_CONFIG_PATH), Path::new(CONFIG_PATH))
}

fn promote_config_files(candidate: &Path, committed: &Path) -> Result<()> {
    if !candidate.exists() {
        return Err(coded_error(
            "pathmask.candidate_missing",
            "candidate config disappeared before commit",
        ));
    }
    fs::rename(candidate, committed).context("failed to atomically commit candidate config")?;
    Ok(())
}

fn verify_loaded_target_count(resolved_count: usize, existing_count: usize) -> Result<()> {
    if resolved_count == 0 {
        return Err(coded_error(
            "pathmask.no_targets_resolved",
            format!(
                "pathmask resolved no target paths out of {existing_count} currently mounted target paths"
            ),
        ));
    }
    Ok(())
}

fn commit_candidate_after_verification(
    candidate: Option<&Path>,
    committed: &Path,
    verification: Result<()>,
) -> Result<()> {
    verification?;
    if let Some(candidate) = candidate {
        promote_config_files(candidate, committed)?;
    }
    Ok(())
}

fn acquire_operation_lock() -> Result<OperationLock> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    let file = OpenOptions::new()
        .create(true)
        .truncate(false)
        .read(true)
        .write(true)
        .open(OPERATION_LOCK_PATH)
        .context("failed to open pathmask operation lock")?;
    let result = unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) };
    if result != 0 {
        return Err(coded_error(
            "pathmask.operation_busy",
            format!(
                "another pathmask operation is active: {}",
                io::Error::last_os_error()
            ),
        ));
    }
    Ok(OperationLock(file))
}

fn mark_unload_suppressed_this_boot() -> Result<()> {
    let boot_id = current_boot_id()?;
    write_atomic(UNLOAD_BOOT_ID_PATH, boot_id.as_bytes())
}

fn clear_boot_unload_suppression() {
    let _ = fs::remove_file(UNLOAD_BOOT_ID_PATH);
}

fn is_unload_suppressed_this_boot() -> bool {
    let Ok(current) = current_boot_id() else {
        return false;
    };
    let saved = fs::read_to_string(UNLOAD_BOOT_ID_PATH)
        .unwrap_or_default()
        .trim()
        .to_owned();
    saved == current
}

fn acquire_auto_load_schedule_lock() -> Result<File> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    let file = OpenOptions::new()
        .create(true)
        .truncate(false)
        .read(true)
        .write(true)
        .open(AUTO_LOAD_SCHEDULE_LOCK_PATH)
        .context("failed to open delayed auto-load lock")?;
    let result = unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) };
    if result != 0 {
        return Err(coded_error(
            "pathmask.auto_load_already_scheduled",
            "a delayed auto-load task is already running",
        ));
    }
    Ok(file)
}

fn boot_auto_load_remaining_seconds(delay_seconds: u64) -> u64 {
    current_boot_uptime_seconds().map_or(0, |uptime_seconds| {
        remaining_delay_seconds(delay_seconds, uptime_seconds)
    })
}

const fn remaining_delay_seconds(delay_seconds: u64, uptime_seconds: u64) -> u64 {
    delay_seconds.saturating_sub(uptime_seconds)
}

fn current_boot_uptime_seconds() -> Result<u64> {
    let uptime =
        fs::read_to_string(UPTIME_PATH).with_context(|| format!("failed to read {UPTIME_PATH}"))?;
    let value = uptime
        .split_whitespace()
        .next()
        .context("missing uptime value")?;
    let whole_seconds = value.split('.').next().unwrap_or(value);
    whole_seconds
        .parse::<u64>()
        .with_context(|| format!("invalid uptime value: {value}"))
}

fn current_boot_id() -> Result<String> {
    let boot_id = fs::read_to_string(BOOT_ID_PATH)
        .with_context(|| format!("failed to read {BOOT_ID_PATH}"))?;
    let boot_id = boot_id.trim().to_owned();
    if boot_id.is_empty() {
        return Err(coded_error(
            "pathmask.boot_id_empty",
            "current boot ID is empty",
        ));
    }
    Ok(boot_id)
}

fn remove_file_if_exists(path: &str) -> Result<()> {
    match fs::remove_file(path) {
        Ok(()) => Ok(()),
        Err(error) if error.kind() == io::ErrorKind::NotFound => Ok(()),
        Err(error) => Err(error).with_context(|| format!("failed to remove {path}")),
    }
}

fn write_runtime_state(phase: &str, code: &str, message: &str, requires_reboot: bool) {
    let observation = read_runtime_state()
        .map(|state| state.observation)
        .unwrap_or_default();
    write_runtime_state_with_observation(phase, code, message, requires_reboot, Some(&observation));
}

fn write_runtime_state_with_observation(
    phase: &str,
    code: &str,
    message: &str,
    requires_reboot: bool,
    observation: Option<&PathmaskRuntimeObservation>,
) {
    let observation = observation
        .cloned()
        .or_else(|| read_runtime_state().map(|state| state.observation).ok())
        .unwrap_or_default();
    let content = json!({
        "phase": phase,
        "errorCode": code,
        "errorMessage": message,
        "requiresReboot": requires_reboot,
        "updatedAt": Local::now().to_rfc3339(),
        "configKey": observation.config_key,
        "configuredTargetPaths": observation.configured_target_paths,
        "availableTargetPaths": observation.available_target_paths,
        "resolvedTargetPaths": observation.resolved_target_paths,
        "resolvedCount": observation.resolved_count,
    })
    .to_string();
    if let Err(error) = write_atomic(RUNTIME_STATE_PATH, content.as_bytes()) {
        append_log(format!("failed to persist runtime state: {error:#}"));
    }
}

fn read_runtime_state() -> Result<PathmaskRuntimeState> {
    read_runtime_state_from(Path::new(RUNTIME_STATE_PATH))
}

fn read_runtime_state_from(path: &Path) -> Result<PathmaskRuntimeState> {
    if !path.exists() {
        return Ok(PathmaskRuntimeState::default());
    }
    let content =
        fs::read_to_string(path).with_context(|| format!("failed to read {}", path.display()))?;
    parse_runtime_state(&content)
}

fn parse_runtime_state(content: &str) -> Result<PathmaskRuntimeState> {
    let value: Value = serde_json::from_str(content).context("invalid pathmask runtime state")?;
    Ok(PathmaskRuntimeState {
        phase: value
            .get("phase")
            .and_then(Value::as_str)
            .unwrap_or_default()
            .to_owned(),
        error_code: value
            .get("errorCode")
            .and_then(Value::as_str)
            .unwrap_or_default()
            .to_owned(),
        error_message: value
            .get("errorMessage")
            .and_then(Value::as_str)
            .unwrap_or_default()
            .to_owned(),
        requires_reboot: value
            .get("requiresReboot")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        updated_at: value
            .get("updatedAt")
            .and_then(Value::as_str)
            .unwrap_or_default()
            .to_owned(),
        observation: PathmaskRuntimeObservation {
            config_key: value
                .get("configKey")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .to_owned(),
            configured_target_paths: string_array(value.get("configuredTargetPaths")),
            available_target_paths: string_array(value.get("availableTargetPaths")),
            resolved_target_paths: string_array(value.get("resolvedTargetPaths")),
            resolved_count: value
                .get("resolvedCount")
                .and_then(Value::as_u64)
                .unwrap_or(0) as usize,
        },
    })
}

fn error_code(error: &anyhow::Error) -> String {
    let rendered = format!("{error:#}");
    rendered
        .split_once(&format!("{ERROR_PREFIX}:"))
        .and_then(|(_, suffix)| suffix.split_once(':').map(|(code, _)| code))
        .unwrap_or("pathmask.unknown")
        .to_owned()
}

fn error_requires_reboot(error: &anyhow::Error) -> bool {
    matches!(
        error_code(error).as_str(),
        "pathmask.module_busy"
            | "pathmask.rollback_failed"
            | "pathmask.rollback_unload_failed"
            | "pathmask.rollback_incomplete"
            | "pathmask.rollback_targets_not_mounted"
    )
}

fn record_runtime_error(error: &anyhow::Error, requires_reboot: bool) {
    write_runtime_state(
        "error",
        &error_code(error),
        &format!("{error:#}"),
        requires_reboot,
    );
}

fn is_module_loaded() -> bool {
    fs::read_to_string("/proc/modules").is_ok_and(|modules| {
        modules
            .lines()
            .any(|line| line.split_whitespace().next() == Some(PATHMASK_MODULE_NAME))
    })
}

fn read_sysfs_param(name: &str) -> Option<String> {
    fs::read_to_string(format!("/sys/module/pathmask/parameters/{name}"))
        .ok()
        .map(|value| value.trim_matches(char::from(0)).trim().to_owned())
        .filter(|value| !value.is_empty())
}

fn append_log(message: impl AsRef<str>) {
    if let Err(err) = append_log_inner(message.as_ref()) {
        log::warn!("pathmask: failed to append manager log: {err}");
    }
}

fn append_log_inner(message: &str) -> Result<()> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    rotate_log_if_needed()?;
    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(LOG_PATH)
        .with_context(|| format!("failed to open {LOG_PATH}"))?;
    let timestamp = Local::now().format("%Y-%m-%d %H:%M:%S");
    writeln!(file, "[{timestamp}] {message}").context("failed to write pathmask log")
}

fn rotate_log_if_needed() -> Result<()> {
    let Ok(metadata) = fs::metadata(LOG_PATH) else {
        return Ok(());
    };
    if metadata.len() < LOG_MAX_BYTES {
        return Ok(());
    }

    remove_file_if_exists(&format!("{LOG_PATH}.{LOG_ROTATION_COUNT}"))?;
    for index in (1..LOG_ROTATION_COUNT).rev() {
        let source = format!("{LOG_PATH}.{index}");
        let destination = format!("{LOG_PATH}.{}", index + 1);
        if Path::new(&source).exists() {
            fs::rename(&source, &destination).with_context(|| {
                format!("failed to rotate pathmask log {source} to {destination}")
            })?;
        }
    }
    fs::rename(LOG_PATH, format!("{LOG_PATH}.1")).context("failed to rotate current pathmask log")
}

fn tail_file(path: &str, max_lines: usize) -> Result<String> {
    if !Path::new(path).exists() {
        return Ok(String::new());
    }

    let content = fs::read_to_string(path).with_context(|| format!("failed to read {path}"))?;
    let lines = content.lines().collect::<Vec<_>>();
    let start = lines.len().saturating_sub(max_lines);
    Ok(lines[start..].join("\n"))
}

fn read_kernel_pathmask_log() -> String {
    let Ok(output) = Command::new("dmesg").arg("-r").output() else {
        return String::new();
    };
    if !output.status.success() {
        return String::new();
    }

    let text = String::from_utf8_lossy(&output.stdout);
    let lines = text
        .lines()
        .filter(|line| line.contains("pathmask"))
        .collect::<Vec<_>>();
    let start = lines.len().saturating_sub(200);
    lines[start..].join("\n")
}

#[cfg(test)]
mod tests {
    use std::fs;

    use super::{
        MAX_AUTO_LOAD_DELAY_SECONDS, MAX_TARGET_PATHS, PathmaskConfig, PathmaskRuntimeObservation,
        PathmaskRuntimeState, ReconcileAction, coded_error, combine_operation_and_rollback_error,
        commit_candidate_after_verification, error_code, error_requires_reboot, parse_config,
        parse_package_uids, read_runtime_state_from, reconcile_action, remaining_delay_seconds,
        resolve_package_uids_from_roots, runtime_config_key, runtime_requires_reload,
        unresolved_target_paths, validate_config, verify_loaded_target_count,
    };

    #[test]
    fn package_list_parser_uses_package_uid_column() {
        let packages = parse_package_uids(
            "\
            com.example.first 10123 0 /data/user/0/com.example.first platform\n\
            com.example.second 10124 0 /data/user/0/com.example.second platform\n",
        );

        assert_eq!(packages.get("com.example.first"), Some(&10123));
        assert_eq!(packages.get("com.example.second"), Some(&10124));
    }

    #[test]
    fn package_list_parser_ignores_invalid_rows_and_keeps_first_uid() {
        let packages = parse_package_uids(
            "\
            invalid missing-uid\n\
            com.example.app 10123 0 /data/user/0/com.example.app platform\n\
            com.example.app 20123 0 /data/user/10/com.example.app platform\n",
        );

        assert_eq!(packages.len(), 1);
        assert_eq!(packages.get("com.example.app"), Some(&10123));
    }

    #[test]
    fn legacy_config_defaults_auto_load_to_enabled() {
        let config = parse_config(
            r#"{
                "targetPaths":["/data/local/tmp/example"],
                "appPackages":["com.example.app"],
                "useAppScope":true
            }"#,
        )
        .expect("legacy config should remain compatible");

        assert!(config.auto_load_enabled);
        assert_eq!(config.auto_load_delay_seconds, 0);
    }

    #[test]
    fn auto_load_delay_is_parsed_and_bounded() {
        let config = parse_config(
            r#"{
                "targetPaths":["/data/local/tmp/example"],
                "appPackages":["com.example.app"],
                "autoLoadDelaySeconds":45
            }"#,
        )
        .expect("valid auto-load delay should be accepted");
        assert_eq!(config.auto_load_delay_seconds, 45);

        let error = parse_config(&format!(
            r#"{{
                "targetPaths":["/data/local/tmp/example"],
                "appPackages":["com.example.app"],
                "autoLoadDelaySeconds":{}
            }}"#,
            MAX_AUTO_LOAD_DELAY_SECONDS + 1
        ))
        .expect_err("out-of-range auto-load delay must be rejected");
        assert_eq!(error_code(&error), "pathmask.invalid_auto_load_delay");

        for value in ["-1", "1.5", "true"] {
            let error = parse_config(&format!(
                r#"{{
                    "targetPaths":["/data/local/tmp/example"],
                    "appPackages":["com.example.app"],
                    "autoLoadDelaySeconds":{value}
                }}"#,
            ))
            .expect_err("invalid auto-load delay type must be rejected");
            assert_eq!(error_code(&error), "pathmask.invalid_auto_load_delay");
        }
    }

    #[test]
    fn auto_load_delay_uses_boot_uptime_without_underflow() {
        assert_eq!(remaining_delay_seconds(30, 12), 18);
        assert_eq!(remaining_delay_seconds(30, 30), 0);
        assert_eq!(remaining_delay_seconds(30, 45), 0);
    }

    #[test]
    fn duplicate_paths_are_rejected_instead_of_silently_dropped() {
        let error = parse_config(
            r#"{
                "targetPaths":["/data/local/tmp/example","/data/local/tmp/example"],
                "appPackages":["com.example.app"]
            }"#,
        )
        .expect_err("duplicate target paths must be rejected");

        assert_eq!(error_code(&error), "pathmask.duplicate_path");
    }

    #[test]
    fn target_count_limit_is_enforced() {
        let config = PathmaskConfig {
            target_paths: (0..=MAX_TARGET_PATHS)
                .map(|index| format!("/data/local/tmp/path-{index}"))
                .collect(),
            app_packages: vec!["com.example.app".to_owned()],
            ..PathmaskConfig::default()
        };

        let error = validate_config(&config).expect_err("target limit must be enforced");
        assert_eq!(error_code(&error), "pathmask.too_many_targets");
    }

    #[test]
    fn late_mounted_target_requests_reload() {
        assert_eq!(
            reconcile_action(true, true, 1, 1, false),
            ReconcileAction::Noop,
        );
        assert_eq!(
            reconcile_action(true, true, 2, 1, true),
            ReconcileAction::Reload,
        );
    }

    #[test]
    fn disabled_and_unmounted_states_do_not_load() {
        assert_eq!(
            reconcile_action(false, false, 1, 0, false),
            ReconcileAction::Disabled,
        );
        assert_eq!(
            reconcile_action(true, false, 0, 0, false),
            ReconcileAction::WaitForTargets,
        );
    }

    #[test]
    fn partial_resolution_is_accepted_but_zero_resolution_is_rejected() {
        verify_loaded_target_count(4, 5).expect("a partially resolved module remains usable");

        let error = verify_loaded_target_count(0, 5)
            .expect_err("a module that resolved no target must be rejected");
        assert_eq!(error_code(&error), "pathmask.no_targets_resolved");
    }

    #[test]
    fn unloaded_runtime_reports_all_mounted_targets_as_unresolved() {
        let config = PathmaskConfig {
            target_paths: vec!["/data/adb/modules".to_owned(), "/data/adb/ksu".to_owned()],
            app_packages: vec!["com.example.app".to_owned()],
            ..PathmaskConfig::default()
        };
        let available = config.target_paths.clone();

        assert_eq!(
            unresolved_target_paths(&config, &available, "", false, 0),
            available
        );
    }

    #[test]
    fn stable_partial_resolution_does_not_reload_until_a_new_target_appears() {
        let config = PathmaskConfig {
            target_paths: vec![
                "/data/adb/modules".to_owned(),
                "/data/adb/ksu".to_owned(),
                "/data/adb/new".to_owned(),
            ],
            app_packages: vec!["com.example.app".to_owned()],
            ..PathmaskConfig::default()
        };
        let available = vec!["/data/adb/modules".to_owned(), "/data/adb/ksu".to_owned()];
        let observation = PathmaskRuntimeObservation {
            config_key: runtime_config_key(&config),
            configured_target_paths: config.target_paths.clone(),
            available_target_paths: available.clone(),
            resolved_target_paths: available.clone(),
            resolved_count: 2,
        };
        let state = PathmaskRuntimeState {
            observation,
            ..PathmaskRuntimeState::default()
        };

        assert!(!runtime_requires_reload(
            &config,
            &available,
            "/data/adb/modules,/data/adb/ksu",
            2,
            &state,
        ));

        let with_late_target = vec![
            "/data/adb/modules".to_owned(),
            "/data/adb/ksu".to_owned(),
            "/data/adb/new".to_owned(),
        ];
        assert!(runtime_requires_reload(
            &config,
            &with_late_target,
            "/data/adb/modules,/data/adb/ksu",
            2,
            &state,
        ));
    }

    #[test]
    fn candidate_is_promoted_only_at_commit_point() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let candidate = temp.path().join("candidate.json");
        let committed = temp.path().join("config.json");
        fs::write(&committed, "old").expect("write committed config");
        fs::write(&candidate, "new").expect("write candidate config");

        commit_candidate_after_verification(Some(&candidate), &committed, Ok(()))
            .expect("promote verified candidate");

        assert!(!candidate.exists());
        assert_eq!(fs::read_to_string(committed).unwrap(), "new");
    }

    #[test]
    fn partially_resolved_candidate_is_committed() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let candidate = temp.path().join("candidate.json");
        let committed = temp.path().join("config.json");
        fs::write(&committed, "old").expect("write committed config");
        fs::write(&candidate, "partial-but-active").expect("write candidate config");

        commit_candidate_after_verification(
            Some(&candidate),
            &committed,
            verify_loaded_target_count(4, 5),
        )
        .expect("a candidate with active targets should be committed");

        assert!(!candidate.exists());
        assert_eq!(fs::read_to_string(committed).unwrap(), "partial-but-active");
    }

    #[test]
    fn rejected_candidate_preserves_committed_config() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let candidate = temp.path().join("candidate.json");
        let committed = temp.path().join("config.json");
        fs::write(&committed, "known-good").expect("write committed config");
        fs::write(&candidate, "rejected").expect("write candidate config");

        let result = commit_candidate_after_verification(
            Some(&candidate),
            &committed,
            Err(coded_error(
                "pathmask.partial_resolution",
                "candidate resolved only some targets",
            )),
        );

        assert_eq!(
            error_code(&result.expect_err("candidate must be rejected")),
            "pathmask.partial_resolution",
        );
        assert_eq!(fs::read_to_string(&committed).unwrap(), "known-good");
        assert_eq!(fs::read_to_string(&candidate).unwrap(), "rejected");
    }

    #[test]
    fn rollback_failure_reports_both_operation_and_rollback_errors() {
        let error = combine_operation_and_rollback_error(
            &coded_error(
                "pathmask.targets_not_mounted",
                "candidate targets are unavailable",
            ),
            &coded_error(
                "pathmask.rollback_incomplete",
                "committed targets are unavailable",
            ),
        );
        let rendered = format!("{error:#}");
        assert_eq!(error_code(&error), "pathmask.rollback_failed");
        assert!(error_requires_reboot(&error));
        assert!(rendered.contains("candidate targets are unavailable"));
        assert!(rendered.contains("committed targets are unavailable"));
    }

    #[test]
    fn package_resolution_includes_secondary_and_work_profile_users() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let credential_root = temp.path().join("user");
        let device_root = temp.path().join("user_de");
        let package = "com.example.app";
        for path in [
            credential_root.join("0").join(package),
            credential_root.join("10").join(package),
            device_root.join("11").join(package),
        ] {
            fs::create_dir_all(path).expect("create user package directory");
        }
        fs::create_dir_all(credential_root.join("not-a-user").join(package))
            .expect("create ignored directory");

        let roots = [credential_root.as_path(), device_root.as_path()];
        let resolved =
            resolve_package_uids_from_roots(package, Some(10123), roots, |package_path| {
                let user_id = package_path
                    .parent()?
                    .file_name()?
                    .to_str()?
                    .parse::<u32>()
                    .ok()?;
                Some(user_id.saturating_mul(100_000).saturating_add(10_123))
            });

        assert_eq!(
            resolved.into_iter().collect::<Vec<_>>(),
            vec![10123, 1_010_123, 1_110_123]
        );
    }

    #[test]
    fn runtime_state_survives_repeated_reads_after_process_restart() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let state_path = temp.path().join("runtime_state.json");
        fs::write(
            &state_path,
            r#"{
                "phase":"partial",
                "errorCode":"pathmask.partial_resolution",
                "errorMessage":"one target is not mounted",
                "requiresReboot":true,
                "updatedAt":"2026-08-27T12:00:00+08:00"
            }"#,
        )
        .expect("persist runtime state");

        let expected = PathmaskRuntimeState {
            phase: "partial".to_owned(),
            error_code: "pathmask.partial_resolution".to_owned(),
            error_message: "one target is not mounted".to_owned(),
            requires_reboot: true,
            updated_at: "2026-08-27T12:00:00+08:00".to_owned(),
            observation: PathmaskRuntimeObservation::default(),
        };
        assert_eq!(read_runtime_state_from(&state_path).unwrap(), expected);
        assert_eq!(read_runtime_state_from(&state_path).unwrap(), expected);
    }

    #[test]
    fn runtime_target_observation_survives_process_restart() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let state_path = temp.path().join("runtime_state.json");
        fs::write(
            &state_path,
            r#"{
                "phase":"partial",
                "errorCode":"",
                "errorMessage":"",
                "requiresReboot":false,
                "updatedAt":"2026-08-28T12:00:00+08:00",
                "configKey":"config-v1",
                "configuredTargetPaths":["/data/adb/modules","/data/adb/ksu"],
                "availableTargetPaths":["/data/adb/modules"],
                "resolvedTargetPaths":["/data/adb/modules"],
                "resolvedCount":1
            }"#,
        )
        .expect("persist runtime observation");

        let state = read_runtime_state_from(&state_path).expect("read runtime observation");
        assert_eq!(state.observation.config_key, "config-v1");
        assert_eq!(
            state.observation.configured_target_paths,
            vec!["/data/adb/modules", "/data/adb/ksu"]
        );
        assert_eq!(
            state.observation.available_target_paths,
            vec!["/data/adb/modules"]
        );
        assert_eq!(state.observation.resolved_count, 1);
    }
}
