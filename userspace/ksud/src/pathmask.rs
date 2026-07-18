use anyhow::{Context, Result, bail};
use chrono::Local;
use const_format::concatcp;
use serde_json::{Value, json};
use std::{
    collections::BTreeSet,
    ffi::CString,
    fs::{self, OpenOptions},
    io::{self, Write},
    path::{Component, Path},
    process::Command,
    thread,
    time::Duration,
};

use crate::{assets, boot_patch, defs, ksucalls, utils};

const PATHMASK_DIR: &str = concatcp!(defs::WORKING_DIR, "pathmask/");
const CONFIG_PATH: &str = concatcp!(PATHMASK_DIR, "config.json");
const LAST_GOOD_CONFIG_PATH: &str = concatcp!(PATHMASK_DIR, "last_good.json");
const LOG_PATH: &str = concatcp!(PATHMASK_DIR, "pathmask.log");
const PATHMASK_MODULE_NAME: &str = "pathmask";
const MAX_TARGET_PATHS_LEN: usize = 1900;
const TARGET_WAIT_SECONDS: u64 = 5;
const TARGET_WAIT_STEP_MS: u64 = 200;
const MODULE_UNLOAD_WAIT_MS: u64 = 2000;
const MODULE_UNLOAD_STEP_MS: u64 = 100;
const MANAGED_ROOT_PATHS: &[&str] = &[
    "/data/adb/modules",
    "/data/adb/modules_update",
    "/data/adb/ksu",
    "/data/adb/ap",
];

#[derive(Clone, Debug)]
struct PathmaskConfig {
    target_paths: Vec<String>,
    app_packages: Vec<String>,
    use_app_scope: bool,
    hide_dirents: bool,
    hide_isolated: bool,
}

impl Default for PathmaskConfig {
    fn default() -> Self {
        Self {
            target_paths: Vec::new(),
            app_packages: Vec::new(),
            use_app_scope: true,
            hide_dirents: true,
            hide_isolated: true,
        }
    }
}

