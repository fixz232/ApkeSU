use anyhow::{Context, Result, anyhow, bail};
use chrono::Local;
use const_format::concatcp;
use rustix::fs::{Mode, OFlags, open};
use serde_json::{Value, json};
use std::{
    collections::{BTreeMap, BTreeSet},
    ffi::CString,
    fs::{self, File, OpenOptions},
    io::{self, Write},
    os::fd::{AsRawFd, FromRawFd},
    path::{Component, Path, PathBuf},
    process::Command,
    thread,
    time::{Duration, Instant},
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
const PACKAGE_UID_WATCHER_LOCK_PATH: &str = concatcp!(PATHMASK_DIR, "package_uid_watcher.lock");
const LATE_TARGET_WATCHER_LOCK_PATH: &str = concatcp!(PATHMASK_DIR, "late_target_watcher.lock");
const LOG_PATH: &str = concatcp!(PATHMASK_DIR, "pathmask.log");
const BOOT_ID_PATH: &str = "/proc/sys/kernel/random/boot_id";
const UPTIME_PATH: &str = "/proc/uptime";
const PATHMASK_MODULE_NAME: &str = "pathmask";
const PACKAGES_LIST_PATH: &str = "/data/system/packages.list";
const USER_DATA_ROOTS: &[&str] = &["/data/user", "/data/user_de"];
// The bundled pathmask LKM stores at most 16 paths in 256-byte slots.
const MAX_TARGET_PATHS: usize = 16;
const MAX_TARGET_PATH_BYTES: usize = 255;
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
const PACKAGE_UID_WATCH_TIMEOUT_MS: i32 = 60_000;
const PACKAGE_UID_WATCH_DEBOUNCE_MS: u64 = 750;
const PACKAGE_UID_CONFIRM_DELAY_MS: u64 = 200;
const PACKAGE_UID_PERIODIC_REFRESH_SECONDS: u64 = 60;
const PACKAGE_UID_REFRESH_RETRY_DELAYS_MS: &[u64] = &[0, 250, 1_000, 3_000];
const LATE_TARGET_WATCH_TIMEOUT_MS: i32 = 60_000;
const LATE_TARGET_WATCH_DEBOUNCE_MS: u64 = 300;
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

#[derive(Clone, Debug, Default, PartialEq, Eq)]
struct DenyUidResolution {
    uids: BTreeSet<u32>,
    unresolved_packages: Vec<String>,
    authoritative: bool,
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
    uid_resolution_key: String,
    resolved_app_uids: Vec<u32>,
    unresolved_app_packages: Vec<String>,
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

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum PackageUidRefreshAction {
    Stop,
    Noop,
    UpdateObservation,
    Load,
    Reload,
    Unload,
}

struct ScopeParams {
    module_params: Vec<String>,
    uid_resolution: DenyUidResolution,
}

struct PackageChangeMonitor {
    file: File,
    package_system_watch: Option<i32>,
    pathmask_config_watch: Option<i32>,
}

struct LateTargetChangeMonitor {
    file: File,
    pathmask_config_watch: Option<i32>,
    target_names_by_watch: BTreeMap<i32, BTreeSet<String>>,
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
    let config_present = Path::new(CONFIG_PATH).exists();
    let config = match read_config_for_status() {
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
    let config_validation_error = config_present
        .then(|| validate_config(&config).err())
        .flatten();
    let current_kmi = boot_patch::get_current_kmi().unwrap_or_default();
    let loaded = is_module_loaded();
    let runtime_state = read_runtime_state().unwrap_or_default();
    let resolved_count = if loaded { read_resolved_count() } else { 0 };
    let active_target_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let currently_available_paths = existing_target_paths(&config);
    let available_paths = effective_available_target_paths(
        &config,
        &currently_available_paths,
        loaded,
        &runtime_state,
    );
    let deny_uid_resolution = if config.use_app_scope {
        resolve_deny_uids(&config.app_packages)
    } else {
        DenyUidResolution::default()
    };
    let missing_target_paths = missing_target_paths(&config, &available_paths);
    let active_config_matches =
        active_target_paths_match(&config, &available_paths, &active_target_paths);
    let uid_mapping_stale = loaded
        && config.use_app_scope
        && uid_mapping_requires_reload(&runtime_state.observation, &deny_uid_resolution);
    let reload_required = loaded
        && (uid_mapping_stale
            || !active_config_matches
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
    } else if config.use_app_scope
        && !config.target_paths.is_empty()
        && !config.app_packages.is_empty()
        && deny_uid_resolution.authoritative
        && deny_uid_resolution.uids.is_empty()
    {
        "waiting_apps"
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
    let stale_partial_error = !(loaded && unresolved_target_count > 0)
        && runtime_state.error_code == "pathmask.partial_resolution"
        && !runtime_state.requires_reboot;
    let config_error_code = config_validation_error
        .as_ref()
        .map(error_code)
        .unwrap_or_default();
    let config_error_message = config_validation_error
        .as_ref()
        .map(|error| format!("{error:#}"))
        .unwrap_or_default();
    let last_error_code = if !config_error_code.is_empty() {
        config_error_code.as_str()
    } else if stale_partial_error {
        ""
    } else {
        runtime_state.error_code.as_str()
    };
    let last_error_message = if !config_error_message.is_empty() {
        config_error_message.as_str()
    } else if stale_partial_error {
        ""
    } else {
        runtime_state.error_message.as_str()
    };
    let last_log = tail_file(LOG_PATH, 40).unwrap_or_default();
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
            "lastErrorCode": last_error_code,
            "lastErrorMessage": last_error_message,
            "lastPhase": runtime_state.phase,
            "stateUpdatedAt": runtime_state.updated_at,
            "lastLog": last_log,
            "resolvedAppUids": deny_uid_resolution.uids,
            "unresolvedAppPackages": deny_uid_resolution.unresolved_packages,
            "activeResolvedAppUids": runtime_state.observation.resolved_app_uids,
            "uidMappingStale": uid_mapping_stale,
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
    apply_config_text_inner(content, source)
}

fn apply_config_text_inner(content: &str, source: &str) -> Result<()> {
    let result = {
        let _lock = acquire_operation_lock()?;
        let config = parse_config(content)?;
        validate_config(&config)?;
        write_config(CANDIDATE_CONFIG_PATH, &config)?;
        append_log(format!("applying imported config from {source}"));
        let result = apply_inner(true);
        if result.is_err()
            && let Err(error) = remove_file_if_exists(CANDIDATE_CONFIG_PATH)
        {
            append_log(format!(
                "failed to discard rejected imported config: {error:#}"
            ));
        }
        result
    };
    ensure_runtime_watchers();
    result
}

pub fn apply() -> Result<()> {
    let result = acquire_operation_lock().and_then(|_lock| apply_inner(true));
    if let Err(err) = &result {
        append_log(format!("apply failed: {err:#}"));
        record_runtime_error(err, error_requires_reboot(err));
    }
    ensure_runtime_watchers();
    result
}

pub fn apply_config_text(content: &str) -> Result<()> {
    let result = apply_config_text_inner(content, "inline JSON");
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

    // Scope validation and UID resolution do not depend on target mounts. Do
    // them before unloading the active module so invalid candidates cannot
    // cause avoidable downtime or a rollback attempt.
    let scope_params = build_scope_params(&config)?;
    let kmi = boot_patch::get_current_kmi().context("failed to detect current KMI")?;
    let ko_name = format!("{kmi}_pathmask.ko");
    let ko_data =
        assets::get_asset_data(&ko_name).with_context(|| format!("failed to load {ko_name}"))?;

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

    let load_target_paths = match wait_for_target_paths(&config) {
        Ok(paths) => paths,
        Err(error) => {
            return Err(error_after_rollback(error, previous_loaded, &kmi, &ko_data));
        }
    };
    let existing_count = load_target_paths.len();
    append_log(format!(
        "target path precheck passed: {existing_count}/{} currently exists",
        config.target_paths.len()
    ));
    let module_params =
        match build_module_params(&config, &load_target_paths, &scope_params.module_params) {
            Ok(params) => params,
            Err(error) => {
                return Err(error_after_rollback(error, previous_loaded, &kmi, &ko_data));
            }
        };
    append_log(format!("applying pathmask for KMI {kmi}: {module_params}"));

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
    // Global scope can hide these paths from ksud immediately after the LKM
    // loads. Keep the pre-load O_PATH result as the authoritative baseline and
    // merge any target that appeared during the load window.
    let available_paths =
        merge_available_target_paths(&config, &load_target_paths, &existing_target_paths(&config));
    let phase = loaded_phase(&config, available_paths.len(), resolved_count);
    let observation = runtime_observation(
        &config,
        &available_paths,
        &active_target_paths,
        resolved_count,
        &scope_params.uid_resolution,
    );
    let unresolved_count = available_paths.len().saturating_sub(resolved_count);
    if unresolved_count > 0 {
        let message = format!(
            "pathmask resolved {resolved_count} of {} currently resolvable target paths; it will retry when target mounts change",
            available_paths.len()
        );
        write_runtime_state_with_observation(
            phase,
            "pathmask.partial_resolution",
            &message,
            false,
            Some(&observation),
        );
        append_log(format!(
            "pathmask partially active: resolved_count={resolved_count}, available={}, configured={}",
            available_paths.len(),
            config.target_paths.len()
        ));
    } else {
        write_runtime_state_with_observation(phase, "", "", false, Some(&observation));
        append_log(format!(
            "pathmask loaded successfully, resolved_count={resolved_count}, available={}, configured={}, waiting_for_mount={}",
            available_paths.len(),
            config.target_paths.len(),
            config
                .target_paths
                .len()
                .saturating_sub(available_paths.len())
        ));
    }
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
        ensure_runtime_watchers();
        return;
    }

    if let Err(error) = ensure_pathmask_runtime_supported() {
        append_log(format!("skip boot auto-load: {error:#}"));
        return;
    }

    if boot_auto_load_remaining_seconds(config.auto_load_delay_seconds) == 0 {
        apply_configured_now();
        ensure_runtime_watchers();
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
        ensure_runtime_watchers();
        return;
    }
    let remaining_seconds = boot_auto_load_remaining_seconds(config.auto_load_delay_seconds);
    if remaining_seconds == 0 {
        drop(schedule_lock);
        apply_configured_now();
        ensure_runtime_watchers();
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
            run_delayed_auto_load();
            drop(schedule_lock);
            ensure_runtime_watchers();
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
    let runtime_state = read_runtime_state().unwrap_or_default();
    let uid_resolution = if config.use_app_scope {
        resolve_deny_uids(&config.app_packages)
    } else {
        DenyUidResolution::default()
    };
    if config.use_app_scope && uid_resolution.authoritative && uid_resolution.uids.is_empty() {
        match park_pathmask_for_missing_apps(&config, &uid_resolution) {
            Ok(()) => append_log("auto-load is waiting for a configured application to return"),
            Err(error) => {
                record_runtime_error(&error, error_requires_reboot(&error));
                append_log(format!(
                    "failed to clear stale UID scope while waiting for applications: {error:#}"
                ));
            }
        }
        return;
    }
    let currently_available_paths = existing_target_paths(&config);
    let available_paths = effective_available_target_paths(
        &config,
        &currently_available_paths,
        loaded,
        &runtime_state,
    );
    let available_count = available_paths.len();
    let resolved_count = read_resolved_count();
    let active_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let active_matches = active_target_paths_match(&config, &available_paths, &active_paths);
    let reload_required = loaded
        && ((config.use_app_scope
            && uid_mapping_requires_reload(&runtime_state.observation, &uid_resolution))
            || !active_matches
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

fn should_watch_package_uids(config: &PathmaskConfig) -> bool {
    config.auto_load_enabled
        && config.use_app_scope
        && !config.target_paths.is_empty()
        && config
            .app_packages
            .iter()
            .any(|app| app.parse::<u32>().is_err())
}

fn package_uid_refresh_action(
    config: &PathmaskConfig,
    loaded: bool,
    unload_suppressed: bool,
    observation: &PathmaskRuntimeObservation,
    resolution: &DenyUidResolution,
) -> PackageUidRefreshAction {
    if !should_watch_package_uids(config) || unload_suppressed {
        return PackageUidRefreshAction::Stop;
    }
    if !resolution.authoritative {
        return PackageUidRefreshAction::Noop;
    }

    let current_key = uid_resolution_key(resolution);
    if loaded {
        if observation.config_key != runtime_config_key(config)
            || uid_mapping_requires_reload(observation, resolution)
        {
            return if resolution.uids.is_empty() {
                PackageUidRefreshAction::Unload
            } else {
                PackageUidRefreshAction::Reload
            };
        }
        if observation.uid_resolution_key != current_key {
            return PackageUidRefreshAction::UpdateObservation;
        }
        return PackageUidRefreshAction::Noop;
    }

    if resolution.uids.is_empty() {
        if observation.uid_resolution_key == current_key {
            PackageUidRefreshAction::Noop
        } else {
            PackageUidRefreshAction::UpdateObservation
        }
    } else {
        PackageUidRefreshAction::Load
    }
}

fn ensure_runtime_watchers() {
    ensure_package_uid_watcher();
    ensure_late_target_watcher();
}

fn ensure_package_uid_watcher() {
    let Ok(config) = read_config() else {
        return;
    };
    if !should_watch_package_uids(&config)
        || is_unload_suppressed_this_boot()
        || ensure_pathmask_runtime_supported().is_err()
    {
        return;
    }

    let watcher_lock = match acquire_package_uid_watcher_lock() {
        Ok(lock) => lock,
        Err(error) if error_code(&error) == "pathmask.package_uid_watcher_active" => return,
        Err(error) => {
            append_log(format!("failed to start package UID watcher: {error:#}"));
            return;
        }
    };

    match utils::create_daemon(true) {
        Ok(true) => {
            let _watcher_lock = watcher_lock;
            run_package_uid_watcher();
            unsafe {
                libc::_exit(0);
            }
        }
        Ok(false) => append_log("started package UID watcher"),
        Err(error) => append_log(format!(
            "failed to daemonize package UID watcher: {error:#}"
        )),
    }
}

fn run_package_uid_watcher() {
    append_log("package UID watcher is active");
    if !refresh_package_uid_mapping_with_retries() {
        append_log("package UID watcher stopped: configuration is inactive");
        return;
    }

    let mut last_periodic_refresh = Instant::now();
    loop {
        if !package_uid_watcher_should_continue() {
            append_log("package UID watcher stopped: configuration is inactive");
            return;
        }

        let monitor = match PackageChangeMonitor::new() {
            Ok(monitor) => monitor,
            Err(error) => {
                append_log(format!(
                    "package UID watcher could not initialize inotify; using periodic retry: {error}"
                ));
                thread::sleep(Duration::from_secs(PACKAGE_UID_PERIODIC_REFRESH_SECONDS));
                if !refresh_package_uid_mapping_with_retries() {
                    return;
                }
                continue;
            }
        };

        loop {
            if !package_uid_watcher_should_continue() {
                append_log("package UID watcher stopped: configuration is inactive");
                return;
            }

            match monitor.wait_for_change(PACKAGE_UID_WATCH_TIMEOUT_MS) {
                Ok(true) => {
                    thread::sleep(Duration::from_millis(PACKAGE_UID_WATCH_DEBOUNCE_MS));
                    for _ in 0..8 {
                        if !monitor.wait_for_change(0).unwrap_or(false) {
                            break;
                        }
                    }
                    if !refresh_package_uid_mapping_with_retries() {
                        return;
                    }
                    last_periodic_refresh = Instant::now();
                    break;
                }
                Ok(false)
                    if last_periodic_refresh.elapsed()
                        >= Duration::from_secs(PACKAGE_UID_PERIODIC_REFRESH_SECONDS) =>
                {
                    if !refresh_package_uid_mapping_with_retries() {
                        return;
                    }
                    last_periodic_refresh = Instant::now();
                }
                Ok(false) => {}
                Err(error) => {
                    append_log(format!(
                        "package UID watcher lost inotify; rebuilding watches: {error}"
                    ));
                    thread::sleep(Duration::from_secs(1));
                    break;
                }
            }
        }
    }
}

fn package_uid_watcher_should_continue() -> bool {
    match read_config() {
        Ok(config) => should_watch_package_uids(&config) && !is_unload_suppressed_this_boot(),
        Err(error) => {
            append_log(format!(
                "package UID watcher retained after config read error: {error:#}"
            ));
            true
        }
    }
}

fn refresh_package_uid_mapping_with_retries() -> bool {
    let mut last_error = None;
    for (attempt, delay_ms) in PACKAGE_UID_REFRESH_RETRY_DELAYS_MS
        .iter()
        .copied()
        .enumerate()
    {
        if delay_ms > 0 {
            thread::sleep(Duration::from_millis(delay_ms));
        }
        match refresh_package_uid_mapping_once() {
            Ok(PackageUidRefreshAction::Stop) => return false,
            Ok(_) => return true,
            Err(error) => {
                let retry = attempt + 1 < PACKAGE_UID_REFRESH_RETRY_DELAYS_MS.len()
                    && package_uid_refresh_error_is_retryable(&error);
                append_log(format!(
                    "package UID refresh attempt {} failed: {error:#}",
                    attempt + 1
                ));
                last_error = Some(error);
                if !retry {
                    break;
                }
            }
        }
    }

    if let Some(error) = last_error {
        let message = format!(
            "package UID mapping changed, but pathmask could not refresh safely: {error:#}"
        );
        write_runtime_state(
            "package_uid_refresh_pending",
            "pathmask.package_uid_refresh_pending",
            &message,
            error_requires_reboot(&error),
        );
        append_log(&message);
    }
    true
}

fn refresh_package_uid_mapping_once() -> Result<PackageUidRefreshAction> {
    let _lock = acquire_operation_lock()?;
    let config = read_config()?;
    let unload_suppressed = is_unload_suppressed_this_boot();
    let mut resolution = resolve_deny_uids(&config.app_packages);
    let loaded = is_module_loaded();
    let runtime_state = read_runtime_state().unwrap_or_default();
    let mut action = package_uid_refresh_action(
        &config,
        loaded,
        unload_suppressed,
        &runtime_state.observation,
        &resolution,
    );
    if matches!(
        action,
        PackageUidRefreshAction::Load
            | PackageUidRefreshAction::Reload
            | PackageUidRefreshAction::Unload
    ) {
        thread::sleep(Duration::from_millis(PACKAGE_UID_CONFIRM_DELAY_MS));
        let confirmed = resolve_deny_uids(&config.app_packages);
        if !confirmed.authoritative
            || uid_resolution_key(&confirmed) != uid_resolution_key(&resolution)
        {
            append_log("package UID mapping is still changing; defer refresh until it settles");
            return Ok(PackageUidRefreshAction::Noop);
        }
        resolution = confirmed;
        action = package_uid_refresh_action(
            &config,
            loaded,
            unload_suppressed,
            &runtime_state.observation,
            &resolution,
        );
    }

    match action {
        PackageUidRefreshAction::Stop | PackageUidRefreshAction::Noop => {}
        PackageUidRefreshAction::UpdateObservation => {
            let mut observation =
                uid_refresh_observation(&config, loaded, &runtime_state, &resolution);
            update_uid_observation(&mut observation, &resolution);
            let phase = if loaded {
                loaded_phase(
                    &config,
                    observation.available_target_paths.len(),
                    observation.resolved_count,
                )
            } else {
                "waiting_apps"
            };
            write_runtime_state_with_observation(phase, "", "", false, Some(&observation));
        }
        PackageUidRefreshAction::Load | PackageUidRefreshAction::Reload => {
            append_log(format!(
                "package UID mapping changed: active={:?}, current={:?}; {} pathmask",
                runtime_state.observation.resolved_app_uids,
                resolution.uids,
                if loaded { "reloading" } else { "loading" }
            ));
            apply_inner(false)?;
        }
        PackageUidRefreshAction::Unload => {
            append_log(format!(
                "all configured packages are absent; unloading stale pathmask UID scope {:?}",
                runtime_state.observation.resolved_app_uids
            ));
            park_pathmask_for_missing_apps(&config, &resolution)?;
        }
    }
    Ok(action)
}

fn park_pathmask_for_missing_apps(
    config: &PathmaskConfig,
    resolution: &DenyUidResolution,
) -> Result<()> {
    if is_module_loaded() {
        unload_loaded_pathmask()?;
    }
    let available_paths = existing_target_paths(config);
    let observation = runtime_observation(config, &available_paths, "", 0, resolution);
    write_runtime_state_with_observation("waiting_apps", "", "", false, Some(&observation));
    Ok(())
}

fn uid_refresh_observation(
    config: &PathmaskConfig,
    loaded: bool,
    runtime_state: &PathmaskRuntimeState,
    resolution: &DenyUidResolution,
) -> PathmaskRuntimeObservation {
    if loaded && runtime_state.observation.config_key == runtime_config_key(config) {
        let mut observation = runtime_state.observation.clone();
        update_uid_observation(&mut observation, resolution);
        return observation;
    }

    let available_paths = existing_target_paths(config);
    let active_target_paths = if loaded {
        read_sysfs_param("target_paths").unwrap_or_default()
    } else {
        String::new()
    };
    let resolved_count = if loaded { read_resolved_count() } else { 0 };
    runtime_observation(
        config,
        &available_paths,
        &active_target_paths,
        resolved_count,
        resolution,
    )
}

fn package_uid_refresh_error_is_retryable(error: &anyhow::Error) -> bool {
    matches!(
        error_code(error).as_str(),
        "pathmask.operation_busy"
            | "pathmask.module_busy"
            | "pathmask.load_failed"
            | "pathmask.resolved_count_unavailable"
    )
}

fn ensure_late_target_watcher() {
    if !late_target_watcher_should_continue() || ensure_pathmask_runtime_supported().is_err() {
        return;
    }

    let watcher_lock = match acquire_late_target_watcher_lock() {
        Ok(lock) => lock,
        Err(error) if error_code(&error) == "pathmask.late_target_watcher_active" => return,
        Err(error) => {
            append_log(format!("failed to start late target watcher: {error:#}"));
            return;
        }
    };

    match utils::create_daemon(true) {
        Ok(true) => {
            let _watcher_lock = watcher_lock;
            run_late_target_watcher();
            unsafe {
                libc::_exit(0);
            }
        }
        Ok(false) => append_log("started late target watcher"),
        Err(error) => append_log(format!(
            "failed to daemonize late target watcher: {error:#}"
        )),
    }
}

fn run_late_target_watcher() {
    append_log("late target watcher is active");
    loop {
        let config = match read_config() {
            Ok(config) => config,
            Err(_error) if !Path::new(CONFIG_PATH).exists() => {
                append_log("late target watcher stopped: configuration was deleted");
                return;
            }
            Err(error) => {
                append_log(format!(
                    "late target watcher retained after config read error: {error:#}"
                ));
                thread::sleep(Duration::from_secs(PACKAGE_UID_PERIODIC_REFRESH_SECONDS));
                continue;
            }
        };

        let monitor = match LateTargetChangeMonitor::new(&config) {
            Ok(monitor) => Some(monitor),
            Err(error) => {
                append_log(format!(
                    "late target watcher could not initialize inotify; using periodic retry: {error}"
                ));
                None
            }
        };

        if !refresh_late_targets_with_retries() {
            append_log("late target watcher stopped: all configured targets are active");
            return;
        }

        match monitor {
            Some(monitor) => match monitor.wait_for_change(LATE_TARGET_WATCH_TIMEOUT_MS) {
                Ok(true) => {
                    thread::sleep(Duration::from_millis(LATE_TARGET_WATCH_DEBOUNCE_MS));
                    for _ in 0..8 {
                        if !monitor.wait_for_change(0).unwrap_or(false) {
                            break;
                        }
                    }
                }
                Ok(false) => {}
                Err(error) => {
                    append_log(format!(
                        "late target watcher lost inotify; rebuilding watches: {error}"
                    ));
                    thread::sleep(Duration::from_secs(1));
                }
            },
            None => thread::sleep(Duration::from_secs(PACKAGE_UID_PERIODIC_REFRESH_SECONDS)),
        }
    }
}

fn late_target_watcher_should_continue() -> bool {
    if !Path::new(CONFIG_PATH).exists() || is_unload_suppressed_this_boot() {
        return false;
    }
    let Ok(config) = read_config() else {
        return true;
    };
    if boot_auto_load_remaining_seconds(config.auto_load_delay_seconds) > 0 {
        return false;
    }

    let loaded = is_module_loaded();
    let runtime_state = read_runtime_state().unwrap_or_default();
    let currently_available_paths = existing_target_paths(&config);
    let available_paths = effective_available_target_paths(
        &config,
        &currently_available_paths,
        loaded,
        &runtime_state,
    );
    let resolved_count = if loaded { read_resolved_count() } else { 0 };
    should_watch_late_targets(&config, loaded, available_paths.len(), resolved_count)
}

const fn should_watch_late_targets(
    config: &PathmaskConfig,
    loaded: bool,
    available_count: usize,
    resolved_count: usize,
) -> bool {
    config.auto_load_enabled
        && !config.target_paths.is_empty()
        && (!loaded
            || available_count < config.target_paths.len()
            || resolved_count < available_count)
}

fn refresh_late_targets_with_retries() -> bool {
    let mut last_error = None;
    for (attempt, delay_ms) in PACKAGE_UID_REFRESH_RETRY_DELAYS_MS
        .iter()
        .copied()
        .enumerate()
    {
        if delay_ms > 0 {
            thread::sleep(Duration::from_millis(delay_ms));
        }
        match refresh_late_targets_once() {
            Ok(should_continue) => return should_continue,
            Err(error) => {
                let retry = attempt + 1 < PACKAGE_UID_REFRESH_RETRY_DELAYS_MS.len()
                    && package_uid_refresh_error_is_retryable(&error);
                append_log(format!(
                    "late target refresh attempt {} failed: {error:#}",
                    attempt + 1
                ));
                last_error = Some(error);
                if !retry {
                    break;
                }
            }
        }
    }

    if let Some(error) = last_error {
        let message = format!(
            "a late target became available, but pathmask could not refresh safely: {error:#}"
        );
        write_runtime_state(
            "late_target_refresh_pending",
            "pathmask.late_target_refresh_pending",
            &message,
            error_requires_reboot(&error),
        );
        append_log(&message);
    }
    true
}

fn refresh_late_targets_once() -> Result<bool> {
    let _lock = acquire_operation_lock()?;
    let config = read_config()?;
    if !config.auto_load_enabled
        || config.target_paths.is_empty()
        || is_unload_suppressed_this_boot()
        || boot_auto_load_remaining_seconds(config.auto_load_delay_seconds) > 0
    {
        return Ok(false);
    }

    let loaded = is_module_loaded();
    let runtime_state = read_runtime_state().unwrap_or_default();
    let currently_available_paths = existing_target_paths(&config);
    let available_paths = effective_available_target_paths(
        &config,
        &currently_available_paths,
        loaded,
        &runtime_state,
    );
    let resolved_count = if loaded { read_resolved_count() } else { 0 };
    if !should_watch_late_targets(&config, loaded, available_paths.len(), resolved_count) {
        return Ok(false);
    }
    if available_paths.is_empty() {
        return Ok(true);
    }

    if config.use_app_scope {
        let uid_resolution = resolve_deny_uids(&config.app_packages);
        if uid_resolution.authoritative && uid_resolution.uids.is_empty() {
            return Ok(true);
        }
    }

    let observed_available = target_path_set(&runtime_state.observation.available_target_paths);
    let newly_available = available_paths
        .iter()
        .any(|path| !observed_available.contains(path.as_str()));
    let runtime_config_changed =
        runtime_state.observation.config_key != runtime_config_key(&config);
    if !loaded || resolved_count == 0 || newly_available || runtime_config_changed {
        append_log(format!(
            "late target availability changed: active={}/{}, available={}/{}; {} pathmask",
            resolved_count,
            runtime_state.observation.available_target_paths.len(),
            available_paths.len(),
            config.target_paths.len(),
            if loaded { "reloading" } else { "loading" }
        ));
        apply_inner(false)?;
    }

    Ok(late_target_watcher_should_continue())
}

fn acquire_package_uid_watcher_lock() -> Result<File> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    let file = OpenOptions::new()
        .create(true)
        .truncate(false)
        .read(true)
        .write(true)
        .open(PACKAGE_UID_WATCHER_LOCK_PATH)
        .context("failed to open package UID watcher lock")?;
    let result = unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) };
    if result != 0 {
        return Err(coded_error(
            "pathmask.package_uid_watcher_active",
            "a package UID watcher is already running",
        ));
    }
    Ok(file)
}

fn acquire_late_target_watcher_lock() -> Result<File> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    let file = OpenOptions::new()
        .create(true)
        .truncate(false)
        .read(true)
        .write(true)
        .open(LATE_TARGET_WATCHER_LOCK_PATH)
        .context("failed to open late target watcher lock")?;
    let result = unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) };
    if result != 0 {
        return Err(coded_error(
            "pathmask.late_target_watcher_active",
            "a late target watcher is already running",
        ));
    }
    Ok(file)
}

