use anyhow::{Context, Result, bail};
use serde_json::json;
use std::{
    collections::HashMap,
    fs,
    io::{Cursor, Read},
    path::{Path, PathBuf},
};

use crate::{boot_patch, defs, metamodule, module, utils};

pub const HYBRID_MOUNT_MODULE_ID: &str = "hybrid_mount";

const CONFIG_PATH: &str = "/data/adb/hybrid-mount/config.toml";
const BUILTIN_VARIANT_PATH: &str = "/data/adb/hybrid-mount/builtin_variant";
const BUILTIN_LITE_ZIP: &[u8] = include_bytes!("../builtin/hybrid-mount-lite.zip");
const BUILTIN_FULL_ZIP: &[u8] = include_bytes!("../builtin/hybrid-mount-full.zip");
const MODULE_NAME_FALLBACK: &str = "Hybrid Mount Lite";
const MODULE_VERSION_FALLBACK: &str = "4.2.0-1815";
const MODULE_VERSION_CODE_FALLBACK: &str = "402000";
const HYBRID_MOUNT_BINARY: &str = "hybrid-mount";
const COMPAT_MARKER_FILE: &str = ".ksu_builtin_mount_compat";
const SOURCE_URL: &str = "https://github.com/Hybrid-Mount/meta-hybrid_mount/releases/tag/v4.2.0";
const KASUMI_LKM_PREFIX: &str = "kasumi_lkm/";
const KASUMI_LKM_SUFFIX: &str = "_arm64_kasumi_lkm.ko";

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum BuiltinMountVariant {
    Lite,
    Full,
}

impl BuiltinMountVariant {
    pub fn parse(value: &str) -> Result<Self> {
        match value.trim().to_ascii_lowercase().as_str() {
            "lite" => Ok(Self::Lite),
            "full" | "kasumi" | "experimental" => Ok(Self::Full),
            _ => bail!("unsupported builtin mount variant: {value}"),
        }
    }

    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Lite => "lite",
            Self::Full => "full",
        }
    }

    pub const fn archive(self) -> &'static [u8] {
        match self {
            Self::Lite => BUILTIN_LITE_ZIP,
            Self::Full => BUILTIN_FULL_ZIP,
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum MountMode {
    Overlay,
    Magic,
}

impl MountMode {
    pub fn parse(value: &str) -> Result<Self> {
        match value.trim().to_ascii_lowercase().as_str() {
            "overlay" => Ok(Self::Overlay),
            "magic" => Ok(Self::Magic),
            _ => bail!("unsupported builtin mount mode: {value}"),
        }
    }

    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Overlay => "overlay",
            Self::Magic => "magic",
        }
    }
}

pub fn print_status() {
    let module_dir = module_dir();
    let installed = is_installed_at(&module_dir);
    let enabled = is_enabled_at(&module_dir);
    let conflict = conflicting_metamodule_id();
    let webui = module_dir.join(defs::MODULE_WEB_DIR).exists();
    let default_mode = read_default_mode().as_str();
    let variant = read_variant();
    let module_prop = if installed {
        read_installed_module_prop()
    } else {
        read_archive_module_prop(variant)
    };
    let module_path = module_dir.display().to_string();
    let name = module_prop
        .get("name")
        .map_or(MODULE_NAME_FALLBACK, String::as_str);
    let version = module_prop
        .get("version")
        .map_or(MODULE_VERSION_FALLBACK, String::as_str);
    let version_code = module_prop
        .get("versionCode")
        .map_or(MODULE_VERSION_CODE_FALLBACK, String::as_str);
    let archive_info = read_archive_lkm_info(variant);
    let current_kmi = boot_patch::get_current_kmi().unwrap_or_default();
    let compatibility = match variant {
        BuiltinMountVariant::Lite => "not_required",
        BuiltinMountVariant::Full if current_kmi.is_empty() => "unknown",
        BuiltinMountVariant::Full if archive_info.supported_kmis.contains(&current_kmi) => {
            "compatible"
        }
        BuiltinMountVariant::Full => "unsupported",
    };

    println!(
        "{}",
        json!({
            "moduleId": HYBRID_MOUNT_MODULE_ID,
            "moduleName": name,
            "modulePath": module_path,
            "version": version,
            "versionCode": version_code,
            "installed": installed,
            "enabled": enabled,
            "conflict": conflict,
            "defaultMode": default_mode,
            "variant": variant.as_str(),
            "webui": webui,
            "sourceUrl": SOURCE_URL,
            "archiveSha256": sha256::digest(variant.archive()),
            "lkmCount": archive_info.lkm_count,
            "supportedKmis": archive_info.supported_kmis,
            "currentKmi": current_kmi,
            "compatibility": compatibility,
            "lkmPurpose": "Kasumi mount and concealment features",
            "apkesuRootDriver": false,
        })
    );
}

