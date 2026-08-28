use anyhow::{Context, Result, anyhow, bail};
use chrono::Local;
use const_format::concatcp;
use serde_json::{Value, json};
#[cfg(target_os = "android")]
use std::os::fd::AsRawFd;
use std::{
    collections::BTreeMap,
    fs::{self, OpenOptions},
    io::{self, Read, Write},
    path::{Component, Path, PathBuf},
    process::Command,
    time::{Duration, Instant, UNIX_EPOCH},
};

use crate::{boot_patch, defs, module, utils};

const RESCUE_DIR: &str = concatcp!(defs::WORKING_DIR, "rescue/");
const CONFIG_PATH: &str = concatcp!(RESCUE_DIR, "config.json");
const MANIFEST_PATH: &str = concatcp!(RESCUE_DIR, "manifest.json");
const ENABLED_PATH: &str = concatcp!(RESCUE_DIR, "enabled");
const LOG_PATH: &str = concatcp!(RESCUE_DIR, "rescue.log");
const BOOT_COUNT_PATH: &str = concatcp!(RESCUE_DIR, "boot_count");
const BOOT_OK_PATH: &str = concatcp!(RESCUE_DIR, "boot_ok");
const PENDING_BOOT_PATH: &str = concatcp!(RESCUE_DIR, "pending_boot");
const RESTORE_LOCK_PATH: &str = concatcp!(RESCUE_DIR, "restore_done.lock");
const RESTORE_TRANSACTION_PATH: &str = concatcp!(RESCUE_DIR, "restore_transaction.json");
const AUTO_RESTORE_ATTEMPTS_PATH: &str = concatcp!(RESCUE_DIR, "auto_restore_attempts");
const FAILURE_BASELINE_PATH: &str = concatcp!(RESCUE_DIR, "failure_baseline.json");
const VERIFIED_PATH: &str = concatcp!(RESCUE_DIR, "verified.json");
const ENVIRONMENT_CHECK_PATH: &str = concatcp!(RESCUE_DIR, "environment_check.json");
const CONFIG_CHANGED_PATH: &str = concatcp!(RESCUE_DIR, "config_changed");
const HASH_CACHE_PATH: &str = concatcp!(RESCUE_DIR, "sha256_cache.json");
const RESCUE_DISABLED_MODULES_PATH: &str = concatcp!(RESCUE_DIR, "disabled_modules.json");
const RECOVERY_CHECK_GUARD_PATH: &str = "/dev/ksu_rescue_recovery_checked";
const SKIP_MODULES_ONCE_PATH: &str = concatcp!(RESCUE_DIR, "skip_modules_once");
const CACHE_SKIP_MODULES_ONCE_PATH: &str = "/cache/ksu_rescue_skip_modules_once";
const SKIP_MODULES_THIS_BOOT_PATH: &str = "/dev/ksu_rescue_skip_modules_this_boot";
const LEGACY_FIX_DONE_LOCK_PATH: &str = "/cache/mochen_fix_done.lock";
const LEGACY_LOOP_FLAG_PATH: &str = "/cache/mochen_boot_loop_flag";
const LEGACY_PANIC_FLAG_PATH: &str = "/cache/mochen_kernel_panic";
const LEGACY_TMP_MODULE_DISABLE_PATH: &str = "/cache/tmp_modules_disable";
const MAX_AUTO_RESTORE_ATTEMPTS: u32 = 3;
const PENDING_BOOT_FAILURE_TRIGGER_COUNT: u32 = 2;
const MODULE_RESCUE_FAILURE_TRIGGER_COUNT: u32 = 2;
const LOG_MAX_BYTES: u64 = 1024 * 1024;
const LOG_ROTATION_COUNT: usize = 3;
const COPY_BUFFER_BYTES: usize = 1024 * 1024;
const COPY_IDLE_TIMEOUT_SECONDS: u64 = 45;
const COPY_TOTAL_TIMEOUT_SECONDS: u64 = 10 * 60;
const FAILURE_ARTIFACT_SCAN_BYTES: u64 = 4 * 1024 * 1024;
const FAILURE_ARTIFACT_MAX_FILES: usize = 32;
const ERROR_PREFIX: &str = "APKESU_ERROR";

#[derive(Clone, Debug)]
struct RestoreTransactionEntry {
    name: String,
    label: String,
    image_path: String,
    device_path: String,
    expected_sha256: String,
    expected_size: u64,
    status: String,
}

#[derive(Clone, Debug)]
struct RestoreTransaction {
    id: String,
    reason: String,
    automatic: bool,
    description: String,
    activate_slot: Option<String>,
    phase: String,
    error_code: String,
    error_message: String,
    started_at: String,
    updated_at: String,
    entries: Vec<RestoreTransactionEntry>,
}

fn coded_error(code: &str, message: impl AsRef<str>) -> anyhow::Error {
    anyhow!("{ERROR_PREFIX}:{code}:{}", message.as_ref())
}

pub fn structured_error(error: anyhow::Error) -> anyhow::Error {
    if format!("{error:#}").contains(ERROR_PREFIX) {
        return error;
    }
    let message = format!("{error:#}");
    let lower = message.to_ascii_lowercase();
    let code = if lower.contains("sha256") || lower.contains("checksum") {
        "rescue.checksum_mismatch"
    } else if lower.contains("partition") && lower.contains("missing") {
        "rescue.partition_missing"
    } else if lower.contains("manifest") || lower.contains("backup") {
        "rescue.backup_invalid"
    } else if lower.contains("fingerprint") || lower.contains("identity") {
        "rescue.device_mismatch"
    } else if lower.contains("restore") {
        "rescue.restore_failed"
    } else if lower.contains("config") {
        "rescue.config_invalid"
    } else {
        "rescue.operation_failed"
    };
    coded_error(code, message)
}

fn error_code(error: &anyhow::Error) -> String {
    let rendered = format!("{error:#}");
    rendered
        .split_once(&format!("{ERROR_PREFIX}:"))
        .and_then(|(_, suffix)| suffix.split_once(':').map(|(code, _)| code))
        .unwrap_or("rescue.operation_failed")
        .to_owned()
}

#[derive(Clone, Debug, Default)]
struct RescueConfig {
    include_dtbo: bool,
    include_vbmeta: bool,
    backup_other_slot: bool,
    dangerous_auto_restore: DangerousAutoRestore,
    custom_partitions: BTreeMap<String, String>,
}

#[derive(Clone, Copy, Debug, Default)]
enum DangerousAutoRestore {
    #[default]
    Skip,
    Allow,
}

impl DangerousAutoRestore {
    const fn is_allowed(self) -> bool {
        matches!(self, Self::Allow)
    }
}

#[derive(Clone)]
struct PartitionSpec {
    name: String,
    label: String,
    image_path: String,
    required: bool,
    custom_path: Option<String>,
    ota: bool,
    restore: bool,
}

struct RestorePlan {
    description: String,
    specs: Vec<PartitionSpec>,
    activate_slot: Option<String>,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum BootRescueAction {
    None,
    RestoreBackups,
    DisableModules,
}

const fn post_fs_data_rescue_action(
    pending_boot: bool,
    previous_boot_ok: bool,
    boot_count: u32,
    failure_hint: bool,
) -> BootRescueAction {
    if pending_boot
        && (failure_hint || (boot_count >= PENDING_BOOT_FAILURE_TRIGGER_COUNT && !previous_boot_ok))
    {
        BootRescueAction::RestoreBackups
    } else if !pending_boot
        && (failure_hint
            || (boot_count >= MODULE_RESCUE_FAILURE_TRIGGER_COUNT && !previous_boot_ok))
    {
        // An unverified image must never be overwritten solely because a normal
        // boot was interrupted. Recover modules first; image rollback stays tied
        // to an explicit post-flash verification marker.
        BootRescueAction::DisableModules
    } else {
        BootRescueAction::None
    }
}

const fn recovery_boot_rescue_action(
    pending_boot: bool,
    failure_hint: bool,
    boot_count: u32,
) -> BootRescueAction {
    if pending_boot && (failure_hint || boot_count >= PENDING_BOOT_FAILURE_TRIGGER_COUNT) {
        BootRescueAction::RestoreBackups
    } else if failure_hint {
        BootRescueAction::DisableModules
    } else {
        BootRescueAction::None
    }
}

pub fn print_status() {
    let config_result = read_config();
    let status_error = config_result
        .as_ref()
        .err()
        .map(ToString::to_string)
        .unwrap_or_default();
    let config = config_result.unwrap_or_default();
    let specs = partition_specs(&config);
    let manifest = read_manifest().unwrap_or_else(|_| json!({}));
    let validation = validate_backups_quick(&config);
    let ready = validation.is_ok();
    let ready_reason = validation
        .err()
        .map(|err| err.to_string())
        .unwrap_or_default();
    let transaction_result = read_restore_transaction();
    let transaction_error = transaction_result
        .as_ref()
        .err()
        .map(|error| format!("{error:#}"))
        .unwrap_or_default();
    let transaction = transaction_result.ok().flatten();
    let restore_interrupted = !transaction_error.is_empty()
        || transaction
            .as_ref()
            .is_some_and(|transaction| !matches!(transaction.phase.as_str(), "completed" | "idle"));
    let verification_current = verification_marker_is_current(&specs, &manifest);
    let config_changed = Path::new(CONFIG_CHANGED_PATH).exists();
    let phase = rescue_phase(
        status_error.is_empty(),
        is_enabled(),
        ready,
        verification_current,
        config_changed,
        restore_interrupted,
    );
    let status = json!({
        "statusOk": status_error.is_empty(),
        "statusErrorCode": if status_error.is_empty() { "" } else { "rescue.config_invalid" },
        "statusError": status_error,
        "phase": phase,
        "enabled": is_enabled(),
        "config": config_json(&config),
        "images": specs.iter().map(|spec| image_status(spec, &manifest, false)).collect::<Vec<_>>(),
        "bootCount": read_boot_count(),
        "autoRestoreAttempts": read_auto_restore_attempts(),
        "pendingBoot": Path::new(PENDING_BOOT_PATH).exists(),
        "currentSlot": current_slot(),
        "bootMode": boot_mode(),
        "device": device_summary(),
        "lastRestoreDone": Path::new(RESTORE_LOCK_PATH).exists(),
        "skipModulesOnce": skip_modules_once_exists(),
        "skipModulesThisBoot": should_skip_modules_this_boot(),
        "manifest": manifest,
        "ready": ready,
        "readyReason": ready_reason,
        "verified": verification_current,
        "environmentChecked": environment_check_is_current(&config),
        "configChangedProtectionDisabled": config_changed,
        "restoreInterrupted": restore_interrupted,
        "restoreTransactionError": transaction_error,
        "restoreTransaction": transaction.as_ref().map(restore_transaction_json),
        "rescueDisabledModules": read_rescue_disabled_modules(),
        "log": tail_file(LOG_PATH, 80).unwrap_or_default(),
    });
    println!("{status}");
}

pub fn print_test_report() {
    let config_result = read_config();
    let config_error = config_result
        .as_ref()
        .err()
        .map(ToString::to_string)
        .unwrap_or_default();
    let config = config_result.unwrap_or_default();
    let specs = partition_specs(&config);
    let environment = validate_environment(&specs);
    let backup_validation = validate_backups_quick(&config);
    let ok = config_error.is_empty() && environment.is_ok();
    let reason = if config_error.is_empty() {
        environment
            .err()
            .map(|err| err.to_string())
            .unwrap_or_default()
    } else {
        config_error.clone()
    };
    let backup_ready = config_error.is_empty() && backup_validation.is_ok();
    let backup_reason = if config_error.is_empty() {
        backup_validation
            .err()
            .map(|err| err.to_string())
            .unwrap_or_default()
    } else {
        config_error
    };
    if ok && let Err(error) = write_environment_check(&config) {
        append_log(format!("failed to persist environment check: {error:#}"));
    }
    let manifest = read_manifest().unwrap_or_else(|_| json!({}));
    let report = json!({
        "ok": ok,
        "errorCode": if ok { "" } else { "rescue.environment_invalid" },
        "reason": reason,
        "backupReady": backup_ready,
        "backupReason": backup_reason,
        "currentSlot": current_slot(),
        "device": device_summary(),
        "images": specs.iter().map(|spec| image_status(spec, &manifest, false)).collect::<Vec<_>>(),
        "manifest": manifest,
        "checks": {
            "bootPartitionFound": find_partition(&specs[0]).is_ok_and(|partition| partition.is_some()),
            "bootBackupReady": Path::new(&specs[0].image_path).is_file(),
            "configWritable": utils::ensure_dir_exists(RESCUE_DIR).is_ok(),
        }
    });
    println!("{report}");
}

pub fn print_verify_report() {
    let result = (|| -> Result<Value> {
        let config = read_config()?;
        let specs = partition_specs(&config);
        validate_backups_full(&config, true)?;
        let manifest = read_manifest()?;
        write_verification_marker(&specs, &manifest)?;
        remove_file_if_exists(Path::new(CONFIG_CHANGED_PATH))?;
        Ok(json!({
            "ok": true,
            "errorCode": "",
            "reason": "",
            "verifiedAt": Local::now().to_rfc3339(),
            "images": specs.iter().map(|spec| image_status(spec, &manifest, true)).collect::<Vec<_>>(),
        }))
    })();

    match result {
        Ok(report) => println!("{report}"),
        Err(error) => {
            let error = structured_error(error);
            println!(
                "{}",
                json!({
                    "ok": false,
                    "errorCode": error_code(&error),
                    "reason": format!("{error:#}"),
                })
            );
        }
    }
}

pub fn refresh_and_enable() -> Result<()> {
    let config = read_config()?;
    let specs = partition_specs(&config);
    validate_environment(&specs).map_err(structured_error)?;
    write_environment_check(&config)?;
    backup(true).map_err(structured_error)?;
    validate_backups_full(&config, true).map_err(structured_error)?;
    let manifest = read_manifest()?;
    write_verification_marker(&specs, &manifest)?;
    enable().map_err(structured_error)
}

pub fn print_diagnostics() {
    println!("=== ApkeSU rescue diagnostic ===");
    println!("generatedAt={}", Local::now().to_rfc3339());
    println!("=== status ===");
    print_status();
    println!("=== logs ===");
    print_logs();
}

pub fn import_config_text(content: &str) -> Result<()> {
    let config = parse_config(content)?;
    let changed = read_config().map_or(true, |current| {
        config_json(&current) != config_json(&config)
    });
    if changed {
        ensure_no_active_restore_transaction()?;
        pause_protection_for_backup_change(
            "partition configuration changed; recheck, backup, verify, and enable again",
        )?;
        remove_file_if_exists(Path::new(ENVIRONMENT_CHECK_PATH))?;
    }
    write_config(&config)?;
    if changed {
        atomic_write(Path::new(CONFIG_CHANGED_PATH), b"1")?;
    }
    append_log("rescue config updated by manager");
    Ok(())
}

pub fn import_image(partition: &str, source: &Path, force: bool) -> Result<()> {
    let result = import_image_inner(partition, source, force);
    if let Err(err) = &result {
        append_log(format!("image import failed: {err:#}"));
    }
    result
}

fn import_image_inner(partition: &str, source: &Path, force: bool) -> Result<()> {
    let name = normalize_partition_name(partition)?;
    let config = read_config()?;
    let specs = partition_specs(&config);
    let spec = specs
        .iter()
        .find(|item| item.label == name)
        .cloned()
        .unwrap_or_else(|| current_slot_spec(&name, name == "boot", &config));

    let source = ensure_safe_import_source(source)?;
    let Some(device) = find_partition(&spec)? else {
        bail!("{} partition is missing", spec.name);
    };
    validate_import_source_against_partition(&source, &spec, &device)?;

    if Path::new(&spec.image_path).exists() && !force {
        bail!(
            "{} backup already exists; pass --force to overwrite it",
            spec.name
        );
    }

    ensure_no_active_restore_transaction()?;
    utils::ensure_dir_exists(RESCUE_DIR)?;
    let protected_files = vec![spec.image_path.clone(), MANIFEST_PATH.to_string()];
    preserve_files(&protected_files)?;
    if let Err(error) = pause_protection_for_backup_change(
        "a rescue image is being replaced; run full verification before enabling again",
    ) {
        cleanup_preserved_files(&protected_files);
        return Err(error);
    }
    let import_result = (|| -> Result<()> {
        atomic_copy(&source, Path::new(&spec.image_path)).with_context(|| {
            format!(
                "failed to import {} backup from {}",
                spec.name,
                source.display()
            )
        })?;
        validate_image_size_against_partition(&spec, &device)?;
        write_manifest(&specs)
    })();
    if let Err(err) = import_result {
        if let Err(restore_err) = restore_preserved_files(&protected_files) {
            bail!("import failed: {err:#}; previous backup restore failed: {restore_err:#}");
        }
        return Err(err).context("previous rescue backup was restored");
    }
    cleanup_preserved_files(&protected_files);
    append_log(format!(
        "imported {} backup from {}, sha256={}",
        spec.name,
        source.display(),
        sha256_of(&spec.image_path)
    ));
    Ok(())
}

pub fn backup(force: bool) -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    let config = read_config()?;
    let specs = partition_specs(&config);
    let backup_exists = Path::new(MANIFEST_PATH).exists()
        || specs
            .iter()
            .any(|spec| Path::new(&spec.image_path).exists());
    if backup_exists && !force {
        bail!("backup already exists; pass --force to overwrite it");
    }
    ensure_no_active_restore_transaction()?;
    append_log("backup requested by manager");
    let protected_files = rescue_file_paths(&specs);
    preserve_files(&protected_files)?;
    if let Err(error) = pause_protection_for_backup_change(
        "rescue backups are being refreshed; run full verification before enabling again",
    ) {
        cleanup_preserved_files(&protected_files);
        return Err(error);
    }
    let backup_result = (|| -> Result<()> {
        for spec in &specs {
            let Some(device) = find_partition(spec)? else {
                if spec.required {
                    bail!("{} partition is missing", spec.name);
                }
                append_log(format!("skip backup: {} partition not found", spec.name));
                continue;
            };
            backup_partition(spec, &device)?;
        }
        write_manifest(&specs)
    })();

