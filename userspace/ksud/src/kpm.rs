use anyhow::{Context, Result, bail, ensure};
use goblin::elf::{Elf, header, section_header};
use log::warn;
use serde_json::{Map, Value, json};
use sha256::digest;
use std::collections::{HashMap, HashSet};
use std::fmt::Write as _;
use std::fs::{self, DirBuilder, File, OpenOptions};
use std::io::{ErrorKind, Read, Write};
use std::os::unix::fs::{DirBuilderExt, OpenOptionsExt};
use std::path::{Path, PathBuf};
use std::process::{Command, Output};
use std::time::{SystemTime, UNIX_EPOCH};

use crate::{defs, kpatch_next, ksucalls};

const MANIFEST_NAME: &str = "manifest.json";
const IMAGE_NAME: &str = "module.kpm";
const MANIFEST_VERSION: u64 = 1;
const MAX_IMAGE_SIZE: usize = 4 * 1024 * 1024;
const MAX_NAME_LEN: usize = 31;
const MAX_VERSION_LEN: usize = 31;
const MAX_LICENSE_LEN: usize = 31;
const MAX_AUTHOR_LEN: usize = 31;
const MAX_DESCRIPTION_LEN: usize = 511;
const MAX_ARGS_LEN: usize = 1023;
const MAX_SECTIONS: usize = 256;
const MAX_SECTION_MEMORY_SIZE: u64 = 8 * 1024 * 1024;
const MAX_INFO_SECTION_SIZE: u64 = 2048;
const MAX_PENDING_MODULES: usize = 32;
const ELF64_EHDR_SIZE: u64 = 64;
const ELF64_SHDR_SIZE: u64 = 64;
const ELF64_ALIGNMENT: u64 = 8;
const ELF64_SYM_SIZE: u64 = 24;
const ELF64_RELA_SIZE: u64 = 24;
const KPATCH_BINARY: &str = "/data/adb/modules/KPatch-Next/bin/kpatch";
const KPATCH_KPM_DIR: &str = "/data/adb/kp-next/kpm";
const KPATCH_KPM_ARGS_SUFFIX: &str = ".args";
const KPATCH_DATA_DIR: &str = "/data/adb/kp-next";
const KPATCH_EXCLUDE_CONFIG_PATH: &str = "/data/adb/kp-next/package_config";
const KPATCH_BOOT_PENDING_PATH: &str = "/data/adb/ksu/kpm/.kpatch_boot_pending";
const KPATCH_ABI_VERSION: u32 = 1;
const KPATCH_MAX_LOADED: u32 = 64;
const ANDROID_USER_RANGE: u32 = 100_000;
const EXCLUDE_CONFIG_NAME: &str = ".package_config";

fn kernel_page_size() -> u64 {
    let page_size = unsafe { libc::sysconf(libc::_SC_PAGESIZE) };
    if page_size > 0 {
        page_size as u64
    } else {
        4096
    }
}

#[derive(Clone, Debug)]
struct KpmMetadata {
    name: String,
    version: String,
    license: String,
    author: String,
    description: String,
}

#[derive(Clone, Debug)]
struct KpmManifest {
    metadata: KpmMetadata,
    sha256: String,
    args: String,
    enabled: bool,
    quarantined: bool,
    quarantine_reason: String,
    imported_at: String,
    source_name: String,
}

#[derive(Clone, Debug)]
struct KpmRuntimeInfo {
    name: String,
    version: String,
}

fn kpm_root() -> &'static Path {
    Path::new(defs::KPM_DIR)
}

fn ensure_kpm_root() -> Result<()> {
    let path = kpm_root();
    if let Ok(metadata) = fs::symlink_metadata(path) {
        ensure!(
            !metadata.file_type().is_symlink() && metadata.is_dir(),
            "refusing to use a symlink or non-directory KPM root: {}",
            path.display()
        );
    } else {
        DirBuilder::new().recursive(true).mode(0o700).create(path)?;
    }
    Ok(())
}

#[derive(Clone, Debug)]
struct ExcludedPackage {
    package: String,
    uid: u32,
}

fn exclude_config_path() -> PathBuf {
    kpm_root().join(EXCLUDE_CONFIG_NAME)
}

fn kpatch_exclude_config_path() -> &'static Path {
    Path::new(KPATCH_EXCLUDE_CONFIG_PATH)
}

fn validate_excluded_package(package: &str) -> Result<()> {
    ensure!(
        !package.is_empty() && package.len() <= 255,
        "invalid package name"
    );
    ensure!(
        package
            .bytes()
            .all(|byte| byte != b',' && !byte.is_ascii_control()),
        "invalid package name"
    );
    Ok(())
}

fn read_excluded_packages_from(path: &Path) -> Result<Vec<ExcludedPackage>> {
    if !path.exists() {
        return Ok(Vec::new());
    }
    let mut content = String::new();
    File::open(path)
        .with_context(|| format!("failed to open {}", path.display()))?
        .read_to_string(&mut content)
        .with_context(|| format!("failed to read {}", path.display()))?;
    let mut packages = Vec::new();
    for line in content
        .lines()
        .map(str::trim)
        .filter(|line| !line.is_empty())
    {
        let fields = line.split(',').collect::<Vec<_>>();
        let (package, uid_text) = if fields.len() >= 4 {
            (fields[0], fields[3])
        } else if fields.len() == 2 {
            (fields[0], fields[1])
        } else {
            continue;
        };
        let Ok(uid) = uid_text.parse::<u32>() else {
            continue;
        };
        let uid = uid % ANDROID_USER_RANGE;
        if validate_excluded_package(package).is_ok()
            && !packages
                .iter()
                .any(|item: &ExcludedPackage| item.package == package)
        {
            packages.push(ExcludedPackage {
                package: package.to_string(),
                uid,
            });
        }
    }
    Ok(packages)
}

fn read_excluded_packages() -> Result<Vec<ExcludedPackage>> {
    let kpatch_path = kpatch_exclude_config_path();
    if kpatch_path.is_file() {
        return read_excluded_packages_from(kpatch_path);
    }
    read_excluded_packages_from(&exclude_config_path())
}

fn should_sync_kpatch_excludes() -> bool {
    Path::new(KPATCH_BINARY).is_file() || Path::new(KPATCH_DATA_DIR).is_dir()
}