#[derive(Default)]
struct ArchiveLkmInfo {
    lkm_count: usize,
    supported_kmis: Vec<String>,
}

fn read_archive_lkm_info(variant: BuiltinMountVariant) -> ArchiveLkmInfo {
    let Ok(mut archive) = zip::ZipArchive::new(Cursor::new(variant.archive())) else {
        return ArchiveLkmInfo::default();
    };
    let mut supported_kmis = Vec::new();
    let mut lkm_count = 0;
    for index in 0..archive.len() {
        let Ok(file) = archive.by_index(index) else {
            continue;
        };
        let name = file.name();
        let Some(file_name) = name.strip_prefix(KASUMI_LKM_PREFIX) else {
            continue;
        };
        let Some(kmi) = file_name.strip_suffix(KASUMI_LKM_SUFFIX) else {
            continue;
        };
        if !kmi.is_empty() {
            lkm_count += 1;
            supported_kmis.push(kmi.to_owned());
        }
    }
    supported_kmis.sort();
    supported_kmis.dedup();
    ArchiveLkmInfo {
        lkm_count,
        supported_kmis,
    }
}

pub fn print_default_mode() {
    println!("{}", read_default_mode().as_str());
}

pub fn enable() -> Result<()> {
    ensure_no_conflicting_metamodule()?;

    let module_dir = module_dir();
    let mode = read_default_mode();
    let variant = read_variant();
    install_or_update_builtin_module(&module_dir, variant)?;
    write_default_mode_for_variant(mode, variant)?;
    cleanup_legacy_module_dirs()?;
    remove_disable_marker(&module_dir)?;
    ensure_compat_module_entry(&module_dir)?;
    metamodule::ensure_symlink(&module_dir)?;

    if !Path::new(CONFIG_PATH).exists() {
        write_default_mode(mode)?;
    }
    if let Err(e) = module::regenerate_preinit_rc() {
        log::warn!("regenerate preinit rc failed: {e}");
    }
    Ok(())
}

pub fn disable() -> Result<()> {
    let module_dir = module_dir();
    remove_compat_module_entry_if_owned()?;
    if is_same_path_as_active_metamodule(&module_dir) || metamodule_symlink_points_to(&module_dir) {
        metamodule::remove_symlink()?;
    }
    if module_dir.exists() {
        utils::ensure_file_exists(module_dir.join(defs::DISABLE_FILE_NAME))?;
    }
    if let Err(e) = module::regenerate_preinit_rc() {
        log::warn!("regenerate preinit rc failed: {e}");
    }
    Ok(())
}

pub fn set_default_mode(mode: MountMode) -> Result<()> {
    write_default_mode(mode)
}

pub fn set_variant(variant: BuiltinMountVariant) -> Result<()> {
    let module_dir = module_dir();
    let enabled = is_enabled_at(&module_dir);

    if enabled {
        ensure_no_conflicting_metamodule()?;
        let mode = read_default_mode();
        install_or_update_builtin_module(&module_dir, variant)?;
        write_default_mode_for_variant(mode, variant)?;
        cleanup_legacy_module_dirs()?;
        remove_disable_marker(&module_dir)?;
        ensure_compat_module_entry(&module_dir)?;
        metamodule::ensure_symlink(&module_dir)?;
        if let Err(e) = module::regenerate_preinit_rc() {
            log::warn!("regenerate preinit rc failed: {e}");
        }
    }

    write_variant(variant)
}

pub fn ensure_active_compat_entry() -> Result<()> {
    let module_dir = module_dir();
    if is_enabled_at(&module_dir) {
        ensure_compat_module_entry(&module_dir)?;
    } else {
        remove_compat_module_entry_if_owned()?;
    }
    Ok(())
}