pub fn print_status() {
    let config = read_config().unwrap_or_default();
    let current_kmi = boot_patch::get_current_kmi().unwrap_or_default();
    let loaded = is_module_loaded();
    let resolved_count = read_sysfs_param("resolved_count").unwrap_or_default();
    let active_target_paths = read_sysfs_param("target_paths").unwrap_or_default();
    let last_log = tail_file(LOG_PATH, 40).unwrap_or_default();

    println!(
        "{}",
        json!({
            "targetPaths": config.target_paths,
            "appPackages": config.app_packages,
            "useAppScope": config.use_app_scope,
            "hideDirents": config.hide_dirents,
            "hideIsolated": config.hide_isolated,
            "loaded": loaded,
            "currentKmi": current_kmi,
            "resolvedCount": resolved_count,
            "activeTargetPaths": active_target_paths,
            "lastLog": last_log,
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
    write_config(CONFIG_PATH, &config)?;
    append_log(format!("imported config from {source}"));
    Ok(())
}

pub fn apply() -> Result<()> {
    let result = apply_inner();
    if let Err(err) = &result {
        append_log(format!("apply failed: {err:#}"));
    }
    result
}

fn apply_inner() -> Result<()> {
    ensure_pathmask_runtime_supported()?;

    let config = read_config()?;
    let module_params = build_module_params(&config)?;
    let kmi = boot_patch::get_current_kmi().context("failed to detect current KMI")?;
    let ko_name = format!("{kmi}_pathmask.ko");
    let ko_data =
        assets::get_asset_data(&ko_name).with_context(|| format!("failed to load {ko_name}"))?;

    append_log(format!("applying pathmask for KMI {kmi}: {module_params}"));

    if is_module_loaded() {
        append_log("pathmask already loaded; reload is required for new target paths");
        unload_loaded_pathmask(true).context("failed to unload old pathmask")?;
        append_log("unloaded old pathmask module");
    }

    let existing_count = match wait_for_any_target_path(&config) {
        Ok(count) => count,
        Err(error) => {
            if let Err(restore_err) = restore_last_good(&kmi, &ko_data) {
                append_log(format!("restore last good config failed: {restore_err:#}"));
            }
            return Err(error);
        }
    };
    append_log(format!(
        "target path precheck passed: {existing_count}/{} currently exists",
        config.target_paths.len()
    ));

    if let Err(err) = load_pathmask(&ko_data, &module_params) {
        append_log(format!("load failed: {err:#}"));
        if let Err(restore_err) = restore_last_good(&kmi, &ko_data) {
            append_log(format!("restore last good config failed: {restore_err:#}"));
        }
        return Err(err.context("failed to load pathmask with current config"));
    }

    let resolved_count = read_sysfs_param("resolved_count")
        .and_then(|value| value.parse::<usize>().ok())
        .unwrap_or(0);
    if resolved_count == 0 {
        append_log("pathmask loaded but resolved_count is zero; rolling back");
        let _ = unload_loaded_pathmask(true);
        if let Err(restore_err) = restore_last_good(&kmi, &ko_data) {
            append_log(format!("restore last good config failed: {restore_err:#}"));
        }
        bail!("pathmask did not resolve any configured target path");
    }

    write_config(LAST_GOOD_CONFIG_PATH, &config)?;
    append_log(format!(
        "pathmask loaded successfully, resolved_count={resolved_count}"
    ));
    Ok(())
}

pub fn apply_if_configured() {
    if !Path::new(CONFIG_PATH).exists() {
        return;
    }

    if let Err(err) = ensure_pathmask_runtime_supported() {
        append_log(format!("skip boot auto-load: {err:#}"));
        return;
    }

    if is_module_loaded() {
        append_log("skip boot auto-load: pathmask already loaded");
        return;
    }

    if let Err(err) = apply() {
        append_log(format!("boot auto-load failed: {err:#}"));
        log::warn!("pathmask: boot auto-load failed: {err:#}");
    }
}

fn ensure_pathmask_runtime_supported() -> Result<()> {
    if ksucalls::is_late_load() {
        bail!("pathmask LKM is disabled in jailbreak mode");
    }
    if !ksucalls::is_lkm_mode() {
        bail!("pathmask LKM is only available in LKM mode; use SUSFS path config in GKI mode");
    }
    Ok(())
}

pub fn unload() -> Result<()> {
    let result = unload_inner();
    if let Err(err) = &result {
        append_log(format!("unload failed: {err:#}"));
    }
    result
}

fn unload_inner() -> Result<()> {
    if !is_module_loaded() {
        append_log("pathmask unload skipped: module is not loaded");
        return Ok(());
    }

    unload_loaded_pathmask(true).context("failed to unload pathmask")?;
    append_log("pathmask unloaded; kernel hidden paths are cleared");
    Ok(())
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

pub fn clear_logs() -> Result<()> {
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    fs::write(LOG_PATH, "").with_context(|| format!("failed to clear {LOG_PATH}"))?;
    append_log("cleared manager log; kernel dmesg history is not cleared");
    Ok(())
}

fn restore_last_good(kmi: &str, ko_data: &[u8]) -> Result<()> {
    let config = read_config_from_path(LAST_GOOD_CONFIG_PATH)?;
    let module_params = build_module_params(&config)?;
    append_log(format!("restoring last good pathmask config for KMI {kmi}"));
    load_pathmask(ko_data, &module_params)?;
    Ok(())
}

fn load_pathmask(ko_data: &[u8], module_params: &str) -> Result<()> {
    let params = CString::new(module_params).context("module params contain NUL byte")?;
    ksuinit::load_module(ko_data, &params).context("init_module failed")
}

fn unload_loaded_pathmask(force_busy: bool) -> Result<()> {
    let graceful_flags = libc::O_NONBLOCK;
    match rustix::system::delete_module(c"pathmask", graceful_flags) {
        Ok(()) => wait_for_pathmask_unloaded(),
        Err(err) if force_busy && is_busy_unload_error(err) => {
            append_log(format!(
                "pathmask is busy during unload ({err}); trying forced unload"
            ));
            rustix::system::delete_module(c"pathmask", libc::O_NONBLOCK | libc::O_TRUNC)
                .context("forced unload failed")?;
            wait_for_pathmask_unloaded()
        }
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

    bail!("no configured target path exists now; pathmask cannot resolve targets at load time");
}

fn count_existing_target_paths(config: &PathmaskConfig) -> usize {
    config
        .target_paths
        .iter()
        .filter(|path| Path::new(path.as_str()).exists())
        .count()
}

fn build_module_params(config: &PathmaskConfig) -> Result<String> {
    if config.target_paths.is_empty() {
        bail!("no target path configured");
    }
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
        let deny_uids = resolve_deny_uids(&config.app_packages);
        if deny_uids.is_empty() {
            bail!("application scope is enabled but no valid app UID was resolved");
        }
        let deny_uids = deny_uids
            .iter()
            .map(u32::to_string)
            .collect::<Vec<_>>()
            .join(",");
        params.push("scope_mode=deny".to_string());
        params.push(format!("deny_uids={deny_uids}"));
    } else {
        params.push("scope_mode=global".to_string());
    }

    Ok(params.join(" "))
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

fn resolve_deny_uids(app_packages: &[String]) -> BTreeSet<u32> {
    let mut uids = BTreeSet::new();
    for app in app_packages {
        if let Ok(uid) = app.parse::<u32>() {
            uids.insert(uid);
            continue;
        }

        let package_path = format!("/data/data/{app}");
        match rustix::fs::stat(package_path.as_str()) {
            Ok(stat) => {
                uids.insert(stat.st_uid);
            }
            Err(err) => {
                append_log(format!("cannot resolve UID for {app}: {err}"));
            }
        }
    }
    uids
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
    let value: Value = serde_json::from_str(content).context("invalid pathmask config JSON")?;
    let target_paths = string_array(value.get("targetPaths"))
        .into_iter()
        .filter_map(|path| sanitize_target_path(&path))
        .collect::<Vec<_>>();
    let app_packages = string_array(value.get("appPackages"))
        .into_iter()
        .filter_map(|package| sanitize_app_entry(&package))
        .collect::<Vec<_>>();

    Ok(PathmaskConfig {
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
    })
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
    utils::ensure_dir_exists(PATHMASK_DIR)?;
    fs::write(
        path,
        json!({
            "targetPaths": config.target_paths,
            "appPackages": config.app_packages,
            "useAppScope": config.use_app_scope,
            "hideDirents": config.hide_dirents,
            "hideIsolated": config.hide_isolated,
        })
        .to_string(),
    )
    .with_context(|| format!("failed to write {path}"))
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
    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(LOG_PATH)
        .with_context(|| format!("failed to open {LOG_PATH}"))?;
    let timestamp = Local::now().format("%Y-%m-%d %H:%M:%S");
    writeln!(file, "[{timestamp}] {message}").context("failed to write pathmask log")
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
