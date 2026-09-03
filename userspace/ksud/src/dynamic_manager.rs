use anyhow::{Context, Result, ensure};
use serde_json::{Value, json};
use std::fs::{self, Permissions};
use std::io::Write;
use std::os::unix::fs::PermissionsExt;
use std::path::{Path, PathBuf};

use crate::{apk_sign, defs, ksucalls};

const SCHEMA_VERSION: u32 = 1;
const PER_USER_RANGE: u32 = 100_000;
const FIRST_APPLICATION_APPID: u32 = 10_000;
const LAST_APPLICATION_APPID: u32 = 19_999;
const MIN_CERT_SIZE: u32 = 0x100;
const MAX_CERT_SIZE: u32 = 1024;

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct DynamicManagerConfig {
    pub enabled: bool,
    pub package_name: String,
    pub appid: u32,
    pub cert_size: u32,
    pub cert_sha256: String,
}

impl DynamicManagerConfig {
    const fn disabled() -> Self {
        Self {
            enabled: false,
            package_name: String::new(),
            appid: 0,
            cert_size: 0,
            cert_sha256: String::new(),
        }
    }

    fn validate(&self) -> Result<()> {
        if !self.enabled {
            return Ok(());
        }
        validate_package_name(&self.package_name)?;
        ensure!(
            (FIRST_APPLICATION_APPID..=LAST_APPLICATION_APPID).contains(&self.appid),
            "dynamic manager App ID must be an Android application ID"
        );
        ensure!(
            (MIN_CERT_SIZE..=MAX_CERT_SIZE).contains(&self.cert_size),
            "dynamic manager certificate size is outside the supported range"
        );
        ensure!(
            self.cert_sha256.len() == 64
                && self
                    .cert_sha256
                    .bytes()
                    .all(|value| value.is_ascii_digit() || (b'a'..=b'f').contains(&value)),
            "dynamic manager certificate SHA-256 must be 64 lowercase hexadecimal characters"
        );
        Ok(())
    }

    fn to_json(&self) -> Value {
        json!({
            "schemaVersion": SCHEMA_VERSION,
            "enabled": self.enabled,
            "packageName": self.package_name,
            "appId": self.appid,
            "certificateSize": self.cert_size,
            "certificateSha256": self.cert_sha256,
        })
    }

    fn from_json(value: &Value) -> Result<Self> {
        ensure!(
            value.get("schemaVersion").and_then(Value::as_u64) == Some(u64::from(SCHEMA_VERSION)),
            "unsupported or missing dynamic manager schema version"
        );
        let enabled = value
            .get("enabled")
            .and_then(Value::as_bool)
            .context("dynamic manager enabled field is missing")?;
        let config = Self {
            enabled,
            package_name: value
                .get("packageName")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .to_owned(),
            appid: value
                .get("appId")
                .and_then(Value::as_u64)
                .and_then(|value| u32::try_from(value).ok())
                .unwrap_or_default(),
            cert_size: value
                .get("certificateSize")
                .and_then(Value::as_u64)
                .and_then(|value| u32::try_from(value).ok())
                .unwrap_or_default(),
            cert_sha256: value
                .get("certificateSha256")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .to_owned(),
        };
        config.validate()?;
        Ok(config)
    }
}

fn validate_package_name(package_name: &str) -> Result<()> {
    ensure!(!package_name.is_empty(), "package name is empty");
    ensure!(package_name.len() < 256, "package name is too long");
    ensure!(
        !package_name.starts_with('.')
            && !package_name.ends_with('.')
            && !package_name.contains(".."),
        "package name contains an empty component"
    );
    ensure!(
        package_name
            .bytes()
            .all(|value| value.is_ascii_alphanumeric() || value == b'_' || value == b'.'),
        "package name contains unsupported characters"
    );
    ensure!(
        !defs::is_trusted_manager_package(package_name),
        "the built-in ApkeSU manager cannot be selected as a dynamic manager"
    );
    Ok(())
}