pub fn is_compat_module_entry(path: &Path) -> bool {
    path.join(COMPAT_MARKER_FILE).exists()
        || (is_same_path(path, &legacy_module_dir())
            && is_same_path_as_active_metamodule(&module_dir()))
}

fn module_dir() -> PathBuf {
    Path::new(defs::WORKING_DIR)
        .join("builtin")
        .join(HYBRID_MOUNT_MODULE_ID)
}

fn legacy_module_dir() -> PathBuf {
    Path::new(defs::MODULE_DIR).join(HYBRID_MOUNT_MODULE_ID)
}

fn legacy_update_dir() -> PathBuf {
    Path::new(defs::MODULE_UPDATE_DIR).join(HYBRID_MOUNT_MODULE_ID)
}

fn is_installed_at(module_dir: &Path) -> bool {
    module_dir.join("module.prop").exists()
}

fn is_enabled_at(module_dir: &Path) -> bool {
    is_installed_at(module_dir)
        && !module_dir.join(defs::DISABLE_FILE_NAME).exists()
        && is_same_path_as_active_metamodule(module_dir)
}

fn ensure_no_conflicting_metamodule() -> Result<()> {
    if let Some(id) = conflicting_metamodule_id() {
        bail!("another metamodule is already active: {id}");
    }
    Ok(())
}

fn conflicting_metamodule_id() -> Option<String> {
    let metamodule_path = metamodule::get_metamodule_path()?;
    if is_same_path(&metamodule_path, &module_dir()) {
        return None;
    }

    let id = module::read_module_prop(&metamodule_path)
        .ok()
        .and_then(|props| props.get("id").cloned())
        .or_else(|| {
            metamodule_path
                .file_name()
                .and_then(|name| name.to_str())
                .map(ToString::to_string)
        })?;

    (id != HYBRID_MOUNT_MODULE_ID).then_some(id)
}

fn install_or_update_builtin_module(module_dir: &Path, variant: BuiltinMountVariant) -> Result<()> {
    let parent = module_dir
        .parent()
        .with_context(|| format!("{} has no parent", module_dir.display()))?;
    utils::ensure_dir_exists(parent)?;

    let tmp_dir = parent.join(format!("{HYBRID_MOUNT_MODULE_ID}.tmp"));
    let backup_dir = parent.join(format!("{HYBRID_MOUNT_MODULE_ID}.backup"));
    if backup_dir.exists() || backup_dir.is_symlink() {
        if module_dir.exists() || module_dir.is_symlink() {
            remove_path(&backup_dir)?;
        } else {
            fs::rename(&backup_dir, module_dir).with_context(|| {
                format!(
                    "failed to restore interrupted builtin mount update from {}",
                    backup_dir.display()
                )
            })?;
        }
    }
    utils::ensure_clean_dir(&tmp_dir)?;

    let mut archive = zip::ZipArchive::new(Cursor::new(variant.archive()))
        .with_context(|| "failed to open builtin mount archive")?;
    archive
        .extract(&tmp_dir)
        .with_context(|| format!("failed to extract builtin mount to {}", tmp_dir.display()))?;

    prepare_extracted_builtin_module(&tmp_dir)?;

    let had_previous = module_dir.exists() || module_dir.is_symlink();
    if had_previous {
        fs::rename(module_dir, &backup_dir).with_context(|| {
            format!(
                "failed to preserve existing builtin mount dir {}",
                module_dir.display()
            )
        })?;
    }
    if let Err(error) = fs::rename(&tmp_dir, module_dir) {
        if had_previous {
            fs::rename(&backup_dir, module_dir).with_context(|| {
                format!(
                    "failed to restore {} after update error: {error}",
                    module_dir.display()
                )
            })?;
        }
        return Err(error).with_context(|| {
            format!(
                "failed to replace builtin mount dir {} with {}",
                module_dir.display(),
                tmp_dir.display()
            )
        });
    }
    if had_previous && let Err(error) = remove_path(&backup_dir) {
        log::warn!(
            "failed to remove builtin mount backup {}: {error}",
            backup_dir.display()
        );
    }
    Ok(())
}