    if let Err(err) = backup_result {
        if let Err(restore_err) = restore_preserved_files(&protected_files) {
            bail!("backup failed: {err:#}; previous backup restore failed: {restore_err:#}");
        }
        append_log(format!(
            "backup failed; restored previous rescue backups: {err:#}"
        ));
        return Err(err);
    }

    cleanup_preserved_files(&protected_files);
    remove_file_if_exists(Path::new(CONFIG_CHANGED_PATH))?;
    Ok(())
}

pub fn enable() -> Result<()> {
    let config = read_config()?;
    let specs = partition_specs(&config);
    validate_backups_quick(&config)?;
    let manifest = read_manifest()?;
    if !verification_marker_is_current(&specs, &manifest) {
        return Err(coded_error(
            "rescue.full_verification_required",
            "run a full backup verification before enabling rescue protection",
        ));
    }
    utils::ensure_dir_exists(RESCUE_DIR)?;
    clear_runtime_markers();
    write_failure_baseline()?;
    atomic_write(Path::new(ENABLED_PATH), b"1").context("failed to enable rescue protection")?;
    let initialize_result = (|| -> Result<()> {
        atomic_write(Path::new(BOOT_OK_PATH), b"1")
            .context("failed to mark current boot as healthy")?;
        atomic_write(Path::new(BOOT_COUNT_PATH), b"0")
            .context("failed to reset rescue boot counter")?;
        atomic_write(Path::new(AUTO_RESTORE_ATTEMPTS_PATH), b"0")
            .context("failed to reset rescue restore attempts")
    })();
    if let Err(err) = initialize_result {
        let _ = remove_file_if_exists(Path::new(ENABLED_PATH));
        return Err(err).context("rescue protection was not enabled");
    }
    append_log("rescue protection enabled");
    Ok(())
}

pub fn disable() -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    remove_file_if_exists(Path::new(ENABLED_PATH))
        .context("failed to disable rescue protection")?;
    remove_file_if_exists(Path::new(BOOT_COUNT_PATH))?;
    remove_file_if_exists(Path::new(AUTO_RESTORE_ATTEMPTS_PATH))?;
    remove_file_if_exists(Path::new(BOOT_OK_PATH))?;
    remove_file_if_exists(Path::new(FAILURE_BASELINE_PATH))?;
    clear_runtime_markers();
    append_log("rescue protection disabled");
    Ok(())
}

fn ensure_no_active_restore_transaction() -> Result<()> {
    if let Some(transaction) = read_restore_transaction()?
        && !matches!(transaction.phase.as_str(), "completed" | "idle")
    {
        return Err(coded_error(
            "rescue.restore_in_progress",
            format!(
                "restore transaction {} is still in phase {}; finish or recover it before changing rescue backups",
                transaction.id, transaction.phase
            ),
        ));
    }
    Ok(())
}

fn pause_protection_for_backup_change(reason: &str) -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    let was_enabled =
        invalidate_protection_markers_at(Path::new(ENABLED_PATH), Path::new(VERIFIED_PATH))?;
    for path in [
        BOOT_COUNT_PATH,
        AUTO_RESTORE_ATTEMPTS_PATH,
        BOOT_OK_PATH,
        FAILURE_BASELINE_PATH,
    ] {
        remove_file_if_exists(Path::new(path))?;
    }
    clear_runtime_markers();
    if was_enabled {
        append_log(format!("rescue protection paused because {reason}"));
    } else {
        append_log(format!("rescue verification invalidated because {reason}"));
    }
    Ok(())
}

fn invalidate_protection_markers_at(enabled: &Path, verified: &Path) -> Result<bool> {
    let was_enabled = enabled.exists();
    remove_file_if_exists(enabled)?;
    remove_file_if_exists(verified)?;
    Ok(was_enabled)
}

pub fn restore_now() -> Result<()> {
    restore_keep_data_now()
}

pub fn restore_keep_data_now() -> Result<()> {
    append_log("manual data-preserving rollback requested by manager");
    restore_backups("manual data-preserving rollback", false)
}

pub fn mark_next_boot_pending(reason: &str) -> Result<()> {
    if !is_enabled() {
        return Ok(());
    }

    utils::ensure_dir_exists(RESCUE_DIR).context("failed to prepare pending boot marker")?;
    write_failure_baseline()?;
    let pending = json!({
        "reason": reason,
        "armedAt": Local::now().to_rfc3339(),
        "bootId": current_boot_id().unwrap_or_default(),
    });
    atomic_write(Path::new(PENDING_BOOT_PATH), pending.to_string().as_bytes())
        .context("failed to write pending boot marker")?;
    let _ = fs::remove_file(BOOT_OK_PATH);
    write_boot_count(0);
    write_auto_restore_attempts(0);
    append_log(format!("next boot marked pending: {reason}"));
    Ok(())
}

pub fn print_logs() {
    let logs = tail_file(LOG_PATH, 240).unwrap_or_default();
    if logs.is_empty() {
        println!("No rescue logs yet.");
    } else {
        println!("{logs}");
    }
}

pub fn clear_logs() -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    atomic_write(Path::new(LOG_PATH), b"").context("failed to clear rescue log")?;
    for index in 1..=LOG_ROTATION_COUNT {
        remove_file_if_exists(Path::new(&format!("{LOG_PATH}.{index}")))?;
    }
    append_log("rescue log cleared");
    Ok(())
}

pub fn take_skip_modules_once() -> bool {
    if Path::new(SKIP_MODULES_THIS_BOOT_PATH).exists() {
        return true;
    }
    let skip_once = skip_modules_once_exists();
    let legacy_skip_once = Path::new(LEGACY_TMP_MODULE_DISABLE_PATH).exists();
    if !skip_once && !legacy_skip_once {
        return false;
    }

    let _ = fs::remove_file(SKIP_MODULES_ONCE_PATH);
    let _ = fs::remove_file(CACHE_SKIP_MODULES_ONCE_PATH);
    let _ = fs::remove_file(LEGACY_TMP_MODULE_DISABLE_PATH);
    if let Err(err) = fs::write(SKIP_MODULES_THIS_BOOT_PATH, b"1") {
        append_log(format!("failed to mark module skip guard: {err:#}"));
    }
    append_log(format!(
        "rescue requested temporary module skip for this boot: skip_once={skip_once}, legacy_skip_once={legacy_skip_once}"
    ));
    true
}

pub fn should_skip_modules_this_boot() -> bool {
    Path::new(SKIP_MODULES_THIS_BOOT_PATH).exists()
}

pub fn check_on_post_fs_data() {
    if !is_enabled() {
        return;
    }

    if is_recovery_boot() {
        check_on_recovery_boot();
        return;
    }

    if Path::new(RESTORE_LOCK_PATH).exists() {
        append_log("restore lock exists; mark boot healthy and skip auto restore");
        clear_restore_markers();
        let _ = fs::write(BOOT_OK_PATH, b"1");
        write_boot_count(0);
        write_auto_restore_attempts(0);
        return;
    }

    let previous_boot_ok = Path::new(BOOT_OK_PATH).exists();
    let pending_boot = Path::new(PENDING_BOOT_PATH).exists();
    let next_count = if previous_boot_ok {
        1
    } else {
        read_boot_count().saturating_add(1)
    };
    let _ = fs::remove_file(BOOT_OK_PATH);
    if let Err(err) = fs::write(BOOT_COUNT_PATH, next_count.to_string()) {
        append_log(format!("failed to update boot counter: {err}"));
    }

    append_log(format!(
        "post-fs-data rescue check: previous_boot_ok={previous_boot_ok}, pending_boot={pending_boot}, boot_count={next_count}"
    ));

    // On the first patched boot, pstore still describes the boot that performed the
    // flash. Only trust failure evidence after another attempted boot.
    let failure_hint = next_count > 1 && has_boot_failure_hint();
    match post_fs_data_rescue_action(pending_boot, previous_boot_ok, next_count, failure_hint) {
        BootRescueAction::RestoreBackups => {
            append_log(format!(
                "auto image rollback triggered on post-fs-data: failure_hint={failure_hint}, pending_boot={pending_boot}, boot_count={next_count}"
            ));
            if let Err(err) = auto_restore_backups() {
                append_log(format!("auto image rollback failed: {err:#}"));
            }
        }
        BootRescueAction::DisableModules => {
            rescue_modules_for_failed_boot(format!(
                "unverified boot failure: failure_hint={failure_hint}, boot_count={next_count}"
            ));
        }
        BootRescueAction::None => {
            if !pending_boot {
                append_log(
                    "no pending flashed boot; image rollback remains armed only for verified flashes",
                );
            }
        }
    }
}