impl PackageChangeMonitor {
    fn new() -> io::Result<Self> {
        let fd = unsafe { libc::inotify_init1(libc::IN_CLOEXEC | libc::IN_NONBLOCK) };
        if fd < 0 {
            return Err(io::Error::last_os_error());
        }
        let file = unsafe { File::from_raw_fd(fd) };
        let mut monitor = Self {
            file,
            package_system_watch: None,
            pathmask_config_watch: None,
        };
        let mask = libc::IN_CREATE
            | libc::IN_DELETE
            | libc::IN_MOVED_FROM
            | libc::IN_MOVED_TO
            | libc::IN_CLOSE_WRITE
            | libc::IN_ATTRIB
            | libc::IN_DELETE_SELF
            | libc::IN_MOVE_SELF;

        let system_path = Path::new("/data/system");
        monitor.package_system_watch = monitor.add_watch(system_path, mask);
        let mut watch_count = usize::from(monitor.package_system_watch.is_some());
        monitor.pathmask_config_watch = monitor.add_watch(Path::new(PATHMASK_DIR), mask);
        watch_count += usize::from(monitor.pathmask_config_watch.is_some());
        for path in package_data_watch_paths() {
            if monitor.add_watch(&path, mask).is_some() {
                watch_count += 1;
            }
        }
        if watch_count == 0 {
            return Err(io::Error::new(
                io::ErrorKind::NotFound,
                "no Android package database path is available",
            ));
        }
        Ok(monitor)
    }

