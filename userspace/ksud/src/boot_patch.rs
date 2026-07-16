#![allow(clippy::ref_option, clippy::needless_pass_by_value)]

use std::fs::File;
use std::io::{Cursor, Seek, SeekFrom};
use std::path::Path;
use std::path::PathBuf;

use android_bootimg::cpio::{Cpio, CpioEntry};
use android_bootimg::parser::{BootImage, BootImageVersion, RamdiskImage};
use android_bootimg::patcher::BootImagePatchOption;
use anyhow::Context;
use anyhow::Result;
use anyhow::anyhow;
use anyhow::bail;
use anyhow::ensure;
use memmap2::{Mmap, MmapOptions};
use regex_lite::Regex;

use crate::assets;

const MANAGER_CERT_HASH: &[u8] =
    b"1c89980c03432844cfe195dab90bfaecbcd987d19309da648014164be78007d1";

#[cfg(target_os = "android")]
mod android {
    use super::Result;
    pub(super) use crate::defs::{BACKUP_FILENAME, KSU_BACKUP_DIR, KSU_BACKUP_FILE_PREFIX};
    use android_bootimg::cpio::{Cpio, CpioEntry};
    use anyhow::{Context, anyhow, bail, ensure};
    use regex_lite::Regex;
    use std::fs::{self, OpenOptions};
    use std::io::Write;
    use std::os::fd::AsRawFd;
    use std::os::unix::fs::PermissionsExt;
    use std::path::{Path, PathBuf};
    use std::process::Command;

    use crate::utils;

    pub(super) fn ensure_gki_kernel() -> Result<()> {
        let version = get_kernel_version()?;
        let is_gki = version.0 > 5 || version.0 == 5 && version.1 >= 10;
        ensure!(is_gki, "only support GKI kernel");
        Ok(())
    }

    pub fn get_kernel_version() -> Result<(i32, i32, i32)> {
        let uname = rustix::system::uname();
        let version = uname.release().to_string_lossy();
        let re = Regex::new(r"(\d+)\.(\d+)\.(\d+)")?;
        if let Some(captures) = re.captures(&version) {
            let major = captures
                .get(1)
                .and_then(|m| m.as_str().parse::<i32>().ok())
                .ok_or_else(|| anyhow!("Major version parse error"))?;
            let minor = captures
                .get(2)
                .and_then(|m| m.as_str().parse::<i32>().ok())
                .ok_or_else(|| anyhow!("Minor version parse error"))?;
            let patch = captures
                .get(3)
                .and_then(|m| m.as_str().parse::<i32>().ok())
                .ok_or_else(|| anyhow!("Patch version parse error"))?;
            Ok((major, minor, patch))
        } else {
            Err(anyhow!("Invalid kernel version string"))
        }
    }

    fn parse_kmi(version: &str) -> Result<String> {
        let re = Regex::new(r"(.* )?(\d+\.\d+)(\S+)?(android\d+)(.*)")?;
        let cap = re
            .captures(version)
            .ok_or_else(|| anyhow::anyhow!("Failed to get KMI from boot/modules"))?;
        let android_version = cap.get(4).map_or("", |m| m.as_str());
        let kernel_version = cap.get(2).map_or("", |m| m.as_str());
        Ok(format!("{android_version}-{kernel_version}"))
    }

    fn parse_kmi_from_uname() -> Result<String> {
        let uname = rustix::system::uname();
        let version = uname.release().to_string_lossy();
        parse_kmi(&version)
    }

    fn parse_kmi_from_modules() -> Result<String> {
        use std::io::BufRead;
        // find a *.ko in /vendor/lib/modules
        let modfile = std::fs::read_dir("/vendor/lib/modules")?
            .filter_map(Result::ok)
            .find(|entry| entry.path().extension().is_some_and(|ext| ext == "ko"))
            .map(|entry| entry.path())
            .ok_or_else(|| anyhow!("No kernel module found"))?;
        let output = Command::new("modinfo").arg(modfile).output()?;
        for line in output.stdout.lines().map_while(Result::ok) {
            if line.starts_with("vermagic") {
                return parse_kmi(&line);
            }
        }
        bail!("Parse KMI from modules failed")
    }

    pub fn get_current_kmi() -> Result<String> {
        parse_kmi_from_uname().or_else(|_| parse_kmi_from_modules())
    }

    fn calculate_sha1(file_path: impl AsRef<Path>) -> Result<String> {
        use sha1::Digest;
        use std::io::Read;
        let mut file = std::fs::File::open(file_path.as_ref())?;
        let mut hasher = sha1::Sha1::new();
        let mut buffer = [0; 1024];

        loop {
            let n = file.read(&mut buffer)?;
            if n == 0 {
                break;
            }
            hasher.update(&buffer[..n]);
        }

        let result = hasher.finalize();
        Ok(base16ct::lower::encode_string(&result))
    }