pub fn check_on_recovery_boot() {
    if !is_enabled() || !is_recovery_boot() {
        return;
    }

    if Path::new(RECOVERY_CHECK_GUARD_PATH).exists() {
        append_log("recovery rescue check already ran in this boot");
        return;
    }
    if let Err(err) = fs::write(RECOVERY_CHECK_GUARD_PATH, b"1") {
        append_log(format!("failed to write recovery check guard: {err:#}"));
    }

    if Path::new(RESTORE_LOCK_PATH).exists() {
        append_log("restore lock exists in recovery; mark boot healthy and skip auto restore");
        clear_restore_markers();
        let _ = fs::write(BOOT_OK_PATH, b"1");
        write_boot_count(0);
        write_auto_restore_attempts(0);
        return;
    }

    let previous_boot_ok = Path::new(BOOT_OK_PATH).exists();
    let pending_boot = Path::new(PENDING_BOOT_PATH).exists();
    let next_count = if previous_boot_ok {
        1
    } else {
        read_boot_count().saturating_add(1)
    };
    let _ = fs::remove_file(BOOT_OK_PATH);
    write_boot_count(next_count);

    let failure_hint = has_boot_failure_hint();
    append_log(format!(
        "recovery rescue check: boot_mode={}, previous_boot_ok={previous_boot_ok}, pending_boot={pending_boot}, boot_count={next_count}, failure_hint={failure_hint}",
        boot_mode()
    ));

    match recovery_boot_rescue_action(pending_boot, failure_hint, next_count) {
        BootRescueAction::RestoreBackups => {
            if let Err(err) = auto_restore_backups() {
                append_log(format!("auto image rollback failed in recovery: {err:#}"));
            }
        }
        BootRescueAction::DisableModules => {
            rescue_modules_for_failed_boot(
                "recovery boot failure evidence without a pending image flash",
            );
        }
        BootRescueAction::None => {}
    }
}

pub fn mark_boot_completed() {
    if !is_enabled() {
        return;
    }

    if let Err(err) = utils::ensure_dir_exists(RESCUE_DIR) {
        append_log(format!(
            "failed to create rescue dir on boot completed: {err:#}"
        ));
        return;
    }

    let _ = fs::write(BOOT_OK_PATH, b"1");
    let _ = fs::write(BOOT_COUNT_PATH, b"0");
    let _ = fs::write(AUTO_RESTORE_ATTEMPTS_PATH, b"0");
    clear_runtime_markers();
    cleanup_legacy_rescue_flags();
    if let Err(error) = write_failure_baseline() {
        append_log(format!(
            "failed to refresh boot failure baseline: {error:#}"
        ));
    }
    append_log("boot completed; rescue counter reset");
}

fn partition_specs(config: &RescueConfig) -> Vec<PartitionSpec> {
    base_partition_names(config)
        .into_iter()
        .map(|(name, required)| current_slot_spec(name, required, config))
        .chain(other_slot_specs(config))
        .collect()
}

fn base_partition_names(config: &RescueConfig) -> Vec<(&'static str, bool)> {
    let mut names = vec![("boot", true), ("vendor_boot", false), ("init_boot", false)];
    if config.include_dtbo {
        names.push(("dtbo", false));
    }
    if config.include_vbmeta {
        names.push(("vbmeta", false));
    }
    names
}

fn current_slot_spec(name: &str, required: bool, config: &RescueConfig) -> PartitionSpec {
    let custom_path = config.custom_partitions.get(name).cloned();
    PartitionSpec {
        name: name.to_string(),
        label: name.to_string(),
        image_path: format!("{RESCUE_DIR}{name}.img"),
        required,
        custom_path,
        ota: false,
        restore: true,
    }
}

fn other_slot_specs(config: &RescueConfig) -> Vec<PartitionSpec> {
    if !config.backup_other_slot || current_slot().is_empty() {
        return Vec::new();
    }

    base_partition_names(config)
        .into_iter()
        .filter(|(name, _)| !config.custom_partitions.contains_key(*name))
        .map(|(name, _required)| PartitionSpec {
            name: name.to_string(),
            label: format!("{name}_other"),
            image_path: format!("{RESCUE_DIR}{name}_other.img"),
            required: false,
            custom_path: None,
            ota: true,
            restore: false,
        })
        .collect()
}

fn validate_environment(specs: &[PartitionSpec]) -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    for spec in specs {
        if find_partition(spec)?.is_none() && spec.required {
            bail!("{} partition is missing", spec.name);
        }
    }
    Ok(())
}

fn validate_backups_quick(config: &RescueConfig) -> Result<()> {
    validate_backups_with_mode(config, false, false)
}

fn validate_backups_full(config: &RescueConfig, refresh_hashes: bool) -> Result<()> {
    validate_backups_with_mode(config, true, refresh_hashes)
}

fn validate_backups_with_mode(
    config: &RescueConfig,
    verify_sha256: bool,
    refresh_hashes: bool,
) -> Result<()> {
    let plans = restore_plans(config)?;
    let mut last_error = None;
    for plan in plans {
        match validate_restore_backups(&plan.specs, config, false, verify_sha256, refresh_hashes) {
            Ok(()) => return Ok(()),
            Err(err) => last_error = Some(err),
        }
    }
    if let Some(err) = last_error {
        return Err(err).context("no usable rescue backup set");
    }
    bail!("no usable rescue backup set");
}

fn validate_restore_backups(
    specs: &[PartitionSpec],
    config: &RescueConfig,
    automatic: bool,
    verify_sha256: bool,
    refresh_hashes: bool,
) -> Result<()> {
    let manifest = validate_manifest_context_for_restore()?;
    let mut available_count = 0usize;
    for spec in specs {
        if !should_restore_spec(spec, config, automatic) {
            continue;
        }

        let Some(device) = find_partition(spec)? else {
            if spec.required {
                bail!("{} partition is missing", spec.name);
            }
            append_log(format!(
                "skip restore validation: {} partition not found",
                spec.name
            ));
            continue;
        };

        let image = Path::new(&spec.image_path);
        if !image.is_file() {
            if spec.required {
                bail!("{} backup is missing", spec.name);
            }
            append_log(format!(
                "skip restore validation: {} backup not found",
                spec.name
            ));
            continue;
        }

        validate_image_against_partition(spec, &device, &manifest, verify_sha256, refresh_hashes)?;
        available_count += 1;
    }

    if available_count == 0 {
        bail!("no rescue backup is available to restore");
    }

    Ok(())
}

fn should_restore_spec(spec: &PartitionSpec, config: &RescueConfig, automatic: bool) -> bool {
    if !spec.restore {
        return false;
    }
    if automatic
        && is_dangerous_partition(&spec.name)
        && !config.dangerous_auto_restore.is_allowed()
    {
        append_log(format!(
            "skip auto restore: {} is a dangerous partition",
            spec.name
        ));
        return false;
    }
    true
}

fn validate_image_against_partition(
    spec: &PartitionSpec,
    device: &str,
    manifest: &Value,
    verify_sha256: bool,
    refresh_hash: bool,
) -> Result<()> {
    let image_size = fs::metadata(&spec.image_path)
        .with_context(|| format!("failed to read {} backup metadata", spec.name))?
        .len();
    if image_size == 0 {
        bail!("{} backup is empty", spec.name);
    }

    let device_size = partition_size(device);
    if device_size > 0 && image_size != device_size {
        bail!(
            "{} backup size mismatch: image={}, partition={}",
            spec.name,
            image_size,
            device_size
        );
    }

    if verify_sha256 && let Some(expected) = manifest_sha256_from(manifest, &spec.label) {
        let actual = cached_sha256(&spec.image_path, refresh_hash);
        if !expected.is_empty() && expected != actual {
            bail!("{} backup sha256 mismatch", spec.name);
        }
    }

    Ok(())
}

fn validate_image_size_against_partition(spec: &PartitionSpec, device: &str) -> Result<()> {
    let image_size = fs::metadata(&spec.image_path)
        .with_context(|| format!("failed to read {} backup metadata", spec.name))?
        .len();
    if image_size == 0 {
        bail!("{} backup is empty", spec.name);
    }

    let device_size = partition_size(device);
    if device_size > 0 && image_size != device_size {
        bail!(
            "{} backup size mismatch: image={}, partition={}",
            spec.name,
            image_size,
            device_size
        );
    }

    Ok(())
}

fn restore_plans(config: &RescueConfig) -> Result<Vec<RestorePlan>> {
    let manifest = validate_manifest_context_for_restore()?;
    let saved_slot = manifest_slot(&manifest);
    let now_slot = current_slot();
    if saved_slot.is_empty() || now_slot.is_empty() || saved_slot == now_slot {
        return Ok(vec![RestorePlan {
            description: "restore backups for current slot".to_string(),
            specs: partition_specs(config),
            activate_slot: None,
        }]);
    }

    let mut plans = Vec::new();
    if config.backup_other_slot {
        let current_slot_specs = current_slot_specs_from_other_backups(config);
        if current_slot_specs.iter().any(|spec| spec.required) {
            plans.push(RestorePlan {
                description: format!(
                    "slot changed from {saved_slot} to {now_slot}; use other-slot backups for current slot"
                ),
                specs: current_slot_specs,
                activate_slot: None,
            });
        }
    }

    plans.push(RestorePlan {
        description: format!(
            "slot changed from {saved_slot} to {now_slot}; restore backup slot and switch active slot back to {saved_slot}"
        ),
        specs: saved_slot_specs(config),
        activate_slot: Some(saved_slot),
    });
    Ok(plans)
}

fn current_slot_specs_from_other_backups(config: &RescueConfig) -> Vec<PartitionSpec> {
    base_partition_names(config)
        .into_iter()
        .filter(|(name, _)| !config.custom_partitions.contains_key(*name))
        .map(|(name, required)| PartitionSpec {
            name: name.to_string(),
            label: format!("{name}_other"),
            image_path: format!("{RESCUE_DIR}{name}_other.img"),
            required,
            custom_path: None,
            ota: false,
            restore: true,
        })
        .collect()
}

fn saved_slot_specs(config: &RescueConfig) -> Vec<PartitionSpec> {
    base_partition_names(config)
        .into_iter()
        .map(|(name, required)| {
            let custom_path = config.custom_partitions.get(name).cloned();
            PartitionSpec {
                name: name.to_string(),
                label: name.to_string(),
                image_path: format!("{RESCUE_DIR}{name}.img"),
                required,
                ota: custom_path.is_none(),
                custom_path,
                restore: true,
            }
        })
        .collect()
}

fn auto_restore_backups() -> Result<()> {
    let attempts = read_auto_restore_attempts();
    if attempts >= MAX_AUTO_RESTORE_ATTEMPTS {
        let _ = fs::remove_file(ENABLED_PATH);
        append_log("auto restore attempt limit reached; rescue protection disabled");
        bail!("auto restore attempt limit reached");
    }
    write_auto_restore_attempts(attempts.saturating_add(1));
    restore_backups("auto data-preserving rollback after failed boot", true)
}

fn restore_backups(reason: &str, automatic: bool) -> Result<()> {
    let config = read_config()?;
    let plans = restore_plans(&config)?;
    let mut selected_plan = None;
    let mut last_error = None;
    for plan in plans {
        match validate_restore_backups(&plan.specs, &config, automatic, true, false) {
            Ok(()) => {
                selected_plan = Some(plan);
                break;
            }
            Err(err) => {
                append_log(format!("skip restore plan '{}': {err:#}", plan.description));
                last_error = Some(err);
            }
        }
    }
    let Some(plan) = selected_plan else {
        if let Some(err) = last_error {
            return Err(err).context("no usable rescue restore plan");
        }
        bail!("no usable rescue restore plan");
    };

    if let Some(slot) = plan.activate_slot.as_deref() {
        validate_active_slot_switch(slot)?;
    }

    append_log(format!("restore started: {reason}"));
    append_log(format!("restore plan: {}", plan.description));
    append_log("restore mode: keep /data untouched; only configured boot images are restored");
    let mut transaction = prepare_restore_transaction(reason, automatic, &plan, &config)?;
    write_restore_transaction(&transaction)?;
    if let Err(error) = execute_restore_transaction(&mut transaction) {
        "failed".clone_into(&mut transaction.phase);
        transaction.error_code = error_code(&structured_error(anyhow!(format!("{error:#}"))));
        transaction.error_message = format!("{error:#}");
        transaction.updated_at = Local::now().to_rfc3339();
        if let Err(write_error) = write_restore_transaction(&transaction) {
            append_log(format!(
                "restore failed and transaction update also failed: {write_error:#}"
            ));
        }
        return Err(coded_error(
            "rescue.restore_interrupted",
            format!(
                "restore transaction {} stopped after a partial write: {error:#}",
                transaction.id
            ),
        ));
    }
    mark_skip_modules_once();
    mark_legacy_tmp_module_disable();
    disable_all_modules_for_rescue();
    mark_legacy_fix_done_lock();
    cleanup_legacy_rescue_flags();
    fs::write(RESTORE_LOCK_PATH, b"1").context("failed to write restore lock")?;
    fs::write(BOOT_COUNT_PATH, b"0").context("failed to reset boot counter")?;
    let _ = fs::remove_file(PENDING_BOOT_PATH);
    append_log("restore finished; rebooting");
    let _ = Command::new("sync").status();
    request_reboot().context("restore completed but reboot request failed")?;
    Ok(())
}