    fn add_watch(&self, path: &Path, mask: u32) -> Option<i32> {
        let path = CString::new(path.to_string_lossy().as_bytes()).ok()?;
        let watch = unsafe { libc::inotify_add_watch(self.file.as_raw_fd(), path.as_ptr(), mask) };
        (watch >= 0).then_some(watch)
    }

    fn wait_for_change(&self, timeout_ms: i32) -> io::Result<bool> {
        let mut poll_fd = libc::pollfd {
            fd: self.file.as_raw_fd(),
            events: libc::POLLIN,
            revents: 0,
        };
        let result = unsafe { libc::poll(&raw mut poll_fd, 1, timeout_ms) };
        if result < 0 {
            let error = io::Error::last_os_error();
            return if error.kind() == io::ErrorKind::Interrupted {
                Ok(false)
            } else {
                Err(error)
            };
        }
        if result == 0 {
            return Ok(false);
        }
        if poll_fd.revents & (libc::POLLERR | libc::POLLHUP | libc::POLLNVAL) != 0 {
            return Err(io::Error::other("inotify poll descriptor became invalid"));
        }
        self.read_relevant_events()
    }

    fn read_relevant_events(&self) -> io::Result<bool> {
        let mut relevant = false;
        let mut buffer = [0_u8; 16 * 1024];
        loop {
            let count = unsafe {
                libc::read(
                    self.file.as_raw_fd(),
                    buffer.as_mut_ptr().cast(),
                    buffer.len(),
                )
            };
            if count < 0 {
                let error = io::Error::last_os_error();
                return match error.kind() {
                    io::ErrorKind::WouldBlock => Ok(relevant),
                    io::ErrorKind::Interrupted => continue,
                    _ => Err(error),
                };
            }
            if count == 0 {
                return Ok(relevant);
            }
            relevant |= self.buffer_contains_relevant_event(&buffer[..count as usize]);
        }
    }

