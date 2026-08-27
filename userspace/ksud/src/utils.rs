use anyhow::{Context, Error, Ok, Result, bail};
use rustix::fs::{Mode, OFlags, open};
use rustix::process::setpgid;
use rustix::stdio::{dup2_stderr, dup2_stdin, dup2_stdout};
use std::{
    collections::BTreeSet,
    ffi::{CStr, CString, c_char, c_void},
    fs::{File, OpenOptions, create_dir_all, remove_file, symlink_metadata, write},
    io::{
        ErrorKind::{AlreadyExists, NotFound},
        Write,
    },
    path::Path,
    process::Command,
};

use crate::defs::KSU_TEMP_BACKUP_DIR_NAME;
use crate::{assets, boot_patch, defs, ksucalls, module, restorecon};
#[allow(unused_imports)]
use std::fs::{Permissions, set_permissions};
#[cfg(unix)]
use std::os::unix::prelude::PermissionsExt;

use std::path::PathBuf;

use crate::boot_patch::BootRestoreArgs;

use rustix::{
    process,
    thread::{LinkNameSpaceType, move_into_link_name_space},
};

const APKESU_GRAPHICS_RENDERER_DIR: &str = "/data/adb/apkesu/graphics_renderer";
const APKESU_FOREGROUND_TOOLS_DIR: &str = "/data/adb/apkesu/foreground_tools";
const APKESU_EXTERNAL_SERVICE_FILES: [&str; 7] = [
    "/data/adb/service.d/97-apkesu-foreground-tools.sh",
    "/data/adb/service.d/97-apkesu-foreground-tools.sh.pending",
    "/data/adb/service.d/97-apkesu-foreground-tools.sh.tmp",
    "/data/adb/service.d/98-apkesu-susfs-paths.sh",
    "/data/adb/service.d/98-apkesu-susfs-paths.sh.pending",
    "/data/adb/service.d/99-apkesu-graphics-renderer.sh",
    "/data/adb/service.d/99-apkesu-graphics-renderer.sh.tmp",
];

type PropertyReadCallback = unsafe extern "C" fn(*mut c_void, *const c_char, *const c_char, u32);

unsafe extern "C" {
    fn __system_property_find(name: *const c_char) -> *const c_void;
    fn __system_property_read_callback(
        property_info: *const c_void,
        callback: PropertyReadCallback,
        cookie: *mut c_void,
    );
}

#[macro_export]
macro_rules! debug_select {
    ($debug:expr, $release:expr) => {{
        #[cfg(debug_assertions)]
        {
            $debug
        }
        #[cfg(not(debug_assertions))]
        {
            $release
        }
    }};
}

pub fn ensure_clean_dir(dir: impl AsRef<Path>) -> Result<()> {
    let path = dir.as_ref();
    log::debug!("ensure_clean_dir: {}", path.display());
    if path.exists() {
        log::debug!("ensure_clean_dir: {} exists, remove it", path.display());
        std::fs::remove_dir_all(path)?;
    }
    Ok(std::fs::create_dir_all(path)?)
}

pub fn ensure_file_exists<T: AsRef<Path>>(file: T) -> Result<()> {
    match File::options().write(true).create_new(true).open(&file) {
        std::result::Result::Ok(_) => Ok(()),
        Err(err) => {
            if err.kind() == AlreadyExists && file.as_ref().is_file() {
                Ok(())
            } else {
                Err(Error::from(err))
                    .with_context(|| format!("{} is not a regular file", file.as_ref().display()))
            }
        }
    }
}

pub fn ensure_dir_exists<T: AsRef<Path>>(dir: T) -> Result<()> {
    let result = create_dir_all(&dir);
    if dir.as_ref().is_dir() && result.is_ok() {
        Ok(())
    } else {
        bail!("{} is not a regular directory", dir.as_ref().display())
    }
}

pub fn ensure_binary<T: AsRef<Path>>(
    path: T,
    contents: &[u8],
    ignore_if_exist: bool,
) -> Result<()> {
    if ignore_if_exist && path.as_ref().exists() {
        return Ok(());
    }

    ensure_dir_exists(path.as_ref().parent().ok_or_else(|| {
        anyhow::anyhow!(
            "{} does not have parent directory",
            path.as_ref().to_string_lossy()
        )
    })?)?;

    if let Err(e) = remove_file(path.as_ref())
        && e.kind() != NotFound
    {
        return Err(Error::from(e))
            .with_context(|| format!("failed to unlink {}", path.as_ref().display()));
    }

    write(&path, contents)?;
    #[cfg(unix)]
    set_permissions(&path, Permissions::from_mode(0o755))?;
    Ok(())
}

