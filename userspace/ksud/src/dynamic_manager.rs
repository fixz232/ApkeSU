use anyhow::{Context, Result, ensure};
use serde_json::{Value, json};
use std::fs::{self, Permissions};
use std::io::Write;
use std::os::unix::fs::PermissionsExt;
use std::path::Path;

use crate::{apk_sign, defs, ksucalls};

const STATUS_SCHEMA_VERSION: u32 = 2;
const MIN_CERT_SIZE: u32 = 0x100;
const MAX_CERT_SIZE: u32 = 0x1000;
const DYNAMIC_MANAGER_SIGNATURE_INDEX: u8 = 255;

#[derive(Clone, Debug, Eq, PartialEq)]
struct Config {
    size: u32,
    hash: String,
}

impl Config {
    const fn disabled() -> Self {
        Self {
            size: 0,
            hash: String::new(),
        }
    }

    fn validate(&self) -> Result<()> {
        ensure!(
            (MIN_CERT_SIZE..=MAX_CERT_SIZE).contains(&self.size),
            "dynamic manager certificate size is outside the supported range"
        );
        ensure!(
            self.hash.len() == 64
                && self
                    .hash
                    .bytes()
                    .all(|value| value.is_ascii_digit() || (b'a'..=b'f').contains(&value)),
            "dynamic manager certificate SHA-256 must be 64 lowercase hexadecimal characters"
        );
        Ok(())
    }

    fn hash_bytes(&self) -> Result<[u8; 64]> {
        self.validate()?;
        self.hash.as_bytes().try_into().map_err(|_| {
            anyhow::anyhow!("dynamic manager certificate SHA-256 has an invalid length")
        })
    }

    const fn is_disabled(&self) -> bool {
        self.size == 0 && self.hash.is_empty()
    }
}

fn parse_config(content: &str) -> Result<Option<Config>> {
    let value: Value = serde_json::from_str(content).context("invalid dynamic manager JSON")?;
    let config = Config {
        size: value
            .get("size")
            .and_then(Value::as_u64)
            .and_then(|size| u32::try_from(size).ok())
            .context("dynamic manager certificate size is missing")?,
        hash: value
            .get("hash")
            .and_then(Value::as_str)
            .context("dynamic manager certificate SHA-256 is missing")?
            .to_owned(),
    };
    if config.is_disabled() {
        return Ok(None);
    }
    config.validate()?;
    Ok(Some(config))
}

fn parse_legacy_config(content: &str) -> Result<Option<Config>> {
    let value: Value =
        serde_json::from_str(content).context("invalid legacy dynamic manager JSON")?;
    ensure!(
        value.get("schemaVersion").and_then(Value::as_u64) == Some(1),
        "unsupported legacy dynamic manager schema"
    );
    if !value
        .get("enabled")
        .and_then(Value::as_bool)
        .unwrap_or(false)
    {
        return Ok(None);
    }
    let config = Config {
        size: value
            .get("certificateSize")
            .and_then(Value::as_u64)
            .and_then(|size| u32::try_from(size).ok())
            .context("legacy dynamic manager certificate size is missing")?,
        hash: value
            .get("certificateSha256")
            .and_then(Value::as_str)
            .context("legacy dynamic manager certificate SHA-256 is missing")?
            .to_owned(),
    };
    config.validate()?;
    Ok(Some(config))
}

fn read_optional(path: &Path) -> Result<Option<String>> {
    match fs::read_to_string(path) {
        Ok(content) => Ok(Some(content)),
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => Ok(None),
        Err(error) => Err(error).with_context(|| format!("failed to read {}", path.display())),
    }
}

fn read_config() -> Result<(Option<Config>, bool)> {
    let path = Path::new(defs::DYNAMIC_MANAGER);
    if let Some(content) = read_optional(path)? {
        return parse_config(&content).map(|config| (config, false));
    }

    let legacy_path = Path::new(defs::LEGACY_DYNAMIC_MANAGER_CONFIG);
    let Some(content) = read_optional(legacy_path)? else {
        return Ok((None, false));
    };
    parse_legacy_config(&content).map(|config| (config, true))
}

fn write_config(config: &Config) -> Result<()> {
    let path = Path::new(defs::DYNAMIC_MANAGER);
    let parent = path
        .parent()
        .context("dynamic manager config has no parent")?;
    fs::create_dir_all(parent).with_context(|| format!("failed to create {}", parent.display()))?;
    let mut temporary = tempfile::NamedTempFile::new_in(parent)
        .context("failed to create temporary dynamic manager config")?;
    temporary
        .as_file()
        .set_permissions(Permissions::from_mode(0o600))
        .context("failed to secure temporary dynamic manager config")?;
    serde_json::to_writer_pretty(
        &mut temporary,
        &json!({
            "size": config.size,
            "hash": config.hash,
        }),
    )?;
    temporary.write_all(b"\n")?;
    temporary.as_file().sync_all()?;
    temporary
        .persist(path)
        .map_err(|error| error.error)
        .with_context(|| format!("failed to atomically replace {}", path.display()))?;
    fs::set_permissions(path, Permissions::from_mode(0o600))?;
    fs::File::open(parent)
        .and_then(|directory| directory.sync_all())
        .context("failed to sync the dynamic manager config directory")?;
    Ok(())
}

