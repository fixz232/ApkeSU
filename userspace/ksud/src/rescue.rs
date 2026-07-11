use anyhow::{Context, Result, bail};
use chrono::Local;
use const_format::concatcp;
use serde_json::{Value, json};
#[cfg(target_os = "android")]
use std::os::fd::AsRawFd;
use std::{
    collections::BTreeMap,
    fs::{self, OpenOptions},
    io::{self, Write},
    path::{Component, Path, PathBuf},
    process::Command,
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
const AUTO_RESTORE_ATTEMPTS_PATH: &str = concatcp!(RESCUE_DIR, "auto_restore_attempts");
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

pub fn print_status() {
    let config = read_config().unwrap_or_default();
    let specs = partition_specs(&config);
    let manifest = read_manifest().unwrap_or_else(|_| json!({}));
    let validation = validate_backups(&config);
    let ready = validation.is_ok();
    let ready_reason = validation
        .err()
        .map(|err| err.to_string())
        .unwrap_or_default();
    let status = json!({
        "enabled": is_enabled(),
        "config": config_json(&config),
        "images": specs.iter().map(image_status).collect::<Vec<_>>(),
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
        "log": tail_file(LOG_PATH, 80).unwrap_or_default(),
    });
    println!("{status}");
}

pub fn print_test_report() {
    let config = read_config().unwrap_or_default();
    let specs = partition_specs(&config);
    let environment = validate_environment(&specs);
    let backup_validation = validate_backups(&config);
    let ok = environment.is_ok();
    let reason = environment
        .err()
        .map(|err| err.to_string())
        .unwrap_or_default();
    let backup_ready = backup_validation.is_ok();
    let backup_reason = backup_validation
        .err()
        .map(|err| err.to_string())
        .unwrap_or_default();
    let report = json!({
        "ok": ok,
        "reason": reason,
        "backupReady": backup_ready,
        "backupReason": backup_reason,
        "currentSlot": current_slot(),
        "device": device_summary(),
        "images": specs.iter().map(image_status).collect::<Vec<_>>(),
        "manifest": read_manifest().unwrap_or_else(|_| json!({})),
        "checks": {
            "bootPartitionFound": find_partition(&specs[0]).is_ok_and(|partition| partition.is_some()),
            "bootBackupReady": Path::new(&specs[0].image_path).is_file(),
            "configWritable": utils::ensure_dir_exists(RESCUE_DIR).is_ok(),
        }
    });
    println!("{report}");
}

pub fn import_config_text(content: &str) -> Result<()> {
    let config = parse_config(content)?;
    write_config(&config)?;
    append_log("rescue config updated by manager");
    Ok(())
}

pub fn import_image(partition: &str, source: &Path, force: bool) -> Result<()> {
    let name = normalize_partition_name(partition)?;
    let config = read_config().unwrap_or_default();
    let specs = partition_specs(&config);
    let spec = specs
        .iter()
        .find(|item| item.label == name)
        .cloned()
        .unwrap_or_else(|| current_slot_spec(&name, name == "boot", &config));

    ensure_safe_import_source(source)?;
    let Some(device) = find_partition(&spec)? else {
        bail!("{} partition is missing", spec.name);
    };

    if Path::new(&spec.image_path).exists() && !force {
        bail!(
            "{} backup already exists; pass --force to overwrite it",
            spec.name
        );
    }

    utils::ensure_dir_exists(RESCUE_DIR)?;
    let protected_files = vec![spec.image_path.clone(), MANIFEST_PATH.to_string()];
    preserve_files(&protected_files)?;
    let import_result = (|| -> Result<()> {
        fs::copy(source, &spec.image_path).with_context(|| {
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
        restore_preserved_files(&protected_files);
        return Err(err);
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
    let config = read_config().unwrap_or_default();
    let specs = partition_specs(&config);
    if Path::new(MANIFEST_PATH).exists() && !force {
        bail!("backup already exists; pass --force to overwrite it");
    }
    append_log("backup requested by manager");
    let protected_files = rescue_file_paths(&specs);
    preserve_files(&protected_files)?;
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
        restore_preserved_files(&protected_files);
        append_log(format!(
            "backup failed; restored previous rescue backups: {err:#}"
        ));
        return Err(err);
    }

    cleanup_preserved_files(&protected_files);
    Ok(())
}

pub fn enable() -> Result<()> {
    let config = read_config().unwrap_or_default();
    validate_backups(&config)?;
    utils::ensure_dir_exists(RESCUE_DIR)?;
    clear_runtime_markers();
    fs::write(ENABLED_PATH, b"1").context("failed to enable rescue protection")?;
    fs::write(BOOT_OK_PATH, b"1").context("failed to mark current boot as healthy")?;
    write_boot_count(0);
    write_auto_restore_attempts(0);
    append_log("rescue protection enabled");
    Ok(())
}

pub fn disable() -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    let _ = fs::remove_file(ENABLED_PATH);
    let _ = fs::remove_file(BOOT_COUNT_PATH);
    let _ = fs::remove_file(AUTO_RESTORE_ATTEMPTS_PATH);
    let _ = fs::remove_file(BOOT_OK_PATH);
    clear_runtime_markers();
    append_log("rescue protection disabled");
    Ok(())
}

pub fn restore_now() -> Result<()> {
    restore_keep_data_now()
}

pub fn restore_keep_data_now() -> Result<()> {
    append_log("manual data-preserving rollback requested by manager");
    restore_backups("manual data-preserving rollback", false)
}

pub fn mark_next_boot_pending(reason: &str) {
    if !is_enabled() {
        return;
    }

    if let Err(err) = utils::ensure_dir_exists(RESCUE_DIR) {
        append_log(format!("failed to mark next boot pending: {err:#}"));
        return;
    }

    let _ = fs::remove_file(BOOT_OK_PATH);
    write_boot_count(0);
    write_auto_restore_attempts(0);
    if let Err(err) = fs::write(PENDING_BOOT_PATH, reason) {
        append_log(format!("failed to write pending boot marker: {err:#}"));
    }
    append_log(format!("next boot marked pending: {reason}"));
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
    fs::write(LOG_PATH, "").context("failed to clear rescue log")?;
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
    let failure_hint = pending_boot && next_count > 1 && has_boot_failure_hint();
    let pending_boot_failed =
        pending_boot && next_count >= PENDING_BOOT_FAILURE_TRIGGER_COUNT && !previous_boot_ok;
    if failure_hint || pending_boot_failed {
        append_log(format!(
            "auto restore triggered on post-fs-data: failure_hint={failure_hint}, pending_boot_failed={pending_boot_failed}"
        ));
    } else if !pending_boot {
        append_log("no pending flashed boot; normal boot counter will not trigger auto restore");
    }

    if (failure_hint || pending_boot_failed)
        && let Err(err) = auto_restore_backups()
    {
        append_log(format!("auto restore failed: {err:#}"));
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

    let failure_hint = pending_boot && has_boot_failure_hint();
    append_log(format!(
        "recovery rescue check: boot_mode={}, previous_boot_ok={previous_boot_ok}, pending_boot={pending_boot}, boot_count={next_count}, failure_hint={failure_hint}",
        boot_mode()
    ));

    if (pending_boot || failure_hint)
        && let Err(err) = auto_restore_backups()
    {
        append_log(format!("auto restore failed in recovery: {err:#}"));
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

fn validate_backups(config: &RescueConfig) -> Result<()> {
    let plans = restore_plans(config)?;
    let mut last_error = None;
    for plan in plans {
        match validate_restore_backups(&plan.specs, config, false) {
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
) -> Result<()> {
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

        validate_image_against_partition(spec, &device)?;
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

fn validate_image_against_partition(spec: &PartitionSpec, device: &str) -> Result<()> {
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

    if let Some(expected) = manifest_sha256(&spec.label) {
        let actual = sha256_of(&spec.image_path);
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
    let config = read_config().unwrap_or_default();
    let plans = restore_plans(&config)?;
    let mut selected_plan = None;
    let mut last_error = None;
    for plan in plans {
        match validate_restore_backups(&plan.specs, &config, automatic) {
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
    let mut restored_count = 0usize;
    for spec in &plan.specs {
        if !should_restore_spec(spec, &config, automatic) || !Path::new(&spec.image_path).is_file()
        {
            continue;
        }
        let Some(device) = find_partition(spec)? else {
            append_log(format!("skip restore: {} partition not found", spec.name));
            continue;
        };
        restore_partition(spec, &device)?;
        restored_count += 1;
    }
    if restored_count == 0 {
        bail!("no rescue backup was restored");
    }
    if let Some(slot) = plan.activate_slot.as_deref() {
        set_active_slot(slot)?;
    }
    mark_skip_modules_once();
    mark_legacy_tmp_module_disable();
    disable_all_modules_after_restore();
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
    if let Err(err) = backup_result {
        restore_preserved_file(&spec.image_path);
        return Err(err);
    }
    append_log(format!(
        "backup {} ok: size={}, sha256={}",
        spec.name,
        fs::metadata(&spec.image_path)?.len(),
        sha256_of(&spec.image_path)
    ));
    Ok(())
}

fn restore_partition(spec: &PartitionSpec, device: &str) -> Result<()> {
    append_log(format!(
        "restore {}: {} -> {}, sha256={}",
        spec.name,
        spec.image_path,
        device,
        sha256_of(&spec.image_path)
    ));
    run_dd(&spec.image_path, device).with_context(|| format!("failed to restore {}", spec.name))?;
    Ok(())
}

fn run_dd(input: &str, output: &str) -> Result<()> {
    let mut input_file = OpenOptions::new()
        .read(true)
        .open(input)
        .with_context(|| format!("open input {input}"))?;

    let output_is_block = output.starts_with("/dev/block/");
    let mut output_options = OpenOptions::new();
    output_options.write(true);
    if !output_is_block {
        output_options.create(true).truncate(true);
    }
    let mut output_file = output_options
        .open(output)
        .with_context(|| format!("open output {output}"))?;

    set_block_writable(&output_file, output)?;
    io::copy(&mut input_file, &mut output_file)
        .with_context(|| format!("copy {input} to {output}"))?;
    output_file
        .sync_all()
        .with_context(|| format!("sync output {output}"))?;
    let _ = Command::new("sync").status();
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

fn disable_all_modules_after_restore() {
    match module::disable_all_modules() {
        Ok(()) => append_log("all modules disabled after rescue restore"),
        Err(err) => append_log(format!(
            "failed to disable all modules after rescue restore: {err:#}"
        )),
    }
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

fn image_status(spec: &PartitionSpec) -> serde_json::Value {
    let device = find_partition(spec).ok().flatten().unwrap_or_default();
    let size = fs::metadata(&spec.image_path).map_or(0, |metadata| metadata.len());
    let partition_size = partition_size(&device);
    let sha256 = if size > 0 {
        sha256_of(&spec.image_path)
    } else {
        String::new()
    };
    let manifest_sha256 = manifest_sha256(&spec.label).unwrap_or_default();
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
        "sha256Ok": manifest_sha256.is_empty() || manifest_sha256 == sha256,
        "sizeOk": partition_size == 0 || size == 0 || partition_size == size,
    })
}

fn write_manifest(specs: &[PartitionSpec]) -> Result<()> {
    let images = specs.iter().map(image_status).collect::<Vec<_>>();
    let manifest = json!({
        "createdAt": Local::now().format("%Y-%m-%d %H:%M:%S").to_string(),
        "slot": current_slot(),
        "device": device_summary(),
        "config": config_json(&read_config().unwrap_or_default()),
        "images": images,
    });
    fs::write(MANIFEST_PATH, manifest.to_string()).context("failed to write rescue manifest")
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

fn manifest_sha256(name: &str) -> Option<String> {
    let manifest = read_manifest().ok()?;
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

fn read_config() -> Result<RescueConfig> {
    if !Path::new(CONFIG_PATH).exists() {
        return Ok(RescueConfig::default());
    }
    let content = fs::read_to_string(CONFIG_PATH).context("failed to read rescue config")?;
    parse_config(&content)
}

fn write_config(config: &RescueConfig) -> Result<()> {
    utils::ensure_dir_exists(RESCUE_DIR)?;
    fs::write(CONFIG_PATH, config_json(config).to_string()).context("failed to write rescue config")
}

fn parse_config(content: &str) -> Result<RescueConfig> {
    let value: Value = serde_json::from_str(content).context("invalid rescue config JSON")?;
    let mut custom_partitions = BTreeMap::new();
    if let Some(object) = value.get("customPartitions").and_then(Value::as_object) {
        for (name, path) in object {
            if is_known_partition(name)
                && let Some(path) = path.as_str().and_then(sanitize_partition_path)
            {
                custom_partitions.insert(name.clone(), path);
            }
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

fn ensure_safe_import_source(source: &Path) -> Result<()> {
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
    Ok(())
}

fn preserve_file(path: &str) -> Result<()> {
    let source = Path::new(path);
    if !source.exists() {
        return Ok(());
    }
    let backup = backup_path(source);
    fs::copy(source, &backup)
        .with_context(|| format!("failed to preserve previous backup {}", source.display()))?;
    Ok(())
}

fn restore_preserved_file(path: &str) {
    let source = Path::new(path);
    let backup = backup_path(source);
    if backup.exists() {
        let _ = fs::copy(&backup, source);
    } else {
        let _ = fs::remove_file(source);
    }
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
    for path in paths {
        preserve_file(path)?;
    }
    Ok(())
}

fn restore_preserved_files(paths: &[String]) {
    for path in paths {
        restore_preserved_file(path);
    }
    cleanup_preserved_files(paths);
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

fn current_slot() -> String {
    boot_patch::get_slot_suffix(false).unwrap_or_default()
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
        .any(|mode| mode == "1" || mode == "recovery" || mode == "rec" || mode.contains("recovery"))
        || boot_reason_text().contains("recovery")
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
    has_legacy_failure_hint()
        || has_kernel_panic_hint()
        || has_boot_reason_failure_hint()
        || has_pstore_failure_hint()
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

fn has_kernel_panic_hint() -> bool {
    let Ok(text) = fs::read_to_string("/proc/last_kmsg") else {
        return false;
    };
    let found = has_failure_text(&text);
    if found {
        append_log("kernel panic hint found in /proc/last_kmsg");
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

fn has_pstore_failure_hint() -> bool {
    let Ok(entries) = fs::read_dir("/sys/fs/pstore") else {
        return false;
    };
    for entry in entries.flatten() {
        let path = entry.path();
        let Some(name) = path.file_name().and_then(|value| value.to_str()) else {
            continue;
        };
        if !name.contains("ramoops") && !name.contains("console") && !name.contains("dmesg") {
            continue;
        }
        if let Ok(text) = fs::read_to_string(&path)
            && has_failure_text(&text)
        {
            append_log(format!(
                "boot failure hint found in pstore: {}",
                path.display()
            ));
            return true;
        }
    }
    false
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
    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(LOG_PATH)
        .with_context(|| format!("failed to open {LOG_PATH}"))?;
    let timestamp = Local::now().format("%Y-%m-%d %H:%M:%S");
    writeln!(file, "[{timestamp}] {message}").context("failed to write rescue log")
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
