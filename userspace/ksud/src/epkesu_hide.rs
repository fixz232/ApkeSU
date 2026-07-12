use anyhow::{Context, Result, bail};
use const_format::concatcp;
use prop_rs_android::resetprop::ResetProp;
use prop_rs_android::sys_prop;
use serde_json::json;
use std::collections::BTreeMap;
use std::fs;
use std::path::Path;

use crate::{defs, utils};

const CONFIG_PATH: &str = concatcp!(defs::WORKING_DIR, ".epkesu_hide");
const BACKUP_PATH: &str = concatcp!(defs::WORKING_DIR, ".epkesu_hide_props");
const BACKUP_SESSION_PATH: &str = concatcp!(defs::WORKING_DIR, ".epkesu_hide_session");
const BOOT_ID_PATH: &str = "/proc/sys/kernel/random/boot_id";

const RESET_PROPS: &[(&str, &str)] = &[
    ("ro.boot.vbmeta.device_state", "locked"),
    ("ro.boot.verifiedbootstate", "green"),
    ("ro.boot.flash.locked", "1"),
    ("ro.boot.veritymode", "enforcing"),
    ("ro.boot.warranty_bit", "0"),
    ("ro.warranty_bit", "0"),
    ("ro.debuggable", "0"),
    ("ro.force.debuggable", "0"),
    ("ro.secure", "1"),
    ("ro.adb.secure", "1"),
    ("ro.build.type", "user"),
    ("ro.build.tags", "release-keys"),
    ("ro.vendor.boot.warranty_bit", "0"),
    ("ro.vendor.warranty_bit", "0"),
    ("vendor.boot.vbmeta.device_state", "locked"),
    ("vendor.boot.verifiedbootstate", "green"),
    ("sys.oem_unlock_allowed", "0"),
    ("ro.secureboot.lockstate", "locked"),
    ("ro.boot.realmebootstate", "green"),
    ("ro.boot.realme.lockstate", "1"),
];

const CONTAINS_PROPS: &[(&str, &str, &str)] = &[
    ("ro.bootmode", "recovery", "unknown"),
    ("ro.boot.bootmode", "recovery", "unknown"),
    ("vendor.boot.bootmode", "recovery", "unknown"),
];

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

pub fn is_enabled() -> bool {
    fs::read_to_string(CONFIG_PATH).is_ok_and(|content| content.trim() == "1")
}

fn is_applied() -> bool {
    if sys_prop::init().is_err() {
        return false;
    }

    let Ok(backup) = read_backup() else {
        return false;
    };
    // A stock device can already report values such as "locked". Only a
    // backup created during this boot proves that this feature changed them.
    if backup.is_empty() {
        return false;
    }
    if !backup_belongs_to_current_boot().unwrap_or(false) {
        return false;
    }
    verify_applied(&resetprop(), &backup)
}

pub fn print_status() {
    let configured = is_enabled();
    let applied = configured && is_applied();
    println!(
        "{}",
        json!({
            "enabled": configured && applied,
            "configured": configured,
            "applied": applied,
        })
    );
}

pub fn enable() -> Result<()> {
    if !is_enabled() {
        clear_backup();
        clear_backup_session();
    }

    apply()?;

    if let Err(err) = write_enabled(true) {
        if let Err(restore_err) = restore_backup() {
            log::warn!("epkesu-hide: rollback failed after state write error: {restore_err:#}");
        }
        return Err(err);
    }

    Ok(())
}

pub fn disable() -> Result<()> {
    restore_backup()?;
    write_enabled(false)
}

pub fn apply_if_enabled() {
    if is_enabled()
        && let Err(err) = apply()
    {
        log::warn!("epkesu-hide: apply failed: {err:#}");
    }
}

pub fn apply() -> Result<()> {
    let result = apply_inner();
    if let Err(err) = &result
        && let Err(restore_err) = restore_backup()
    {
        log::warn!("epkesu-hide: rollback failed after apply error: {restore_err:#}");
        log::warn!("epkesu-hide: original apply error: {err:#}");
    }
    result
}

fn apply_inner() -> Result<()> {
    prepare_backup_for_current_boot()?;
    sys_prop::init().context("Failed to initialize system property API")?;

    let rp = resetprop();
    let mut backup = read_backup()?;
    let mut backup_changed = false;

    for (name, expected) in RESET_PROPS {
        backup_changed |= reset_prop_if_needed(&rp, &mut backup, name, expected)?;
    }

    for (name, contains, new_value) in CONTAINS_PROPS {
        backup_changed |= reset_prop_if_contains(&rp, &mut backup, name, contains, new_value)?;
    }

    if backup_changed {
        write_backup(&backup)?;
    }

    if !verify_applied(&rp, &backup) {
        bail!("no supported boot properties were applied");
    }

    Ok(())
}

fn write_enabled(enabled: bool) -> Result<()> {
    utils::ensure_dir_exists(Path::new(defs::WORKING_DIR))?;
    write_atomic(CONFIG_PATH, if enabled { "1\n" } else { "0\n" })
        .with_context(|| format!("failed to write {CONFIG_PATH}"))
}

fn reset_prop_if_needed(
    rp: &ResetProp,
    backup: &mut BTreeMap<String, String>,
    name: &str,
    expected: &str,
) -> Result<bool> {
    let Some(value) = rp.get(name) else {
        return Ok(false);
    };

    if value.is_empty() || value == expected {
        return Ok(false);
    }

    let backup_changed = backup_original(backup, name, &value);
    if backup_changed {
        write_backup(backup)?;
    }
    rp.set(name, expected)
        .with_context(|| format!("Failed to set {name}"))?;
    Ok(backup_changed)
}

