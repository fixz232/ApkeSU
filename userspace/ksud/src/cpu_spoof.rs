use anyhow::{Context, Result, bail};
use const_format::concatcp;
use prop_rs_android::resetprop::ResetProp;
use prop_rs_android::sys_prop;
use serde_json::{Value, json};
use std::fs;
use std::io::ErrorKind;
use std::path::Path;

use crate::{defs, utils};

const CONFIG_PATH: &str = concatcp!(defs::WORKING_DIR, ".cpu_spoof.json");
const CPU_MODEL_PROPERTY: &str = "ro.soc.model";
const CPU_MANUFACTURER_PROPERTY: &str = "ro.soc.manufacturer";
const CPU_PLATFORM_PROPERTY: &str = "ro.board.platform";
const MAX_PROPERTY_VALUE_BYTES: usize = 91;

#[derive(Clone, Debug, Default)]
struct CpuSpoofConfig {
    enabled: bool,
    target: String,
    original: String,
}

#[derive(Debug)]
struct CpuSnapshot {
    model: String,
    manufacturer: String,
    platform: String,
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

pub fn print_status() {
    let (config, configured, config_error) = match read_config() {
        Ok(Some(config)) => (config, true, String::new()),
        Ok(None) => (CpuSpoofConfig::default(), false, String::new()),
        Err(err) => (CpuSpoofConfig::default(), false, format!("{err:#}")),
    };
    let (snapshot, runtime_error) = match read_cpu_snapshot() {
        Ok(snapshot) => (Some(snapshot), String::new()),
        Err(err) => (None, format!("{err:#}")),
    };
    let current = snapshot.as_ref().map_or("", |value| value.model.as_str());
    let supported = !current.is_empty();
    let applied = config.enabled && supported && current == config.target;
    let error = if config_error.is_empty() {
        runtime_error
    } else {
        config_error
    };

    println!(
        "{}",
        json!({
            "supported": supported,
            "configured": configured && !config.target.is_empty(),
            "enabled": config.enabled,
            "applied": applied,
            "current": current,
            "target": config.target,
            "original": config.original,
            "manufacturer": snapshot.as_ref().map_or("", |value| value.manufacturer.as_str()),
            "platform": snapshot.as_ref().map_or("", |value| value.platform.as_str()),
            "error": error,
        })
    );
}

pub fn configure(model: &str) -> Result<()> {
    let target = validate_model(model)?;
    let snapshot = read_cpu_snapshot()?;
    let previous = read_config()?;
    let mut next = previous.clone().unwrap_or_default();
    next.target = target;

    if next.enabled && next.original.is_empty() {
        next.original.clone_from(&snapshot.model);
    }

    write_config(&next)?;
    if next.enabled
        && let Err(err) = apply_config(&next)
    {
        restore_runtime_value(&snapshot.model)?;
        restore_config(previous.as_ref())?;
        return Err(err);
    }

    Ok(())
}

pub fn enable() -> Result<()> {
    let snapshot = read_cpu_snapshot()?;
    let previous = read_config()?;
    let mut next = previous
        .clone()
        .context("configure a target CPU before enabling CPU spoof")?;
    next.target = validate_model(&next.target)?;
    next.enabled = true;

    if next.original.is_empty() || !previous.as_ref().is_some_and(|config| config.enabled) {
        next.original.clone_from(&snapshot.model);
    }

    write_config(&next)?;
    if let Err(err) = apply_config(&next) {
        restore_runtime_value(&snapshot.model)?;
        restore_config(previous.as_ref())?;
        return Err(err);
    }

    Ok(())
}

pub fn disable() -> Result<()> {
    let Some(previous) = read_config()? else {
        return Ok(());
    };
    if !previous.enabled {
        return Ok(());
    }

    let snapshot = read_cpu_snapshot()?;
    let mut next = previous.clone();
    next.enabled = false;
    next.original.clear();
    write_config(&next)?;

    if snapshot.model == previous.target
        && !previous.original.is_empty()
        && let Err(err) = restore_runtime_value(&previous.original)
    {
        restore_config(Some(&previous))?;
        return Err(err);
    }

    Ok(())
}

pub fn restore_default() -> Result<()> {
    let Some(config) = read_config()? else {
        return Ok(());
    };

    if config.enabled && !config.original.is_empty() {
        restore_runtime_value(&config.original)?;
    }
    clear_config()
}

pub fn apply_if_enabled() {
    let Ok(Some(mut config)) = read_config() else {
        return;
    };
    if !config.enabled {
        return;
    }

    let snapshot = match read_cpu_snapshot() {
        Ok(snapshot) => snapshot,
        Err(err) => {
            log::warn!("cpu-spoof: failed to read CPU properties: {err:#}");
            return;
        }
    };

    // Each cold boot starts from the device's real property value. Refresh the
    // backup when it differs so an explicit restore returns to this boot's base.
    if snapshot.model != config.target && snapshot.model != config.original {
        config.original = snapshot.model;
        if let Err(err) = write_config(&config) {
            log::warn!("cpu-spoof: failed to refresh original CPU value: {err:#}");
            return;
        }
    }

    if let Err(err) = apply_config(&config) {
        log::warn!("cpu-spoof: apply failed: {err:#}");
    }
}

fn read_cpu_snapshot() -> Result<CpuSnapshot> {
    sys_prop::init().context("failed to initialize system property API")?;
    let rp = resetprop();
    let model = rp
        .get(CPU_MODEL_PROPERTY)
        .unwrap_or_default()
        .trim()
        .to_owned();
    if model.is_empty() {
        bail!("{CPU_MODEL_PROPERTY} is unavailable on this device");
    }

    Ok(CpuSnapshot {
        model,
        manufacturer: rp
            .get(CPU_MANUFACTURER_PROPERTY)
            .unwrap_or_default()
            .trim()
            .to_owned(),
        platform: rp
            .get(CPU_PLATFORM_PROPERTY)
            .unwrap_or_default()
            .trim()
            .to_owned(),
    })
}

fn apply_config(config: &CpuSpoofConfig) -> Result<()> {
    let target = validate_model(&config.target)?;
    sys_prop::init().context("failed to initialize system property API")?;
    let rp = resetprop();
    rp.set(CPU_MODEL_PROPERTY, &target)
        .with_context(|| format!("failed to set {CPU_MODEL_PROPERTY}"))?;

    if rp.get(CPU_MODEL_PROPERTY).as_deref() != Some(target.as_str()) {
        bail!("{CPU_MODEL_PROPERTY} did not retain the requested value");
    }
    Ok(())
}

fn restore_runtime_value(value: &str) -> Result<()> {
    let original = validate_model(value)?;
    sys_prop::init().context("failed to initialize system property API")?;
    let rp = resetprop();
    rp.set(CPU_MODEL_PROPERTY, &original)
        .with_context(|| format!("failed to restore {CPU_MODEL_PROPERTY}"))?;
    if rp.get(CPU_MODEL_PROPERTY).as_deref() != Some(original.as_str()) {
        bail!("{CPU_MODEL_PROPERTY} did not retain the restored value");
    }
    Ok(())
}

fn validate_model(model: &str) -> Result<String> {
    let value = model.trim();
    if value.is_empty() {
        bail!("CPU model cannot be empty");
    }
    if value.starts_with('-') {
        bail!("CPU model cannot start with a hyphen");
    }
    if value.len() > MAX_PROPERTY_VALUE_BYTES {
        bail!("CPU model exceeds Android property value limit");
    }
    if value.chars().any(char::is_control) {
        bail!("CPU model contains control characters");
    }
    Ok(value.to_owned())
}

fn read_config() -> Result<Option<CpuSpoofConfig>> {
    let path = Path::new(CONFIG_PATH);
    if !path.exists() {
        return Ok(None);
    }

    let content =
        fs::read_to_string(path).with_context(|| format!("failed to read {}", path.display()))?;
    let value: Value = serde_json::from_str(&content)
        .with_context(|| format!("failed to parse {}", path.display()))?;
    if !value.is_object() {
        bail!("{} must contain a JSON object", path.display());
    }

    Ok(Some(CpuSpoofConfig {
        enabled: value
            .get("enabled")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        target: value
            .get("target")
            .and_then(Value::as_str)
            .unwrap_or_default()
            .trim()
            .to_owned(),
        original: value
            .get("original")
            .and_then(Value::as_str)
            .unwrap_or_default()
            .trim()
            .to_owned(),
    }))
}

fn write_config(config: &CpuSpoofConfig) -> Result<()> {
    utils::ensure_dir_exists(Path::new(defs::WORKING_DIR))?;
    let content = serde_json::to_string_pretty(&json!({
        "enabled": config.enabled,
        "target": config.target,
        "original": config.original,
    }))?;
    write_atomic(CONFIG_PATH, &format!("{content}\n"))
}

fn restore_config(config: Option<&CpuSpoofConfig>) -> Result<()> {
    config.map_or_else(clear_config, write_config)
}

fn clear_config() -> Result<()> {
    match fs::remove_file(CONFIG_PATH) {
        Ok(()) => Ok(()),
        Err(err) if err.kind() == ErrorKind::NotFound => Ok(()),
        Err(err) => Err(err).with_context(|| format!("failed to delete {CONFIG_PATH}")),
    }
}

fn write_atomic(path: &str, content: &str) -> Result<()> {
    let temporary = format!("{path}.tmp.{}", std::process::id());
    fs::write(&temporary, content).with_context(|| format!("failed to write {temporary}"))?;
    if let Err(err) = fs::rename(&temporary, path) {
        _ = fs::remove_file(&temporary);
        return Err(err).with_context(|| format!("failed to replace {path}"));
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::validate_model;

    #[test]
    fn accepts_normal_cpu_models() {
        assert_eq!(validate_model(" SM8750-AB ").unwrap(), "SM8750-AB");
        assert_eq!(validate_model("Tensor G4").unwrap(), "Tensor G4");
    }

    #[test]
    fn rejects_unsafe_cpu_models() {
        assert!(validate_model("\nSM8750").is_err());
        assert!(validate_model("-SM8750").is_err());
        assert!(validate_model(" ").is_err());
    }
}