    fn buffer_contains_relevant_event(&self, buffer: &[u8]) -> bool {
        let header_size = std::mem::size_of::<libc::inotify_event>();
        let mut offset = 0;
        while offset + header_size <= buffer.len() {
            let event = unsafe {
                std::ptr::read_unaligned(buffer[offset..].as_ptr().cast::<libc::inotify_event>())
            };
            let name_start = offset + header_size;
            let name_end = name_start
                .saturating_add(event.len as usize)
                .min(buffer.len());
            let name = buffer[name_start..name_end]
                .split(|byte| *byte == 0)
                .next()
                .unwrap_or_default();
            let relevant = if Some(event.wd) == self.package_system_watch {
                name.is_empty() || name.starts_with(b"packages.list")
            } else if Some(event.wd) == self.pathmask_config_watch {
                name.is_empty() || name == b"config.json" || name == b"unload_boot_id"
            } else {
                true
            };
            if relevant {
                return true;
            }
            offset = name_end;
        }
        false
    }
}

impl LateTargetChangeMonitor {
    fn new(config: &PathmaskConfig) -> io::Result<Self> {
        let fd = unsafe { libc::inotify_init1(libc::IN_CLOEXEC | libc::IN_NONBLOCK) };
        if fd < 0 {
            return Err(io::Error::last_os_error());
        }
        let file = unsafe { File::from_raw_fd(fd) };
        let mut monitor = Self {
            file,
            pathmask_config_watch: None,
            target_names_by_watch: BTreeMap::new(),
        };
        let mask = libc::IN_CREATE
            | libc::IN_DELETE
            | libc::IN_MOVED_FROM
            | libc::IN_MOVED_TO
            | libc::IN_CLOSE_WRITE
            | libc::IN_ATTRIB
            | libc::IN_DELETE_SELF
            | libc::IN_MOVE_SELF;

        monitor.pathmask_config_watch = monitor.add_watch(Path::new(PATHMASK_DIR), mask);
        for (parent, name) in late_target_watch_specs(config) {
            if let Some(watch) = monitor.add_watch(&parent, mask) {
                monitor
                    .target_names_by_watch
                    .entry(watch)
                    .or_default()
                    .insert(name);
            }
        }
        if monitor.pathmask_config_watch.is_none() && monitor.target_names_by_watch.is_empty() {
            return Err(io::Error::new(
                io::ErrorKind::NotFound,
                "no pathmask target parent is available",
            ));
        }
        Ok(monitor)
    }