fn package_from_apk_path(path: &Path) -> Option<&str> {
    let directory = path.parent()?.file_name()?.to_str()?;
    directory
        .split_once('-')
        .map_or(Some(directory), |(package, _)| Some(package))
}

fn validate_packages_list(packages: &str, package_name: &str, appid: u32) -> Result<()> {
    let mut identity_exists = false;
    let mut appid_shared = false;
    for line in packages.lines() {
        let mut fields = line.split_whitespace();
        let package = fields.next();
        let uid = fields.next().and_then(|value| value.parse::<u32>().ok());
        if uid.is_none_or(|uid| uid % PER_USER_RANGE != appid) {
            continue;
        }
        if package == Some(package_name) {
            identity_exists = true;
        } else {
            appid_shared = true;
        }
    }
    ensure!(
        identity_exists,
        "package name and App ID do not match packages.list"
    );
    ensure!(
        !appid_shared,
        "dynamic manager App ID is shared by another package"
    );
    Ok(())
}

fn validate_installed_identity(package_name: &str, appid: u32) -> Result<()> {
    let packages = fs::read_to_string("/data/system/packages.list")
        .context("failed to read Android packages.list")?;
    validate_packages_list(&packages, package_name, appid)
}

fn canonical_installed_apk(apk: &str, package_name: &str) -> Result<PathBuf> {
    let path =
        fs::canonicalize(apk).with_context(|| format!("failed to resolve APK path {apk}"))?;
    ensure!(path.is_file(), "dynamic manager APK is not a regular file");
    ensure!(
        path.starts_with("/data/app")
            && path.file_name().and_then(|name| name.to_str()) == Some("base.apk"),
        "dynamic manager APK must be an installed /data/app base.apk"
    );
    ensure!(
        package_from_apk_path(&path) == Some(package_name),
        "APK path does not belong to the requested package"
    );
    Ok(path)
}

fn read_config() -> Result<Option<DynamicManagerConfig>> {
    let path = Path::new(defs::DYNAMIC_MANAGER_CONFIG);
    let content = match fs::read_to_string(path) {
        Ok(content) => content,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(None),
        Err(error) => {
            return Err(error).with_context(|| format!("failed to read {}", path.display()));
        }
    };
    let value: Value = serde_json::from_str(&content).context("invalid dynamic manager JSON")?;
    DynamicManagerConfig::from_json(&value).map(Some)
}