unsafe extern "C" fn property_read_callback(
    cookie: *mut c_void,
    _name: *const c_char,
    value: *const c_char,
    _serial: u32,
) {
    if cookie.is_null() || value.is_null() {
        return;
    }

    let result = unsafe { &mut *cookie.cast::<Option<String>>() };
    let value = unsafe { CStr::from_ptr(value) };
    *result = Some(value.to_string_lossy().into_owned());
}

pub fn getprop(name: &str) -> Option<String> {
    let name = CString::new(name).ok()?;
    let property_info = unsafe { __system_property_find(name.as_ptr()) };
    if property_info.is_null() {
        return None;
    }

    let mut value = None;
    unsafe {
        __system_property_read_callback(
            property_info,
            property_read_callback,
            std::ptr::addr_of_mut!(value).cast(),
        );
    }
    value
}

pub fn is_safe_mode() -> bool {
    let safemode = getprop("persist.sys.safemode")
        .as_ref()
        .is_some_and(|prop| prop == "1")
        || getprop("ro.sys.safemode")
            .as_ref()
            .is_some_and(|prop| prop == "1");
    log::info!("safemode: {safemode}");
    if safemode {
        return true;
    }
    let safemode = ksucalls::check_kernel_safemode();
    log::info!("kernel_safemode: {safemode}");
    safemode
}

pub fn get_zip_uncompressed_size(zip_path: &str) -> Result<u64> {
    let mut zip = zip::ZipArchive::new(std::fs::File::open(zip_path)?)?;
    let total: u64 = (0..zip.len())
        .map(|i| zip.by_index(i).unwrap().size())
        .sum();
    Ok(total)
}

pub fn switch_mnt_ns(pid: i32) -> Result<()> {
    use rustix::{
        fd::AsFd,
        fs::{Mode, OFlags, open},
    };
    let path = format!("/proc/{pid}/ns/mnt");
    let fd = open(path, OFlags::RDONLY, Mode::from_raw_mode(0))?;
    let current_dir = std::env::current_dir();
    move_into_link_name_space(fd.as_fd(), Some(LinkNameSpaceType::Mount))?;
    if let std::result::Result::Ok(current_dir) = current_dir {
        let _ = std::env::set_current_dir(current_dir);
    }
    Ok(())
}

fn switch_cgroup<T: AsRef<Path>>(grp: T, pid: u32) {
    let path = grp.as_ref().join("cgroup.procs");
    if !path.exists() {
        return;
    }

    let fp = OpenOptions::new().append(true).open(path);
    if let std::result::Result::Ok(mut fp) = fp {
        let _ = write!(fp, "{pid}");
    }
}

fn unescape_mount_path(path: &str) -> String {
    path.replace("\\040", " ")
        .replace("\\011", "\t")
        .replace("\\012", "\n")
        .replace("\\134", "\\")
}

fn cgroup_mount_points() -> BTreeSet<PathBuf> {
    let mut points = BTreeSet::from([
        PathBuf::from("/acct"),
        PathBuf::from("/dev/blkio"),
        PathBuf::from("/dev/cg2_bpf"),
        PathBuf::from("/dev/cpuctl"),
        PathBuf::from("/dev/freezer"),
        PathBuf::from("/dev/memcg"),
        PathBuf::from("/dev/memcg/apps"),
        PathBuf::from("/dev/stune"),
        PathBuf::from("/sys/fs/cgroup"),
    ]);

    if let std::result::Result::Ok(mountinfo) = std::fs::read_to_string("/proc/self/mountinfo") {
        for line in mountinfo.lines() {
            let Some((pre, post)) = line.split_once(" - ") else {
                continue;
            };
            let fstype = post.split_whitespace().next();
            if !matches!(fstype, Some("cgroup" | "cgroup2")) {
                continue;
            }
            if let Some(mount_point) = pre.split_whitespace().nth(4) {
                points.insert(PathBuf::from(unescape_mount_path(mount_point)));
            }
        }
    }

    points
}

pub fn switch_cgroups() {
    let pid = std::process::id();
    for point in cgroup_mount_points() {
        switch_cgroup(point, pid);
    }

    if getprop("ro.config.per_app_memcg")
        .as_ref()
        .is_none_or(|prop| prop != "false")
    {
        switch_cgroup("/dev/memcg/apps", pid);
    }
}