    fn add_watch(&self, path: &Path, mask: u32) -> Option<i32> {
        let path = CString::new(path.to_string_lossy().as_bytes()).ok()?;
        let watch = unsafe { libc::inotify_add_watch(self.file.as_raw_fd(), path.as_ptr(), mask) };
        (watch >= 0).then_some(watch)
    }

    fn wait_for_change(&self, timeout_ms: i32) -> io::Result<bool> {
        let mut poll_fd = libc::pollfd {
            fd: self.file.as_raw_fd(),
            events: libc::POLLIN,
            revents: 0,
        };
        let result = unsafe { libc::poll(&raw mut poll_fd, 1, timeout_ms) };
        if result < 0 {
            let error = io::Error::last_os_error();
            return if error.kind() == io::ErrorKind::Interrupted {
                Ok(false)
            } else {
                Err(error)
            };
        }
        if result == 0 {
            return Ok(false);
        }
        if poll_fd.revents & (libc::POLLERR | libc::POLLHUP | libc::POLLNVAL) != 0 {
            return Err(io::Error::other("inotify poll descriptor became invalid"));
        }
        self.read_relevant_events()
    }

    fn read_relevant_events(&self) -> io::Result<bool> {
        let mut relevant = false;
        let mut buffer = [0_u8; 16 * 1024];
        loop {
            let count = unsafe {
                libc::read(
                    self.file.as_raw_fd(),
                    buffer.as_mut_ptr().cast(),
                    buffer.len(),
                )
            };
            if count < 0 {
                let error = io::Error::last_os_error();
                return match error.kind() {
                    io::ErrorKind::WouldBlock => Ok(relevant),
                    io::ErrorKind::Interrupted => continue,
                    _ => Err(error),
                };
            }
            if count == 0 {
                return Ok(relevant);
            }
            relevant |= self.buffer_contains_relevant_event(&buffer[..count as usize]);
        }
    }

    fn buffer_contains_relevant_event(&self, buffer: &[u8]) -> bool {
        let header_size = std::mem::size_of::<libc::inotify_event>();
        let mut offset = 0;
        while offset + header_size <= buffer.len() {
            let event = unsafe {
                std::ptr::read_unaligned(buffer[offset..].as_ptr().cast::<libc::inotify_event>())
            };
            let name_start = offset + header_size;
            let name_end = name_start
                .saturating_add(event.len as usize)
                .min(buffer.len());
            let name = buffer[name_start..name_end]
                .split(|byte| *byte == 0)
                .next()
                .unwrap_or_default();
            let relevant = if Some(event.wd) == self.pathmask_config_watch {
                name.is_empty() || name == b"config.json" || name == b"unload_boot_id"
            } else if let Some(target_names) = self.target_names_by_watch.get(&event.wd) {
                name.is_empty()
                    || target_names
                        .iter()
                        .any(|target_name| name == target_name.as_bytes())
            } else {
                false
            };
            if relevant {
                return true;
            }
            offset = name_end;
        }
        false
    }
}

fn late_target_watch_specs(config: &PathmaskConfig) -> Vec<(PathBuf, String)> {
    let mut specs = BTreeSet::new();
    for target in &config.target_paths {
        if let Some(spec) = nearest_existing_target_watch(Path::new(target)) {
            specs.insert(spec);
        }
    }
    specs.into_iter().collect()
}

fn nearest_existing_target_watch(target: &Path) -> Option<(PathBuf, String)> {
    let mut watched_name = target.file_name()?.to_string_lossy().into_owned();
    let mut parent = target.parent()?.to_path_buf();
    loop {
        if parent.is_dir() {
            return Some((parent, watched_name));
        }
        watched_name = parent.file_name()?.to_string_lossy().into_owned();
        parent = parent.parent()?.to_path_buf();
    }
}