fn ensure_kpatch_data_dir() -> Result<()> {
    let path = Path::new(KPATCH_DATA_DIR);
    if let Ok(metadata) = fs::symlink_metadata(path) {
        ensure!(
            !metadata.file_type().is_symlink() && metadata.is_dir(),
            "refusing to use an invalid KPatch Next data directory: {}",
            path.display()
        );
    } else {
        DirBuilder::new().recursive(true).mode(0o700).create(path)?;
    }
    Ok(())
}

fn write_excluded_packages(packages: &[ExcludedPackage]) -> Result<()> {
    let mut manager_content = String::new();
    let mut kpatch_content = String::from("pkg,exclude,allow,uid\n");
    for item in packages {
        let _ = writeln!(manager_content, "{},1,0,{}", item.package, item.uid);
        let _ = writeln!(kpatch_content, "{},1,0,{}", item.package, item.uid);
    }
    write_atomic(&exclude_config_path(), manager_content.as_bytes())?;
    if should_sync_kpatch_excludes() {
        ensure_kpatch_data_dir()?;
        write_atomic(kpatch_exclude_config_path(), kpatch_content.as_bytes())?;
    }
    Ok(())
}

fn set_exclude_runtime(uid: u32, enabled: bool) -> Result<()> {
    if !kpatch_runtime_ready() {
        return Ok(());
    }
    let uid_text = uid.to_string();
    let state = if enabled { "1" } else { "0" };
    kpatch_command(&["exclude_set", &uid_text, state])?;
    Ok(())
}

pub fn set_excluded_package(package: &str, uid: u32, enabled: bool) -> Result<()> {
    ensure_kpm_root()?;
    validate_excluded_package(package)?;
    let app_id = uid % ANDROID_USER_RANGE;
    ensure!(app_id > 0, "invalid application UID");
    set_exclude_runtime(app_id, enabled)?;
    let mut packages = read_excluded_packages()?;
    packages.retain(|item| item.package != package);
    if enabled {
        packages.push(ExcludedPackage {
            package: package.to_string(),
            uid: app_id,
        });
    }
    write_excluded_packages(&packages)?;
    println!(
        "KPM exclusion {} for {} ({app_id})",
        if enabled { "enabled" } else { "disabled" },
        package
    );
    Ok(())
}

pub fn print_exclude_list() -> Result<()> {
    ensure_kpm_root()?;
    let packages = read_excluded_packages()?;
    println!(
        "{}",
        json!(
            packages
                .iter()
                .map(|item| json!({
                    "package": item.package,
                    "uid": item.uid,
                    "active": true,
                }))
                .collect::<Vec<_>>()
        )
    );
    Ok(())
}

fn ensure_kpatch_kpm_dir() -> Result<()> {
    let path = Path::new(KPATCH_KPM_DIR);
    if let Ok(metadata) = fs::symlink_metadata(path) {
        ensure!(
            !metadata.file_type().is_symlink() && metadata.is_dir(),
            "refusing to use an invalid KPatch KPM directory: {}",
            path.display()
        );
    } else {
        DirBuilder::new().recursive(true).mode(0o700).create(path)?;
    }
    Ok(())
}

fn kpatch_command(args: &[&str]) -> Result<Output> {
    let binary = Path::new(KPATCH_BINARY);
    let metadata = fs::symlink_metadata(binary).context("KPatch Next runtime is not installed")?;
    ensure!(
        metadata.is_file() && !metadata.file_type().is_symlink(),
        "KPatch runtime binary is invalid"
    );
    let old_path = std::env::var("PATH").unwrap_or_default();
    let output = Command::new(binary)
        .args(args)
        .env(
            "PATH",
            format!("/data/adb/modules/KPatch-Next/bin:{old_path}"),
        )
        .output()
        .with_context(|| format!("execute kpatch {}", args.join(" ")))?;
    if output.status.success() {
        return Ok(output);
    }
    let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();
    let stdout = String::from_utf8_lossy(&output.stdout).trim().to_string();
    let detail = if stderr.is_empty() { stdout } else { stderr };
    bail!(
        "kpatch {} failed with {}{}",
        args.join(" "),
        output.status,
        if detail.is_empty() {
            String::new()
        } else {
            format!(": {detail}")
        }
    )
}

fn kpatch_runtime_ready() -> bool {
    kpatch_command(&["hello"])
        .is_ok_and(|output| !String::from_utf8_lossy(&output.stdout).trim().is_empty())
}

fn kpatch_live_names() -> Result<Vec<String>> {
    if !kpatch_runtime_ready() {
        return Ok(Vec::new());
    }
    let output = kpatch_command(&["kpm", "list"])?;
    Ok(String::from_utf8_lossy(&output.stdout)
        .lines()
        .map(str::trim)
        .filter(|line| !line.is_empty())
        .map(ToOwned::to_owned)
        .collect())
}

fn kpatch_image_path(id: &str) -> PathBuf {
    Path::new(KPATCH_KPM_DIR).join(format!("{id}.kpm"))
}

fn kpatch_args_path(id: &str) -> PathBuf {
    Path::new(KPATCH_KPM_DIR).join(format!("{id}{KPATCH_KPM_ARGS_SUFFIX}"))
}

fn sync_kpatch_image(id: &str, manifest: &KpmManifest) -> Result<PathBuf> {
    ensure_kpatch_kpm_dir()?;
    let bytes = hash_and_validate_image(id, manifest)?;
    let path = kpatch_image_path(id);
    write_atomic(&path, &bytes)?;
    // KPatch-Next accepts the module arguments as the second load argument.
    // Keep them in a private sidecar so service.sh can restore the same
    // runtime configuration after a reboot without parsing JSON in shell.
    write_atomic(&kpatch_args_path(id), manifest.args.as_bytes())?;
    Ok(path)
}

fn unsync_kpatch_image(id: &str) -> Result<()> {
    let path = kpatch_image_path(id);
    if path.exists() {
        fs::remove_file(&path)
            .with_context(|| format!("failed to remove KPatch KPM image: {}", path.display()))?;
    }
    let args_path = kpatch_args_path(id);
    if args_path.exists() {
        fs::remove_file(&args_path).with_context(|| {
            format!(
                "failed to remove KPatch KPM arguments: {}",
                args_path.display()
            )
        })?;
    }
    Ok(())
}

fn kpatch_load(id: &str, manifest: &KpmManifest) -> Result<KpmRuntimeInfo> {
    ensure!(
        kpatch_runtime_ready(),
        "KPatch Next is installed but the boot kernel is not patched or still needs a reboot"
    );
    let path = sync_kpatch_image(id, manifest)?;
    let path = path
        .to_str()
        .context("KPatch KPM path is not valid UTF-8")?;
    let args = manifest.args.as_str();
    if args.is_empty() {
        kpatch_command(&["kpm", "load", path])?;
    } else {
        kpatch_command(&["kpm", "load", path, args])?;
    }
    Ok(KpmRuntimeInfo {
        name: manifest.metadata.name.clone(),
        version: manifest.metadata.version.clone(),
    })
}