fn prepare_extracted_builtin_module(module_dir: &Path) -> Result<()> {
    let binary_source = module_dir.join("binaries").join(HYBRID_MOUNT_BINARY);
    let binary_target = module_dir.join(HYBRID_MOUNT_BINARY);
    fs::copy(&binary_source, &binary_target).with_context(|| {
        format!(
            "failed to copy {} to {}",
            binary_source.display(),
            binary_target.display()
        )
    })?;

    remove_path_if_exists(&module_dir.join("binaries"))?;
    remove_path_if_exists(&module_dir.join("system"))?;
    set_builtin_permissions(module_dir)?;
    Ok(())
}

fn set_builtin_permissions(module_dir: &Path) -> Result<()> {
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;

        fn visit(path: &Path) -> Result<()> {
            let metadata = fs::symlink_metadata(path)
                .with_context(|| format!("failed to stat {}", path.display()))?;
            let mode = if metadata.is_dir() { 0o755 } else { 0o644 };
            fs::set_permissions(path, fs::Permissions::from_mode(mode))
                .with_context(|| format!("failed to chmod {}", path.display()))?;

            if metadata.is_dir() {
                for entry in fs::read_dir(path)
                    .with_context(|| format!("failed to read dir {}", path.display()))?
                {
                    visit(&entry?.path())?;
                }
            }
            Ok(())
        }

        visit(module_dir)?;
        fs::set_permissions(
            module_dir.join(HYBRID_MOUNT_BINARY),
            fs::Permissions::from_mode(0o755),
        )
        .with_context(|| "failed to chmod builtin hybrid-mount binary")?;
    }
    Ok(())
}

fn remove_disable_marker(module_dir: &Path) -> Result<()> {
    let disable = module_dir.join(defs::DISABLE_FILE_NAME);
    if disable.exists() {
        fs::remove_file(&disable)
            .with_context(|| format!("failed to remove {}", disable.display()))?;
    }
    Ok(())
}

fn cleanup_legacy_module_dirs() -> Result<()> {
    remove_path_if_exists(&legacy_module_dir())?;
    remove_path_if_exists(&legacy_update_dir())?;
    Ok(())
}

fn ensure_compat_module_entry(module_dir: &Path) -> Result<()> {
    let compat_dir = legacy_module_dir();
    if compat_dir.exists() || compat_dir.is_symlink() {
        remove_path(&compat_dir)?;
    }

    utils::ensure_dir_exists(&compat_dir)?;

    let binary = module_dir.join(HYBRID_MOUNT_BINARY);
    let wrapper = compat_dir.join(HYBRID_MOUNT_BINARY);
    fs::write(
        &wrapper,
        format!(
            "#!/system/bin/sh\nexec {} \"$@\"\n",
            shell_single_quote(&binary.display().to_string())
        ),
    )
    .with_context(|| format!("failed to write {}", wrapper.display()))?;
    fs::copy(
        module_dir.join("module.prop"),
        compat_dir.join("module.prop"),
    )
    .with_context(|| "failed to copy builtin mount module.prop to compat entry")?;

    let kasumi_lkm_dir = module_dir.join("kasumi_lkm");
    if kasumi_lkm_dir.exists() {
        let compat_kasumi_lkm_dir = compat_dir.join("kasumi_lkm");
        #[cfg(unix)]
        std::os::unix::fs::symlink(&kasumi_lkm_dir, &compat_kasumi_lkm_dir).with_context(|| {
            format!(
                "failed to symlink {} to {}",
                kasumi_lkm_dir.display(),
                compat_kasumi_lkm_dir.display()
            )
        })?;
    }

    utils::ensure_file_exists(compat_dir.join(COMPAT_MARKER_FILE))?;

    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;

        fs::set_permissions(&compat_dir, fs::Permissions::from_mode(0o755))
            .with_context(|| format!("failed to chmod {}", compat_dir.display()))?;
        fs::set_permissions(&wrapper, fs::Permissions::from_mode(0o755))
            .with_context(|| format!("failed to chmod {}", wrapper.display()))?;
    }

    Ok(())
}

fn remove_compat_module_entry_if_owned() -> Result<()> {
    let compat_dir = legacy_module_dir();
    let owned = compat_dir.join(COMPAT_MARKER_FILE).exists()
        || is_same_path_as_active_metamodule(&module_dir())
        || compat_dir.is_symlink();
    if owned && (compat_dir.exists() || compat_dir.is_symlink()) {
        remove_path_if_exists(&compat_dir)?;
    }
    Ok(())
}