pub fn umask(mask: u32) {
    process::umask(rustix::fs::Mode::from_raw_mode(mask));
}

pub fn has_magisk() -> bool {
    which::which("magisk").is_ok()
}

pub fn remove_legacy_magisk_module_link() -> Result<bool> {
    let adb_dir = Path::new(defs::ADB_DIR);
    let module_dir = Path::new(defs::MODULE_DIR.trim_end_matches('/'));
    remove_legacy_magisk_module_link_at(adb_dir, module_dir)
}

fn remove_legacy_magisk_module_link_at(adb_dir: &Path, module_dir: &Path) -> Result<bool> {
    let magisk_dir = adb_dir.join(".magisk");
    let module_link = magisk_dir.join("modules");
    let metadata = match symlink_metadata(&module_link) {
        std::result::Result::Ok(metadata) => metadata,
        Err(error) if error.kind() == NotFound => return Ok(false),
        Err(error) => return Err(error.into()),
    };

    if !metadata.file_type().is_symlink() || std::fs::read_link(&module_link)? != module_dir {
        return Ok(false);
    }

    remove_file(&module_link)?;
    let _ = std::fs::remove_dir(&magisk_dir);
    Ok(true)
}

fn link_ksud_to_bin() -> Result<()> {
    let ksu_bin = PathBuf::from(defs::DAEMON_PATH);
    let ksu_bin_link = PathBuf::from(defs::DAEMON_LINK_PATH);
    if ksu_bin.exists() && !ksu_bin_link.exists() {
        std::os::unix::fs::symlink(&ksu_bin, &ksu_bin_link)?;
    }
    Ok(())
}

pub fn install(libadbroot: Option<PathBuf>, data_path: Option<PathBuf>) -> Result<()> {
    ensure_dir_exists(defs::ADB_DIR)?;
    if !has_magisk()
        && let Err(error) = remove_legacy_magisk_module_link()
    {
        log::warn!("failed to remove legacy Magisk module link: {error:#}");
    }
    let _ = std::fs::remove_file(defs::DAEMON_PATH);
    std::fs::copy("/proc/self/exe", defs::DAEMON_PATH)?;
    restorecon::lsetfilecon(defs::DAEMON_PATH, restorecon::KSU_CON)?;
    // install binary assets
    assets::ensure_binaries(false).with_context(|| "Failed to extract assets")?;

    link_ksud_to_bin()?;

    if let Some(libadbroot) = libadbroot {
        ensure_dir_exists(defs::LIBRARY_DIR)?;
        let _ = std::fs::remove_file(defs::LIBADBROOT_PATH);
        let _ = std::fs::copy(libadbroot, defs::LIBADBROOT_PATH);
    }

    if let Some(data_path) = data_path {
        let backup_path = data_path.join(KSU_TEMP_BACKUP_DIR_NAME);
        if backup_path.is_dir() {
            for entry in backup_path.read_dir()? {
                let entry = entry?;
                if entry.file_type().is_ok_and(|file_type| file_type.is_file()) {
                    let name = entry.file_name().to_string_lossy().to_string();
                    let target = format!("{}{name}", defs::KSU_BACKUP_DIR);
                    if name.starts_with(defs::KSU_BACKUP_FILE_PREFIX)
                        && std::fs::rename(entry.path(), &target).is_err()
                    {
                        std::fs::copy(entry.path(), &target).with_context(|| {
                            format!("failed to move {} -> {target}", entry.path().display())
                        })?;
                        log::info!("copied boot backup {name}");
                    }
                }
            }
            std::fs::remove_dir_all(&backup_path)?;
        }
    }

    Ok(())
}