fn prepare_restore_transaction(
    reason: &str,
    automatic: bool,
    plan: &RestorePlan,
    config: &RescueConfig,
) -> Result<RestoreTransaction> {
    let manifest = read_manifest()?;
    let mut entries = Vec::new();
    for spec in &plan.specs {
        if !should_restore_spec(spec, config, automatic) || !Path::new(&spec.image_path).is_file() {
            continue;
        }
        let Some(device_path) = find_partition(spec)? else {
            if spec.required {
                return Err(coded_error(
                    "rescue.partition_missing",
                    format!(
                        "{} partition disappeared after restore preflight",
                        spec.name
                    ),
                ));
            }
            continue;
        };
        let expected_sha256 = manifest_sha256_from(&manifest, &spec.label).unwrap_or_default();
        if expected_sha256.is_empty() {
            return Err(coded_error(
                "rescue.manifest_digest_missing",
                format!("{} has no SHA256 in the rescue manifest", spec.label),
            ));
        }
        entries.push(RestoreTransactionEntry {
            name: spec.name.clone(),
            label: spec.label.clone(),
            image_path: spec.image_path.clone(),
            device_path,
            expected_sha256,
            expected_size: fs::metadata(&spec.image_path)?.len(),
            status: "pending".to_owned(),
        });
    }
    if entries.is_empty() {
        return Err(coded_error(
            "rescue.no_restore_entries",
            "no rescue backup was selected for restore",
        ));
    }

    if let Some(existing) = read_restore_transaction()?
        && let Some(resumable) =
            select_resumable_restore_transaction(existing, &entries, plan.activate_slot.as_deref())?
    {
        append_log(format!(
            "resuming interrupted restore transaction {} in phase {}",
            resumable.id, resumable.phase
        ));
        return Ok(resumable);
    }

    let now = Local::now();
    Ok(RestoreTransaction {
        id: format!("{}-{}", now.format("%Y%m%d%H%M%S"), std::process::id()),
        reason: reason.to_owned(),
        automatic,
        description: plan.description.clone(),
        activate_slot: plan.activate_slot.clone(),
        phase: "prepared".to_owned(),
        error_code: String::new(),
        error_message: String::new(),
        started_at: now.to_rfc3339(),
        updated_at: now.to_rfc3339(),
        entries,
    })
}

fn execute_restore_transaction(transaction: &mut RestoreTransaction) -> Result<()> {
    "writing".clone_into(&mut transaction.phase);
    transaction.error_code.clear();
    transaction.error_message.clear();
    transaction.updated_at = Local::now().to_rfc3339();
    write_restore_transaction(transaction)?;

    for index in 0..transaction.entries.len() {
        let entry = transaction.entries[index].clone();
        if entry.status == "verified"
            && cached_sha256(&entry.device_path, true) == entry.expected_sha256
        {
            append_log(format!(
                "restore transaction {}: {} remains verified; skip rewrite",
                transaction.id, entry.label
            ));
            continue;
        }

        "writing".clone_into(&mut transaction.entries[index].status);
        transaction.updated_at = Local::now().to_rfc3339();
        write_restore_transaction(transaction)?;
        append_log(format!(
            "restore transaction {}: writing {}",
            transaction.id, entry.label
        ));

        if let Err(error) = restore_transaction_entry(&entry) {
            "failed".clone_into(&mut transaction.entries[index].status);
            transaction.updated_at = Local::now().to_rfc3339();
            let _ = write_restore_transaction(transaction);
            return Err(error);
        }
        "verified".clone_into(&mut transaction.entries[index].status);
        transaction.updated_at = Local::now().to_rfc3339();
        write_restore_transaction(transaction)?;
    }

    if let Some(slot) = transaction.activate_slot.as_deref() {
        set_active_slot(slot)?;
    }
    "completed".clone_into(&mut transaction.phase);
    transaction.updated_at = Local::now().to_rfc3339();
    write_restore_transaction(transaction)
}

fn restore_transaction_entry(entry: &RestoreTransactionEntry) -> Result<()> {
    let metadata = fs::metadata(&entry.image_path)
        .with_context(|| format!("failed to read {} backup metadata", entry.label))?;
    if metadata.len() != entry.expected_size {
        return Err(coded_error(
            "rescue.backup_size_changed",
            format!("{} backup size changed after preflight", entry.label),
        ));
    }
    let actual_backup_sha256 = cached_sha256(&entry.image_path, true);
    if actual_backup_sha256 != entry.expected_sha256 {
        return Err(coded_error(
            "rescue.backup_changed",
            format!("{} backup SHA256 changed after preflight", entry.label),
        ));
    }
    run_dd(&entry.image_path, &entry.device_path)
        .with_context(|| format!("failed to restore {}", entry.name))?;
    let actual_partition_sha256 = cached_sha256(&entry.device_path, true);
    if actual_partition_sha256 != entry.expected_sha256 {
        return Err(coded_error(
            "rescue.restore_verification_failed",
            format!(
                "{} restore verification failed: SHA256 mismatch",
                entry.label
            ),
        ));
    }
    append_log(format!("restore {} verified", entry.label));
    Ok(())
}

fn restore_transactions_match(
    existing: &RestoreTransaction,
    entries: &[RestoreTransactionEntry],
    activate_slot: Option<&str>,
) -> bool {
    existing.activate_slot.as_deref() == activate_slot
        && existing.entries.len() == entries.len()
        && existing.entries.iter().zip(entries).all(|(left, right)| {
            left.label == right.label
                && left.image_path == right.image_path
                && left.device_path == right.device_path
                && left.expected_sha256 == right.expected_sha256
                && left.expected_size == right.expected_size
        })
}

fn select_resumable_restore_transaction(
    existing: RestoreTransaction,
    entries: &[RestoreTransactionEntry],
    activate_slot: Option<&str>,
) -> Result<Option<RestoreTransaction>> {
    if matches!(existing.phase.as_str(), "completed" | "idle") {
        return Ok(None);
    }
    if restore_transactions_match(&existing, entries, activate_slot) {
        return Ok(Some(existing));
    }
    Err(coded_error(
        "rescue.restore_transaction_conflict",
        format!(
            "unfinished restore transaction {} does not match the current restore plan",
            existing.id
        ),
    ))
}

fn restore_transaction_json(transaction: &RestoreTransaction) -> Value {
    json!({
        "schemaVersion": 1,
        "id": transaction.id,
        "reason": transaction.reason,
        "automatic": transaction.automatic,
        "description": transaction.description,
        "activateSlot": transaction.activate_slot,
        "phase": transaction.phase,
        "errorCode": transaction.error_code,
        "errorMessage": transaction.error_message,
        "startedAt": transaction.started_at,
        "updatedAt": transaction.updated_at,
        "entries": transaction.entries.iter().map(|entry| json!({
            "name": entry.name,
            "label": entry.label,
            "imagePath": entry.image_path,
            "devicePath": entry.device_path,
            "expectedSha256": entry.expected_sha256,
            "expectedSize": entry.expected_size,
            "status": entry.status,
        })).collect::<Vec<_>>(),
    })
}

fn write_restore_transaction(transaction: &RestoreTransaction) -> Result<()> {
    atomic_write(
        Path::new(RESTORE_TRANSACTION_PATH),
        restore_transaction_json(transaction).to_string().as_bytes(),
    )
    .context("failed to persist rescue restore transaction")
}

fn read_restore_transaction() -> Result<Option<RestoreTransaction>> {
    read_restore_transaction_from(Path::new(RESTORE_TRANSACTION_PATH))
}

fn read_restore_transaction_from(path: &Path) -> Result<Option<RestoreTransaction>> {
    if !path.exists() {
        return Ok(None);
    }
    let content = fs::read_to_string(path).with_context(|| {
        format!(
            "failed to read rescue restore transaction {}",
            path.display()
        )
    })?;
    parse_restore_transaction(&content).map(Some)
}

fn parse_restore_transaction(content: &str) -> Result<RestoreTransaction> {
    let value: Value =
        serde_json::from_str(content).context("invalid rescue restore transaction JSON")?;
    let required_string = |key: &str| -> Result<String> {
        value
            .get(key)
            .and_then(Value::as_str)
            .filter(|text| !text.is_empty())
            .map(ToOwned::to_owned)
            .with_context(|| format!("restore transaction is missing {key}"))
    };
    let entries = value
        .get("entries")
        .and_then(Value::as_array)
        .context("restore transaction is missing entries")?
        .iter()
        .map(|entry| {
            let string = |key: &str| -> Result<String> {
                entry
                    .get(key)
                    .and_then(Value::as_str)
                    .filter(|text| !text.is_empty())
                    .map(ToOwned::to_owned)
                    .with_context(|| format!("restore transaction entry is missing {key}"))
            };
            Ok(RestoreTransactionEntry {
                name: string("name")?,
                label: string("label")?,
                image_path: string("imagePath")?,
                device_path: string("devicePath")?,
                expected_sha256: string("expectedSha256")?,
                expected_size: entry
                    .get("expectedSize")
                    .and_then(Value::as_u64)
                    .context("restore transaction entry is missing expectedSize")?,
                status: string("status")?,
            })
        })
        .collect::<Result<Vec<_>>>()?;
    Ok(RestoreTransaction {
        id: required_string("id")?,
        reason: required_string("reason")?,
        automatic: value
            .get("automatic")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        description: required_string("description")?,
        activate_slot: value
            .get("activateSlot")
            .and_then(Value::as_str)
            .filter(|slot| !slot.is_empty())
            .map(ToOwned::to_owned),
        phase: required_string("phase")?,
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
        started_at: required_string("startedAt")?,
        updated_at: required_string("updatedAt")?,
        entries,
    })
}

fn backup_partition(spec: &PartitionSpec, device: &str) -> Result<()> {
    append_log(format!(
        "backup {}: {} -> {}",
        spec.name, device, spec.image_path
    ));
    let backup_result = (|| -> Result<()> {
        run_dd(device, &spec.image_path)
            .with_context(|| format!("failed to backup {}", spec.name))?;
        validate_image_size_against_partition(spec, device)
    })();
    backup_result?;
    append_log(format!(
        "backup {} ok: size={}, sha256={}",
        spec.name,
        fs::metadata(&spec.image_path)?.len(),
        sha256_of(&spec.image_path)
    ));
    Ok(())
}

fn run_dd(input: &str, output: &str) -> Result<()> {
    let mut input_file = OpenOptions::new()
        .read(true)
        .open(input)
        .with_context(|| format!("open input {input}"))?;

    let output_is_block = output.starts_with("/dev/block/");
    if output_is_block {
        let mut output_file = OpenOptions::new()
            .write(true)
            .open(output)
            .with_context(|| format!("open output {output}"))?;
        set_block_writable(&output_file, output)?;
        copy_with_timeout(&mut input_file, &mut output_file)
            .with_context(|| format!("copy {input} to {output}"))?;
        output_file
            .sync_all()
            .with_context(|| format!("sync output {output}"))?;
    } else {
        let output_path = Path::new(output);
        let parent = output_path
            .parent()
            .with_context(|| format!("{output} has no parent directory"))?;
        utils::ensure_dir_exists(parent)?;
        let mut temporary = tempfile::NamedTempFile::new_in(parent)
            .with_context(|| format!("create temporary backup in {}", parent.display()))?;
        copy_with_timeout(&mut input_file, &mut temporary)
            .with_context(|| format!("copy {input} to temporary backup for {output}"))?;
        temporary
            .as_file()
            .sync_all()
            .with_context(|| format!("sync temporary backup for {output}"))?;
        temporary
            .persist(output_path)
            .map_err(|error| error.error)
            .with_context(|| format!("atomically replace {output}"))?;
        sync_parent_directory(parent);
    }
    let _ = Command::new("sync").status();
    Ok(())
}

fn copy_with_timeout<R: Read, W: Write>(input: &mut R, output: &mut W) -> Result<u64> {
    let started = Instant::now();
    copy_with_clock(
        input,
        output,
        Duration::from_secs(COPY_IDLE_TIMEOUT_SECONDS),
        Duration::from_secs(COPY_TOTAL_TIMEOUT_SECONDS),
        || started.elapsed(),
    )
}

fn copy_with_clock<R, W, F>(
    input: &mut R,
    output: &mut W,
    idle_timeout: Duration,
    total_timeout: Duration,
    mut elapsed: F,
) -> Result<u64>
where
    R: Read,
    W: Write,
    F: FnMut() -> Duration,
{
    let mut buffer = vec![0_u8; COPY_BUFFER_BYTES];
    let mut copied = 0_u64;
    let mut last_progress = Duration::ZERO;
    loop {
        ensure_copy_deadline(elapsed(), last_progress, idle_timeout, total_timeout)?;
        let count = input.read(&mut buffer).context("read copy source")?;
        if count == 0 {
            return Ok(copied);
        }
        ensure_copy_deadline(elapsed(), last_progress, idle_timeout, total_timeout)?;
        output
            .write_all(&buffer[..count])
            .context("write copy destination")?;
        let now = elapsed();
        ensure_copy_deadline(now, last_progress, idle_timeout, total_timeout)?;
        copied = copied.saturating_add(count as u64);
        last_progress = now;
    }
}