fn unload_runtime(id: &str) -> Result<()> {
    if kpatch_runtime_ready() {
        kpatch_command(&["kpm", "unload", id])?;
    }
    Ok(())
}

/// Stop KPatch-owned KPMs before its module is disabled or removed. Without
/// this handoff, the Manager would immediately switch back to the legacy
/// backend while the old KPatch instances were still resident in the kernel.
pub fn stop_kpatch_runtime() -> Result<()> {
    if kpatch_runtime_ready() {
        for id in kpatch_live_names()? {
            kpatch_command(&["kpm", "unload", &id])
                .with_context(|| format!("failed to unload KPatch KPM '{id}'"))?;
        }
    }
    let pending = Path::new(KPATCH_BOOT_PENDING_PATH);
    if pending.exists() {
        fs::remove_file(pending).context("failed to clear KPatch KPM pending marker")?;
    }
    Ok(())
}

// The policy is deliberately kept next to the imported images so it survives
// Manager updates while remaining outside the module enumeration.
fn read_policy() -> Result<bool> {
    ensure_kpm_root()?;
    let path = Path::new(defs::KPM_POLICY_PATH);
    let metadata = match fs::symlink_metadata(path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == ErrorKind::NotFound => return Ok(true),
        Err(error) => return Err(error.into()),
    };
    ensure!(
        metadata.is_file() && !metadata.file_type().is_symlink(),
        "invalid KPM policy file"
    );
    let value: Value =
        serde_json::from_slice(&fs::read(path)?).context("invalid KPM policy JSON")?;
    let map = value.as_object().context("KPM policy is not an object")?;
    ensure!(
        map.get("schemaVersion").and_then(Value::as_u64) == Some(1),
        "unsupported KPM policy version"
    );
    map.get("enabled")
        .and_then(Value::as_bool)
        .context("KPM policy enabled state is missing")
}

fn validate_id(id: &str) -> Result<()> {
    ensure!(
        !id.is_empty() && id.len() <= MAX_NAME_LEN,
        "invalid KPM name"
    );
    ensure!(
        id.bytes().enumerate().all(|(index, byte)| {
            byte.is_ascii_alphanumeric() || (index > 0 && matches!(byte, b'.' | b'_' | b'-'))
        }),
        "KPM name contains unsupported characters"
    );
    ensure!(
        id.as_bytes()[0].is_ascii_alphanumeric(),
        "KPM name must start with an ASCII letter or digit"
    );
    Ok(())
}

fn validate_metadata_field(value: &str, max_len: usize, field: &str) -> Result<()> {
    ensure!(!value.is_empty(), "KPM {field} is empty");
    ensure!(value.len() <= max_len, "KPM {field} is too long");
    ensure!(!value.as_bytes().contains(&0), "KPM {field} contains NUL");
    Ok(())
}

fn validate_args(args: &str) -> Result<()> {
    ensure!(args.len() <= MAX_ARGS_LEN, "KPM arguments are too long");
    ensure!(!args.as_bytes().contains(&0), "KPM arguments contain NUL");
    Ok(())
}

fn section_name<'a>(elf: &'a Elf<'a>, index: usize) -> Option<&'a str> {
    let section = elf.section_headers.get(index)?;
    elf.shdr_strtab.get_at(section.sh_name)
}

fn section_bytes<'a>(
    bytes: &'a [u8],
    section: &goblin::elf::section_header::SectionHeader,
) -> Result<&'a [u8]> {
    let offset = usize::try_from(section.sh_offset).context("KPM section offset overflow")?;
    let size = usize::try_from(section.sh_size).context("KPM section size overflow")?;
    let end = offset
        .checked_add(size)
        .context("KPM section range overflow")?;
    bytes
        .get(offset..end)
        .context("KPM section is outside the file")
}

fn parse_metadata(bytes: &[u8], elf: &Elf<'_>) -> Result<KpmMetadata> {
    let section = elf
        .section_headers
        .iter()
        .enumerate()
        .find(|(index, _)| section_name(elf, *index) == Some(".kpm.info"))
        .map(|(_, section)| section)
        .context(".kpm.info section is missing")?;
    ensure!(
        section.sh_flags & u64::from(section_header::SHF_ALLOC) != 0,
        ".kpm.info is not allocatable"
    );
    let data = section_bytes(bytes, section)?;
    ensure!(
        data.last() == Some(&0),
        "KPM metadata section is not NUL terminated"
    );
    let mut fields = HashMap::new();
    for raw in data.split(|byte| *byte == 0) {
        if raw.is_empty() {
            continue;
        }
        let separator = raw.iter().position(|byte| *byte == b'=');
        let separator = separator.context("KPM metadata entry has no '='")?;
        ensure!(
            separator > 0 && separator + 1 < raw.len(),
            "KPM metadata value is empty"
        );
        let key =
            std::str::from_utf8(&raw[..separator]).context("KPM metadata key is invalid UTF-8")?;
        let value = std::str::from_utf8(&raw[separator + 1..])
            .context("KPM metadata value is invalid UTF-8")?;
        ensure!(
            fields.insert(key.to_string(), value.to_string()).is_none(),
            "duplicate KPM metadata key"
        );
    }

    let metadata = KpmMetadata {
        name: fields
            .remove("name")
            .context("KPM name metadata is missing")?,
        version: fields
            .remove("version")
            .context("KPM version metadata is missing")?,
        license: fields
            .remove("license")
            .unwrap_or_else(|| "unknown".to_string()),
        author: fields
            .remove("author")
            .unwrap_or_else(|| "unknown".to_string()),
        description: fields.remove("description").unwrap_or_default(),
    };
    validate_id(&metadata.name)?;
    validate_metadata_field(&metadata.name, MAX_NAME_LEN, "name")?;
    validate_metadata_field(&metadata.version, MAX_VERSION_LEN, "version")?;
    validate_metadata_field(&metadata.license, MAX_LICENSE_LEN, "license")?;
    validate_metadata_field(&metadata.author, MAX_AUTHOR_LEN, "author")?;
    ensure!(
        metadata.description.len() <= MAX_DESCRIPTION_LEN,
        "KPM description is too long"
    );
    Ok(metadata)
}