pub fn uninstall(package_name: &str) -> Result<()> {
    println!("- Unload hidden path runtime..");
    if let Err(err) = crate::pathmask::unload() {
        // Restoring the boot image removes Pathmask on the next boot even when it is busy now.
        println!("- Warning: unable to unload Pathmask now: {err:#}");
    }

    println!("- Restore boot image..");
    boot_patch::restore(BootRestoreArgs {
        boot: None,
        flash: true,
        out: None,
        out_name: None,
    })?;

    if Path::new(defs::MODULE_DIR).exists() {
        println!("- Uninstall modules..");
        module::uninstall_all_modules()?;
        module::prune_modules()?;
    }

    println!("- Removing ApkeSU service extensions..");
    cleanup_apkesu_uninstall_artifacts()?;

    // The stock-image backup is stored in WORKING_DIR, so remove it only after restore succeeds.
    println!("- Removing directories..");
    std::fs::remove_dir_all(defs::WORKING_DIR).ok();
    std::fs::remove_file(defs::DAEMON_PATH).ok();
    std::fs::remove_dir_all(defs::MODULE_DIR).ok();
    std::fs::remove_dir_all(defs::PREINIT_DIR_WATCHDOG).ok();
    std::fs::remove_dir_all(defs::PREINIT_DIR_DEFAULT).ok();
    println!("- Uninstall KernelSU manager..");
    Command::new("pm")
        .args(["uninstall", package_name])
        .spawn()?;
    println!("- Rebooting in 5 seconds..");
    std::thread::sleep(std::time::Duration::from_secs(5));
    Command::new("reboot").spawn()?;
    Ok(())
}

fn cleanup_apkesu_uninstall_artifacts() -> Result<()> {
    let service_files = APKESU_EXTERNAL_SERVICE_FILES.map(Path::new);
    let state_dirs = [
        Path::new(APKESU_FOREGROUND_TOOLS_DIR),
        Path::new(APKESU_GRAPHICS_RENDERER_DIR),
    ];
    cleanup_owned_uninstall_paths(&state_dirs, &service_files)
}

fn cleanup_owned_uninstall_paths(state_dirs: &[&Path], service_files: &[&Path]) -> Result<()> {
    for path in service_files {
        remove_owned_uninstall_path(path)?;
    }
    for path in state_dirs {
        remove_owned_uninstall_path(path)?;
    }
    Ok(())
}

fn remove_owned_uninstall_path(path: &Path) -> Result<()> {
    let metadata = match symlink_metadata(path) {
        std::result::Result::Ok(metadata) => metadata,
        Err(error) if error.kind() == NotFound => return Ok(()),
        Err(error) => {
            return Err(Error::from(error)).with_context(|| {
                format!("failed to inspect uninstall artifact {}", path.display())
            });
        }
    };

    let result = if metadata.file_type().is_dir() {
        std::fs::remove_dir_all(path)
    } else {
        std::fs::remove_file(path)
    };
    result.with_context(|| format!("failed to remove uninstall artifact {}", path.display()))
}

#[cfg(test)]
mod tests {
    use super::{cleanup_owned_uninstall_paths, remove_legacy_magisk_module_link_at};
    use std::fs;
    #[cfg(unix)]
    use std::os::unix::fs::symlink as symlink_dir;
    #[cfg(windows)]
    use std::os::windows::fs::symlink_dir;

    #[cfg(any(unix, windows))]
    #[test]
    fn removes_only_the_legacy_magisk_modules_link() {
        let temp = tempfile::tempdir().unwrap();
        let adb_dir = temp.path().join("adb");
        let module_dir = adb_dir.join("modules");
        let magisk_dir = adb_dir.join(".magisk");
        let module_link = magisk_dir.join("modules");

        fs::create_dir_all(&module_dir).unwrap();
        fs::create_dir_all(&magisk_dir).unwrap();
        symlink_dir(&module_dir, &module_link).unwrap();

        assert!(remove_legacy_magisk_module_link_at(&adb_dir, &module_dir).unwrap());
        assert!(!module_link.exists());
        assert!(!magisk_dir.exists());
        assert!(module_dir.is_dir());
    }

    #[cfg(any(unix, windows))]
    #[test]
    fn preserves_a_non_apkesu_magisk_modules_link() {
        let temp = tempfile::tempdir().unwrap();
        let adb_dir = temp.path().join("adb");
        let module_dir = adb_dir.join("modules");
        let external_modules = temp.path().join("external-modules");
        let magisk_dir = adb_dir.join(".magisk");
        let module_link = magisk_dir.join("modules");

        fs::create_dir_all(&module_dir).unwrap();
        fs::create_dir_all(&external_modules).unwrap();
        fs::create_dir_all(&magisk_dir).unwrap();
        symlink_dir(&external_modules, &module_link).unwrap();

        assert!(!remove_legacy_magisk_module_link_at(&adb_dir, &module_dir).unwrap());
        assert!(module_link.exists());
    }