fn ensure_copy_deadline(
    elapsed: Duration,
    last_progress: Duration,
    idle_timeout: Duration,
    total_timeout: Duration,
) -> Result<()> {
    if elapsed > total_timeout {
        return Err(coded_error(
            "rescue.io_timeout",
            format!(
                "rescue image copy exceeded the {} second total timeout",
                total_timeout.as_secs()
            ),
        ));
    }
    if elapsed.saturating_sub(last_progress) > idle_timeout {
        return Err(coded_error(
            "rescue.io_timeout",
            format!(
                "rescue image copy made no progress for more than {} seconds",
                idle_timeout.as_secs()
            ),
        ));
    }
    Ok(())
}

#[cfg(target_os = "android")]
fn set_block_writable(file: &fs::File, path: &str) -> Result<()> {
    if !path.starts_with("/dev/block/") {
        return Ok(());
    }
    unsafe {
        const BLKROSET: i32 = libc::_IO(0x12, 93);
        let mut val: libc::c_int = 0;
        if libc::ioctl(file.as_raw_fd(), BLKROSET, &raw mut val) != 0 {
            bail!("Failed to set rw for {path}: {}", *libc::__errno());
        }
    }
    Ok(())
}

#[cfg(not(target_os = "android"))]
fn set_block_writable(_file: &fs::File, _path: &str) -> Result<()> {
    Ok(())
}

fn request_reboot() -> Result<()> {
    let commands: [(&str, &[&str]); 4] = [
        ("/system/bin/reboot", &[]),
        ("reboot", &[]),
        ("svc", &["power", "reboot"]),
        ("setprop", &["sys.powerctl", "reboot"]),
    ];

    for (program, args) in commands {
        match Command::new(program).args(args).status() {
            Ok(status) if status.success() => {
                append_log(format!("requested reboot via {program}"));
                return Ok(());
            }
            Ok(status) => {
                append_log(format!("reboot command {program} exited with {status}"));
            }
            Err(err) => {
                append_log(format!("reboot command {program} failed: {err:#}"));
            }
        }
    }
    bail!("all reboot commands failed")
}

fn set_active_slot(slot: &str) -> Result<()> {
    let slot_number = bootctl_slot_number(slot)?;
    let mut errors = Vec::new();
    prepare_bootctl_for_slot_switch();
    for program in bootctl_commands() {
        match Command::new(program)
            .arg("set-active-boot-slot")
            .arg(slot_number)
            .status()
        {
            Ok(status) if status.success() => {
                append_log(format!("set active slot to {slot} via {program}"));
                return Ok(());
            }
            Ok(status) => {
                errors.push(format!("{program} exited with {status}"));
            }
            Err(err) => {
                errors.push(format!("{program} failed: {err:#}"));
            }
        }
    }
    bail!("failed to set active slot to {slot}: {}", errors.join("; "))
}

fn validate_active_slot_switch(slot: &str) -> Result<()> {
    let _ = bootctl_slot_number(slot)?;
    let mut errors = Vec::new();
    prepare_bootctl_for_slot_switch();
    for program in bootctl_commands() {
        match Command::new(program).arg("hal-info").status() {
            Ok(status) if status.success() => {
                append_log(format!(
                    "validated active slot switch support via {program}"
                ));
                return Ok(());
            }
            Ok(status) => {
                errors.push(format!("{program} hal-info exited with {status}"));
            }
            Err(err) => {
                errors.push(format!("{program} hal-info failed: {err:#}"));
            }
        }
    }
    bail!(
        "cannot switch active slot to {slot}; bootctl is unavailable: {}",
        errors.join("; ")
    )
}

fn prepare_bootctl_for_slot_switch() {
    if let Err(err) = crate::assets::ensure_binaries(true) {
        append_log(format!(
            "failed to extract bootctl before slot switch: {err:#}"
        ));
    }
}

const fn bootctl_commands() -> [&'static str; 3] {
    [
        crate::assets::BOOTCTL_PATH,
        "/system/bin/bootctl",
        "bootctl",
    ]
}

fn bootctl_slot_number(slot: &str) -> Result<&'static str> {
    match slot.trim() {
        "_a" | "a" | "0" => Ok("0"),
        "_b" | "b" | "1" => Ok("1"),
        value => bail!("unsupported slot suffix for rescue restore: {value}"),
    }
}

fn mark_skip_modules_once() {
    if let Err(err) = utils::ensure_dir_exists(RESCUE_DIR) {
        append_log(format!(
            "failed to create rescue dir for module skip: {err:#}"
        ));
        return;
    }

    let primary = fs::write(SKIP_MODULES_ONCE_PATH, b"1");
    let cache_compat = fs::write(CACHE_SKIP_MODULES_ONCE_PATH, b"1");
    if primary.is_ok() || cache_compat.is_ok() {
        append_log("temporary module skip will be applied on next boot");
    } else {
        if let Err(err) = primary {
            append_log(format!("failed to mark temporary module skip: {err:#}"));
        }
        if let Err(err) = cache_compat {
            append_log(format!(
                "failed to mark cache temporary module skip: {err:#}"
            ));
        }
    }
}

fn skip_modules_once_exists() -> bool {
    Path::new(SKIP_MODULES_ONCE_PATH).exists() || Path::new(CACHE_SKIP_MODULES_ONCE_PATH).exists()
}

fn clear_runtime_markers() {
    for path in [
        PENDING_BOOT_PATH,
        RESTORE_LOCK_PATH,
        SKIP_MODULES_ONCE_PATH,
        CACHE_SKIP_MODULES_ONCE_PATH,
        SKIP_MODULES_THIS_BOOT_PATH,
        LEGACY_TMP_MODULE_DISABLE_PATH,
    ] {
        let _ = fs::remove_file(path);
    }
}

fn clear_restore_markers() {
    for path in [PENDING_BOOT_PATH, RESTORE_LOCK_PATH] {
        let _ = fs::remove_file(path);
    }
}

fn mark_legacy_tmp_module_disable() {
    if let Err(err) = fs::write(LEGACY_TMP_MODULE_DISABLE_PATH, b"1") {
        append_log(format!(
            "failed to mark legacy temporary module disable: {err:#}"
        ));
    } else {
        append_log("legacy temporary module disable marker will be applied on next boot");
    }
}

fn rescue_modules_for_failed_boot(reason: impl AsRef<str>) {
    append_log(format!("module rescue triggered: {}", reason.as_ref()));
    mark_skip_modules_once();
    disable_all_modules_for_rescue();
}

fn disable_all_modules_for_rescue() {
    let active_modules = active_module_ids();
    if let Err(error) = write_rescue_disabled_module_ids(&active_modules) {
        append_log(format!(
            "failed to record modules disabled by rescue: {error:#}"
        ));
    }
    match module::disable_all_modules() {
        Ok(()) => append_log(format!(
            "{} active modules were persistently disabled for rescue; re-enable them individually after confirming a stable boot",
            active_modules.len()
        )),
        Err(err) => append_log(format!("failed to disable all modules for rescue: {err:#}")),
    }
}

fn active_module_ids() -> Vec<String> {
    let mut ids = Vec::new();
    if let Err(error) = module::foreach_module(module::ModuleType::Active, |path| {
        if let Some(id) = path.file_name().and_then(|name| name.to_str()) {
            ids.push(id.to_owned());
        }
        Ok(())
    }) {
        append_log(format!("failed to enumerate active modules: {error:#}"));
    }
    ids.sort();
    ids.dedup();
    ids
}

fn write_rescue_disabled_module_ids(ids: &[String]) -> Result<()> {
    let value = json!({
        "updatedAt": Local::now().to_rfc3339(),
        "moduleIds": ids,
    });
    atomic_write(
        Path::new(RESCUE_DISABLED_MODULES_PATH),
        value.to_string().as_bytes(),
    )
    .context("failed to persist rescue-disabled modules")
}

fn rescue_disabled_module_ids() -> Vec<String> {
    read_json_file(RESCUE_DISABLED_MODULES_PATH)
        .and_then(|value| value.get("moduleIds").and_then(Value::as_array).cloned())
        .unwrap_or_default()
        .iter()
        .filter_map(Value::as_str)
        .map(ToOwned::to_owned)
        .collect()
}

fn read_rescue_disabled_modules() -> Vec<Value> {
    rescue_disabled_module_ids()
        .into_iter()
        .map(|id| {
            let path = Path::new(defs::MODULE_DIR).join(&id);
            let properties = module::read_module_prop(&path).unwrap_or_default();
            json!({
                "id": id,
                "name": properties.get("name").cloned().unwrap_or_default(),
                "version": properties.get("version").cloned().unwrap_or_default(),
                "installed": path.is_dir(),
                "disabled": path.join(defs::DISABLE_FILE_NAME).exists(),
            })
        })
        .collect()
}

pub fn enable_rescue_module(id: &str) -> Result<()> {
    let mut ids = rescue_disabled_module_ids();
    if !ids.iter().any(|saved| saved == id) {
        return Err(coded_error(
            "rescue.module_not_recorded",
            format!("module {id} was not disabled by rescue protection"),
        ));
    }
    module::enable_module(id).map_err(structured_error)?;
    ids.retain(|saved| saved != id);
    write_rescue_disabled_module_ids(&ids)?;
    append_log(format!("module {id} re-enabled after rescue"));
    Ok(())
}

fn mark_legacy_fix_done_lock() {
    if let Err(err) = fs::write(LEGACY_FIX_DONE_LOCK_PATH, b"1") {
        append_log(format!("failed to mark legacy rescue lock: {err:#}"));
    }
}

fn cleanup_legacy_rescue_flags() {
    for path in [LEGACY_LOOP_FLAG_PATH, LEGACY_PANIC_FLAG_PATH] {
        let _ = fs::remove_file(path);
    }
}

fn find_partition(spec: &PartitionSpec) -> Result<Option<String>> {
    if let Some(path) = spec.custom_path.as_deref() {
        if Path::new(path).exists() {
            let resolved = Path::new(path)
                .canonicalize()
                .with_context(|| format!("failed to resolve custom partition path {path}"))?;
            if !resolved.starts_with("/dev/block") {
                bail!(
                    "custom partition path for {} resolves outside /dev/block: {}",
                    spec.name,
                    resolved.display()
                );
            }
            #[cfg(target_os = "android")]
            {
                use std::os::unix::fs::FileTypeExt;
                if !fs::metadata(&resolved)?.file_type().is_block_device() {
                    bail!(
                        "custom partition path for {} is not a block device: {}",
                        spec.name,
                        resolved.display()
                    );
                }
            }
            return Ok(Some(resolved.display().to_string()));
        }
        bail!(
            "custom partition path for {} does not exist: {path}",
            spec.name
        );
    }
    Ok(
        boot_patch::find_partition_path(&spec.name, spec.ota)
            .map(|path| path.display().to_string()),
    )
}

fn image_status(spec: &PartitionSpec, manifest: &Value, deep: bool) -> serde_json::Value {
    let device = find_partition(spec).ok().flatten().unwrap_or_default();
    let size = fs::metadata(&spec.image_path).map_or(0, |metadata| metadata.len());
    let partition_size = partition_size(&device);
    let manifest_sha256 = manifest_sha256_from(manifest, &spec.label).unwrap_or_default();
    let actual_sha256 = if deep && size > 0 {
        cached_sha256(&spec.image_path, false)
    } else {
        String::new()
    };
    let sha256 = if actual_sha256.is_empty() {
        manifest_sha256.clone()
    } else {
        actual_sha256.clone()
    };
    json!({
        "name": spec.name,
        "label": spec.label,
        "partition": device,
        "image": spec.image_path,
        "required": spec.required,
        "custom": spec.custom_path.is_some(),
        "otherSlot": spec.ota,
        "restore": spec.restore,
        "dangerous": is_dangerous_partition(&spec.name),
        "exists": size > 0,
        "size": size,
        "partitionSize": partition_size,
        "sha256": sha256,
        "sha256Ok": !deep || manifest_sha256.is_empty() || manifest_sha256 == actual_sha256,
        "verificationState": if deep { "verified" } else if manifest_sha256.is_empty() { "unknown" } else { "cached" },
        "sizeOk": partition_size == 0 || size == 0 || partition_size == size,
    })
}