fn shell_single_quote(value: &str) -> String {
    format!("'{}'", value.replace('\'', "'\\''"))
}

fn remove_path_if_exists(path: &Path) -> Result<()> {
    if path.exists() || path.is_symlink() {
        remove_path(path)?;
    }
    Ok(())
}

fn remove_path(path: &Path) -> Result<()> {
    if path.is_symlink() || path.is_file() {
        fs::remove_file(path).with_context(|| format!("failed to remove {}", path.display()))?;
    } else {
        fs::remove_dir_all(path).with_context(|| format!("failed to remove {}", path.display()))?;
    }
    Ok(())
}

fn is_same_path_as_active_metamodule(path: &Path) -> bool {
    metamodule::get_metamodule_path()
        .is_some_and(|metamodule_path| is_same_path(path, &metamodule_path))
}

fn metamodule_symlink_points_to(path: &Path) -> bool {
    let symlink = Path::new(defs::METAMODULE_DIR.trim_end_matches('/'));
    let Ok(target) = fs::read_link(symlink) else {
        return false;
    };
    let target = if target.is_absolute() {
        target
    } else {
        symlink
            .parent()
            .unwrap_or_else(|| Path::new("/"))
            .join(target)
    };
    is_same_path(&target, path)
}

fn is_same_path(left: &Path, right: &Path) -> bool {
    match (left.canonicalize(), right.canonicalize()) {
        (Ok(left), Ok(right)) => left == right,
        _ => left == right,
    }
}

fn read_installed_module_prop() -> HashMap<String, String> {
    module::read_module_prop(&module_dir()).unwrap_or_default()
}

fn read_archive_module_prop(variant: BuiltinMountVariant) -> HashMap<String, String> {
    let Ok(mut archive) = zip::ZipArchive::new(Cursor::new(variant.archive())) else {
        return HashMap::new();
    };
    let Ok(mut file) = archive.by_name("module.prop") else {
        return HashMap::new();
    };
    let mut content = String::new();
    if file.read_to_string(&mut content).is_err() {
        return HashMap::new();
    }
    parse_prop_content(&content)
}

fn parse_prop_content(content: &str) -> HashMap<String, String> {
    content
        .lines()
        .filter_map(|line| {
            let trimmed = line.trim();
            if trimmed.is_empty() || trimmed.starts_with('#') {
                return None;
            }
            let (key, value) = trimmed.split_once('=')?;
            Some((key.trim().to_string(), value.trim().to_string()))
        })
        .collect()
}

fn read_variant() -> BuiltinMountVariant {
    if let Ok(content) = fs::read_to_string(BUILTIN_VARIANT_PATH)
        && let Ok(variant) = BuiltinMountVariant::parse(&content)
    {
        return variant;
    }

    BuiltinMountVariant::Lite
}

fn write_variant(variant: BuiltinMountVariant) -> Result<()> {
    let path = Path::new(BUILTIN_VARIANT_PATH);
    let parent = path
        .parent()
        .with_context(|| format!("{} has no parent", path.display()))?;
    utils::ensure_dir_exists(parent)?;
    let temp_path = PathBuf::from(format!("{}.tmp.{}", path.display(), std::process::id()));
    fs::write(&temp_path, format!("{}\n", variant.as_str()))
        .with_context(|| format!("failed to write {}", temp_path.display()))?;
    if let Err(err) = fs::rename(&temp_path, path) {
        _ = fs::remove_file(&temp_path);
        return Err(err).with_context(|| format!("failed to replace {}", path.display()));
    }
    Ok(())
}

fn read_default_mode() -> MountMode {
    let Ok(content) = fs::read_to_string(CONFIG_PATH) else {
        return MountMode::Overlay;
    };

    content
        .lines()
        .find_map(parse_default_mode_line)
        .unwrap_or(MountMode::Overlay)
}

fn parse_default_mode_line(line: &str) -> Option<MountMode> {
    let trimmed = line.trim();
    if trimmed.starts_with('#') {
        return None;
    }
    let (key, value) = trimmed.split_once('=')?;
    if key.trim() != "default_mode" {
        return None;
    }
    let value = value.trim().trim_matches('"').trim_matches('\'');
    MountMode::parse(value).ok()
}