    pub(super) fn do_backup(cpio: &mut Cpio, image: &Path) -> Result<()> {
        let sha1 = calculate_sha1(image)?;
        let filename = format!("{KSU_BACKUP_FILE_PREFIX}{sha1}");

        println!("- Backup stock boot image");
        let target = format!("{KSU_BACKUP_DIR}{filename}");
        let mut target_file = OpenOptions::new()
            .create(true)
            .truncate(true)
            .write(true)
            .open(&target)?;
        let mut source = OpenOptions::new()
            .create(false)
            .truncate(false)
            .read(true)
            .write(false)
            .open(image)?;

        // Use io::copy instead of fs::copy to allow copy block device
        std::io::copy(&mut source, &mut target_file)
            .with_context(|| format!("failed to backup to {target}"))?;

        let backup_file = CpioEntry::regular(0o755, Box::new(sha1));
        cpio.add(BACKUP_FILENAME, backup_file)?;
        println!("- Stock image has been backup to");
        println!("- {target}");
        Ok(())
    }

    pub(super) fn clean_backup(sha1: &str) -> Result<()> {
        println!("- Clean up backup");
        let backup_name = format!("{KSU_BACKUP_FILE_PREFIX}{sha1}");
        let dir = std::fs::read_dir(KSU_BACKUP_DIR)?;
        for entry in dir.flatten() {
            let path = entry.path();
            if !path.is_file() {
                continue;
            }
            if let Some(name) = path.file_name() {
                let name = name.to_string_lossy().to_string();
                if name != backup_name
                    && name.starts_with(KSU_BACKUP_FILE_PREFIX)
                    && std::fs::remove_file(path).is_ok()
                {
                    println!("- removed {name}");
                }
            }
        }
        Ok(())
    }

    pub(super) fn flash_partition(partition: &str, data: &[u8]) -> Result<()> {
        let mut blk = std::fs::OpenOptions::new()
            .write(true)
            .truncate(false)
            .create(false)
            .open(partition)
            .with_context(|| format!("open {partition}"))?;
        unsafe {
            const BLKROSET: i32 = libc::_IO(0x12, 93);
            let mut val: libc::c_int = 0;
            if libc::ioctl(blk.as_raw_fd(), BLKROSET, &raw mut val) != 0 {
                bail!("Failed to set rw for {partition}: {}", *libc::__errno());
            }
        }
        blk.write_all(data).context("flash boot failed")?;
        blk.sync_all().context("sync boot failed")?;
        Ok(())
    }

    pub fn choose_boot_partition(
        kmi: &str,
        is_replace_kernel: bool,
        partition: &Option<String>,
        ota: bool,
    ) -> String {
        let skip_init_boot = kmi.starts_with("android12-");
        let init_boot_exists = find_partition_path("init_boot", ota).is_some();

        // if specific partition is specified, use it
        if let Some(part) = partition {
            return match part.as_str() {
                "boot" | "init_boot" | "vendor_boot" => part.clone(),
                _ => "boot".to_string(),
            };
        }

        // if init_boot exists and not skipping it, use it
        if !is_replace_kernel && init_boot_exists && !skip_init_boot {
            return "init_boot".to_string();
        }

        "boot".to_string()
    }

    fn normalize_slot_suffix(value: &str) -> String {
        let slot = value.trim().trim_start_matches('_');
        if slot.is_empty() {
            String::new()
        } else {
            format!("_{slot}")
        }
    }

    pub fn get_slot_suffix(ota: bool) -> Result<String> {
        let mut slot_suffix =
            normalize_slot_suffix(&utils::getprop("ro.boot.slot_suffix").unwrap_or_default());
        if slot_suffix.is_empty()
            && let Some(slot) = utils::getprop("ro.boot.slot")
        {
            slot_suffix = normalize_slot_suffix(&slot);
        }
        if slot_suffix.is_empty()
            && let Some(slot) = slot_suffix_from_bootctl()
        {
            slot_suffix = slot;
        }

        if !ota {
            return Ok(slot_suffix);
        }

        match slot_suffix.as_str() {
            "_a" => Ok("_b".to_string()),
            "_b" => Ok("_a".to_string()),
            "" => bail!("cannot determine the current A/B slot"),
            value => bail!("unsupported A/B slot suffix: {value}"),
        }
    }

    fn slot_suffix_from_bootctl() -> Option<String> {
        for bootctl in [
            crate::assets::BOOTCTL_PATH,
            "/system/bin/bootctl",
            "/system_ext/bin/bootctl",
        ] {
            let Ok(output) = Command::new(bootctl).arg("get-current-slot").output() else {
                continue;
            };
            if !output.status.success() {
                continue;
            }
            let Some(slot_suffix) =
                String::from_utf8(output.stdout)
                    .ok()
                    .and_then(|slot| match slot.trim() {
                        "0" => Some("_a".to_string()),
                        "1" => Some("_b".to_string()),
                        _ => None,
                    })
            else {
                continue;
            };
            return Some(slot_suffix);
        }
        None
    }

    pub fn list_available_partitions(ota: bool) -> Vec<String> {
        let candidates = vec!["boot", "init_boot", "vendor_boot"];
        candidates
            .into_iter()
            .filter(|name| find_partition_path(name, ota).is_some())
            .map(ToString::to_string)
            .collect()
    }

    pub fn find_partition_path(name: &str, ota: bool) -> Option<PathBuf> {
        let slot_suffix = get_slot_suffix(ota).ok()?;
        partition_path_candidates(name, &slot_suffix, !ota)
            .into_iter()
            .find(|path| path.exists())
    }