fn write_manifest(specs: &[PartitionSpec]) -> Result<()> {
    let images = specs
        .iter()
        .map(|spec| {
            let device = find_partition(spec).ok().flatten().unwrap_or_default();
            let size = fs::metadata(&spec.image_path).map_or(0, |metadata| metadata.len());
            let partition_size = partition_size(&device);
            let sha256 = if size > 0 {
                cached_sha256(&spec.image_path, true)
            } else {
                String::new()
            };
            json!({
                "name": spec.name,
                "label": spec.label,
                "partition": device,
                "image": spec.image_path,
                "required": spec.required,
                "custom": spec.custom_path.is_some(),
                "otherSlot": spec.ota,
                "restore": spec.restore,
                "dangerous": is_dangerous_partition(&spec.name),
                "exists": size > 0,
                "size": size,
                "partitionSize": partition_size,
                "sha256": sha256,
                "sha256Ok": true,
                "sizeOk": partition_size == 0 || size == 0 || partition_size == size,
            })
        })
        .collect::<Vec<_>>();
    let manifest = json!({
        "createdAt": Local::now().format("%Y-%m-%d %H:%M:%S").to_string(),
        "slot": current_slot(),
        "device": device_summary(),
        "config": config_json(&read_config()?),
        "images": images,
    });
    atomic_write(Path::new(MANIFEST_PATH), manifest.to_string().as_bytes())
        .context("failed to write rescue manifest")
}

fn read_manifest() -> Result<Value> {
    if !Path::new(MANIFEST_PATH).exists() {
        return Ok(json!({}));
    }
    let content = fs::read_to_string(MANIFEST_PATH).context("failed to read rescue manifest")?;
    serde_json::from_str(&content).context("invalid rescue manifest")
}

fn validate_manifest_context_for_restore() -> Result<Value> {
    let manifest = read_manifest()?;
    if manifest.as_object().is_none_or(serde_json::Map::is_empty) {
        bail!("rescue manifest is missing; please backup first");
    }

    let saved_fingerprint = manifest
        .get("device")
        .and_then(|device| device.get("fingerprint"))
        .and_then(Value::as_str)
        .unwrap_or_default();
    let now_fingerprint = utils::getprop("ro.build.fingerprint").unwrap_or_default();
    if !saved_fingerprint.is_empty() && saved_fingerprint != now_fingerprint {
        if !is_recovery_boot() {
            bail!("device fingerprint mismatch; please create a fresh backup");
        }

        let saved_device = manifest
            .get("device")
            .and_then(|device| device.get("device"))
            .and_then(Value::as_str)
            .unwrap_or_default();
        let now_device = utils::getprop("ro.product.device").unwrap_or_default();
        if saved_device.is_empty() || now_device.is_empty() || saved_device != now_device {
            bail!("device identity mismatch in recovery; refusing rescue restore");
        }
        append_log("recovery fingerprint differs; device codename matched backup manifest");
    }

    let saved_config = manifest.get("config").cloned().unwrap_or_else(|| json!({}));
    let saved_config = parse_config(&saved_config.to_string())
        .map(|config| config_json(&config))
        .unwrap_or(saved_config);
    let current_config = config_json(&read_config().unwrap_or_default());
    if saved_config != current_config {
        bail!("rescue config changed after backup; please create a fresh backup");
    }

    Ok(manifest)
}

fn manifest_slot(manifest: &Value) -> String {
    manifest
        .get("slot")
        .and_then(Value::as_str)
        .unwrap_or_default()
        .to_string()
}

fn manifest_sha256_from(manifest: &Value, name: &str) -> Option<String> {
    let images = manifest.get("images")?.as_array()?;
    images.iter().find_map(|image| {
        let image_name = image.get("label").or_else(|| image.get("name"))?.as_str()?;
        if image_name == name {
            image.get("sha256")?.as_str().map(ToOwned::to_owned)
        } else {
            None
        }
    })
}

fn cached_sha256(path: &str, refresh: bool) -> String {
    let Some(signature) = file_signature(Path::new(path)) else {
        return String::new();
    };
    let mut cache = read_json_file(HASH_CACHE_PATH).unwrap_or_else(|| json!({}));
    if !refresh
        && let Some(entry) = cache.get(path)
        && entry.get("size").and_then(Value::as_u64) == Some(signature.0)
        && entry.get("modifiedNanos").and_then(Value::as_u64) == Some(signature.1)
        && let Some(digest) = entry.get("sha256").and_then(Value::as_str)
        && !digest.is_empty()
    {
        return digest.to_owned();
    }

    let digest = sha256_of(path);
    if digest.is_empty() {
        return digest;
    }
    if !cache.is_object() {
        cache = json!({});
    }
    if let Some(entries) = cache.as_object_mut() {
        entries.insert(
            path.to_owned(),
            json!({
                "size": signature.0,
                "modifiedNanos": signature.1,
                "sha256": digest,
            }),
        );
        if let Err(error) = atomic_write(Path::new(HASH_CACHE_PATH), cache.to_string().as_bytes()) {
            append_log(format!("failed to persist SHA256 cache: {error:#}"));
        }
    }
    digest
}

fn file_signature(path: &Path) -> Option<(u64, u64)> {
    let metadata = fs::metadata(path).ok()?;
    let modified = metadata.modified().ok()?.duration_since(UNIX_EPOCH).ok()?;
    let modified_nanos = modified
        .as_secs()
        .saturating_mul(1_000_000_000)
        .saturating_add(u64::from(modified.subsec_nanos()));
    Some((metadata.len(), modified_nanos))
}

fn file_signature_json(path: &Path) -> Value {
    file_signature(path).map_or_else(
        || json!({}),
        |(size, modified_nanos)| {
            json!({
                "path": path.display().to_string(),
                "size": size,
                "modifiedNanos": modified_nanos,
            })
        },
    )
}

fn read_json_file(path: &str) -> Option<Value> {
    let content = fs::read_to_string(path).ok()?;
    serde_json::from_str(&content).ok()
}

fn write_verification_marker(specs: &[PartitionSpec], manifest: &Value) -> Result<()> {
    let files = specs
        .iter()
        .filter(|spec| Path::new(&spec.image_path).is_file())
        .map(|spec| file_signature_json(Path::new(&spec.image_path)))
        .collect::<Vec<_>>();
    let marker = json!({
        "verifiedAt": Local::now().to_rfc3339(),
        "manifestSha256": sha256_of(MANIFEST_PATH),
        "manifestCreatedAt": manifest.get("createdAt").and_then(Value::as_str).unwrap_or_default(),
        "files": files,
    });
    atomic_write(Path::new(VERIFIED_PATH), marker.to_string().as_bytes())
        .context("failed to persist rescue verification marker")
}

fn verification_marker_is_current(specs: &[PartitionSpec], manifest: &Value) -> bool {
    let Some(marker) = read_json_file(VERIFIED_PATH) else {
        return false;
    };
    let manifest_sha256 = marker
        .get("manifestSha256")
        .and_then(Value::as_str)
        .unwrap_or_default();
    if manifest_sha256.is_empty() || manifest_sha256 != sha256_of(MANIFEST_PATH) {
        return false;
    }
    let Some(files) = marker.get("files").and_then(Value::as_array) else {
        return false;
    };
    let expected = files
        .iter()
        .filter_map(|entry| {
            Some((
                entry.get("path")?.as_str()?.to_owned(),
                (
                    entry.get("size")?.as_u64()?,
                    entry.get("modifiedNanos")?.as_u64()?,
                ),
            ))
        })
        .collect::<BTreeMap<_, _>>();
    specs
        .iter()
        .filter(|spec| Path::new(&spec.image_path).is_file())
        .all(|spec| {
            file_signature(Path::new(&spec.image_path))
                .is_some_and(|signature| expected.get(&spec.image_path) == Some(&signature))
        })
        && !expected.is_empty()
        && manifest.get("createdAt").and_then(Value::as_str)
            == marker.get("manifestCreatedAt").and_then(Value::as_str)
}

fn write_environment_check(config: &RescueConfig) -> Result<()> {
    let marker = json!({
        "checkedAt": Local::now().to_rfc3339(),
        "config": config_json(config),
        "device": device_summary(),
    });
    atomic_write(
        Path::new(ENVIRONMENT_CHECK_PATH),
        marker.to_string().as_bytes(),
    )
    .context("failed to persist rescue environment check")
}

fn environment_check_is_current(config: &RescueConfig) -> bool {
    let Some(marker) = read_json_file(ENVIRONMENT_CHECK_PATH) else {
        return false;
    };
    marker.get("config") == Some(&config_json(config))
        && marker
            .get("device")
            .and_then(|device| device.get("device"))
            .and_then(Value::as_str)
            == device_summary().get("device").and_then(Value::as_str)
}

#[allow(clippy::fn_params_excessive_bools)]
const fn rescue_phase(
    status_ok: bool,
    enabled: bool,
    backup_ready: bool,
    verified: bool,
    config_changed: bool,
    restore_interrupted: bool,
) -> &'static str {
    if !status_ok {
        "unavailable"
    } else if restore_interrupted {
        "restore_error"
    } else if config_changed {
        "config_changed"
    } else if !backup_ready {
        "needs_backup"
    } else if !verified {
        "needs_verification"
    } else if enabled {
        "protected"
    } else {
        "ready_to_enable"
    }
}

fn read_config() -> Result<RescueConfig> {
    if !Path::new(CONFIG_PATH).exists() {
        return Ok(RescueConfig::default());
    }
    let content = fs::read_to_string(CONFIG_PATH).context("failed to read rescue config")?;
    parse_config(&content)
}

fn write_config(config: &RescueConfig) -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    atomic_write(
        Path::new(CONFIG_PATH),
        config_json(config).to_string().as_bytes(),
    )
    .context("failed to write rescue config")
}

fn parse_config(content: &str) -> Result<RescueConfig> {
    let value: Value = serde_json::from_str(content).context("invalid rescue config JSON")?;
    let mut custom_partitions = BTreeMap::new();
    if let Some(object) = value.get("customPartitions").and_then(Value::as_object) {
        for (name, path) in object {
            if !is_known_partition(name) {
                bail!("unsupported custom rescue partition: {name}");
            }
            let path = path
                .as_str()
                .and_then(sanitize_partition_path)
                .with_context(|| format!("invalid custom partition path for {name}"))?;
            custom_partitions.insert(name.clone(), path);
        }
    }
    Ok(RescueConfig {
        include_dtbo: value
            .get("includeDtbo")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        include_vbmeta: value
            .get("includeVbmeta")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        backup_other_slot: value
            .get("backupOtherSlot")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        dangerous_auto_restore: if value
            .get("allowDangerousAutoRestore")
            .and_then(Value::as_bool)
            .unwrap_or(false)
        {
            DangerousAutoRestore::Allow
        } else {
            DangerousAutoRestore::Skip
        },
        custom_partitions,
    })
}

fn config_json(config: &RescueConfig) -> Value {
    json!({
        "includeDtbo": config.include_dtbo,
        "includeVbmeta": config.include_vbmeta,
        "backupOtherSlot": config.backup_other_slot,
        "allowDangerousAutoRestore": config.dangerous_auto_restore.is_allowed(),
        "customPartitions": config.custom_partitions,
    })
}

fn sanitize_partition_path(path: &str) -> Option<String> {
    let path = path.trim();
    let candidate = Path::new(path);
    if candidate.is_absolute()
        && candidate.starts_with("/dev/block")
        && candidate
            .components()
            .all(|component| matches!(component, Component::RootDir | Component::Normal(_)))
        && !path.contains('\0')
        && !path.contains('"')
        && !path.contains('\'')
    {
        Some(path.to_string())
    } else {
        None
    }
}

fn is_known_partition(name: &str) -> bool {
    matches!(
        name,
        "boot" | "vendor_boot" | "init_boot" | "dtbo" | "vbmeta"
    )
}

fn is_dangerous_partition(name: &str) -> bool {
    matches!(name, "dtbo" | "vbmeta")
}

fn normalize_partition_name(name: &str) -> Result<String> {
    let name = name.trim();
    match name {
        "boot" | "dtbo" | "vbmeta" => Ok(name.to_string()),
        "init_boot" | "initboot" | "intboot" => Ok("init_boot".to_string()),
        "vendor_boot" | "vendorboot" | "verboot" => Ok("vendor_boot".to_string()),
        _ => bail!("unsupported rescue partition: {name}"),
    }
}

fn ensure_safe_import_source(source: &Path) -> Result<PathBuf> {
    let path = source
        .canonicalize()
        .with_context(|| format!("failed to resolve {}", source.display()))?;
    if !path.is_file() {
        bail!("source image is not a regular file: {}", source.display());
    }
    let size = fs::metadata(&path)?.len();
    if size == 0 {
        bail!("source image is empty");
    }
    Ok(path)
}

fn preserve_file(path: &str) -> Result<()> {
    let source = Path::new(path);
    let backup = backup_path(source);
    if !source.exists() {
        remove_file_if_exists(&backup)?;
        return Ok(());
    }
    atomic_copy(source, &backup)
        .with_context(|| format!("failed to preserve previous backup {}", source.display()))?;
    Ok(())
}

fn restore_preserved_file(path: &str) -> Result<()> {
    let source = Path::new(path);
    let backup = backup_path(source);
    if backup.exists() {
        atomic_copy(&backup, source)
            .with_context(|| format!("failed to restore previous backup {}", source.display()))?;
    } else {
        remove_file_if_exists(source)?;
    }
    Ok(())
}

fn rescue_file_paths(specs: &[PartitionSpec]) -> Vec<String> {
    let mut paths = Vec::with_capacity(specs.len() + 1);
    paths.push(MANIFEST_PATH.to_string());
    paths.extend(specs.iter().map(|spec| spec.image_path.clone()));
    paths.sort();
    paths.dedup();
    paths
}