fn parse_kpm(bytes: &[u8]) -> Result<KpmMetadata> {
    ensure!(
        !bytes.is_empty() && bytes.len() <= MAX_IMAGE_SIZE,
        "KPM file size is unsupported"
    );
    let elf = Elf::parse(bytes).context("invalid KPM ELF")?;
    ensure!(
        elf.header.e_type == header::ET_REL,
        "KPM must be an ET_REL ELF"
    );
    ensure!(
        elf.header.e_machine == header::EM_AARCH64,
        "KPM must target AArch64"
    );
    ensure!(
        elf.is_64 && elf.little_endian,
        "KPM must be a little-endian 64-bit ELF"
    );
    ensure!(
        elf.header.e_phoff == 0 && elf.header.e_phnum == 0 && elf.header.e_phentsize == 0,
        "KPM must not contain program headers"
    );
    ensure!(
        u64::from(elf.header.e_ehsize) == ELF64_EHDR_SIZE
            && u64::from(elf.header.e_shentsize) == ELF64_SHDR_SIZE
            && elf.header.e_shoff % ELF64_ALIGNMENT == 0,
        "KPM ELF section table is malformed"
    );
    ensure!(
        !elf.section_headers.is_empty() && elf.section_headers.len() <= MAX_SECTIONS,
        "KPM section table is unsupported"
    );
    ensure!(
        elf.header.e_shstrndx != section_header::SHN_UNDEF as u16
            && (elf.header.e_shstrndx as usize) < elf.section_headers.len(),
        "KPM section-name table index is invalid"
    );
    let section_string_table = &elf.section_headers[elf.header.e_shstrndx as usize];
    ensure!(
        section_string_table.sh_type == section_header::SHT_STRTAB,
        "KPM section-name table is invalid"
    );
    let section_string_data = section_bytes(bytes, section_string_table)?;
    ensure!(
        !section_string_data.is_empty() && section_string_data[0] == 0,
        "KPM section-name table is invalid"
    );

    let mut special_sections = HashSet::new();
    let mut symbol_section = None;
    let page_size = kernel_page_size();
    for (index, section) in elf.section_headers.iter().enumerate() {
        let name = section_name(&elf, index)
            .with_context(|| format!("section {index} has an invalid name"))?;
        if section.sh_type != section_header::SHT_NOBITS {
            let _ = section_bytes(bytes, section)
                .with_context(|| format!("section {name} is outside the KPM file"))?;
        }
        ensure!(
            section.sh_size <= MAX_SECTION_MEMORY_SIZE,
            "section {name} is too large"
        );
        ensure!(
            (section.sh_addralign == 0 || section.sh_addralign.is_power_of_two())
                && section.sh_addralign <= page_size,
            "section {name} has invalid alignment"
        );
        ensure!(
            section.sh_flags & u64::from(section_header::SHF_COMPRESSED) == 0,
            "compressed KPM sections are unsupported"
        );
        ensure!(
            section.sh_flags & u64::from(section_header::SHF_TLS) == 0,
            "TLS KPM sections are unsupported"
        );
        ensure!(
            section.sh_flags
                & (u64::from(section_header::SHF_ALLOC)
                    | u64::from(section_header::SHF_WRITE)
                    | u64::from(section_header::SHF_EXECINSTR))
                != (u64::from(section_header::SHF_ALLOC)
                    | u64::from(section_header::SHF_WRITE)
                    | u64::from(section_header::SHF_EXECINSTR)),
            "writable executable KPM sections are unsupported"
        );
        if matches!(
            name,
            ".kpm.info" | ".kpm.init" | ".kpm.exit" | ".kpm.ctl0" | ".kpm.ctl1" | ".kpm.event"
        ) {
            ensure!(
                special_sections.insert(name.to_string()),
                "duplicate KPM special section: {name}"
            );
        }
        if name == ".kpm.info" {
            ensure!(
                section.sh_type == section_header::SHT_PROGBITS
                    && section.sh_flags & u64::from(section_header::SHF_ALLOC) != 0
                    && section.sh_size > 0
                    && section.sh_size <= MAX_INFO_SECTION_SIZE,
                ".kpm.info has an invalid layout"
            );
        }
        if matches!(
            name,
            ".kpm.init" | ".kpm.exit" | ".kpm.ctl0" | ".kpm.ctl1" | ".kpm.event"
        ) {
            ensure!(
                section.sh_type == section_header::SHT_PROGBITS
                    && section.sh_flags & u64::from(section_header::SHF_ALLOC) != 0
                    && section.sh_size == std::mem::size_of::<u64>() as u64
                    && section.sh_addralign >= std::mem::size_of::<u64>() as u64,
                "KPM callback section has an invalid layout"
            );
        }
        if section.sh_type == section_header::SHT_REL {
            bail!("AArch64 REL sections are unsupported");
        }
        if section.sh_type == section_header::SHT_SYMTAB {
            ensure!(
                symbol_section.is_none()
                    && (section.sh_link as usize) < elf.section_headers.len()
                    && elf.section_headers[section.sh_link as usize].sh_type
                        == section_header::SHT_STRTAB
                    && section.sh_entsize == ELF64_SYM_SIZE
                    && section.sh_size >= ELF64_SYM_SIZE
                    && section.sh_size % ELF64_SYM_SIZE == 0
                    && section.sh_info > 0
                    && u64::from(section.sh_info) <= section.sh_size / ELF64_SYM_SIZE
                    && section.sh_offset % ELF64_ALIGNMENT == 0,
                "KPM symbol table is invalid"
            );
            symbol_section = Some(index);
        }
        if section.sh_type == section_header::SHT_RELA {
            ensure!(
                section.sh_entsize == ELF64_RELA_SIZE
                    && section.sh_size % ELF64_RELA_SIZE == 0
                    && (section.sh_info as usize) < elf.section_headers.len()
                    && (section.sh_link as usize) < elf.section_headers.len()
                    && section.sh_offset % ELF64_ALIGNMENT == 0,
                "KPM relocation section is invalid"
            );
        }
    }
    ensure!(symbol_section.is_some(), "KPM symbol table is missing");
    let symbol_section = symbol_section.expect("symbol section was checked above");
    let symbol_strings = &elf.section_headers[elf.section_headers[symbol_section].sh_link as usize];
    let symbol_string_data = section_bytes(bytes, symbol_strings)?;
    ensure!(
        !symbol_string_data.is_empty() && symbol_string_data[0] == 0,
        "KPM symbol string table is invalid"
    );
    ensure!(
        [".kpm.info", ".kpm.init", ".kpm.exit"]
            .iter()
            .all(|required| special_sections.contains(*required)),
        "required KPM callback sections are missing"
    );
    parse_metadata(bytes, &elf)
}