    pub(super) fn auto_boot_partition_path(
        kmi: &str,
        ota: bool,
        is_replace_kernel: bool,
        partition: &Option<String>,
    ) -> Result<PathBuf> {
        let slot_suffix = get_slot_suffix(ota)?;
        let name = choose_boot_partition(kmi, is_replace_kernel, partition, ota);
        find_partition_path(&name, ota).ok_or_else(|| {
            anyhow!(
                "target partition {name}{slot_suffix} is unavailable; choose a partition present in the target slot"
            )
        })
    }

    fn partition_path_candidates(
        name: &str,
        slot_suffix: &str,
        allow_unsuffixed: bool,
    ) -> Vec<PathBuf> {
        let mut candidates = vec![
            PathBuf::from(format!("/dev/block/by-name/{name}{slot_suffix}")),
            PathBuf::from(format!("/dev/block/bootdevice/by-name/{name}{slot_suffix}")),
        ];
        candidates.extend(platform_by_name_candidates(
            name,
            slot_suffix,
            allow_unsuffixed,
        ));
        if allow_unsuffixed || slot_suffix.is_empty() {
            candidates.extend([
                PathBuf::from(format!("/dev/block/by-name/{name}")),
                PathBuf::from(format!("/dev/block/bootdevice/by-name/{name}")),
                PathBuf::from(format!("/dev/block/{name}")),
            ]);
        }
        candidates
    }

    fn platform_by_name_candidates(
        name: &str,
        slot_suffix: &str,
        allow_unsuffixed: bool,
    ) -> Vec<PathBuf> {
        let mut candidates = Vec::new();
        let platform = Path::new("/dev/block/platform");
        collect_platform_by_name_candidates(
            platform,
            name,
            slot_suffix,
            allow_unsuffixed,
            4,
            &mut candidates,
        );
        candidates
    }

    fn collect_platform_by_name_candidates(
        base: &Path,
        name: &str,
        slot_suffix: &str,
        allow_unsuffixed: bool,
        depth: u8,
        candidates: &mut Vec<PathBuf>,
    ) {
        push_by_name_children(base, name, slot_suffix, allow_unsuffixed, candidates);
        if depth == 0 {
            return;
        }

        let Ok(entries) = fs::read_dir(base) else {
            return;
        };
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_dir() {
                collect_platform_by_name_candidates(
                    &path,
                    name,
                    slot_suffix,
                    allow_unsuffixed,
                    depth - 1,
                    candidates,
                );
            }
        }
    }

    fn push_by_name_children(
        base: &Path,
        name: &str,
        slot_suffix: &str,
        allow_unsuffixed: bool,
        candidates: &mut Vec<PathBuf>,
    ) {
        let by_name = base.join("by-name");
        candidates.push(by_name.join(format!("{name}{slot_suffix}")));
        if allow_unsuffixed || slot_suffix.is_empty() {
            candidates.push(by_name.join(name));
        }
    }

    const fn bootctl_commands() -> [&'static str; 4] {
        [
            crate::assets::BOOTCTL_PATH,
            "/system/bin/bootctl",
            "/system_ext/bin/bootctl",
            "bootctl",
        ]
    }

    fn command_failure(program: &str, args: &[&str], output: &std::process::Output) -> String {
        let stderr = String::from_utf8_lossy(&output.stderr);
        let detail = stderr.trim();
        if detail.is_empty() {
            format!("{program} {} exited with {}", args.join(" "), output.status)
        } else {
            format!(
                "{program} {} exited with {}: {detail}",
                args.join(" "),
                output.status
            )
        }
    }

    fn set_active_slot_with_bootctl(target_slot: &str) -> Result<String> {
        let args = ["set-active-boot-slot", target_slot];
        let mut errors = Vec::new();

        // BootControl can be briefly unavailable while an OTA service is finishing.
        for attempt in 1..=3 {
            for program in bootctl_commands() {
                match Command::new(program).args(args).output() {
                    Ok(output) if output.status.success() => return Ok(program.to_string()),
                    Ok(output) => errors.push(command_failure(program, &args, &output)),
                    Err(err) => errors.push(format!("{program} failed: {err}")),
                }
            }
            if attempt < 3 {
                std::thread::sleep(std::time::Duration::from_millis(250 * attempt));
            }
        }

        bail!("{}", errors.join("; "))
    }

    fn set_active_slot_with_update_engine() -> Result<String> {
        const PROGRAMS: [&str; 2] = ["/system/bin/update_engine_client", "update_engine_client"];
        const ACTIONS: [&[&str]; 2] = [&["--switch_slot=true"], &["--oplus_switch_slot"]];
        let mut errors = Vec::new();

        for program in PROGRAMS {
            for args in ACTIONS {
                match Command::new(program).args(args).output() {
                    Ok(output) if output.status.success() => {
                        return Ok(format!("{program} {}", args.join(" ")));
                    }
                    Ok(output) => errors.push(command_failure(program, args, &output)),
                    Err(err) => errors.push(format!("{program} failed: {err}")),
                }
            }
        }

        bail!("{}", errors.join("; "))
    }

    pub(super) fn post_ota() -> Result<()> {
        use crate::assets::BOOTCTL_PATH;
        use crate::defs::ADB_DIR;

        let target_suffix = get_slot_suffix(true)?;
        let target_slot = match target_suffix.as_str() {
            "_a" => "0",
            "_b" => "1",
            value => bail!("invalid target boot slot: {value}"),
        };

        println!("- Activating target boot slot {target_suffix}");
        match set_active_slot_with_bootctl(target_slot) {
            Ok(program) => println!("- Activated {target_suffix} via {program}"),
            Err(bootctl_err) => {
                println!("- bootctl unavailable, trying OTA slot switch service");
                match set_active_slot_with_update_engine() {
                    Ok(program) => println!("- Requested {target_suffix} via {program}"),
                    Err(update_engine_err) => {
                        // The OTA updater normally marks its target active before this command.
                        // Keep the successful inactive-slot flash instead of reporting it as lost.
                        println!("- Warning: unable to force {target_suffix} active");
                        println!("- bootctl: {bootctl_err:#}");
                        println!("- update_engine: {update_engine_err:#}");
                        println!(
                            "- Patched image is installed in {target_suffix}; verify the OTA selected this slot before reboot"
                        );
                    }
                }
            }
        }

        let post_fs_data = Path::new(ADB_DIR).join("post-fs-data.d");
        utils::ensure_dir_exists(&post_fs_data)?;
        let post_ota_sh = post_fs_data.join("post_ota.sh");

        let sh_content = format!(
            r#"
for bootctl in {BOOTCTL_PATH} /system/bin/bootctl /system_ext/bin/bootctl bootctl; do
  "$bootctl" mark-boot-successful >/dev/null 2>&1 && break
done
rm -f /data/adb/post-fs-data.d/post_ota.sh
"#
        );

        std::fs::write(&post_ota_sh, sh_content)?;
        std::fs::set_permissions(post_ota_sh, std::fs::Permissions::from_mode(0o755))?;

        Ok(())
    }
}

