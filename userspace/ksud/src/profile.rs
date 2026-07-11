use crate::utils::ensure_dir_exists;
use crate::{defs, sepolicy};
use anyhow::{Context, Result, bail};
use std::fs;
use std::path::{Path, PathBuf};

fn entry_path(base: &str, name: &str) -> Result<PathBuf> {
    if name.is_empty()
        || name == "."
        || name == ".."
        || name.contains('/')
        || name.contains('\\')
        || name.contains('\0')
    {
        bail!("invalid profile entry name");
    }
    Ok(Path::new(base).join(name))
}

fn write_atomic(path: &Path, contents: impl AsRef<[u8]>) -> Result<()> {
    let parent = path
        .parent()
        .with_context(|| format!("{} has no parent", path.display()))?;
    let file_name = path
        .file_name()
        .and_then(|name| name.to_str())
        .with_context(|| format!("{} has no valid file name", path.display()))?;
    let temp_path = parent.join(format!(".{file_name}.tmp-{}", std::process::id()));

    fs::write(&temp_path, contents)
        .with_context(|| format!("failed to write {}", temp_path.display()))?;
    if let Err(error) = fs::rename(&temp_path, path) {
        let _ = fs::remove_file(&temp_path);
        return Err(error).with_context(|| format!("failed to replace {}", path.display()));
    }
    Ok(())
}

pub fn set_sepolicy(pkg: &str, policy: String) -> Result<()> {
    ensure_dir_exists(defs::PROFILE_SELINUX_DIR)?;
    let policy_file = entry_path(defs::PROFILE_SELINUX_DIR, pkg)?;
    sepolicy::live_patch_strict(&policy)?;
    write_atomic(&policy_file, policy)?;
    Ok(())
}

pub fn get_sepolicy(pkg: &str) -> Result<()> {
    let policy_file = entry_path(defs::PROFILE_SELINUX_DIR, pkg)?;
    let policy = std::fs::read_to_string(policy_file)?;
    println!("{policy}");
    Ok(())
}

// ksud does not validate template contents; it only persists them.
pub fn set_template(id: &str, template: String) -> Result<()> {
    ensure_dir_exists(defs::PROFILE_TEMPLATE_DIR)?;
    let template_file = entry_path(defs::PROFILE_TEMPLATE_DIR, id)?;
    write_atomic(&template_file, template)?;
    Ok(())
}

pub fn get_template(id: &str) -> Result<()> {
    let template_file = entry_path(defs::PROFILE_TEMPLATE_DIR, id)?;
    let template = std::fs::read_to_string(template_file)?;
    println!("{template}");
    Ok(())
}

pub fn delete_template(id: &str) -> Result<()> {
    let template_file = entry_path(defs::PROFILE_TEMPLATE_DIR, id)?;
    std::fs::remove_file(template_file)?;
    Ok(())
}

pub fn list_templates() -> Result<()> {
    let templates = std::fs::read_dir(defs::PROFILE_TEMPLATE_DIR);
    let Ok(templates) = templates else {
        return Ok(());
    };
    for template in templates {
        let template = template?;
        let template = template.file_name();
        if let Some(template) = template.to_str() {
            println!("{template}");
        }
    }
    Ok(())
}

pub fn apply_sepolies() -> Result<()> {
    let path = Path::new(defs::PROFILE_SELINUX_DIR);
    if !path.exists() {
        log::info!("profile sepolicy dir not exists.");
        return Ok(());
    }

    let sepolicies =
        std::fs::read_dir(path).with_context(|| "profile sepolicy dir open failed.".to_string())?;
    for sepolicy in sepolicies {
        let Ok(sepolicy) = sepolicy else {
            log::info!("profile sepolicy dir read failed.");
            continue;
        };
        let sepolicy = sepolicy.path();
        if sepolicy::apply_file(&sepolicy).is_ok() {
            log::info!("profile sepolicy applied: {}", sepolicy.display());
        } else {
            log::info!("profile sepolicy apply failed: {}", sepolicy.display());
        }
    }
    Ok(())
}