fn write_config(config: &DynamicManagerConfig) -> Result<()> {
    let path = Path::new(defs::DYNAMIC_MANAGER_CONFIG);
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
    let content = serde_json::to_vec_pretty(&config.to_json())?;
    temporary.write_all(&content)?;
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

fn apply_to_kernel(config: &DynamicManagerConfig) -> Result<ksucalls::DynamicManagerKernelState> {
    config.validate()?;
    ensure!(config.enabled, "dynamic manager config is disabled");
    ksucalls::set_dynamic_manager(
        config.appid,
        &config.package_name,
        config.cert_size,
        &config.cert_sha256,
    )
    .context("kernel rejected dynamic manager configuration")?;
    let state =
        ksucalls::get_dynamic_manager().context("failed to verify kernel dynamic manager state")?;
    ensure!(
        state.enabled
            && state.active
            && state.appid == config.appid
            && state.package_name == config.package_name
            && state.cert_size == config.cert_size
            && state.cert_sha256 == config.cert_sha256,
        "kernel could not verify the selected package identity"
    );
    Ok(state)
}

pub fn set_apk(apk: &str, package_name: &str, appid: u32) -> Result<()> {
    validate_package_name(package_name)?;
    ensure!(
        (FIRST_APPLICATION_APPID..=LAST_APPLICATION_APPID).contains(&appid),
        "invalid Android application ID"
    );
    validate_installed_identity(package_name, appid)?;
    let apk_path = canonical_installed_apk(apk, package_name)?;
    let (cert_size, cert_sha256) =
        apk_sign::get_apk_signature(apk_path.to_str().context("APK path is not valid UTF-8")?)?;
    let config = DynamicManagerConfig {
        enabled: true,
        package_name: package_name.to_owned(),
        appid,
        cert_size,
        cert_sha256,
    };
    config.validate()?;
    apply_to_kernel(&config)?;
    if let Err(error) = write_config(&config) {
        let _ = ksucalls::clear_dynamic_manager();
        return Err(error);
    }
    Ok(())
}

pub fn clear() -> Result<()> {
    write_config(&DynamicManagerConfig::disabled())?;
    ksucalls::clear_dynamic_manager().context("failed to clear kernel dynamic manager state")
}

pub fn restore_at_boot() -> Result<()> {
    let Some(config) = read_config()? else {
        return Ok(());
    };
    if !config.enabled {
        return Ok(());
    }
    apply_to_kernel(&config).map(|_| ())
}

pub fn print_status() -> Result<()> {
    let mut errors = Vec::new();
    let stored = match read_config() {
        Ok(config) => config,
        Err(error) => {
            errors.push(format!("stored configuration: {error:#}"));
            None
        }
    };
    let (supported, kernel) = match ksucalls::get_dynamic_manager() {
        Ok(state) => (true, Some(state)),
        Err(error) => {
            errors.push(format!("kernel runtime: {error}"));
            (false, None)
        }
    };
    let configured = stored.as_ref().is_some_and(|config| config.enabled);
    let source = kernel.as_ref().filter(|state| state.enabled).map_or_else(
        || {
            stored
                .as_ref()
                .filter(|config| config.enabled)
                .map(|config| {
                    json!({
                        "packageName": config.package_name,
                        "appId": config.appid,
                        "certificateSize": config.cert_size,
                        "certificateSha256": config.cert_sha256,
                    })
                })
        },
        |state| {
            Some(json!({
                "packageName": state.package_name,
                "appId": state.appid,
                "certificateSize": state.cert_size,
                "certificateSha256": state.cert_sha256,
            }))
        },
    );
    let mut output = json!({
        "schemaVersion": SCHEMA_VERSION,
        "supported": supported,
        "configured": configured || kernel.as_ref().is_some_and(|state| state.enabled),
        "active": kernel.as_ref().is_some_and(|state| state.active),
        "error": if errors.is_empty() { Value::Null } else { Value::String(errors.join("; ")) },
    });
    if let (Some(object), Some(source)) = (output.as_object_mut(), source)
        && let Some(source) = source.as_object()
    {
        object.extend(source.clone());
    }
    println!("{}", serde_json::to_string_pretty(&output)?);
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validates_safe_package_names() {
        assert!(validate_package_name("com.example.manager").is_ok());
        assert!(validate_package_name("com..example").is_err());
        assert!(validate_package_name("com.example-manager").is_err());
        assert!(validate_package_name(defs::DEFAULT_MANAGER_PACKAGE).is_err());
    }

    #[test]
    fn rejects_legacy_unversioned_config() {
        let legacy = json!({"size": 744, "hash": "0".repeat(64)});
        assert!(DynamicManagerConfig::from_json(&legacy).is_err());
    }

    #[test]
    fn config_round_trip_preserves_identity() {
        let config = DynamicManagerConfig {
            enabled: true,
            package_name: "com.example.manager".to_owned(),
            appid: 10_123,
            cert_size: 744,
            cert_sha256: "a".repeat(64),
        };
        assert_eq!(
            DynamicManagerConfig::from_json(&config.to_json()).unwrap(),
            config
        );
    }

    #[test]
    fn packages_list_requires_an_exact_unique_appid() {
        let packages = "com.example.manager 10123 0 /data/user/0/com.example.manager default:targetSdkVersion=36 none 0 1\n";
        assert!(validate_packages_list(packages, "com.example.manager", 10_123).is_ok());
        assert!(validate_packages_list(packages, "com.example.other", 10_123).is_err());

        let shared = format!(
            "{packages}com.example.shared 10123 0 /data/user/0/com.example.shared default:targetSdkVersion=36 none 0 1\n"
        );
        assert!(validate_packages_list(&shared, "com.example.manager", 10_123).is_err());
    }
}