#[cfg(target_os = "android")]
pub use android::*;

fn map_file(file: &Path) -> Result<Mmap> {
    let mut f = File::open(file).with_context(|| format!("open {}", file.display()))?;
    let len = f
        .seek(SeekFrom::End(0))
        .with_context(|| format!("seek end of {}", file.display()))? as usize;
    let mmap = unsafe { MmapOptions::new().len(len).map(&f)? };
    Ok(mmap)
}

fn parse_kmi(buffer: &[u8]) -> Result<String> {
    let re = Regex::new(r"(\d+\.\d+)(?:\S+)?(android\d+)").context("Failed to compile regex")?;
    buffer
        .windows(4)
        .enumerate()
        .filter(|(_, x)| {
            x[1] == b'.'
                && x[2].is_ascii_digit()
                && match x[0] {
                    b'5' => x[3].is_ascii_digit(),
                    b'6'..=b'9' => true,
                    _ => false,
                }
        })
        .find_map(|(i, _)| {
            let a = &buffer[i..buffer.len().min(i + 100)];
            if let Some(e) = a.iter().position(|c| *c == 0)
                && let Ok(s) = std::str::from_utf8(&a[..e])
                && let Some(caps) = re.captures(s)
                && let (Some(kernel_version), Some(android_version)) = (caps.get(1), caps.get(2))
            {
                Some(format!(
                    "{}-{}",
                    android_version.as_str(),
                    kernel_version.as_str()
                ))
            } else {
                None
            }
        })
        .ok_or_else(|| {
            println!("- Failed to get KMI version");
            anyhow!("Try to choose LKM manually")
        })
}

fn parse_kmi_from_kernel(kernel: &Path) -> Result<String> {
    let data = std::fs::read(kernel).context("Failed to read kernel file")?;
    parse_kmi(&data)
}

fn parse_kmi_from_boot(image: &Path) -> Result<String> {
    let data = map_file(image)?;
    let boot = BootImage::parse(&data)?;
    if let Some(kernel) = boot.get_blocks().get_kernel() {
        let mut output = Vec::<u8>::new();
        kernel.dump(&mut output, false)?;
        parse_kmi(&output)
    } else {
        bail!("no kernel found in boot image")
    }
}

/// For vendor boot, prefer the `init_boot` ramdisk entry over the one with empty name,
/// matching the original magiskboot lookup order (init_boot.cpio before ramdisk.cpio).
fn extract_ramdisk(ramdisk_image: &RamdiskImage) -> Result<(Cpio, Option<usize>)> {
    if ramdisk_image.is_vendor_ramdisk() {
        let (pos, target) = ramdisk_image
            .iter_vendor_ramdisk()
            .enumerate()
            .find(|e| e.1.get_name_raw() == b"init_boot")
            .or_else(|| {
                ramdisk_image
                    .iter_vendor_ramdisk()
                    .enumerate()
                    .find(|e| e.1.get_name_raw() == b"")
            })
            .ok_or_else(|| anyhow!("No suitable vendor ramdisk entry found"))?;
        let mut buf = Vec::<u8>::new();
        target.dump(&mut buf, false)?;
        Ok((Cpio::load_from_data(&buf)?, Some(pos)))
    } else {
        let mut buf = Vec::<u8>::new();
        ramdisk_image.dump(&mut buf, false)?;
        Ok((Cpio::load_from_data(&buf)?, None))
    }
}