    #[test]
    fn uninstall_cleanup_removes_only_owned_artifacts() {
        let temp = tempfile::tempdir().unwrap();
        let state_root = temp.path().join("apkesu");
        let renderer = state_root.join("graphics_renderer");
        let foreground = state_root.join("foreground_tools");
        let unrelated = state_root.join("unrelated");
        let service_dir = temp.path().join("service.d");
        let graphics_service = service_dir.join("99-apkesu-graphics-renderer.sh");
        let susfs_service = service_dir.join("98-apkesu-susfs-paths.sh");
        let unrelated_service = service_dir.join("other-service.sh");

        fs::create_dir_all(&renderer).unwrap();
        fs::create_dir_all(&foreground).unwrap();
        fs::create_dir_all(&unrelated).unwrap();
        fs::create_dir_all(&service_dir).unwrap();
        fs::write(renderer.join("mode"), "vulkan").unwrap();
        fs::write(foreground.join("targets.list"), "com.example.target\n").unwrap();
        fs::write(unrelated.join("keep"), "keep").unwrap();
        fs::write(&graphics_service, "graphics").unwrap();
        fs::write(&susfs_service, "susfs").unwrap();
        fs::write(&unrelated_service, "keep").unwrap();

        cleanup_owned_uninstall_paths(
            &[renderer.as_path(), foreground.as_path()],
            &[graphics_service.as_path(), susfs_service.as_path()],
        )
        .unwrap();

        assert!(!renderer.exists());
        assert!(!foreground.exists());
        assert!(!graphics_service.exists());
        assert!(!susfs_service.exists());
        assert!(unrelated.join("keep").is_file());
        assert!(unrelated_service.is_file());
    }
}

pub fn reset_std() -> Result<()> {
    let null_fd = open("/dev/null", OFlags::RDWR, Mode::empty())?;
    dup2_stdin(&null_fd)?;
    dup2_stdout(&null_fd)?;
    dup2_stderr(&null_fd)?;
    Ok(())
}

pub fn daemonize_with<F: FnOnce() -> Result<()>>(use_init_pgrp: bool, configure: F) -> Result<()> {
    if !create_daemon_impl(use_init_pgrp, configure)? {
        unsafe { libc::_exit(0) }
    }
    Ok(())
}

pub fn daemonize(use_init_pgrp: bool) -> Result<()> {
    daemonize_with(use_init_pgrp, || Ok(()))
}

pub fn create_daemon(use_init_pgrp: bool) -> Result<bool> {
    create_daemon_with(use_init_pgrp, || Ok(()))
}

pub fn create_daemon_with<F: FnOnce() -> Result<()>>(
    use_init_pgrp: bool,
    configure: F,
) -> Result<bool> {
    create_daemon_impl(use_init_pgrp, configure)
}

fn create_daemon_impl<F: FnOnce() -> Result<()>>(
    use_init_pgrp: bool,
    configure: F,
) -> Result<bool> {
    unsafe {
        let pid = libc::fork();
        if pid < 0 {
            bail!("fork error {}", std::io::Error::last_os_error());
        } else if pid > 0 {
            let mut status: i32 = -1;
            loop {
                if libc::waitpid(pid, &raw mut status, 0) < 0 {
                    if *libc::__errno() != libc::EINTR {
                        libc::_exit(1);
                    }
                } else {
                    break;
                }
            }
            if !libc::WIFEXITED(status) || libc::WEXITSTATUS(status) != 0 {
                bail!("child exited with unexpected status {status}")
            }
            return Ok(false);
        }
    }

    let do_configure = || -> Result<()> {
        detach_process_group(use_init_pgrp);
        switch_cgroups();
        configure()?;
        reset_std()?;

        unsafe {
            let pid = libc::fork();
            if pid < 0 {
                bail!("fork error {}", std::io::Error::last_os_error());
            } else if pid > 0 {
                libc::_exit(0);
            }
        }
        Ok(())
    };

    if let Err(e) = do_configure() {
        log::error!("failed to configure daemon: {e:?}");
        unsafe {
            libc::_exit(1);
        }
    }

    Ok(true)
}

pub fn detach_process_group(use_init_pgrp: bool) {
    if use_init_pgrp {
        if let Err(e) = ksucalls::set_init_pgrp() {
            log::error!("failed to switch to init group: {e:?}");
        } else {
            return;
        }
    }
    if let Err(e2) = setpgid(None, None) {
        log::error!("failed to set process group: {e2:?}");
    }
}