fn manifest_path(id: &str) -> PathBuf {
    kpm_root().join(id).join(MANIFEST_NAME)
}

fn image_path(id: &str) -> PathBuf {
    kpm_root().join(id).join(IMAGE_NAME)
}

fn validated_image_path(id: &str) -> Result<PathBuf> {
    validate_id(id)?;
    let path = image_path(id);
    let metadata = fs::symlink_metadata(&path)
        .with_context(|| format!("KPM image does not exist: {}", path.display()))?;
    ensure!(
        metadata.is_file() && !metadata.file_type().is_symlink(),
        "KPM image must be a regular file"
    );
    Ok(path)
}

fn value_string(map: &Map<String, Value>, key: &str, default: &str) -> String {
    map.get(key)
        .and_then(Value::as_str)
        .unwrap_or(default)
        .to_string()
}

fn read_manifest(id: &str) -> Result<KpmManifest> {
    validate_id(id)?;
    let directory = kpm_root().join(id);
    let metadata = fs::symlink_metadata(&directory)
        .with_context(|| format!("KPM directory does not exist: {id}"))?;
    ensure!(
        !metadata.file_type().is_symlink() && metadata.is_dir(),
        "invalid KPM directory"
    );
    let raw = fs::read(manifest_path(id)).context("failed to read KPM manifest")?;
    let value: Value = serde_json::from_slice(&raw).context("invalid KPM manifest JSON")?;
    let map = value.as_object().context("KPM manifest is not an object")?;
    ensure!(
        map.get("schemaVersion").and_then(Value::as_u64) == Some(MANIFEST_VERSION),
        "unsupported KPM manifest version"
    );
    let metadata_value = map
        .get("metadata")
        .and_then(Value::as_object)
        .context("KPM manifest metadata is missing")?;
    let metadata = KpmMetadata {
        name: value_string(metadata_value, "name", ""),
        version: value_string(metadata_value, "version", ""),
        license: value_string(metadata_value, "license", "unknown"),
        author: value_string(metadata_value, "author", "unknown"),
        description: value_string(metadata_value, "description", ""),
    };
    validate_id(&metadata.name)?;
    ensure!(
        metadata.name == id,
        "KPM manifest name does not match directory"
    );
    validate_metadata_field(&metadata.version, MAX_VERSION_LEN, "version")?;
    validate_metadata_field(&metadata.license, MAX_LICENSE_LEN, "license")?;
    validate_metadata_field(&metadata.author, MAX_AUTHOR_LEN, "author")?;
    ensure!(
        metadata.description.len() <= MAX_DESCRIPTION_LEN,
        "KPM description is too long"
    );
    let args = value_string(map, "args", "");
    validate_args(&args)?;
    let sha256 = value_string(map, "sha256", "");
    ensure!(
        sha256.len() == 64 && sha256.bytes().all(|byte| byte.is_ascii_hexdigit()),
        "invalid KPM SHA-256"
    );
    Ok(KpmManifest {
        metadata,
        sha256,
        args,
        enabled: map.get("enabled").and_then(Value::as_bool).unwrap_or(false),
        quarantined: map
            .get("quarantined")
            .and_then(Value::as_bool)
            .unwrap_or(false),
        quarantine_reason: value_string(map, "quarantineReason", ""),
        imported_at: value_string(map, "importedAt", ""),
        source_name: value_string(map, "sourceName", ""),
    })
}

fn manifest_value(manifest: &KpmManifest) -> Value {
    json!({
        "schemaVersion": MANIFEST_VERSION,
        "metadata": {
            "name": manifest.metadata.name,
            "version": manifest.metadata.version,
            "license": manifest.metadata.license,
            "author": manifest.metadata.author,
            "description": manifest.metadata.description,
        },
        "sha256": manifest.sha256,
        "args": manifest.args,
        "enabled": manifest.enabled,
        "quarantined": manifest.quarantined,
        "quarantineReason": manifest.quarantine_reason,
        "importedAt": manifest.imported_at,
        "sourceName": manifest.source_name,
    })
}

fn write_atomic(path: &Path, bytes: &[u8]) -> Result<()> {
    let parent = path.parent().context("atomic file has no parent")?;
    let temp_path = parent.join(format!(
        ".{}.tmp-{}-{}",
        path.file_name()
            .and_then(|name| name.to_str())
            .unwrap_or("kpm"),
        std::process::id(),
        monotonic_nonce()
    ));
    let result = (|| -> Result<()> {
        let mut file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .mode(0o600)
            .open(&temp_path)?;
        file.write_all(bytes)?;
        file.sync_all()?;
        fs::rename(&temp_path, path)?;
        Ok(())
    })();
    if result.is_err() {
        let _ = fs::remove_file(&temp_path);
    }
    result
}

fn write_policy(enabled: bool) -> Result<()> {
    write_atomic(
        Path::new(defs::KPM_POLICY_PATH),
        &serde_json::to_vec_pretty(&json!({
            "schemaVersion": 1,
            "enabled": enabled,
        }))?,
    )
}

fn write_manifest(id: &str, manifest: &KpmManifest) -> Result<()> {
    let bytes = serde_json::to_vec_pretty(&manifest_value(manifest))?;
    write_atomic(&manifest_path(id), &bytes)
}

fn monotonic_nonce() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_or(0, |duration| duration.as_nanos())
}

fn current_boot_id() -> Result<String> {
    let value = fs::read_to_string("/proc/sys/kernel/random/boot_id")
        .context("failed to read Android boot id")?;
    let value = value.trim();
    ensure!(!value.is_empty(), "Android boot id is empty");
    Ok(value.to_string())
}

fn write_pending(ids: &[String], boot_id: &str) -> Result<()> {
    ensure!(
        !ids.is_empty() && ids.len() <= MAX_PENDING_MODULES,
        "invalid pending KPM batch"
    );
    ensure!(!boot_id.is_empty(), "pending KPM boot id is empty");
    for id in ids {
        validate_id(id)?;
    }
    ensure!(
        ids.iter().collect::<HashSet<_>>().len() == ids.len(),
        "pending KPM batch contains duplicate ids"
    );
    let pending = json!({"ids": ids, "bootId": boot_id});
    write_atomic(
        Path::new(defs::KPM_BOOT_PENDING_PATH),
        &serde_json::to_vec(&pending)?,
    )
}

