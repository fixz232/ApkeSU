use anyhow::{Context, Result, bail};
use serde_json::json;
use std::os::unix::fs::PermissionsExt;
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
    let cleanup_synchronized_kpms_on_failure = !is_enabled();
    crate::kpm::migrate_to_kpatch_next()?;
    let module_dir = module_dir();
    let had_module_prop = module_dir.join("module.prop").exists();

    if let Err(error) = prepare_module_dir_for_installer(&module_dir) {
        return cleanup_after_install_failure(error, cleanup_synchronized_kpms_on_failure);
    }
    if let Err(error) = ensure_data_dirs() {
        return cleanup_after_install_failure(error, cleanup_synchronized_kpms_on_failure);
    }

    let zip_path = match write_builtin_zip() {
        Ok(path) => path,
        Err(error) => {
            return cleanup_after_install_failure(error, cleanup_synchronized_kpms_on_failure);
        }
    };
    let Some(zip_path) = zip_path.to_str() else {
        return cleanup_after_install_failure(
            anyhow::anyhow!("builtin KPatch Next zip path is not valid UTF-8"),
            cleanup_synchronized_kpms_on_failure,
        );
    };

    if let Err(e) = module::install_module(zip_path) {
        cleanup_partial_module_dir(&module_dir, had_module_prop);
        return cleanup_after_install_failure(e, cleanup_synchronized_kpms_on_failure);
    }

    remove_marker_if_exists(&module_dir.join(defs::DISABLE_FILE_NAME))?;
    remove_marker_if_exists(&update_dir().join(defs::DISABLE_FILE_NAME))?;
    remove_marker_if_exists(&module_dir.join(defs::REMOVE_FILE_NAME))?;
    remove_marker_if_exists(&module_dir.join("unresolved"))?;
    ensure_policy_aware_service(&module_dir)?;
    Ok(())
}

fn cleanup_after_install_failure(
    error: anyhow::Error,
    cleanup_synchronized_kpms: bool,
) -> Result<()> {
    if !cleanup_synchronized_kpms {
        return Err(error);
    }
    match crate::kpm::cleanup_kpatch_after_install_failure() {
        Ok(()) => Err(error),
        Err(cleanup_error) => Err(error.context(format!(
            "KPatch-Next installation failed and KPM synchronization cleanup also failed: {cleanup_error:#}"
        ))),
    }
}

pub fn is_enabled() -> bool {
    let module_dir = module_dir();
    module_dir.join("module.prop").exists()
        && !module_dir.join(defs::DISABLE_FILE_NAME).exists()
        && !module_dir.join(defs::REMOVE_FILE_NAME).exists()
}

pub fn disable() -> Result<()> {
    crate::kpm::stop_kpatch_runtime()?;
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

fn ensure_policy_aware_service(module_dir: &Path) -> Result<()> {
    let service = module_dir.join("service.sh");
    let content = r#"#!/system/bin/sh

MODDIR=${0%/*}
KPNDIR="/data/adb/kp-next"
PATH="$MODDIR/bin:$PATH"
CONFIG="$KPNDIR/package_config"
REHOOK="$(cat "$KPNDIR/rehook" 2>/dev/null)"
KSU_KPM_POLICY="/data/adb/ksu/kpm/.policy.json"
KSU_KPM_PENDING="/data/adb/ksu/kpm/.kpatch_boot_pending"
KSU_KPM_EXCLUDES="/data/adb/ksu/kpm/.package_config"

remove_pending_id() {
    [ -f "$KSU_KPM_PENDING" ] || return 0
    pending_tmp="$KSU_KPM_PENDING.tmp.$$"
    while IFS= read -r pending_id; do
        [ "$pending_id" = "$1" ] || printf '%s\n' "$pending_id"
    done < "$KSU_KPM_PENDING" > "$pending_tmp"
    if [ -s "$pending_tmp" ]; then
        mv -f "$pending_tmp" "$KSU_KPM_PENDING"
    else
        rm -f "$pending_tmp" "$KSU_KPM_PENDING"
    fi
}

if [ -z "$(kpatch hello)" ]; then
    touch "$MODDIR/unresolved"
    exit 0
fi
rm -f "$MODDIR/unresolved"

LOAD_KPM=1
if [ -f "$KSU_KPM_POLICY" ] && ! grep -q '"enabled"[[:space:]]*:[[:space:]]*true' "$KSU_KPM_POLICY"; then
    LOAD_KPM=0
fi

if [ "$LOAD_KPM" = "1" ]; then
    for kpm in "$KPNDIR"/kpm/*.kpm; do
        [ -s "$kpm" ] || continue
        id="$(basename "$kpm" .kpm)"
        echo "$id" >> "$KSU_KPM_PENDING"
        args_file="$KPNDIR/kpm/$id.args"
        if [ -s "$args_file" ]; then
            args="$(cat "$args_file" 2>/dev/null)"
            load_ok=false
            kpatch kpm load "$kpm" "$args" && load_ok=true
        else
            load_ok=false
            kpatch kpm load "$kpm" && load_ok=true
        fi
        if [ "$load_ok" != "true" ]; then
            remove_pending_id "$id"
        fi
    done
fi

if [ -n "$REHOOK" ]; then
    if [ "$REHOOK" = "enable" ] || [ "$REHOOK" = "disable" ]; then
        kpatch rehook "$REHOOK"
    else
        rm -f "$KPNDIR/rehook"
    fi
fi

until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done
rm -f "$KSU_KPM_PENDING"

if [ -f "$CONFIG" ]; then
    tail -n +2 "$CONFIG" | while IFS=, read -r pkg exclude allow uid; do
        if [ "$exclude" = "1" ]; then
            resolved_uid=$(awk -v package="$pkg" '$1 == package { print $2; exit }' /data/system/packages.list)
            [ -z "$resolved_uid" ] && resolved_uid="$uid"
            [ -n "$resolved_uid" ] && kpatch exclude_set "$resolved_uid" 1
        fi
    done
fi

if [ -f "$KSU_KPM_EXCLUDES" ]; then
    while IFS=, read -r pkg exclude allow uid; do
        [ "$exclude" = "1" ] || continue
        resolved_uid=$(awk -v package="$pkg" '$1 == package { print $2; exit }' /data/system/packages.list)
        [ -z "$resolved_uid" ] && resolved_uid="$uid"
        [ -n "$resolved_uid" ] && kpatch exclude_set "$resolved_uid" 1
    done < "$KSU_KPM_EXCLUDES"
fi
"#;
    fs::write(&service, content)?;
    fs::set_permissions(&service, fs::Permissions::from_mode(0o755))?;
    Ok(())
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