fn preserve_files(paths: &[String]) -> Result<()> {
    let mut preserved = Vec::with_capacity(paths.len());
    for path in paths {
        if let Err(err) = preserve_file(path) {
            cleanup_preserved_files(&preserved);
            return Err(err);
        }
        preserved.push(path.clone());
    }
    Ok(())
}

fn restore_preserved_files(paths: &[String]) -> Result<()> {
    let mut errors = Vec::new();
    for path in paths {
        if let Err(err) = restore_preserved_file(path) {
            errors.push(format!("{path}: {err:#}"));
        }
    }
    if errors.is_empty() {
        cleanup_preserved_files(paths);
        Ok(())
    } else {
        bail!("{}", errors.join("; "))
    }
}

fn cleanup_preserved_files(paths: &[String]) {
    for path in paths {
        let _ = fs::remove_file(backup_path(Path::new(path)));
    }
}

fn backup_path(path: &Path) -> PathBuf {
    let file_name = path
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("backup");
    path.with_file_name(format!("{file_name}.bak"))
}

fn validate_import_source_against_partition(
    source: &Path,
    spec: &PartitionSpec,
    device: &str,
) -> Result<()> {
    let image_size = fs::metadata(source)
        .with_context(|| format!("failed to read import metadata for {}", spec.name))?
        .len();
    let device_size = partition_size(device);
    if device_size == 0 {
        bail!("failed to determine {} partition size", spec.name);
    }
    if image_size != device_size {
        bail!(
            "{} import size mismatch: image={}, partition={}",
            spec.name,
            image_size,
            device_size
        );
    }
    Ok(())
}

fn atomic_copy(source: &Path, destination: &Path) -> Result<u64> {
    let parent = destination
        .parent()
        .with_context(|| format!("{} has no parent", destination.display()))?;
    utils::ensure_dir_exists(parent)?;
    let mut input =
        fs::File::open(source).with_context(|| format!("failed to open {}", source.display()))?;
    let mut temporary = tempfile::NamedTempFile::new_in(parent)
        .with_context(|| format!("failed to create temporary file in {}", parent.display()))?;
    let copied = io::copy(&mut input, &mut temporary)
        .with_context(|| format!("failed to copy {}", source.display()))?;
    temporary.as_file().sync_all().with_context(|| {
        format!(
            "failed to sync temporary copy for {}",
            destination.display()
        )
    })?;
    temporary
        .persist(destination)
        .map_err(|err| err.error)
        .with_context(|| format!("failed to atomically replace {}", destination.display()))?;
    sync_parent_directory(parent);
    Ok(copied)
}

fn atomic_write(path: &Path, content: &[u8]) -> Result<()> {
    let parent = path
        .parent()
        .with_context(|| format!("{} has no parent", path.display()))?;
    utils::ensure_dir_exists(parent)?;
    let mut temporary = tempfile::NamedTempFile::new_in(parent)
        .with_context(|| format!("failed to create temporary file in {}", parent.display()))?;
    temporary
        .write_all(content)
        .with_context(|| format!("failed to write temporary file for {}", path.display()))?;
    temporary
        .as_file()
        .sync_all()
        .with_context(|| format!("failed to sync temporary file for {}", path.display()))?;
    temporary
        .persist(path)
        .map_err(|err| err.error)
        .with_context(|| format!("failed to atomically replace {}", path.display()))?;
    sync_parent_directory(parent);
    Ok(())
}

fn remove_file_if_exists(path: &Path) -> Result<()> {
    match fs::remove_file(path) {
        Ok(()) => Ok(()),
        Err(err) if err.kind() == io::ErrorKind::NotFound => Ok(()),
        Err(err) => Err(err).with_context(|| format!("failed to remove {}", path.display())),
    }
}

fn sync_parent_directory(parent: &Path) {
    if let Ok(directory) = fs::File::open(parent) {
        let _ = directory.sync_all();
    }
}

fn current_slot() -> String {
    boot_patch::get_slot_suffix(false).unwrap_or_default()
}

fn current_boot_id() -> Result<String> {
    let value = fs::read_to_string("/proc/sys/kernel/random/boot_id")
        .context("failed to read current boot ID")?;
    let value = value.trim().to_owned();
    if value.is_empty() {
        bail!("current boot ID is empty");
    }
    Ok(value)
}

fn boot_mode() -> String {
    boot_mode_values().into_iter().next().unwrap_or_default()
}

fn boot_mode_values() -> Vec<String> {
    [
        "ro.bootmode",
        "ro.boot.bootmode",
        "vendor.boot.bootmode",
        "ro.boot.mode",
        "ro.boot.recovery",
    ]
    .into_iter()
    .filter_map(utils::getprop)
    .map(|value| value.trim().to_ascii_lowercase())
    .filter(|value| !value.is_empty() && value != "unknown")
    .collect()
}

fn is_recovery_boot() -> bool {
    boot_mode_values()
        .iter()
        .any(|mode| is_recovery_mode_value(mode))
}

fn is_recovery_mode_value(mode: &str) -> bool {
    matches!(
        mode.trim().to_ascii_lowercase().as_str(),
        "1" | "true" | "yes" | "recovery" | "rec"
    )
}

fn device_summary() -> Value {
    json!({
        "brand": utils::getprop("ro.product.brand").unwrap_or_default(),
        "model": utils::getprop("ro.product.model").unwrap_or_default(),
        "device": utils::getprop("ro.product.device").unwrap_or_default(),
        "fingerprint": utils::getprop("ro.build.fingerprint").unwrap_or_default(),
        "kernel": std::fs::read_to_string("/proc/sys/kernel/osrelease").unwrap_or_default().trim(),
    })
}

fn is_enabled() -> bool {
    Path::new(ENABLED_PATH).exists()
}

fn read_boot_count() -> u32 {
    fs::read_to_string(BOOT_COUNT_PATH)
        .ok()
        .and_then(|value| value.trim().parse().ok())
        .unwrap_or(0)
}

fn read_auto_restore_attempts() -> u32 {
    fs::read_to_string(AUTO_RESTORE_ATTEMPTS_PATH)
        .ok()
        .and_then(|value| value.trim().parse().ok())
        .unwrap_or(0)
}

fn write_auto_restore_attempts(value: u32) {
    if let Err(err) = fs::write(AUTO_RESTORE_ATTEMPTS_PATH, value.to_string()) {
        append_log(format!("failed to write auto restore attempts: {err:#}"));
    }
}

fn write_boot_count(value: u32) {
    if let Err(err) = fs::write(BOOT_COUNT_PATH, value.to_string()) {
        append_log(format!("failed to write boot counter: {err:#}"));
    }
}

fn has_boot_failure_hint() -> bool {
    has_legacy_failure_hint() || has_boot_reason_failure_hint() || has_fresh_failure_artifact_hint()
}

fn has_legacy_failure_hint() -> bool {
    let mut found = false;
    for path in [LEGACY_LOOP_FLAG_PATH, LEGACY_PANIC_FLAG_PATH] {
        if Path::new(path).exists() {
            append_log(format!("legacy rescue failure hint found: {path}"));
            found = true;
        }
    }
    found
}

fn has_boot_reason_failure_hint() -> bool {
    let text = boot_reason_text();
    let found = has_boot_reason_failure_text(&text);
    if found {
        append_log(format!("boot failure hint found in boot reason: {text}"));
    }
    found
}

fn boot_reason_text() -> String {
    [
        "sys.boot.reason",
        "ro.boot.bootreason",
        "ro.boot.boot_reason",
        "ro.boot.hardware.reboot_reason",
    ]
    .into_iter()
    .filter_map(utils::getprop)
    .map(|value| value.trim().to_ascii_lowercase())
    .collect::<Vec<_>>()
    .join("\n")
}

fn has_fresh_failure_artifact_hint() -> bool {
    let current = failure_artifacts();
    let Some(baseline_value) = read_json_file(FAILURE_BASELINE_PATH) else {
        append_log(
            "failure evidence baseline is missing; record current artifacts without triggering rollback",
        );
        if let Err(error) = write_failure_baseline() {
            append_log(format!(
                "failed to initialize failure evidence baseline: {error:#}"
            ));
        }
        return false;
    };
    let baseline = baseline_value
        .get("artifacts")
        .and_then(Value::as_object)
        .cloned()
        .unwrap_or_default();

    if let Some(path) = fresh_failure_artifact_path(&baseline, &current) {
        append_log(format!("fresh boot failure evidence found in {path}"));
        return true;
    }
    false
}

fn fresh_failure_artifact_path(
    baseline: &serde_json::Map<String, Value>,
    current: &BTreeMap<String, Value>,
) -> Option<String> {
    for (path, artifact) in current {
        let digest = artifact
            .get("sha256")
            .and_then(Value::as_str)
            .unwrap_or_default();
        let has_failure = artifact
            .get("hasFailure")
            .and_then(Value::as_bool)
            .unwrap_or(false);
        let baseline_digest = baseline
            .get(path.as_str())
            .and_then(Value::as_str)
            .unwrap_or_default();
        if has_failure && !digest.is_empty() && digest != baseline_digest {
            return Some(path.clone());
        }
    }
    None
}

fn write_failure_baseline() -> Result<()> {
    let artifacts = failure_artifacts()
        .into_iter()
        .map(|(path, value)| {
            (
                path,
                Value::String(
                    value
                        .get("sha256")
                        .and_then(Value::as_str)
                        .unwrap_or_default()
                        .to_owned(),
                ),
            )
        })
        .collect::<serde_json::Map<_, _>>();
    let baseline = json!({
        "createdAt": Local::now().to_rfc3339(),
        "bootId": current_boot_id().unwrap_or_default(),
        "artifacts": artifacts,
    });
    atomic_write(
        Path::new(FAILURE_BASELINE_PATH),
        baseline.to_string().as_bytes(),
    )
    .context("failed to persist failure evidence baseline")
}

fn failure_artifacts() -> BTreeMap<String, Value> {
    let mut paths = vec![PathBuf::from("/proc/last_kmsg")];
    if let Ok(entries) = fs::read_dir("/sys/fs/pstore") {
        let mut pstore_paths = entries
            .flatten()
            .filter_map(|entry| {
                let path = entry.path();
                let name = path.file_name()?.to_str()?;
                (name.contains("ramoops") || name.contains("console") || name.contains("dmesg"))
                    .then_some(path)
            })
            .collect::<Vec<_>>();
        pstore_paths.sort();
        pstore_paths.truncate(FAILURE_ARTIFACT_MAX_FILES);
        paths.extend(pstore_paths);
    }

    paths
        .into_iter()
        .filter_map(|path| {
            failure_artifact_from_path(&path, FAILURE_ARTIFACT_SCAN_BYTES)
                .map(|artifact| (path.display().to_string(), artifact))
        })
        .collect()
}

fn failure_artifact_from_path(path: &Path, max_scan_bytes: u64) -> Option<Value> {
    let digest = sha256::try_digest(path).ok()?;
    let mut sample = Vec::new();
    fs::File::open(path)
        .ok()?
        .take(max_scan_bytes.saturating_add(1))
        .read_to_end(&mut sample)
        .ok()?;
    let truncated = sample.len() as u64 > max_scan_bytes;
    sample.truncate(max_scan_bytes.min(usize::MAX as u64) as usize);
    let text = String::from_utf8_lossy(&sample);
    Some(json!({
        "sha256": digest,
        "hasFailure": has_failure_text(&text),
        "scannedBytes": sample.len(),
        "scanTruncated": truncated,
    }))
}

fn has_failure_text(text: &str) -> bool {
    text.lines().any(|line| {
        let line = line.to_ascii_lowercase();
        let has_failure_word = line.contains("fail")
            || line.contains("error")
            || line.contains("invalid")
            || line.contains("mismatch")
            || line.contains("corrupt");
        line.contains("kernel panic")
            || line.contains("panic - not syncing")
            || line.contains("watchdog bite")
            || line.contains("watchdog bark")
            || line.contains("watchdog timeout")
            || line.contains("watchdog reset")
            || line.contains("ramdump")
            || line.contains("boot verification")
            || line.contains("dtb load fail")
            || line.contains("verification failed")
            || line.contains("invalid magic")
            || line.contains("bad magic")
            || has_failure_word
                && [
                    "dm-verity",
                    "dtb",
                    "dtbo",
                    "avb",
                    "vbmeta",
                    "init_boot",
                    "vendor_boot",
                    "gki",
                    "kmi",
                ]
                .iter()
                .any(|token| line.contains(token))
    })
}

fn has_boot_reason_failure_text(text: &str) -> bool {
    let text = text.to_ascii_lowercase();
    [
        "kernel_panic",
        "kernel panic",
        "watchdog",
        "ramdump",
        "dm-verity",
        "boot verification",
        "hard_reset",
    ]
    .iter()
    .any(|token| text.contains(token))
}

fn sha256_of(path: &str) -> String {
    sha256::try_digest(Path::new(path)).unwrap_or_default()
}

fn partition_size(path: &str) -> u64 {
    if path.is_empty() {
        return 0;
    }

    if let Ok(output) = Command::new("blockdev")
        .arg("--getsize64")
        .arg(path)
        .output()
        && output.status.success()
        && let Ok(text) = String::from_utf8(output.stdout)
        && let Ok(size) = text.trim().parse::<u64>()
    {
        return size;
    }

    fs::metadata(path).map_or(0, |metadata| metadata.len())
}