fn clear_pending() -> Result<()> {
    let path = Path::new(defs::KPM_BOOT_PENDING_PATH);
    if path.exists() {
        fs::remove_file(path).context("failed to clear pending KPM marker")?;
    }
    Ok(())
}

fn hash_and_validate_image(id: &str, manifest: &KpmManifest) -> Result<Vec<u8>> {
    let path = validated_image_path(id)?;
    let bytes = fs::read(path).context("failed to read KPM image")?;
    ensure!(bytes.len() <= MAX_IMAGE_SIZE, "KPM image is too large");
    ensure!(
        digest(&bytes) == manifest.sha256,
        "KPM image SHA-256 does not match manifest"
    );
    let metadata = parse_kpm(&bytes)?;
    ensure!(
        metadata.name == manifest.metadata.name,
        "KPM name changed after import"
    );
    ensure!(
        metadata.version == manifest.metadata.version,
        "KPM version changed after import"
    );
    Ok(bytes)
}

fn live_names() -> Result<Vec<String>> {
    kpatch_live_names()
}

fn is_live(id: &str) -> Result<bool> {
    Ok(live_names()?.iter().any(|name| name == id))
}

fn ensure_kpm_management_available() -> Result<()> {
    ensure!(
        !ksucalls::is_late_load(),
        "KPM is disabled in jailbreak (late-load) mode"
    );
    ensure!(
        kpatch_next::is_enabled(),
        "KPM requires the KPatch-Next module to be enabled"
    );
    Ok(())
}

fn ensure_kpm_available() -> Result<()> {
    ensure_kpm_management_available()?;
    ensure!(
        read_policy()?,
        "KPM loading is disabled by user policy; enable it first"
    );
    Ok(())
}

fn load_manifest_now(
    id: &str,
    manifest: &KpmManifest,
    keep_pending: bool,
    pending_ids: Option<&[String]>,
) -> Result<KpmRuntimeInfo> {
    ensure_kpm_available()?;
    ensure!(manifest.enabled, "KPM is disabled");
    ensure!(
        !manifest.quarantined,
        "KPM is quarantined; enable it to retry"
    );
    let _ = hash_and_validate_image(id, manifest)?;
    let boot_id = current_boot_id()?;
    let single_pending_id;
    let pending_ids = if let Some(ids) = pending_ids {
        ids
    } else {
        single_pending_id = vec![id.to_string()];
        &single_pending_id
    };
    write_pending(pending_ids, &boot_id)?;
    match kpatch_load(id, manifest) {
        Ok(info) => {
            if !keep_pending {
                clear_pending()?;
            }
            Ok(info)
        }
        Err(error) => {
            let _ = clear_pending();
            bail!("failed to load KPM {id} through KPatch Next: {error:#}")
        }
    }
}

fn quarantine(id: &str, reason: &str) -> Result<()> {
    let mut manifest = read_manifest(id)?;
    manifest.enabled = false;
    manifest.quarantined = true;
    manifest.quarantine_reason = reason.to_string();
    write_manifest(id, &manifest)
}

fn recover_kpatch_previous_boot() -> Result<()> {
    let path = Path::new(KPATCH_BOOT_PENDING_PATH);
    if !path.exists() {
        return Ok(());
    }
    let content =
        fs::read_to_string(path).context("failed to read KPatch Next boot pending marker")?;
    let ids = content
        .lines()
        .map(str::trim)
        .filter(|id| !id.is_empty())
        .collect::<HashSet<_>>();
    if ids.is_empty() {
        warn!("KPatch Next boot pending marker is empty; clearing it");
        fs::remove_file(path).context("failed to clear empty KPatch Next boot pending marker")?;
        return Ok(());
    }
    warn!(
        "previous boot stopped while {} KPatch KPM(s) were active; quarantining them",
        ids.len()
    );
    for id in ids {
        if let Err(error) = validate_id(id) {
            warn!("ignoring invalid KPatch KPM pending id '{id}': {error:#}");
            continue;
        }
        if let Err(error) = quarantine(
            id,
            "previous boot did not complete after KPatch Next KPM load",
        ) {
            warn!("failed to quarantine KPatch KPM '{id}': {error:#}");
        }
        if let Err(error) = unsync_kpatch_image(id) {
            warn!("failed to remove KPatch KPM '{id}' after recovery: {error:#}");
        }
    }
    fs::remove_file(path).context("failed to clear KPatch Next boot pending marker")?;
    Ok(())
}

/// Synchronize enabled KPM images to the KPatch-Next service directory before
/// enabling the backend. The canonical manifests remain under ApkeSU so the
/// Manager keeps one source of truth.
pub fn migrate_to_kpatch_next() -> Result<()> {
    ensure!(
        !ksucalls::is_late_load(),
        "KPatch Next is disabled in jailbreak (late-load) mode"
    );
    ensure_kpm_root()?;
    ensure_kpatch_kpm_dir()?;
    for entry in fs::read_dir(kpm_root())? {
        let entry = entry?;
        let id = entry.file_name().to_string_lossy().into_owned();
        if id.starts_with('.') || entry.file_type()?.is_symlink() {
            continue;
        }
        match read_manifest(&id) {
            Ok(manifest) if manifest.enabled && !manifest.quarantined => {
                if let Err(error) = sync_kpatch_image(&id, &manifest) {
                    warn!("failed to synchronize KPM '{id}' with KPatch Next: {error:#}");
                    if let Err(quarantine_error) =
                        quarantine(&id, &format!("KPatch synchronization failed: {error:#}"))
                    {
                        warn!("failed to quarantine KPM '{id}': {quarantine_error:#}");
                    }
                    if let Err(cleanup_error) = unsync_kpatch_image(&id) {
                        warn!("failed to remove stale KPatch KPM '{id}': {cleanup_error:#}");
                    }
                }
            }
            Ok(_) => unsync_kpatch_image(&id)?,
            Err(error) => {
                warn!("ignoring invalid KPM '{id}' during KPatch migration: {error:#}");
                unsync_kpatch_image(&id)?;
            }
        }
    }

    clear_pending()?;
    Ok(())
}