fn package_data_watch_paths() -> Vec<PathBuf> {
    let mut paths = BTreeSet::new();
    for root in USER_DATA_ROOTS.iter().map(Path::new) {
        paths.insert(root.to_path_buf());
        let Ok(users) = fs::read_dir(root) else {
            continue;
        };
        for user in users.flatten() {
            if user.file_name().to_string_lossy().parse::<u32>().is_ok() {
                paths.insert(user.path());
            }
        }
    }
    paths.into_iter().collect()
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
    ensure_runtime_watchers();
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
    let scope_params = build_scope_params(&config)?;
    let load_target_paths = wait_for_target_paths(&config).map_err(|error| {
        coded_error(
            "pathmask.rollback_targets_not_mounted",
            format!("committed config cannot be restored: {error:#}"),
        )
    })?;
    let load_target_count = load_target_paths.len();
    let module_params =
        build_module_params(&config, &load_target_paths, &scope_params.module_params)?;
    append_log(format!("restoring committed pathmask config for KMI {kmi}"));
    load_pathmask(ko_data, &module_params)?;
    let (resolved_count, active_target_paths) = read_loaded_target_state().map_err(|error| {
        coded_error(
            "pathmask.rollback_incomplete",
            format!("rollback runtime state could not be read: {error:#}"),
        )
    })?;
    verify_loaded_target_count(resolved_count, load_target_count).map_err(|error| {
        coded_error(
            "pathmask.rollback_incomplete",
            format!("rollback did not resolve any mounted target path: {error:#}"),
        )
    })?;
    let available_paths =
        merge_available_target_paths(&config, &load_target_paths, &existing_target_paths(&config));
    let available_count = available_paths.len();
    let phase = loaded_phase(&config, available_paths.len(), resolved_count);
    let observation = runtime_observation(
        &config,
        &available_paths,
        &active_target_paths,
        resolved_count,
        &scope_params.uid_resolution,
    );
    if resolved_count < available_count {
        let message = format!(
            "rollback resolved {resolved_count} of {available_count} currently resolvable target paths"
        );
        write_runtime_state_with_observation(
            phase,
            "pathmask.partial_resolution",
            &message,
            false,
            Some(&observation),
        );
        append_log(format!(
            "rollback restored with partial resolution: resolved_count={resolved_count}, available_before_load={available_count}"
        ));
    } else {
        write_runtime_state_with_observation(phase, "", "", false, Some(&observation));
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

fn wait_for_target_paths(config: &PathmaskConfig) -> Result<Vec<String>> {
    let rounds = TARGET_WAIT_SECONDS * 1000 / TARGET_WAIT_STEP_MS;
    for _ in 0..=rounds {
        let existing = existing_target_paths(config);
        if !existing.is_empty() {
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
        .filter(|path| kernel_can_resolve_target(path))
        .cloned()
        .collect()
}

fn missing_target_paths(config: &PathmaskConfig, available_paths: &[String]) -> Vec<String> {
    let available = target_path_set(available_paths);
    config
        .target_paths
        .iter()
        .filter(|path| !available.contains(path.as_str()))
        .cloned()
        .collect()
}

fn merge_available_target_paths(
    config: &PathmaskConfig,
    first: &[String],
    second: &[String],
) -> Vec<String> {
    let mut available = target_path_set(first);
    available.extend(second.iter().map(String::as_str));
    config
        .target_paths
        .iter()
        .filter(|path| available.contains(path.as_str()))
        .cloned()
        .collect()
}

fn effective_available_target_paths(
    config: &PathmaskConfig,
    currently_available_paths: &[String],
    loaded: bool,
    runtime_state: &PathmaskRuntimeState,
) -> Vec<String> {
    let observation = &runtime_state.observation;
    if !loaded || config.use_app_scope || observation.config_key != runtime_config_key(config) {
        return currently_available_paths.to_vec();
    }

    merge_available_target_paths(
        config,
        currently_available_paths,
        &observation.available_target_paths,
    )
}

fn kernel_can_resolve_target(path: &str) -> bool {
    // KernelSU's sucompat fakes stat/access for /system/bin/su. O_PATH is not
    // rewritten and therefore mirrors the path lookup used by the LKM's
    // kern_path() much more closely than Path::exists().
    open(path, OFlags::PATH | OFlags::CLOEXEC, Mode::empty()).is_ok()
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
        .split([',', '\n', '\r', '\0'])
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
    uid_resolution: &DenyUidResolution,
) -> PathmaskRuntimeObservation {
    PathmaskRuntimeObservation {
        config_key: runtime_config_key(config),
        configured_target_paths: config.target_paths.clone(),
        available_target_paths: available_paths.to_vec(),
        resolved_target_paths: parse_target_paths(active_target_paths),
        resolved_count,
        uid_resolution_key: uid_resolution_key(uid_resolution),
        resolved_app_uids: uid_resolution.uids.iter().copied().collect(),
        unresolved_app_packages: uid_resolution.unresolved_packages.clone(),
    }
}

fn uid_resolution_key(resolution: &DenyUidResolution) -> String {
    json!({
        "resolvedAppUids": resolution.uids,
        "unresolvedAppPackages": resolution.unresolved_packages,
    })
    .to_string()
}

fn uid_mapping_requires_reload(
    observation: &PathmaskRuntimeObservation,
    resolution: &DenyUidResolution,
) -> bool {
    resolution.authoritative
        && (observation.uid_resolution_key.is_empty()
            || observation.resolved_app_uids != resolution.uids.iter().copied().collect::<Vec<_>>())
}

fn update_uid_observation(
    observation: &mut PathmaskRuntimeObservation,
    resolution: &DenyUidResolution,
) {
    observation.uid_resolution_key = uid_resolution_key(resolution);
    observation.resolved_app_uids = resolution.uids.iter().copied().collect();
    observation
        .unresolved_app_packages
        .clone_from(&resolution.unresolved_packages);
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
    if resolved_count < available_paths.len() {
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
        // Older state files do not contain the pre-load visibility snapshot.
        // App-scoped hiding leaves root probing intact, but global hiding does
        // not. Reload global mode once so a reliable snapshot can be recorded.
        return !config.use_app_scope;
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
    _config: &PathmaskConfig,
    available_count: usize,
    resolved_count: usize,
) -> &'static str {
    if resolved_count < available_count {
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
    } else if loaded && reload_required {
        ReconcileAction::Reload
    } else if available_count == 0 {
        ReconcileAction::WaitForTargets
    } else if !loaded {
        ReconcileAction::Load
    } else if resolved_count == 0 {
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
    let runtime_state = read_runtime_state().unwrap_or_default();
    let uid_resolution = if config.use_app_scope {
        resolve_deny_uids(&config.app_packages)
    } else {
        DenyUidResolution::default()
    };
    let currently_available_paths = existing_target_paths(config);
    let available_paths = effective_available_target_paths(
        config,
        &currently_available_paths,
        loaded,
        &runtime_state,
    );
    let available_count = available_paths.len();
    let resolved_count = read_resolved_count();
    let active_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let active_matches = active_target_paths_match(config, &available_paths, &active_paths);
    let reload_required = loaded
        && ((config.use_app_scope
            && uid_mapping_requires_reload(&runtime_state.observation, &uid_resolution))
            || !active_matches
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
    } else if loaded && requires_reload {
        "reload_required"
    } else if available_count == 0 {
        "waiting_targets"
    } else if !loaded {
        "waiting_load"
    } else if resolved_count < available_count {
        "partial"
    } else {
        "active"
    }
}

fn build_module_params(
    config: &PathmaskConfig,
    load_target_paths: &[String],
    scope_params: &[String],
) -> Result<String> {
    validate_config(config)?;

    if load_target_paths.is_empty() {
        return Err(coded_error(
            "pathmask.targets_not_mounted",
            "no configured target path can currently be resolved by the kernel",
        ));
    }
    let target_paths = load_target_paths.join(",");
    if target_paths.len() > MAX_TARGET_PATHS_LEN {
        bail!("target_paths is too long");
    }

    let mut params = vec![
        format!("target_paths={}", quote_module_param_value(&target_paths)),
        format!("hide_dirents={}", bool_param(config.hide_dirents)),
        format!("hide_isolated={}", bool_param(config.hide_isolated)),
    ];
    params.extend(scope_params.iter().cloned());

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

fn build_scope_params(config: &PathmaskConfig) -> Result<ScopeParams> {
    validate_config(config)?;
    ensure_safe_scope(config)?;

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
        Ok(ScopeParams {
            module_params: vec![
                "scope_mode=deny".to_string(),
                format!("deny_uids={deny_uids}"),
            ],
            uid_resolution: resolution,
        })
    } else {
        Ok(ScopeParams {
            module_params: vec!["scope_mode=global".to_string()],
            uid_resolution: DenyUidResolution::default(),
        })
    }
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
    resolve_deny_uids_from_sources(app_packages, package_uids.as_ref(), |package, base_uid| {
        resolve_package_uids_for_all_users(
            package,
            base_uid.or_else(|| resolve_uid_from_data_dir(package)),
        )
    })
}

fn resolve_deny_uids_from_sources<F>(
    app_packages: &[String],
    package_uids: Option<&BTreeMap<String, u32>>,
    resolve_package_uids: F,
) -> DenyUidResolution
where
    F: Fn(&str, Option<u32>) -> BTreeSet<u32>,
{
    let mut resolution = DenyUidResolution {
        authoritative: package_uids.is_some()
            || app_packages.iter().all(|app| app.parse::<u32>().is_ok()),
        ..DenyUidResolution::default()
    };
    for app in app_packages {
        if let Ok(uid) = app.parse::<u32>() {
            resolution.uids.insert(uid);
            continue;
        }

        let base_uid = match package_uids.as_ref() {
            Some(package_uids) => {
                let Some(uid) = package_uids.get(app).copied() else {
                    // packages.list is authoritative once PackageManager is
                    // available. A leftover /data/user directory must not keep
                    // an uninstalled package's recycled UID in the kernel.
                    resolution.unresolved_packages.push(app.clone());
                    continue;
                };
                Some(uid)
            }
            None => None,
        };
        let resolved_uids = resolve_package_uids(app, base_uid);
        if resolved_uids.is_empty() {
            resolution.unresolved_packages.push(app.clone());
        } else {
            resolution.uids.extend(resolved_uids);
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

fn read_package_uids() -> Option<BTreeMap<String, u32>> {
    let contents = fs::read_to_string(PACKAGES_LIST_PATH).ok()?;
    let package_uids = parse_package_uids(&contents);
    (!package_uids.is_empty()).then_some(package_uids)
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
    if let Some(path) = config
        .target_paths
        .iter()
        .find(|path| path.len() > MAX_TARGET_PATH_BYTES)
    {
        return Err(coded_error(
            "pathmask.target_too_long",
            format!(
                "target path is {} bytes; the pathmask LKM limit is {MAX_TARGET_PATH_BYTES}: {path}",
                path.len()
            ),
        ));
    }
    let target_paths_length = config.target_paths.iter().map(String::len).sum::<usize>()
        + config.target_paths.len().saturating_sub(1);
    if target_paths_length > MAX_TARGET_PATHS_LEN {
        return Err(coded_error(
            "pathmask.path_parameter_too_long",
            format!(
                "combined target path parameter is {target_paths_length} bytes; maximum is {MAX_TARGET_PATHS_LEN}"
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

fn read_config_for_status() -> Result<PathmaskConfig> {
    if !Path::new(CONFIG_PATH).exists() {
        return Ok(PathmaskConfig::default());
    }

    let content =
        fs::read_to_string(CONFIG_PATH).with_context(|| format!("failed to read {CONFIG_PATH}"))?;
    parse_config_unvalidated(&content)
}

fn read_config_from_path(path: &str) -> Result<PathmaskConfig> {
    if !Path::new(path).exists() {
        return Ok(PathmaskConfig::default());
    }

    let content = fs::read_to_string(path).with_context(|| format!("failed to read {path}"))?;
    parse_config(&content)
}

fn parse_config(content: &str) -> Result<PathmaskConfig> {
    let config = parse_config_unvalidated(content)?;
    validate_config(&config)?;
    Ok(config)
}

fn parse_config_unvalidated(content: &str) -> Result<PathmaskConfig> {
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

fn u32_array(value: Option<&Value>) -> Vec<u32> {
    value
        .and_then(Value::as_array)
        .map(|array| {
            array
                .iter()
                .filter_map(Value::as_u64)
                .filter_map(|uid| u32::try_from(uid).ok())
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
        "uidResolutionKey": observation.uid_resolution_key,
        "resolvedAppUids": observation.resolved_app_uids,
        "unresolvedAppPackages": observation.unresolved_app_packages,
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
            uid_resolution_key: value
                .get("uidResolutionKey")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .to_owned(),
            resolved_app_uids: u32_array(value.get("resolvedAppUids")),
            unresolved_app_packages: string_array(value.get("unresolvedAppPackages")),
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
    use std::{
        collections::{BTreeMap, BTreeSet},
        fs,
    };

    use super::{
        DenyUidResolution, LateTargetChangeMonitor, MAX_AUTO_LOAD_DELAY_SECONDS, MAX_TARGET_PATHS,
        PackageUidRefreshAction, PathmaskConfig, PathmaskRuntimeObservation, PathmaskRuntimeState,
        ReconcileAction, coded_error, combine_operation_and_rollback_error,
        commit_candidate_after_verification, derive_phase, effective_available_target_paths,
        error_code, error_requires_reboot, loaded_phase, nearest_existing_target_watch,
        package_uid_refresh_action, package_uid_refresh_error_is_retryable, parse_config,
        parse_config_unvalidated, parse_package_uids, parse_target_paths, read_runtime_state_from,
        reconcile_action, remaining_delay_seconds, resolve_deny_uids_from_sources,
        resolve_package_uids_from_roots, runtime_config_key, runtime_requires_reload,
        should_watch_late_targets, uid_resolution_key, unresolved_target_paths, validate_config,
        verify_loaded_target_count,
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

    fn test_uid_resolution(uids: &[u32], unresolved: &[&str]) -> DenyUidResolution {
        DenyUidResolution {
            uids: uids.iter().copied().collect(),
            unresolved_packages: unresolved.iter().map(ToString::to_string).collect(),
            authoritative: true,
        }
    }

    fn test_app_scope_config() -> PathmaskConfig {
        PathmaskConfig {
            target_paths: vec!["/data/adb/modules".to_owned()],
            app_packages: vec!["com.example.hidden".to_owned()],
            ..PathmaskConfig::default()
        }
    }

    fn test_uid_observation(
        config: &PathmaskConfig,
        resolution: &DenyUidResolution,
    ) -> PathmaskRuntimeObservation {
        PathmaskRuntimeObservation {
            config_key: runtime_config_key(config),
            uid_resolution_key: uid_resolution_key(resolution),
            resolved_app_uids: resolution.uids.iter().copied().collect(),
            unresolved_app_packages: resolution.unresolved_packages.clone(),
            ..PathmaskRuntimeObservation::default()
        }
    }

    #[test]
    fn authoritative_package_list_ignores_uninstalled_package_data_directory() {
        let packages = BTreeMap::new();
        let resolution = resolve_deny_uids_from_sources(
            &["com.example.removed".to_owned()],
            Some(&packages),
            |_package, _base_uid| BTreeSet::from([10_384]),
        );

        assert!(resolution.authoritative);
        assert!(resolution.uids.is_empty());
        assert_eq!(resolution.unresolved_packages, ["com.example.removed"]);
    }

    #[test]
    fn package_data_fallback_is_not_authoritative_without_package_list() {
        let resolution = resolve_deny_uids_from_sources(
            &["com.example.hidden".to_owned()],
            None,
            |_package, _base_uid| BTreeSet::from([10_384]),
        );

        assert!(!resolution.authoritative);
        assert_eq!(resolution.uids, BTreeSet::from([10_384]));
    }

    #[test]
    fn unchanged_package_mapping_does_not_reload_for_unrelated_install() {
        let config = test_app_scope_config();
        let resolution = test_uid_resolution(&[10_384], &[]);
        let observation = test_uid_observation(&config, &resolution);

        assert_eq!(
            package_uid_refresh_action(&config, true, false, &observation, &resolution),
            PackageUidRefreshAction::Noop,
        );
    }

    #[test]
    fn changed_or_recycled_package_uid_refreshes_loaded_scope() {
        let config = test_app_scope_config();
        let old_resolution = test_uid_resolution(&[10_384], &[]);
        let observation = test_uid_observation(&config, &old_resolution);
        let moved_resolution = test_uid_resolution(&[10_385], &[]);
        let removed_resolution = test_uid_resolution(&[], &["com.example.hidden"]);

        assert_eq!(
            package_uid_refresh_action(&config, true, false, &observation, &moved_resolution,),
            PackageUidRefreshAction::Reload,
        );
        assert_eq!(
            package_uid_refresh_action(&config, true, false, &observation, &removed_resolution,),
            PackageUidRefreshAction::Unload,
        );
    }

    #[test]
    fn installed_configured_package_loads_waiting_scope() {
        let config = test_app_scope_config();
        let absent = test_uid_resolution(&[], &["com.example.hidden"]);
        let waiting_observation = test_uid_observation(&config, &absent);
        let installed = test_uid_resolution(&[10_385], &[]);

        assert_eq!(
            package_uid_refresh_action(&config, false, false, &waiting_observation, &installed,),
            PackageUidRefreshAction::Load,
        );
    }

    #[test]
    fn disabled_global_and_manually_unloaded_scopes_stop_watcher() {
        let config = test_app_scope_config();
        let resolution = test_uid_resolution(&[10_384], &[]);
        let observation = test_uid_observation(&config, &resolution);
        let disabled = PathmaskConfig {
            auto_load_enabled: false,
            ..config.clone()
        };
        let global = PathmaskConfig {
            use_app_scope: false,
            ..config.clone()
        };

        for candidate in [&disabled, &global] {
            assert_eq!(
                package_uid_refresh_action(candidate, true, false, &observation, &resolution),
                PackageUidRefreshAction::Stop,
            );
        }
        assert_eq!(
            package_uid_refresh_action(&config, false, true, &observation, &resolution),
            PackageUidRefreshAction::Stop,
        );
    }

    #[test]
    fn package_uid_refresh_retries_only_transient_runtime_failures() {
        for code in [
            "pathmask.operation_busy",
            "pathmask.module_busy",
            "pathmask.load_failed",
            "pathmask.resolved_count_unavailable",
        ] {
            assert!(package_uid_refresh_error_is_retryable(&coded_error(
                code, "retry"
            )));
        }
        assert!(!package_uid_refresh_error_is_retryable(&coded_error(
            "pathmask.invalid_json",
            "do not retry"
        )));
    }

    #[test]
    fn late_target_watcher_waits_until_every_saved_target_is_active() {
        let mut config = test_app_scope_config();
        config.target_paths.push("/data/adb/ksu".to_owned());

        assert!(should_watch_late_targets(&config, false, 0, 0));
        assert!(should_watch_late_targets(&config, true, 1, 1));
        assert!(should_watch_late_targets(&config, true, 2, 1));
        assert!(!should_watch_late_targets(&config, true, 2, 2));

        config.auto_load_enabled = false;
        assert!(!should_watch_late_targets(&config, false, 0, 0));
        config.auto_load_enabled = true;
        config.target_paths.clear();
        assert!(!should_watch_late_targets(&config, false, 0, 0));
    }

    #[test]
    fn late_target_watch_uses_nearest_existing_parent() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let mounted = temp.path().join("mounted");
        fs::create_dir_all(&mounted).expect("create mounted parent");
        let target = mounted.join("late").join("target");

        let (watch_path, watched_name) =
            nearest_existing_target_watch(&target).expect("find existing parent");

        assert_eq!(watch_path, mounted);
        assert_eq!(watched_name, "late");
    }

    #[test]
    fn late_target_monitor_follows_nested_path_creation() {
        let temp = tempfile::tempdir().expect("create temp directory");
        let target = temp.path().join("late").join("target");
        let config = PathmaskConfig {
            target_paths: vec![target.to_string_lossy().into_owned()],
            ..PathmaskConfig::default()
        };

        let parent_monitor =
            LateTargetChangeMonitor::new(&config).expect("watch existing ancestor");
        fs::create_dir(temp.path().join("late")).expect("create late parent");
        assert!(
            parent_monitor
                .wait_for_change(1_000)
                .expect("read parent event")
        );

        let target_monitor =
            LateTargetChangeMonitor::new(&config).expect("watch newly created parent");
        fs::write(&target, b"ready").expect("create target");
        assert!(
            target_monitor
                .wait_for_change(1_000)
                .expect("read target event")
        );
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
    fn target_path_slot_limit_is_enforced() {
        let path = format!("/{}", "a".repeat(super::MAX_TARGET_PATH_BYTES));
        let error = parse_config(&format!(
            r#"{{"targetPaths":["{path}"],"appPackages":["com.example.app"]}}"#
        ))
        .expect_err("a path larger than the LKM slot must be rejected");

        assert_eq!(error_code(&error), "pathmask.target_too_long");
    }

    #[test]
    fn legacy_oversized_config_remains_readable_for_status_and_editing() {
        let paths = (0..=MAX_TARGET_PATHS)
            .map(|index| format!(r#""/data/local/tmp/path-{index}""#))
            .collect::<Vec<_>>()
            .join(",");
        let config = parse_config_unvalidated(&format!(
            r#"{{"targetPaths":[{paths}],"appPackages":["com.example.app"]}}"#
        ))
        .expect("legacy config should remain readable without applying it");

        assert_eq!(config.target_paths.len(), MAX_TARGET_PATHS + 1);
        let error = validate_config(&config).expect_err("oversized config must not be applied");
        assert_eq!(error_code(&error), "pathmask.too_many_targets");
    }

    #[test]
    fn combined_target_parameter_limit_is_enforced_before_loading() {
        let config = PathmaskConfig {
            target_paths: (0..MAX_TARGET_PATHS)
                .map(|index| format!("/data/local/tmp/{index}-{}", "a".repeat(110)))
                .collect(),
            app_packages: vec!["com.example.app".to_owned()],
            ..PathmaskConfig::default()
        };

        let error = validate_config(&config).expect_err("oversized parameter must be rejected");
        assert_eq!(error_code(&error), "pathmask.path_parameter_too_long");
    }

    #[test]
    fn sysfs_target_paths_accept_commas_newlines_and_nul_padding() {
        assert_eq!(
            parse_target_paths("/data/adb/ksu, /data/adb/modules\n/data/adb/ap\0\0"),
            vec!["/data/adb/ap", "/data/adb/ksu", "/data/adb/modules"]
        );
    }

    #[test]
    fn unavailable_saved_targets_do_not_make_loaded_runtime_partial() {
        let config = PathmaskConfig {
            target_paths: vec!["/available".to_owned(), "/not-mounted".to_owned()],
            app_packages: vec!["com.example.app".to_owned()],
            ..PathmaskConfig::default()
        };

        assert_eq!(loaded_phase(&config, 1, 1), "active");
        assert_eq!(loaded_phase(&config, 1, 0), "partial");
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
    fn unavailable_configured_path_does_not_reload_until_it_appears() {
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
            ..PathmaskRuntimeObservation::default()
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
    fn mounted_but_unresolved_target_requests_retry() {
        let config = PathmaskConfig {
            target_paths: vec!["/first".to_owned(), "/second".to_owned()],
            app_packages: vec!["com.example.app".to_owned()],
            ..PathmaskConfig::default()
        };
        let available = config.target_paths.clone();
        let state = PathmaskRuntimeState {
            observation: PathmaskRuntimeObservation {
                config_key: runtime_config_key(&config),
                configured_target_paths: config.target_paths.clone(),
                available_target_paths: available.clone(),
                resolved_target_paths: vec!["/first".to_owned()],
                resolved_count: 1,
                ..PathmaskRuntimeObservation::default()
            },
            ..PathmaskRuntimeState::default()
        };

        assert!(runtime_requires_reload(
            &config,
            &available,
            "/first,/second",
            1,
            &state,
        ));
    }

    #[test]
    fn loaded_runtime_keeps_preload_visibility_when_global_scope_hides_root() {
        let config = PathmaskConfig {
            target_paths: vec!["/first".to_owned(), "/late".to_owned()],
            use_app_scope: false,
            ..PathmaskConfig::default()
        };
        let state = PathmaskRuntimeState {
            observation: PathmaskRuntimeObservation {
                config_key: runtime_config_key(&config),
                configured_target_paths: config.target_paths.clone(),
                available_target_paths: vec!["/first".to_owned()],
                resolved_target_paths: vec!["/first".to_owned()],
                resolved_count: 1,
                ..PathmaskRuntimeObservation::default()
            },
            ..PathmaskRuntimeState::default()
        };

        assert_eq!(
            effective_available_target_paths(&config, &[], true, &state),
            vec!["/first"]
        );
        assert_eq!(
            effective_available_target_paths(&config, &["/late".to_owned()], true, &state,),
            vec!["/first", "/late"]
        );

        let app_scoped = PathmaskConfig {
            use_app_scope: true,
            ..config
        };
        let app_state = PathmaskRuntimeState {
            observation: PathmaskRuntimeObservation {
                config_key: runtime_config_key(&app_scoped),
                configured_target_paths: app_scoped.target_paths.clone(),
                available_target_paths: vec!["/first".to_owned()],
                resolved_target_paths: vec!["/first".to_owned()],
                resolved_count: 1,
                ..PathmaskRuntimeObservation::default()
            },
            ..PathmaskRuntimeState::default()
        };
        assert!(effective_available_target_paths(&app_scoped, &[], true, &app_state).is_empty());
    }

    #[test]
    fn legacy_global_runtime_reloads_once_to_capture_visibility() {
        let config = PathmaskConfig {
            target_paths: vec!["/first".to_owned()],
            use_app_scope: false,
            ..PathmaskConfig::default()
        };

        assert!(runtime_requires_reload(
            &config,
            &[],
            "/first",
            1,
            &PathmaskRuntimeState::default(),
        ));
        assert_eq!(
            reconcile_action(true, true, 0, 1, true),
            ReconcileAction::Reload,
        );
        assert_eq!(
            derive_phase(&config, true, 0, 1, true, false),
            "reload_required",
        );
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