fn enforce_bootimage_version(boot: &BootImage<'_>) -> Result<()> {
    if let BootImageVersion::Android(ver) = boot.get_header().get_version()
        && ver < 3
    {
        bail!("bootimage version {ver} is not supported!")
    }
    Ok(())
}

fn enforce_apkesu_lkm_identity(kernelsu_ko: &[u8], source: &str) -> Result<()> {
    ensure!(
        kernelsu_ko
            .windows(MANAGER_CERT_HASH.len())
            .any(|window| window == MANAGER_CERT_HASH),
        "{source} does not contain the ApkeSU manager certificate hash; rebuild or patch the LKM assets before patching a boot image"
    );
    Ok(())
}

#[allow(clippy::struct_excessive_bools)]
#[derive(clap::Args, Debug)]
pub struct BootPatchArgs {
    /// boot image path, if not specified, will try to find the boot image automatically
    #[arg(short, long)]
    pub boot: Option<PathBuf>,

    /// kernel image path to replace
    #[arg(short, long)]
    pub kernel: Option<PathBuf>,

    /// LKM module path to replace, if not specified, will use the builtin one
    #[arg(short, long)]
    pub module: Option<PathBuf>,

    /// init to be replaced
    #[arg(short, long)]
    pub init: Option<PathBuf>,

    /// will use another slot when boot image is not specified
    #[cfg(target_os = "android")]
    #[arg(short = 'u', long, default_value = "false")]
    pub ota: bool,

    /// Flash it to boot partition after patch
    #[cfg(target_os = "android")]
    #[arg(short, long, default_value = "false")]
    pub flash: bool,

    /// Output path. If not specified, will use current directory.
    /// If specified, the boot image will be written to the directory
    /// even if --flash is specified.
    #[cfg(target_os = "android")]
    #[arg(short, long, default_value = None)]
    pub out: Option<PathBuf>,

    /// Output path. If not specified, will use current directory.
    #[cfg(not(target_os = "android"))]
    #[arg(short, long, default_value = None)]
    pub out: Option<PathBuf>,

    /// KMI version, if specified, will use the specified KMI
    #[arg(long, default_value = None)]
    pub kmi: Option<String>,

    /// target partition override (init_boot | boot | vendor_boot)
    #[cfg(target_os = "android")]
    #[arg(long, default_value = None)]
    pub partition: Option<String>,

    /// File name of the output. If specified, the boot image will be
    /// written to the output directory even if --flash is specified.
    #[cfg(target_os = "android")]
    #[arg(long, default_value = None)]
    pub out_name: Option<String>,

    /// File name of the output.
    #[cfg(not(target_os = "android"))]
    #[arg(long, default_value = None)]
    pub out_name: Option<String>,

    /// Extra cmdline to append to boot image header
    #[arg(long, default_value = None)]
    pub cmdline: Option<String>,

    /// Always allow shell to get root permission
    #[arg(long, default_value = "false")]
    allow_shell: bool,

    /// Force enable adbd and disable adbd auth
    #[arg(long, default_value = "false")]
    enable_adbd: bool,

    /// Add more adb_debug prop
    #[arg(long, required = false)]
    adb_debug_prop: Option<String>,

    /// Do not (re-)install kernelsu, only modify configs (allow_shell, etc.)
    #[arg(long, default_value = "false")]
    no_install: bool,

    /// Do not load custom rc
    #[arg(long, default_value = "false")]
    no_custom_rc: bool,

    /// Add built-in pathmask LKM for hidden path patching
    #[arg(long, default_value = "false")]
    pathmask_lkm: bool,
}