fn append_log(message: impl AsRef<str>) {
    if let Err(err) = append_log_inner(message.as_ref()) {
        log::warn!("rescue: failed to append log: {err}");
    }
}

fn append_log_inner(message: &str) -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    rotate_log_if_needed()?;
    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(LOG_PATH)
        .with_context(|| format!("failed to open {LOG_PATH}"))?;
    let timestamp = Local::now().format("%Y-%m-%d %H:%M:%S");
    writeln!(file, "[{timestamp}] {message}").context("failed to write rescue log")
}

fn rotate_log_if_needed() -> Result<()> {
    let Ok(metadata) = fs::metadata(LOG_PATH) else {
        return Ok(());
    };
    if metadata.len() < LOG_MAX_BYTES {
        return Ok(());
    }

    remove_file_if_exists(Path::new(&format!("{LOG_PATH}.{LOG_ROTATION_COUNT}")))?;
    for index in (1..LOG_ROTATION_COUNT).rev() {
        let source = format!("{LOG_PATH}.{index}");
        let destination = format!("{LOG_PATH}.{}", index + 1);
        if Path::new(&source).exists() {
            fs::rename(&source, &destination).with_context(|| {
                format!("failed to rotate rescue log {source} to {destination}")
            })?;
        }
    }
    fs::rename(LOG_PATH, format!("{LOG_PATH}.1")).context("failed to rotate rescue log")
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

#[cfg(test)]
mod tests {
    use super::{
        BootRescueAction, COPY_TOTAL_TIMEOUT_SECONDS, PartitionSpec, RestoreTransaction,
        RestoreTransactionEntry, backup_path, copy_with_clock, error_code,
        failure_artifact_from_path, fresh_failure_artifact_path, invalidate_protection_markers_at,
        is_recovery_mode_value, normalize_partition_name, parse_config, post_fs_data_rescue_action,
        preserve_file, read_restore_transaction_from, recovery_boot_rescue_action,
        restore_preserved_file, restore_transaction_json, restore_transactions_match,
        select_resumable_restore_transaction, validate_import_source_against_partition,
    };
    use serde_json::{Map, Value, json};
    use std::{collections::BTreeMap, fs, io::Cursor, time::Duration};

    #[test]
    fn normalizes_supported_partition_aliases() {
        assert_eq!(normalize_partition_name("initboot").unwrap(), "init_boot");
        assert_eq!(normalize_partition_name("verboot").unwrap(), "vendor_boot");
        assert!(normalize_partition_name("system").is_err());
    }

    #[test]
    fn rejects_invalid_custom_partition_paths() {
        let invalid_path = r#"{"customPartitions":{"boot":"/data/local/tmp/boot.img"}}"#;
        let unknown_partition = r#"{"customPartitions":{"system":"/dev/block/system"}}"#;
        assert!(parse_config(invalid_path).is_err());
        assert!(parse_config(unknown_partition).is_err());
    }

    #[test]
    fn validates_import_size_before_replacing_backup() {
        let directory = tempfile::tempdir().unwrap();
        let source = directory.path().join("source.img");
        let partition = directory.path().join("boot");
        fs::write(&source, b"short").unwrap();
        fs::write(&partition, b"partition").unwrap();
        let spec = PartitionSpec {
            name: "boot".to_string(),
            label: "boot".to_string(),
            image_path: directory.path().join("boot.img").display().to_string(),
            required: true,
            custom_path: None,
            ota: false,
            restore: true,
        };
        assert!(
            validate_import_source_against_partition(
                &source,
                &spec,
                &partition.display().to_string(),
            )
            .is_err()
        );
    }

    #[test]
    fn restores_preserved_file_and_does_not_reuse_stale_backup() {
        let directory = tempfile::tempdir().unwrap();
        let target = directory.path().join("boot.img");
        let target_text = target.display().to_string();
        fs::write(&target, b"known-good").unwrap();
        preserve_file(&target_text).unwrap();
        fs::write(&target, b"new-image").unwrap();
        restore_preserved_file(&target_text).unwrap();
        assert_eq!(fs::read(&target).unwrap(), b"known-good");

        fs::remove_file(&target).unwrap();
        assert!(backup_path(&target).exists());
        preserve_file(&target_text).unwrap();
        assert!(!backup_path(&target).exists());
        fs::write(&target, b"partial-import").unwrap();
        restore_preserved_file(&target_text).unwrap();
        assert!(!target.exists());
    }

    #[test]
    fn keeps_image_rollback_scoped_to_pending_flashes() {
        assert_eq!(
            post_fs_data_rescue_action(true, false, 2, false),
            BootRescueAction::RestoreBackups,
        );
        assert_eq!(
            post_fs_data_rescue_action(false, false, 2, false),
            BootRescueAction::DisableModules,
        );
        assert_eq!(
            post_fs_data_rescue_action(false, true, 1, false),
            BootRescueAction::None,
        );
    }

    #[test]
    fn recovery_requires_failure_evidence_or_repeated_pending_boots() {
        assert_eq!(
            recovery_boot_rescue_action(true, false, 1),
            BootRescueAction::None,
        );
        assert_eq!(
            recovery_boot_rescue_action(true, true, 1),
            BootRescueAction::RestoreBackups,
        );
        assert_eq!(
            recovery_boot_rescue_action(true, false, 2),
            BootRescueAction::RestoreBackups,
        );
        assert_eq!(
            recovery_boot_rescue_action(false, true, 1),
            BootRescueAction::DisableModules,
        );
    }

    #[test]
    fn recovery_detection_accepts_only_explicit_current_modes() {
        for mode in ["1", "true", "yes", "recovery", "REC"] {
            assert!(
                is_recovery_mode_value(mode),
                "expected recovery mode: {mode}"
            );
        }
        for mode in [
            "normal",
            "reboot,recovery",
            "recovery-requested",
            "kernel_panic_recovery",
        ] {
            assert!(
                !is_recovery_mode_value(mode),
                "ambiguous value must not trigger recovery restore: {mode}"
            );
        }
    }

    #[test]
    fn invalidating_verification_pauses_enabled_protection() {
        let directory = tempfile::tempdir().unwrap();
        let enabled = directory.path().join("enabled");
        let verified = directory.path().join("verified.json");
        fs::write(&enabled, b"1").unwrap();
        fs::write(&verified, b"verified").unwrap();

        assert!(invalidate_protection_markers_at(&enabled, &verified).unwrap());
        assert!(!enabled.exists());
        assert!(!verified.exists());

        fs::write(&verified, b"verified-again").unwrap();
        assert!(!invalidate_protection_markers_at(&enabled, &verified).unwrap());
        assert!(!verified.exists());
    }

    #[test]
    fn failure_artifact_sampling_is_bounded_but_digest_is_complete() {
        let directory = tempfile::tempdir().unwrap();
        let artifact = directory.path().join("console-ramoops");
        let content = b"kernel panic\n0123456789abcdefghijklmnopqrstuvwxyz";
        fs::write(&artifact, content).unwrap();

        let report = failure_artifact_from_path(&artifact, 16).unwrap();
        assert_eq!(report["sha256"], sha256::digest(content));
        assert_eq!(report["hasFailure"], true);
        assert_eq!(report["scannedBytes"], 16);
        assert_eq!(report["scanTruncated"], true);
    }

    #[test]
    fn stale_pstore_evidence_does_not_trigger_again() {
        let mut baseline = Map::new();
        baseline.insert("/sys/fs/pstore/console-ramoops".to_owned(), json!("old"));
        let current = BTreeMap::from([(
            "/sys/fs/pstore/console-ramoops".to_owned(),
            json!({"sha256":"old","hasFailure":true}),
        )]);

        assert!(fresh_failure_artifact_path(&baseline, &current).is_none());
    }

    #[test]
    fn fresh_pstore_evidence_is_detected_against_baseline() {
        let mut baseline = Map::new();
        baseline.insert("/sys/fs/pstore/console-ramoops".to_owned(), json!("old"));
        let current = BTreeMap::from([(
            "/sys/fs/pstore/console-ramoops".to_owned(),
            json!({"sha256":"new","hasFailure":true}),
        )]);

        assert_eq!(
            fresh_failure_artifact_path(&baseline, &current),
            Some("/sys/fs/pstore/console-ramoops".to_owned()),
        );
    }

    #[test]
    fn restore_transaction_round_trip_preserves_verified_progress() {
        let entries = vec![RestoreTransactionEntry {
            name: "boot".to_owned(),
            label: "boot".to_owned(),
            image_path: "/data/adb/ksu/rescue/boot.img".to_owned(),
            device_path: "/dev/block/by-name/boot_a".to_owned(),
            expected_sha256: "abc".to_owned(),
            expected_size: 4096,
            status: "verified".to_owned(),
        }];
        let transaction = RestoreTransaction {
            id: "test".to_owned(),
            reason: "unit test".to_owned(),
            automatic: false,
            description: "restore current slot".to_owned(),
            activate_slot: None,
            phase: "writing".to_owned(),
            error_code: String::new(),
            error_message: String::new(),
            started_at: "start".to_owned(),
            updated_at: "update".to_owned(),
            entries: entries.clone(),
        };

        let value = restore_transaction_json(&transaction);
        assert_eq!(value["phase"], Value::String("writing".to_owned()));
        assert_eq!(value["entries"][0]["status"], "verified");
        assert!(restore_transactions_match(&transaction, &entries, None));
    }

    #[test]
    fn interrupted_restore_resumes_verified_progress_after_process_restart() {
        let entries = vec![
            RestoreTransactionEntry {
                name: "boot".to_owned(),
                label: "boot".to_owned(),
                image_path: "/data/adb/ksu/rescue/boot.img".to_owned(),
                device_path: "/dev/block/by-name/boot_a".to_owned(),
                expected_sha256: "boot-digest".to_owned(),
                expected_size: 4096,
                status: "verified".to_owned(),
            },
            RestoreTransactionEntry {
                name: "init_boot".to_owned(),
                label: "init_boot".to_owned(),
                image_path: "/data/adb/ksu/rescue/init_boot.img".to_owned(),
                device_path: "/dev/block/by-name/init_boot_a".to_owned(),
                expected_sha256: "init-boot-digest".to_owned(),
                expected_size: 2048,
                status: "failed".to_owned(),
            },
        ];
        let transaction = RestoreTransaction {
            id: "interrupted".to_owned(),
            reason: "unit test".to_owned(),
            automatic: false,
            description: "restore current slot".to_owned(),
            activate_slot: None,
            phase: "failed".to_owned(),
            error_code: "rescue.io_timeout".to_owned(),
            error_message: "copy timed out".to_owned(),
            started_at: "start".to_owned(),
            updated_at: "update".to_owned(),
            entries: entries.clone(),
        };
        let temp = tempfile::tempdir().expect("create temp directory");
        let state_path = temp.path().join("restore_transaction.json");
        fs::write(
            &state_path,
            restore_transaction_json(&transaction).to_string(),
        )
        .expect("persist transaction");

        let restarted = read_restore_transaction_from(&state_path)
            .expect("read persisted transaction")
            .expect("transaction must exist");
        let resumed = select_resumable_restore_transaction(restarted, &entries, None)
            .expect("matching interrupted transaction should resume")
            .expect("transaction should be resumable");

        assert_eq!(resumed.id, "interrupted");
        assert_eq!(resumed.entries[0].status, "verified");
        assert_eq!(resumed.entries[1].status, "failed");
    }

    #[test]
    fn conflicting_restore_transaction_is_rejected() {
        let entries = vec![RestoreTransactionEntry {
            name: "boot".to_owned(),
            label: "boot".to_owned(),
            image_path: "/data/adb/ksu/rescue/boot.img".to_owned(),
            device_path: "/dev/block/by-name/boot_a".to_owned(),
            expected_sha256: "new-digest".to_owned(),
            expected_size: 4096,
            status: "pending".to_owned(),
        }];
        let existing = RestoreTransaction {
            id: "old-transaction".to_owned(),
            reason: "unit test".to_owned(),
            automatic: false,
            description: "restore current slot".to_owned(),
            activate_slot: None,
            phase: "writing".to_owned(),
            error_code: String::new(),
            error_message: String::new(),
            started_at: "start".to_owned(),
            updated_at: "update".to_owned(),
            entries: vec![RestoreTransactionEntry {
                expected_sha256: "old-digest".to_owned(),
                ..entries[0].clone()
            }],
        };

        let error = select_resumable_restore_transaction(existing, &entries, None)
            .expect_err("mismatched unfinished transaction must be rejected");
        assert_eq!(error_code(&error), "rescue.restore_transaction_conflict");
    }

    #[test]
    fn slow_storage_copy_returns_structured_timeout() {
        let mut input = Cursor::new(vec![1_u8; 32]);
        let mut output = Vec::new();
        let mut ticks = [Duration::ZERO, Duration::from_secs(46)].into_iter();

        let error = copy_with_clock(
            &mut input,
            &mut output,
            Duration::from_secs(45),
            Duration::from_secs(COPY_TOTAL_TIMEOUT_SECONDS),
            || ticks.next().unwrap_or(Duration::from_secs(46)),
        )
        .expect_err("stalled copy must time out");

        assert_eq!(error_code(&error), "rescue.io_timeout");
        assert!(output.is_empty());
    }
}