fn apply_to_kernel(config: &Config, synchronous: bool) -> Result<()> {
    let hash = config.hash_bytes()?;
    ksucalls::set_dynamic_manager(config.size, hash, synchronous)
        .context("kernel rejected dynamic manager configuration")
}

pub fn parse_hash(value: &str) -> std::result::Result<[u8; 64], String> {
    if value.len() != 64
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_digit() || (b'a'..=b'f').contains(&byte))
    {
        return Err("hash must contain exactly 64 lowercase hexadecimal characters".to_owned());
    }
    value
        .as_bytes()
        .try_into()
        .map_err(|_| "hash has an invalid length".to_owned())
}

pub fn set(size: u32, hash: [u8; 64]) -> Result<()> {
    let config = Config {
        size,
        hash: String::from_utf8(hash.to_vec()).context("dynamic manager hash is not UTF-8")?,
    };
    config.validate()?;
    apply_to_kernel(&config, true)?;
    if let Err(error) = write_config(&config) {
        let _ = ksucalls::clear_dynamic_manager();
        return Err(error);
    }
    Ok(())
}

pub fn set_apk(apk: &str) -> Result<()> {
    let (size, hash) = apk_sign::get_apk_signature(apk)?;
    set(size, parse_hash(&hash).map_err(anyhow::Error::msg)?)
}

pub fn clear() -> Result<()> {
    write_config(&Config::disabled())?;
    ksucalls::clear_dynamic_manager().context("failed to clear kernel dynamic manager state")
}

pub fn restore_at_boot() -> Result<()> {
    let (config, migrated) = read_config()?;
    let Some(config) = config else {
        return Ok(());
    };
    apply_to_kernel(&config, false)?;
    if migrated {
        write_config(&config).context("failed to migrate legacy dynamic manager configuration")?;
    }
    Ok(())
}

fn ioctl_is_unsupported(error: &std::io::Error) -> bool {
    matches!(
        error.raw_os_error(),
        Some(code) if code == libc::ENOTTY || code == libc::ENOSYS || code == libc::EOPNOTSUPP
    )
}

pub fn print_status() -> Result<()> {
    let mut errors = Vec::new();
    let stored = match read_config() {
        Ok((config, _)) => config,
        Err(error) => {
            errors.push(format!("stored configuration: {error:#}"));
            None
        }
    };

    let (supported, kernel) = match ksucalls::get_dynamic_manager() {
        Ok(state) => (true, Some(state)),
        Err(error) if error.raw_os_error() == Some(libc::ENODATA) => (true, None),
        Err(error) if ioctl_is_unsupported(&error) => (false, None),
        Err(error) => {
            errors.push(format!("kernel runtime: {error}"));
            (true, None)
        }
    };

    let managers = if supported {
        match ksucalls::get_managers() {
            Ok(managers) => managers,
            Err(error) => {
                errors.push(format!("manager registry: {error}"));
                Vec::new()
            }
        }
    } else {
        Vec::new()
    };
    let dynamic_appids: Vec<u32> = managers
        .iter()
        .filter(|manager| manager.signature_index == DYNAMIC_MANAGER_SIGNATURE_INDEX)
        .map(|manager| manager.appid)
        .collect();
    let effective_size = kernel
        .as_ref()
        .map(|state| state.cert_size)
        .or_else(|| stored.as_ref().map(|config| config.size));
    let effective_hash = kernel
        .as_ref()
        .map(|state| String::from_utf8_lossy(&state.cert_sha256).into_owned())
        .or_else(|| stored.as_ref().map(|config| config.hash.clone()));
    let configured = effective_size.is_some() && effective_hash.is_some();

    let output = json!({
        "schemaVersion": STATUS_SCHEMA_VERSION,
        "supported": supported,
        "configured": configured,
        "active": !dynamic_appids.is_empty(),
        "certificateSize": effective_size.unwrap_or_default(),
        "certificateSha256": effective_hash.unwrap_or_default(),
        "dynamicManagerAppIds": dynamic_appids,
        "managers": managers.iter().map(|manager| json!({
            "appId": manager.appid,
            "signatureIndex": manager.signature_index,
        })).collect::<Vec<_>>(),
        "error": if errors.is_empty() { Value::Null } else { Value::String(errors.join("; ")) },
    });
    println!("{}", serde_json::to_string_pretty(&output)?);
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn config_round_trip_matches_resukisu_format() {
        let content = format!(r#"{{"size":744,"hash":"{}"}}"#, "a".repeat(64));
        assert_eq!(
            parse_config(&content).unwrap(),
            Some(Config {
                size: 744,
                hash: "a".repeat(64),
            })
        );
    }

    #[test]
    fn migrates_apkesu_package_bound_config() {
        let content = json!({
            "schemaVersion": 1,
            "enabled": true,
            "packageName": "com.example.manager",
            "appId": 10_123,
            "certificateSize": 744,
            "certificateSha256": "b".repeat(64),
        })
        .to_string();
        assert_eq!(
            parse_legacy_config(&content).unwrap(),
            Some(Config {
                size: 744,
                hash: "b".repeat(64),
            })
        );
    }

    #[test]
    fn rejects_invalid_hashes() {
        assert!(parse_hash(&"a".repeat(64)).is_ok());
        assert!(parse_hash(&"A".repeat(64)).is_err());
        assert!(parse_hash("abc").is_err());
    }
}