pub fn patch(args: BootPatchArgs) -> Result<()> {
    let inner = move || {
        let BootPatchArgs {
            boot: image,
            init,
            kernel,
            module: kmod,
            out,
            kmi,
            out_name,
            cmdline,
            allow_shell,
            enable_adbd,
            adb_debug_prop,
            no_install,
            pathmask_lkm,
            #[cfg(target_os = "android")]
            ota,
            #[cfg(target_os = "android")]
            flash,
            #[cfg(target_os = "android")]
            partition,
            no_custom_rc,
        } = args;

        println!(include_str!("banner"));

        #[cfg(target_os = "android")]
        let patch_file = image.is_some();

        #[cfg(target_os = "android")]
        if !patch_file {
            ensure_gki_kernel()?;
        }

        let is_replace_kernel = kernel.is_some();

        if is_replace_kernel {
            ensure!(
                init.is_none() && kmod.is_none(),
                "init and module must not be specified."
            );
        }
        ensure!(
            !(pathmask_lkm && no_install),
            "pathmask LKM patch cannot be used with no-install"
        );

        let kmi = kmi.map_or_else(
            || -> Result<_> {
                if kmod.is_some() {
                    return Ok(String::new());
                }

                if let Some(image_path) = &image {
                    println!(
                        "- Trying to auto detect KMI version for {}",
                        image_path.display()
                    );
                    match parse_kmi_from_boot(image_path) {
                        Ok(value) => {
                            return Ok(value);
                        }
                        Err(e) => {
                            println!("- Failed to auto detect KMI from selected image: {e}");
                        }
                    }
                }

                #[cfg(target_os = "android")]
                match get_current_kmi() {
                    Ok(value) => {
                        return Ok(value);
                    }
                    Err(e) => {
                        println!("- {e}");
                    }
                }

                Ok(if let Some(kernel_path) = &kernel {
                    println!(
                        "- Trying to auto detect KMI version for {}",
                        kernel_path.display()
                    );
                    parse_kmi_from_kernel(kernel_path)?
                } else {
                    String::new()
                })
            },
            Ok,
        )?;

        let boot_image_file = if let Some(image) = image {
            ensure!(image.exists(), "boot image not found");
            std::fs::canonicalize(image)?
        } else {
            #[cfg(target_os = "android")]
            {
                auto_boot_partition_path(&kmi, ota, is_replace_kernel, &partition)?
            }
            #[cfg(not(target_os = "android"))]
            {
                bail!("Please specify a boot image");
            }
        };

        #[cfg(target_os = "android")]
        println!("- Bootdevice: {}", boot_image_file.display());

        // try extract bootctl/busybox
        #[cfg(target_os = "android")]
        let _ = assets::ensure_binaries(false);

        println!("- Preparing assets");
        println!("- Unpacking boot image");
        let boot_image_data = map_file(&boot_image_file)?;
        let boot_image = BootImage::parse(&boot_image_data)?;
        enforce_bootimage_version(&boot_image)?;

        let mut patcher = BootImagePatchOption::new(&boot_image);

        if let Some(cmdline_value) = &cmdline {
            patcher.override_cmdline(cmdline_value.as_bytes());
            println!("- Set cmdline to: {cmdline_value}");
        }

        if let Some(kernel_path) = kernel {
            println!("- Adding Kernel");
            let kernel_data = map_file(&kernel_path)?;
            patcher.replace_kernel(Box::new(Cursor::new(kernel_data)), false);
        }

        let kernelsu_ko: Box<dyn AsRef<[u8]>> = if no_install {
            Box::new(Vec::<u8>::new())
        } else if let Some(kmod_path) = kmod {
            let module_data = map_file(&kmod_path)?;
            enforce_apkesu_lkm_identity(module_data.as_ref(), &kmod_path.display().to_string())?;
            Box::new(module_data)
        } else {
            println!("- KMI: {kmi}");
            let name = format!("{kmi}_kernelsu.ko");
            let module_data =
                assets::get_asset_data(&name).with_context(|| format!("Failed to load {name}"))?;
            enforce_apkesu_lkm_identity(module_data.as_ref(), &name)?;
            Box::new(module_data)
        };

        let ksu_init: Box<dyn AsRef<[u8]>> = if no_install {
            Box::new(Vec::<u8>::new())
        } else if let Some(init_path) = init {
            Box::new(map_file(&init_path)?)
        } else {
            assets::get_asset("ksuinit").context("Failed to load ksuinit")?
        };

        let (mut cpio, vendor_ramdisk_idx) =
            if let Some(ramdisk_image) = boot_image.get_blocks().get_ramdisk() {
                extract_ramdisk(ramdisk_image)?
            } else {
                println!("- No ramdisk, create by default");
                (Cpio::new(), None)
            };

        if !no_install {
            ensure!(
                !cpio.is_magisk_patched(),
                "Cannot work with Magisk patched image"
            );

            println!("- Adding ApkeSU LKM");
            let is_kernelsu_patched = cpio.exists("kernelsu.ko");

            if !is_kernelsu_patched && cpio.exists("init") {
                cpio.mv("init", "init.real")?;
            }

            cpio.add("init", CpioEntry::regular(0o755, ksu_init))?;
            cpio.add("kernelsu.ko", CpioEntry::regular(0o755, kernelsu_ko))?;
            apply_pathmask_lkm(&mut cpio, pathmask_lkm, &kmi)?;

            #[cfg(target_os = "android")]
            if !is_kernelsu_patched
                && flash
                && let Err(e) = do_backup(&mut cpio, &boot_image_file)
            {
                println!("- Backup stock image failed: {e:?}");
            }
        }

        let mut ksu_config: Vec<String> = cpio
            .entry_by_name("ksu_config")
            .and_then(CpioEntry::data)
            .and_then(|v| str::from_utf8(v).ok())
            .map(|v| v.split(' ').map(std::borrow::ToOwned::to_owned).collect())
            .unwrap_or_default();

        let mut apply_config = |name: &str, value: &str, add: bool| {
            let has_value = ksu_config.iter().any(|v| v == value);

            if add {
                println!("- Adding {name} config");
                if !has_value {
                    ksu_config.push(value.to_owned());
                }
            } else if has_value {
                println!("- Removing {name} config");
                ksu_config.retain(|v| v != value);
            }
        };

        apply_config("no custom rc", "norc=1", no_custom_rc);
        apply_config("allow shell", "allow_shell=1", allow_shell);

        if ksu_config.is_empty() {
            cpio.rm("ksu_config", false);
        } else {
            let data = ksu_config.join(" ").into_bytes();
            cpio.add("ksu_config", CpioEntry::regular(0o644, Box::new(data)))?;
        }

        // remove legacy config file
        cpio.rm("allow_shell", false);

        if enable_adbd || adb_debug_prop.is_some() {
            println!("- Adding adb_debug props");
            cpio.add(
                "force_debuggable",
                CpioEntry::regular(0o644, Box::new(Vec::<u8>::new())),
            )?;

            let mut prop = Vec::<u8>::new();
            if enable_adbd {
                println!("- Adding props to enable adbd");
                prop.extend_from_slice(
                    b"ro.debuggable=1\nro.force.debuggable=1\nro.adb.secure=0\n",
                );
            }
            if let Some(extra) = adb_debug_prop {
                println!("- Adding custom props");
                prop.extend_from_slice(extra.as_bytes());
            }
            cpio.add("adb_debug.prop", CpioEntry::regular(0o644, Box::new(prop)))?;
        } else {
            if cpio.exists("force_debuggable") {
                println!("- Removing /force_debuggable");
                cpio.rm("force_debuggable", false);
            }
            if cpio.exists("adb_debug.prop") {
                println!("- Removing /adb_debug.prop");
                cpio.rm("adb_debug.prop", false);
            }
        }

        let mut new_cpio = Vec::<u8>::new();
        cpio.dump(&mut new_cpio)?;

        if let Some(idx) = vendor_ramdisk_idx {
            patcher.replace_vendor_ramdisk(idx, Box::new(Cursor::new(new_cpio)), false);
        } else {
            patcher.replace_ramdisk(Box::new(Cursor::new(new_cpio)), false);
        }

        println!("- Repacking boot image");
        let mut new_boot_buf = Cursor::new(Vec::<u8>::with_capacity(boot_image.get_size()));
        patcher.patch(&mut new_boot_buf)?;
        let new_boot_bytes = new_boot_buf.into_inner();

        // Free the source mmap so the boot partition is no longer mapped read-only,
        // otherwise some kernels reject the subsequent write.
        drop(boot_image);
        drop(boot_image_data);

        #[cfg(target_os = "android")]
        if flash {
            println!("- Flashing new boot image");
            let bootdevice = boot_image_file.display().to_string();
            flash_partition(&bootdevice, &new_boot_bytes)?;
            crate::rescue::mark_next_boot_pending("boot image flashed");
            if ota {
                post_ota()?;
            }
        }

        #[cfg(target_os = "android")]
        let should_write_output = patch_file || !flash || out_name.is_some() || out.is_some();
        #[cfg(not(target_os = "android"))]
        let should_write_output = true;

        if should_write_output {
            let output_dir = out.unwrap_or(std::env::current_dir()?);
            let name = out_name.unwrap_or_else(|| {
                let now = chrono::Utc::now();
                format!("apkesu_patched_{}.img", now.format("%Y%m%d_%H%M%S"))
            });
            let output_image = output_dir.join(name);
            std::fs::write(&output_image, &new_boot_bytes).context("write out new boot failed")?;
            println!("- Output file is written to");
            println!("- {}", output_image.display().to_string().trim_matches('"'));
        }

        println!("- Done!");
        Ok(())
    };

    let result = inner();
    if let Err(ref e) = result {
        println!("- Patch Error: {e}");
    }
    result
}