/// Remove synchronized KPM files when KPatch-Next installation fails before
/// its service can own them.
pub fn cleanup_kpatch_after_install_failure() -> Result<()> {
    ensure_kpm_root()?;
    for entry in fs::read_dir(kpm_root())? {
        let entry = entry?;
        let id = entry.file_name().to_string_lossy().into_owned();
        if id.starts_with('.') || entry.file_type()?.is_symlink() {
            continue;
        }
        if let Err(error) = unsync_kpatch_image(&id) {
            warn!("failed to remove migrated KPatch KPM '{id}': {error:#}");
        }
    }
    clear_pending()?;
    Ok(())
}

pub fn recover_boot_state() {
    if let Err(error) = ensure_kpm_root() {
        warn!("KPM boot recovery could not prepare storage: {error:#}");
        return;
    }
    if let Err(error) = recover_kpatch_previous_boot() {
        warn!("KPatch Next KPM boot recovery failed: {error:#}");
    }
}

pub fn load_enabled_at_boot() {
    recover_boot_state();
    // KPatch-Next service.sh owns boot loading and its runtime pending marker.
}

pub fn mark_boot_completed() {
    let marker = Path::new(KPATCH_BOOT_PENDING_PATH);
    if marker.exists()
        && let Err(error) = fs::remove_file(marker)
    {
        warn!("failed to clear KPatch Next boot pending marker: {error:#}");
    }
    if let Err(error) = clear_pending() {
        warn!("failed to clear KPM pending marker: {error:#}");
    }
}

pub fn print_caps() {
    let late_load = ksucalls::is_late_load();
    let policy = read_policy();
    let policy_enabled = policy.as_ref().is_ok_and(|enabled| *enabled);
    let module_enabled = kpatch_next::is_enabled();
    let kernel_supported = kpatch_runtime_ready();
    let disabled_reason = if late_load {
        "jailbreak (late-load) mode"
    } else if policy.is_err() {
        "invalid KPM policy"
    } else if !policy_enabled {
        "disabled by user policy"
    } else if !module_enabled {
        "KPatch-Next is not enabled"
    } else if !kernel_supported {
        "KPatch-Next runtime is not active; reboot may be required"
    } else {
        ""
    };
    println!(
        "{}",
        json!({
            "backend": "kpatch-next",
            "managementAvailable": module_enabled && !late_load,
            "abiVersion": KPATCH_ABI_VERSION,
            "capabilities": i32::from(kernel_supported),
            "maxImageSize": MAX_IMAGE_SIZE,
            "maxLoaded": KPATCH_MAX_LOADED,
            "kernelSupported": kernel_supported,
            "policyEnabled": policy_enabled,
            "lateLoad": late_load,
            "supported": module_enabled && kernel_supported && policy_enabled && !late_load,
            "disabledReason": disabled_reason,
            "policyError": policy.as_ref().err().map(|error| format!("{error:#}")),
        })
    );
}

pub fn print_policy() {
    print_caps();
}

pub fn set_policy(enabled: bool) -> Result<()> {
    ensure_kpm_management_available()?;
    ensure_kpm_root()?;
    if !enabled {
        for id in live_names()? {
            unload_runtime(&id).with_context(|| format!("failed to unload KPM '{id}'"))?;
        }
        clear_pending()?;
    }
    let current = read_policy()?;
    if current == enabled {
        println!(
            "KPM policy is already {}",
            if enabled { "enabled" } else { "disabled" }
        );
        return Ok(());
    }
    write_policy(enabled)?;
    if enabled {
        migrate_to_kpatch_next()?;
        if kpatch_runtime_ready() {
            let ids = fs::read_dir(kpm_root())?
                .flatten()
                .filter_map(|entry| entry.file_name().to_str().map(ToOwned::to_owned))
                .collect::<Vec<_>>();
            for id in ids {
                let Ok(manifest) = read_manifest(&id) else {
                    continue;
                };
                if manifest.enabled && !manifest.quarantined && !is_live(&id)? {
                    load_manifest_now(&id, &manifest, false, None)?;
                }
            }
        }
    }
    println!(
        "KPM policy {}",
        if enabled { "enabled" } else { "disabled" }
    );
    Ok(())
}

fn manifest_json(id: &str, manifest: &KpmManifest, live: bool) -> Value {
    json!({
        "id": id,
        "name": manifest.metadata.name,
        "version": manifest.metadata.version,
        "license": manifest.metadata.license,
        "author": manifest.metadata.author,
        "description": manifest.metadata.description,
        "sha256": manifest.sha256,
        "args": manifest.args,
        "enabled": manifest.enabled,
        "quarantined": manifest.quarantined,
        "quarantineReason": manifest.quarantine_reason,
        "sourceName": manifest.source_name,
        "importedAt": manifest.imported_at,
        "loaded": live,
    })
}

pub fn print_list() -> Result<()> {
    ensure_kpm_root()?;
    let live = if ksucalls::is_late_load() {
        Vec::new()
    } else {
        live_names()?
    };
    let mut values = Vec::new();
    for entry in fs::read_dir(kpm_root())? {
        let entry = entry?;
        let id = entry.file_name().to_string_lossy().into_owned();
        if id.starts_with('.') || entry.file_type()?.is_symlink() {
            continue;
        }
        match read_manifest(&id) {
            Ok(manifest) => values.push(manifest_json(
                &id,
                &manifest,
                live.iter().any(|name| name == &id),
            )),
            Err(error) => values.push(json!({"id": id, "error": format!("{error:#}")})),
        }
    }
    println!("{}", Value::Array(values));
    Ok(())
}

pub fn print_info(id: &str) -> Result<()> {
    validate_id(id)?;
    let manifest = read_manifest(id)?;
    let loaded = is_live(id)?;
    let image_size =
        u32::try_from(fs::metadata(validated_image_path(id)?)?.len()).unwrap_or(u32::MAX);
    println!(
        "{}",
        json!({
            "backend": "kpatch-next",
            "name": manifest.metadata.name,
            "version": manifest.metadata.version,
            "license": manifest.metadata.license,
            "author": manifest.metadata.author,
            "description": manifest.metadata.description,
            "state": if loaded { 2 } else { 0 },
            "imageSize": image_size,
            "textSize": 0,
            "roSize": 0,
            "loadedAtNs": 0,
        })
    );
    Ok(())
}