fn reset_prop_if_contains(
    rp: &ResetProp,
    backup: &mut BTreeMap<String, String>,
    name: &str,
    contains: &str,
    new_value: &str,
) -> Result<bool> {
    let Some(value) = rp.get(name) else {
        return Ok(false);
    };

    if !value.contains(contains) {
        return Ok(false);
    }

    let backup_changed = backup_original(backup, name, &value);
    if backup_changed {
        write_backup(backup)?;
    }
    rp.set(name, new_value)
        .with_context(|| format!("Failed to set {name}"))?;
    Ok(backup_changed)
}

fn backup_original(backup: &mut BTreeMap<String, String>, name: &str, value: &str) -> bool {
    if backup.contains_key(name) {
        return false;
    }

    backup.insert(name.to_owned(), value.to_owned());
    true
}

fn restore_backup() -> Result<()> {
    let backup = read_backup()?;
    if backup.is_empty() {
        clear_backup_session();
        return Ok(());
    }

    if !backup_belongs_to_current_boot()? {
        clear_backup();
        clear_backup_session();
        return Ok(());
    }

    sys_prop::init().context("Failed to initialize system property API")?;
    let rp = resetprop();
    for (name, value) in &backup {
        rp.set(name, value)
            .with_context(|| format!("Failed to restore {name}"))?;
    }
    clear_backup();
    clear_backup_session();
    Ok(())
}

fn clear_backup() {
    _ = fs::remove_file(BACKUP_PATH);
}

fn clear_backup_session() {
    _ = fs::remove_file(BACKUP_SESSION_PATH);
}

fn prepare_backup_for_current_boot() -> Result<()> {
    let boot_id = current_boot_id()?;
    let session = fs::read_to_string(BACKUP_SESSION_PATH)
        .ok()
        .map(|value| value.trim().to_owned());

    if session.as_deref() != Some(boot_id.as_str()) {
        clear_backup();
        write_atomic(BACKUP_SESSION_PATH, &format!("{boot_id}\n"))
            .with_context(|| format!("failed to write {BACKUP_SESSION_PATH}"))?;
    }

    Ok(())
}

fn backup_belongs_to_current_boot() -> Result<bool> {
    let boot_id = current_boot_id()?;
    let session = fs::read_to_string(BACKUP_SESSION_PATH)
        .ok()
        .map(|value| value.trim().to_owned());
    Ok(session.as_deref() == Some(boot_id.as_str()))
}

fn current_boot_id() -> Result<String> {
    let boot_id = fs::read_to_string(BOOT_ID_PATH)
        .with_context(|| format!("failed to read {BOOT_ID_PATH}"))?
        .trim()
        .to_owned();
    if boot_id.is_empty() {
        bail!("{BOOT_ID_PATH} is empty");
    }
    Ok(boot_id)
}

fn verify_applied(rp: &ResetProp, backup: &BTreeMap<String, String>) -> bool {
    let mut found_property = false;

    for (name, expected) in RESET_PROPS {
        let tracked = backup.contains_key(*name);
        let Some(value) = rp.get(name) else {
            if tracked {
                return false;
            }
            continue;
        };

        if tracked {
            found_property = true;
            if value != *expected {
                return false;
            }
        } else if !value.is_empty() && value == *expected {
            found_property = true;
        }
    }

    for (name, _, new_value) in CONTAINS_PROPS {
        let tracked = backup.contains_key(*name);
        let Some(value) = rp.get(name) else {
            if tracked {
                return false;
            }
            continue;
        };

        if tracked {
            found_property = true;
            if value != *new_value {
                return false;
            }
        } else if value == *new_value {
            found_property = true;
        }
    }

    found_property
}

fn read_backup() -> Result<BTreeMap<String, String>> {
    let path = Path::new(BACKUP_PATH);
    if !path.exists() {
        return Ok(BTreeMap::new());
    }

    let content =
        fs::read_to_string(path).with_context(|| format!("failed to read {}", path.display()))?;
    let mut backup = BTreeMap::new();
    for line in content.lines() {
        let Some((name, value)) = line.split_once('=') else {
            continue;
        };
        if !name.is_empty() {
            backup.insert(name.to_owned(), value.to_owned());
        }
    }
    Ok(backup)
}

fn write_backup(backup: &BTreeMap<String, String>) -> Result<()> {
    utils::ensure_dir_exists(Path::new(defs::WORKING_DIR))?;

    let mut content = String::new();
    for (name, value) in backup {
        content.push_str(name);
        content.push('=');
        content.push_str(value);
        content.push('\n');
    }

    write_atomic(BACKUP_PATH, &content).with_context(|| format!("failed to write {BACKUP_PATH}"))
}

fn write_atomic(path: &str, content: &str) -> Result<()> {
    let temp_path = format!("{path}.tmp.{}", std::process::id());
    fs::write(&temp_path, content).with_context(|| format!("failed to write {temp_path}"))?;
    if let Err(err) = fs::rename(&temp_path, path) {
        _ = fs::remove_file(&temp_path);
        return Err(err).with_context(|| format!("failed to replace {path}"));
    }
    Ok(())
}