#[derive(clap::Args, Debug)]
pub struct BootRestoreArgs {
    /// boot image path, if not specified, will try to find the boot image automatically
    #[arg(short, long)]
    pub boot: Option<PathBuf>,

    /// Flash it to boot partition after restore
    #[cfg(target_os = "android")]
    #[arg(short, long, default_value = "false")]
    pub flash: bool,

    /// Output path. If not specified, will use current directory.
    /// If specified, the boot image will be written to the directory
    /// even if --flash is specified.
    #[cfg(target_os = "android")]
    #[arg(short, long, default_value = None)]
    pub out: Option<PathBuf>,

    /// Output path. If not specified, will use current directory.
    #[cfg(not(target_os = "android"))]
    #[arg(short, long, default_value = None)]
    pub out: Option<PathBuf>,

    /// File name of the output. If specified, the boot image will be
    /// written to the output directory even if --flash is specified.
    #[cfg(target_os = "android")]
    #[arg(long, default_value = None)]
    pub out_name: Option<String>,

    /// File name of the output.
    #[cfg(not(target_os = "android"))]
    #[arg(long, default_value = None)]
    pub out_name: Option<String>,
}

pub fn restore(args: BootRestoreArgs) -> Result<()> {
    let BootRestoreArgs {
        boot: image,
        out_name,
        out,
        #[cfg(target_os = "android")]
        flash,
    } = args;

    #[cfg(target_os = "android")]
    let kmi = get_current_kmi().unwrap_or_default();

    #[cfg(target_os = "android")]
    let image_supplied = image.is_some();

    let boot_image_file = if let Some(image) = image {
        ensure!(image.exists(), "boot image not found");
        std::fs::canonicalize(image)?
    } else {
        #[cfg(target_os = "android")]
        {
            auto_boot_partition_path(&kmi, false, false, &None)?
        }
        #[cfg(not(target_os = "android"))]
        {
            bail!("Please specify a boot image");
        }
    };

    #[cfg(target_os = "android")]
    println!("- Bootdevice: {}", boot_image_file.display());

    println!("- Unpacking boot image");
    let bootimage_data = map_file(&boot_image_file)?;
    let boot_image = BootImage::parse(&bootimage_data)?;
    enforce_bootimage_version(&boot_image)?;

    let (mut cpio, vendor_ramdisk_idx) =
        if let Some(ramdisk_image) = boot_image.get_blocks().get_ramdisk() {
            extract_ramdisk(ramdisk_image)?
        } else {
            bail!("No compatible ramdisk found.")
        };

    ensure!(
        cpio.exists("kernelsu.ko"),
        "boot image is not patched by ApkeSU"
    );

    #[cfg(target_os = "android")]
    let mut stock_boot: Option<PathBuf> = None;

    #[cfg(target_os = "android")]
    if let Some(backup_file) = cpio.entry_by_name(BACKUP_FILENAME) {
        let sha = String::from_utf8(backup_file.data().unwrap_or_default().to_vec())?;
        let sha = sha.trim();
        let backup_path =
            PathBuf::from(KSU_BACKUP_DIR).join(format!("{KSU_BACKUP_FILE_PREFIX}{sha}"));
        if backup_path.is_file() {
            println!("- Using backup file {}", backup_path.display());
            stock_boot = Some(backup_path);
        } else {
            println!("- Warning: no backup {} found!", backup_path.display());
        }
        if let Err(e) = clean_backup(sha) {
            println!("- Warning: Cleanup backup image failed: {e}");
        }
    } else {
        println!("- Backup info is absent!");
    }

    #[cfg(target_os = "android")]
    let mut stock_source: Option<PathBuf> = None;

    let new_boot_bytes: Vec<u8> = {
        #[cfg(target_os = "android")]
        {
            if let Some(stock_path) = stock_boot {
                let bytes = std::fs::read(&stock_path)
                    .with_context(|| format!("read stock boot {}", stock_path.display()))?;
                stock_source = Some(stock_path);
                bytes
            } else {
                rebuild_without_ksu(&boot_image, &mut cpio, vendor_ramdisk_idx)?
            }
        }
        #[cfg(not(target_os = "android"))]
        {
            rebuild_without_ksu(&boot_image, &mut cpio, vendor_ramdisk_idx)?
        }
    };

    drop(boot_image);
    drop(bootimage_data);

    #[cfg(target_os = "android")]
    if flash {
        if let Some(ref source) = stock_source {
            println!("- Flashing new boot image from {}", source.display());
        } else {
            println!("- Flashing new boot image");
        }
        let bootdevice = boot_image_file.display().to_string();
        flash_partition(&bootdevice, &new_boot_bytes)?;
        crate::rescue::mark_next_boot_pending("boot image restored");
    }

    #[cfg(target_os = "android")]
    let should_write_output = image_supplied || !flash || out_name.is_some() || out.is_some();
    #[cfg(not(target_os = "android"))]
    let should_write_output = true;

    if should_write_output {
        let output_dir = out.unwrap_or(std::env::current_dir()?);
        let name = out_name.unwrap_or_else(|| {
            let now = chrono::Utc::now();
            format!("apkesu_restore_{}.img", now.format("%Y%m%d_%H%M%S"))
        });
        let output_image = output_dir.join(name);
        std::fs::write(&output_image, &new_boot_bytes).context("copy out new boot failed")?;
        println!("- Output file is written to");
        println!("- {}", output_image.display().to_string().trim_matches('"'));
    }

    println!("- Done!");
    Ok(())
}