pub fn import(path: &Path, args: &str, trusted: bool, force: bool, enable: bool) -> Result<()> {
    ensure_kpm_available()?;
    ensure!(trusted, "KPM import requires --trusted acknowledgement");
    validate_args(args)?;
    ensure_kpm_root()?;
    let source_metadata = fs::symlink_metadata(path).context("KPM source does not exist")?;
    ensure!(
        source_metadata.is_file() && !source_metadata.file_type().is_symlink(),
        "KPM source must be a regular file"
    );
    let bytes = fs::read(path).context("failed to read KPM source")?;
    let metadata = parse_kpm(&bytes)?;
    let id = metadata.name.clone();
    let target = kpm_root().join(&id);
    let target_metadata = fs::symlink_metadata(&target).ok();
    ensure!(
        target_metadata
            .as_ref()
            .is_none_or(|metadata| !metadata.file_type().is_symlink()),
        "refusing to replace a symlinked KPM directory"
    );
    if target_metadata.is_some() && !force {
        bail!("KPM '{id}' already exists; pass --force to replace it");
    }
    if let Some(metadata) = target_metadata.as_ref() {
        ensure!(metadata.is_dir(), "existing KPM path is not a directory");
    }
    if force && is_live(&id)? {
        unload_runtime(&id).context("failed to unload existing KPM before replacement")?;
    }
    if force {
        unsync_kpatch_image(&id)?;
    }
    let operation_id = monotonic_nonce();
    let temp = kpm_root().join(format!(
        ".import-{id}-{}-{operation_id}",
        std::process::id()
    ));
    DirBuilder::new().mode(0o700).create(&temp)?;
    let result = (|| -> Result<()> {
        let image = temp.join(IMAGE_NAME);
        let mut file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .mode(0o600)
            .open(&image)?;
        file.write_all(&bytes)?;
        file.sync_all()?;
        let manifest = KpmManifest {
            metadata,
            sha256: digest(&bytes),
            args: args.to_string(),
            enabled: false,
            quarantined: false,
            quarantine_reason: String::new(),
            imported_at: chrono::Local::now().to_rfc3339(),
            source_name: path
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("module.kpm")
                .to_string(),
        };
        let manifest_bytes = serde_json::to_vec_pretty(&manifest_value(&manifest))?;
        let manifest_path = temp.join(MANIFEST_NAME);
        let mut manifest_file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .mode(0o600)
            .open(&manifest_path)?;
        manifest_file.write_all(&manifest_bytes)?;
        manifest_file.sync_all()?;
        if target_metadata.is_some() {
            let backup = kpm_root().join(format!(
                ".replace-{id}-{}-{operation_id}",
                std::process::id()
            ));
            fs::rename(&target, &backup)?;
            if let Err(error) = fs::rename(&temp, &target) {
                fs::rename(&backup, &target).with_context(|| {
                    format!("failed to restore existing KPM after replacement error: {error}")
                })?;
                return Err(error.into());
            }
            if let Err(error) = fs::remove_dir_all(&backup) {
                warn!(
                    "failed to remove KPM replacement backup {}: {error}",
                    backup.display()
                );
            }
        } else {
            fs::rename(&temp, &target)?;
        }
        Ok(())
    })();
    if result.is_err() {
        let _ = fs::remove_dir_all(&temp);
    }
    result?;
    if enable {
        enable_module(&id)?;
    }
    println!("Imported KPM '{id}'");
    Ok(())
}

pub fn enable_module(id: &str) -> Result<()> {
    ensure_kpm_available()?;
    ensure_kpm_root()?;
    let previous = read_manifest(id)?;
    let mut manifest = previous.clone();
    manifest.enabled = true;
    manifest.quarantined = false;
    manifest.quarantine_reason.clear();
    write_manifest(id, &manifest)?;
    if let Err(error) = sync_kpatch_image(id, &manifest) {
        if let Err(rollback_error) = write_manifest(id, &previous) {
            bail!(
                "failed to synchronize KPM '{id}' with KPatch Next: {error:#}; manifest rollback failed: {rollback_error:#}"
            );
        }
        return Err(error);
    }
    let runtime_ready = kpatch_runtime_ready();
    if runtime_ready
        && !is_live(id)?
        && let Err(error) = load_manifest_now(id, &manifest, false, None)
    {
        let _ = quarantine(id, &format!("enable failed: {error:#}"));
        let _ = unsync_kpatch_image(id);
        return Err(error);
    }
    println!(
        "Enabled KPatch Next KPM '{id}'{}",
        if runtime_ready {
            ""
        } else {
            "; it will load after the KPatch runtime is active"
        }
    );
    Ok(())
}

pub fn disable_module(id: &str) -> Result<()> {
    let mut manifest = read_manifest(id)?;
    let previous_manifest = manifest.clone();
    if is_live(id)? {
        unload_runtime(id).context("failed to unload KPM")?;
    }
    let restore_kpatch_state = kpatch_image_path(id).exists();
    unsync_kpatch_image(id)?;
    manifest.enabled = false;
    if let Err(error) = write_manifest(id, &manifest) {
        if restore_kpatch_state
            && let Err(rollback_error) = sync_kpatch_image(id, &previous_manifest)
        {
            bail!(
                "failed to disable KPM '{id}': {error:#}; KPatch state rollback failed: {rollback_error:#}"
            );
        }
        return Err(error);
    }
    println!("Disabled KPM '{id}'");
    Ok(())
}

pub fn remove_module(id: &str) -> Result<()> {
    validate_id(id)?;
    if is_live(id)? {
        unload_runtime(id).context("failed to unload KPM before removal")?;
    }
    unsync_kpatch_image(id)?;
    let path = kpm_root().join(id);
    let metadata = fs::symlink_metadata(&path).context("KPM does not exist")?;
    ensure!(
        !metadata.file_type().is_symlink() && metadata.is_dir(),
        "refusing to remove invalid KPM path"
    );
    fs::remove_dir_all(&path).context("failed to remove KPM")?;
    println!("Removed KPM '{id}'");
    Ok(())
}

pub fn load_module(id: &str) -> Result<()> {
    let manifest = read_manifest(id)?;
    let info = load_manifest_now(id, &manifest, false, None)?;
    println!(
        "{}",
        json!({"loaded": true, "name": info.name, "version": info.version})
    );
    Ok(())
}

pub fn unload_module(id: &str) -> Result<()> {
    validate_id(id)?;
    unload_runtime(id)?;
    println!("Unloaded KPM '{id}'");
    Ok(())
}

pub fn control_module(id: &str, args: &str) -> Result<()> {
    ensure_kpm_available()?;
    validate_id(id)?;
    validate_args(args)?;
    ensure!(kpatch_runtime_ready(), "KPatch Next runtime is not active");
    let output = kpatch_command(&["kpm", "ctl0", id, args])?;
    println!("{}", String::from_utf8_lossy(&output.stdout).trim());
    Ok(())
}