fn write_default_mode(mode: MountMode) -> Result<()> {
    write_default_mode_for_variant(mode, read_variant())
}

fn write_default_mode_for_variant(mode: MountMode, variant: BuiltinMountVariant) -> Result<()> {
    let config = Path::new(CONFIG_PATH);
    let parent = config
        .parent()
        .with_context(|| format!("{} has no parent", config.display()))?;
    utils::ensure_dir_exists(parent)?;

    let content = fs::read_to_string(config).unwrap_or_else(|_| default_config(mode, variant));
    let content = reconcile_variant_config(content, variant);
    let mut found = false;
    let mut output = String::new();
    for line in content.lines() {
        let trimmed = line.trim_start();
        let is_default_mode = trimmed
            .split_once('=')
            .is_some_and(|(key, _)| key.trim() == "default_mode");
        if is_default_mode {
            output.push_str("default_mode = \"");
            output.push_str(mode.as_str());
            output.push_str("\"\n");
            found = true;
        } else {
            output.push_str(line);
            output.push('\n');
        }
    }
    if !found {
        output.push_str("default_mode = \"");
        output.push_str(mode.as_str());
        output.push_str("\"\n");
    }

    let tmp = config.with_extension("toml.tmp");
    fs::write(&tmp, output)
        .with_context(|| format!("failed to write temp config {}", tmp.display()))?;
    fs::rename(&tmp, config).with_context(|| {
        format!(
            "failed to replace config {} with {}",
            config.display(),
            tmp.display()
        )
    })?;
    Ok(())
}

fn reconcile_variant_config(mut content: String, variant: BuiltinMountVariant) -> String {
    match variant {
        BuiltinMountVariant::Lite => remove_kasumi_sections(&content),
        BuiltinMountVariant::Full => {
            if !content.lines().any(|line| line.trim() == "[kasumi]") {
                if !content.ends_with('\n') {
                    content.push('\n');
                }
                content.push_str(full_kasumi_config());
            }
            content
        }
    }
}

fn remove_kasumi_sections(content: &str) -> String {
    let mut output = String::new();
    let mut skip_section = false;
    for line in content.lines() {
        let trimmed = line.trim();
        if let Some(section) = trimmed
            .strip_prefix('[')
            .and_then(|value| value.strip_suffix(']'))
        {
            skip_section = section == "kasumi" || section.starts_with("kasumi.");
        }
        if !skip_section {
            output.push_str(line);
            output.push('\n');
        }
    }
    output
}

fn default_config(mode: MountMode, variant: BuiltinMountVariant) -> String {
    let mut config = format!(
        "default_mode = \"{}\"\n\
         disable_umount = false\n\
         enable_overlay_fallback = false\n\
         moduledir = \"/data/adb/modules\"\n\
         mountsource = \"KSU\"\n\
         overlay_mode = \"ext4\"\n",
        mode.as_str()
    );
    if variant == BuiltinMountVariant::Full {
        config.push_str(full_kasumi_config());
    }
    config
}

const fn full_kasumi_config() -> &'static str {
    "\n\
     [kasumi]\n\
     cmdline_value = \"\"\n\
     enable_hidexattr = false\n\
     enable_kernel_debug = false\n\
     enable_maps_spoof = false\n\
     enable_mount_hide = false\n\
     enable_statfs_spoof = false\n\
     enable_stealth = false\n\
     enabled = false\n\
     hide_uids = []\n\
     lkm_autoload = true\n\
     lkm_dir = \"/data/adb/modules/hybrid_mount/kasumi_lkm\"\n\
     lkm_kmi_override = \"\"\n\
     mirror_path = \"/dev/kasumi_mirror\"\n\
     uname_mode = \"scoped\"\n\
     \n\
     [kasumi.mount_hide]\n\
     enabled = false\n\
     path_pattern = \"\"\n\
     \n\
     [kasumi.statfs_spoof]\n\
     enabled = false\n\
     path = \"\"\n\
     spoof_f_type = 0\n\
     \n\
     [kasumi.uname]\n\
     domainname = \"\"\n\
     machine = \"\"\n\
     nodename = \"\"\n\
     release = \"\"\n\
     sysname = \"\"\n\
     version = \"\"\n"
}
