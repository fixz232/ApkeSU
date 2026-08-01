use anyhow::{Context, Result, bail};
use serde_json::json;
use std::{
    fs,
    path::{Path, PathBuf},
};

use crate::{defs, module, utils};

pub const KPATCH_NEXT_MODULE_ID: &str = "KPatch-Next";

const BUILTIN_ZIP: &[u8] = include_bytes!("../builtin/kpatch-next-module.zip");
const MODULE_NAME_FALLBACK: &str = "KPatch-Next";
const MODULE_VERSION_FALLBACK: &str = "v0.0.1";
const MODULE_VERSION_CODE_FALLBACK: &str = "1";
const KPATCH_NEXT_DATA_DIR: &str = "/data/adb/kp-next";

pub fn print_status() {
    let module_dir = module_dir();
    let update_dir = update_dir();
    let module_prop = module::read_module_prop(&module_dir).unwrap_or_default();
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
    let installed = module_dir.join("module.prop").exists();
    let pending_update = update_dir.join("module.prop").exists();
    let pending_remove = module_dir.join(defs::REMOVE_FILE_NAME).exists()
        || update_dir.join(defs::REMOVE_FILE_NAME).exists();
    let enabled =
        installed && !module_dir.join(defs::DISABLE_FILE_NAME).exists() && !pending_remove;
    let webui = module_dir
        .join(defs::MODULE_WEB_DIR)
        .join("index.html")
        .is_file();
    let unresolved = module_dir.join("unresolved").exists();
    let data_dir = Path::new(KPATCH_NEXT_DATA_DIR).exists();

    println!(
        "{}",
        json!({
            "moduleId": KPATCH_NEXT_MODULE_ID,
            "moduleName": name,
            "modulePath": module_path,
            "version": version,
            "versionCode": version_code,
            "installed": installed,
            "enabled": enabled,
            "pendingUpdate": pending_update,
            "pendingRemove": pending_remove,
            "webui": webui,
            "unresolved": unresolved,
            "dataDir": data_dir,
            "builtinAvailable": !BUILTIN_ZIP.is_empty(),
            "conflict": serde_json::Value::Null,
        })
    );
}

pub fn enable() -> Result<()> {
    let module_dir = module_dir();
    let had_module_prop = module_dir.join("module.prop").exists();

    prepare_module_dir_for_installer(&module_dir)?;
    ensure_data_dirs()?;

    let zip_path = write_builtin_zip()?;
    let zip_path = zip_path
        .to_str()
        .context("builtin KPatch Next zip path is not valid UTF-8")?;

    if let Err(e) = module::install_module(zip_path) {
        cleanup_partial_module_dir(&module_dir, had_module_prop);
        return Err(e);
    }

    remove_marker_if_exists(&module_dir.join(defs::DISABLE_FILE_NAME))?;
    remove_marker_if_exists(&update_dir().join(defs::DISABLE_FILE_NAME))?;
    remove_marker_if_exists(&module_dir.join(defs::REMOVE_FILE_NAME))?;
    remove_marker_if_exists(&module_dir.join("unresolved"))?;
    Ok(())
}

pub fn disable() -> Result<()> {
    let module_dir = module_dir();
    let update_dir = update_dir();
    for dir in [&module_dir, &update_dir] {
        if dir.is_symlink() {
            bail!("{} is a symlink, refusing to remove it", dir.display());
        }
    }
    let active_touched = ensure_remove_marker_if_dir_exists(&module_dir)?;
    let update_touched = ensure_remove_marker_if_dir_exists(&update_dir)?;
    let touched = active_touched || update_touched;

    if touched && let Err(e) = module::regenerate_preinit_rc() {
        log::warn!("regenerate preinit rc failed: {e}");
    }
    Ok(())
}

fn module_dir() -> PathBuf {
    Path::new(defs::MODULE_DIR).join(KPATCH_NEXT_MODULE_ID)
}

fn update_dir() -> PathBuf {
    Path::new(defs::MODULE_UPDATE_DIR).join(KPATCH_NEXT_MODULE_ID)
}

fn prepare_module_dir_for_installer(module_dir: &Path) -> Result<()> {
    if module_dir.is_symlink() {
        bail!(
            "{} is a symlink, refusing to update it",
            module_dir.display()
        );
    }

    utils::ensure_dir_exists(module_dir)?;
    utils::ensure_dir_exists(module_dir.join("bin"))?;
    utils::ensure_dir_exists(module_dir.join("patch"))?;
    utils::ensure_dir_exists(module_dir.join(defs::MODULE_WEB_DIR))?;
    Ok(())
}

fn ensure_data_dirs() -> Result<()> {
    utils::ensure_dir_exists(KPATCH_NEXT_DATA_DIR)?;
    utils::ensure_dir_exists(Path::new(KPATCH_NEXT_DATA_DIR).join("kpm"))?;
    Ok(())
}

fn write_builtin_zip() -> Result<PathBuf> {
    let dir = Path::new(defs::WORKING_DIR).join("builtin");
    utils::ensure_dir_exists(&dir)?;
    let zip_path = dir.join("KPatch-Next-Module.zip");
    fs::write(&zip_path, BUILTIN_ZIP)
        .with_context(|| format!("failed to write {}", zip_path.display()))?;
    Ok(zip_path)
}

fn ensure_remove_marker_if_dir_exists(dir: &Path) -> Result<bool> {
    if !dir.exists() {
        return Ok(false);
    }
    utils::ensure_file_exists(dir.join(defs::REMOVE_FILE_NAME))?;
    Ok(true)
}

fn remove_marker_if_exists(path: &Path) -> Result<()> {
    if path.exists() {
        fs::remove_file(path).with_context(|| format!("failed to remove {}", path.display()))?;
    }
    Ok(())
}

fn cleanup_partial_module_dir(module_dir: &Path, had_module_prop: bool) {
    if had_module_prop || module_dir.join("module.prop").exists() {
        return;
    }
    if let Err(e) = fs::remove_dir_all(module_dir) {
        log::warn!("failed to clean partial KPatch Next module dir: {e}");
    }
}