fn rebuild_without_ksu(
    boot_image: &BootImage<'_>,
    cpio: &mut Cpio,
    vendor_ramdisk_idx: Option<usize>,
) -> Result<Vec<u8>> {
    println!("- Removing ApkeSU from boot image");
    cpio.rm("kernelsu.ko", false);
    cpio.rm("pathmask.ko", false);
    if cpio.exists("init.real") {
        cpio.mv("init.real", "init")?;
    }

    let mut new_cpio = Vec::<u8>::new();
    cpio.dump(&mut new_cpio)?;

    println!("- Repacking boot image");
    let mut patcher = BootImagePatchOption::new(boot_image);
    if let Some(idx) = vendor_ramdisk_idx {
        patcher.replace_vendor_ramdisk(idx, Box::new(Cursor::new(new_cpio)), false);
    } else {
        patcher.replace_ramdisk(Box::new(Cursor::new(new_cpio)), false);
    }

    let mut buf = Cursor::new(Vec::<u8>::with_capacity(boot_image.get_size()));
    patcher.patch(&mut buf)?;
    Ok(buf.into_inner())
}

fn apply_pathmask_lkm(cpio: &mut Cpio, enabled: bool, kmi: &str) -> Result<()> {
    if !enabled {
        cpio.rm("pathmask.ko", false);
        return Ok(());
    }

    ensure!(!kmi.is_empty(), "KMI is required for pathmask LKM patch");
    let name = format!("{kmi}_pathmask.ko");
    let pathmask = assets::get_asset(&name)
        .with_context(|| format!("Failed to load built-in pathmask LKM for {kmi}"))?;
    println!("- Adding hidden path LKM: {name}");
    cpio.add("pathmask.ko", CpioEntry::regular(0o755, pathmask))?;
    Ok(())
}
