#![allow(clippy::unreadable_literal)]
use anyhow::bail;

use crate::ksu_uapi;
use std::fs;
use std::io;
use std::mem;
use std::os::fd::RawFd;
use std::ptr;
use std::sync::{Mutex, OnceLock};

// Global driver fd cache
static DRIVER_FD: Mutex<RawFd> = Mutex::new(-1);
static INFO_CACHE: OnceLock<ksu_uapi::ksu_get_info_cmd> = OnceLock::new();

fn scan_driver_fd() -> Option<RawFd> {
    let fd_dir = fs::read_dir("/proc/self/fd").ok()?;

    for entry in fd_dir.flatten() {
        if let Ok(fd_num) = entry.file_name().to_string_lossy().parse::<i32>() {
            let link_path = format!("/proc/self/fd/{fd_num}");
            if let Ok(target) = fs::read_link(&link_path) {
                let target_str = target.to_string_lossy();
                if target_str.contains("[ksu_driver]") {
                    return Some(fd_num);
                }
            }
        }
    }

    None
}

fn close_fd(fd: RawFd) {
    if fd >= 0 {
        unsafe {
            libc::close(fd);
        }
    }
}

fn wait_for_child(pid: libc::pid_t) {
    let mut status = 0;
    loop {
        let ret = unsafe { libc::waitpid(pid, &raw mut status, 0) };
        if ret >= 0 || io::Error::last_os_error().raw_os_error() != Some(libc::EINTR) {
            break;
        }
    }
}

fn set_recv_timeout(fd: RawFd) {
    let timeout = libc::timeval {
        tv_sec: 1,
        tv_usec: 500_000,
    };
    unsafe {
        libc::setsockopt(
            fd,
            libc::SOL_SOCKET,
            libc::SO_RCVTIMEO,
            (&raw const timeout).cast(),
            mem::size_of_val(&timeout) as libc::socklen_t,
        );
    }
}

const fn cmsg_align(len: usize) -> usize {
    let align = mem::size_of::<usize>();
    (len + align - 1) & !(align - 1)
}

const fn cmsg_space(payload_len: usize) -> usize {
    cmsg_align(mem::size_of::<libc::cmsghdr>()) + cmsg_align(payload_len)
}

const fn cmsg_len(payload_len: usize) -> usize {
    cmsg_align(mem::size_of::<libc::cmsghdr>()) + payload_len
}

fn send_fd(socket_fd: RawFd, fd: RawFd) -> io::Result<()> {
    let payload = [0u8; 1];
    let mut iov = libc::iovec {
        iov_base: payload.as_ptr().cast_mut().cast(),
        iov_len: payload.len(),
    };
    let control_len = cmsg_space(mem::size_of::<RawFd>());
    let mut control = [0usize; 8];
    let mut msg = unsafe { mem::zeroed::<libc::msghdr>() };
    msg.msg_iov = &raw mut iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control.as_mut_ptr().cast();
    msg.msg_controllen = control_len;

    let cmsg = msg.msg_control.cast::<libc::cmsghdr>();
    unsafe {
        (*cmsg).cmsg_level = libc::SOL_SOCKET;
        (*cmsg).cmsg_type = libc::SCM_RIGHTS;
        (*cmsg).cmsg_len = cmsg_len(mem::size_of::<RawFd>()) as _;
        let data = cmsg
            .cast::<u8>()
            .add(cmsg_align(mem::size_of::<libc::cmsghdr>()));
        let fd_bytes = fd.to_ne_bytes();
        ptr::copy_nonoverlapping(fd_bytes.as_ptr(), data, fd_bytes.len());

        if libc::sendmsg(socket_fd, &raw const msg, 0) < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(())
        }
    }
}

fn recv_fd(socket_fd: RawFd) -> io::Result<RawFd> {
    let mut payload = [0u8; 1];
    let mut iov = libc::iovec {
        iov_base: payload.as_mut_ptr().cast(),
        iov_len: payload.len(),
    };
    let control_len = cmsg_space(mem::size_of::<RawFd>());
    let mut control = [0usize; 8];
    let mut msg = unsafe { mem::zeroed::<libc::msghdr>() };
    msg.msg_iov = &raw mut iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control.as_mut_ptr().cast();
    msg.msg_controllen = control_len;

    let n = unsafe { libc::recvmsg(socket_fd, &raw mut msg, 0) };
    if n < 0 {
        return Err(io::Error::last_os_error());
    }
    if n == 0 {
        return Err(io::Error::new(
            io::ErrorKind::UnexpectedEof,
            "driver fd was not sent",
        ));
    }

    let cmsg = msg.msg_control.cast::<libc::cmsghdr>();
    unsafe {
        if msg.msg_controllen < cmsg_len(mem::size_of::<RawFd>())
            || (*cmsg).cmsg_level != libc::SOL_SOCKET
            || (*cmsg).cmsg_type != libc::SCM_RIGHTS
        {
            return Err(io::Error::other("driver fd control message is missing"));
        }

        let data = cmsg
            .cast::<u8>()
            .add(cmsg_align(mem::size_of::<libc::cmsghdr>()));
        let mut fd_bytes = [0u8; mem::size_of::<RawFd>()];
        ptr::copy_nonoverlapping(data, fd_bytes.as_mut_ptr(), fd_bytes.len());
        let fd = RawFd::from_ne_bytes(fd_bytes);
        if fd >= 0 {
            Ok(fd)
        } else {
            Err(io::Error::other("invalid driver fd"))
        }
    }
}

fn install_driver_fd_via_reboot() -> Option<RawFd> {
    let mut sockets = [-1; 2];
    if unsafe { libc::socketpair(libc::AF_UNIX, libc::SOCK_STREAM, 0, sockets.as_mut_ptr()) } < 0 {
        return None;
    }
    set_recv_timeout(sockets[0]);

    let pid = unsafe { libc::fork() };
    if pid < 0 {
        close_fd(sockets[0]);
        close_fd(sockets[1]);
        return None;
    }

    if pid == 0 {
        close_fd(sockets[0]);
        let mut fd = -1;
        unsafe {
            libc::syscall(
                libc::SYS_reboot,
                ksu_uapi::KSU_INSTALL_MAGIC1,
                ksu_uapi::KSU_INSTALL_MAGIC2,
                0,
                &raw mut fd,
            );
        };
        let sent = fd >= 0 && send_fd(sockets[1], fd).is_ok();
        close_fd(fd);
        close_fd(sockets[1]);
        unsafe {
            libc::_exit(i32::from(!sent));
        }
    }

    close_fd(sockets[1]);
    let fd = recv_fd(sockets[0]).ok();
    close_fd(sockets[0]);
    wait_for_child(pid);
    fd
}

// Get cached driver fd
fn init_driver_fd() -> Option<RawFd> {
    if let Some(fd) = scan_driver_fd() {
        return Some(fd);
    }

    install_driver_fd_via_reboot()
}

// ioctl wrapper using libc
fn ksuctl<T>(request: u32, arg: *mut T) -> std::io::Result<i32> {
    use std::io;

    let mut fd = DRIVER_FD
        .lock()
        .unwrap_or_else(std::sync::PoisonError::into_inner);
    if *fd < 0 {
        *fd = init_driver_fd().unwrap_or(-1);
    }

    let mut ret = unsafe { libc::ioctl(*fd as libc::c_int, request as i32, arg) };
    if ret < 0 {
        let err = io::Error::last_os_error();
        if matches!(
            err.raw_os_error(),
            Some(code) if code == libc::EBADF || code == libc::ENOTTY
        ) {
            if *fd >= 0 {
                unsafe {
                    libc::close(*fd as libc::c_int);
                }
            }
            *fd = init_driver_fd().unwrap_or(-1);
            ret = unsafe { libc::ioctl(*fd as libc::c_int, request as i32, arg) };
        }
    }

    if ret < 0 {
        Err(io::Error::last_os_error())
    } else {
        Ok(ret)
    }
}

// API implementations
pub fn get_info() -> ksu_uapi::ksu_get_info_cmd {
    *INFO_CACHE.get_or_init(|| {
        let mut cmd = ksu_uapi::ksu_get_info_cmd {
            version: 0,
            flags: 0,
            features: 0,
            uapi_version: 0,
        };
        if ksuctl(ksu_uapi::KSU_IOCTL_GET_INFO, &raw mut cmd).is_err() {
            let _ = ksuctl(ksu_uapi::KSU_IOCTL_GET_INFO_LEGACY, &raw mut cmd);
        }
        cmd
    })
}

pub fn get_version() -> i32 {
    get_info().version as i32
}

pub fn is_late_load() -> bool {
    get_info().flags & ksu_uapi::KSU_GET_INFO_FLAG_LATE_LOAD != 0
}

pub fn is_lkm_mode() -> bool {
    get_info().flags & ksu_uapi::KSU_GET_INFO_FLAG_LKM != 0
}

pub fn is_uapi_version_mismatch() -> bool {
    get_info().uapi_version != ksu_uapi::KERNEL_SU_UAPI_VERSION
}

pub fn grant_root() -> std::io::Result<()> {
    ksuctl(ksu_uapi::KSU_IOCTL_GRANT_ROOT, std::ptr::null_mut::<u8>())?;
    Ok(())
}

fn report_event(event: u32) {
    let mut cmd = ksu_uapi::ksu_report_event_cmd { event };
    let _ = ksuctl(ksu_uapi::KSU_IOCTL_REPORT_EVENT, &raw mut cmd);
}

pub fn report_post_fs_data() {
    report_event(ksu_uapi::EVENT_POST_FS_DATA);
}

pub fn report_boot_complete() {
    report_event(ksu_uapi::EVENT_BOOT_COMPLETED);
}

pub fn report_module_mounted() {
    report_event(ksu_uapi::EVENT_MODULE_MOUNTED);
}

pub fn check_kernel_safemode() -> bool {
    let mut cmd = ksu_uapi::ksu_check_safemode_cmd { in_safe_mode: 0 };
    let _ = ksuctl(ksu_uapi::KSU_IOCTL_CHECK_SAFEMODE, &raw mut cmd);
    cmd.in_safe_mode != 0
}

pub fn set_sepolicy(payload: *const u8, payload_len: u64) -> std::io::Result<i32> {
    let mut ioctl_cmd = crate::ksu_uapi::ksu_set_sepolicy_cmd {
        data_len: payload_len,
        data: payload as u64,
    };

    ksuctl(ksu_uapi::KSU_IOCTL_SET_SEPOLICY, &raw mut ioctl_cmd)
}

/// Get feature value and support status from kernel
/// Returns (value, supported)
pub fn get_feature(feature_id: u32) -> std::io::Result<(u64, bool)> {
    let mut cmd = ksu_uapi::ksu_get_feature_cmd {
        feature_id,
        value: 0,
        supported: 0,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_GET_FEATURE, &raw mut cmd)?;
    Ok((cmd.value, cmd.supported != 0))
}

/// Set feature value in kernel
pub fn set_feature(feature_id: u32, value: u64) -> std::io::Result<()> {
    let mut cmd = ksu_uapi::ksu_set_feature_cmd { feature_id, value };
    ksuctl(ksu_uapi::KSU_IOCTL_SET_FEATURE, &raw mut cmd)?;
    Ok(())
}

pub fn get_wrapped_fd(fd: RawFd) -> std::io::Result<RawFd> {
    let mut cmd = ksu_uapi::ksu_get_wrapper_fd_cmd {
        fd: fd as u32,
        flags: 0,
    };
    let result = ksuctl(ksu_uapi::KSU_IOCTL_GET_WRAPPER_FD, &raw mut cmd)?;
    Ok(result)
}

pub fn get_sulog_fd() -> std::io::Result<RawFd> {
    let mut cmd = ksu_uapi::ksu_get_sulog_fd_cmd { flags: 0 };
    let result = ksuctl(ksu_uapi::KSU_IOCTL_GET_SULOG_FD, &raw mut cmd)?;
    Ok(result)
}

fn set_manager_appid_ioctl(appid: u32) -> std::io::Result<()> {
    let mut cmd = ksu_uapi::ksu_set_manager_appid_cmd { appid };
    ksuctl(ksu_uapi::KSU_IOCTL_SET_MANAGER_APPID, &raw mut cmd)?;
    Ok(())
}

pub fn get_manager_appid() -> std::io::Result<u32> {
    let mut cmd = ksu_uapi::ksu_get_manager_appid_cmd { appid: 0 };
    ksuctl(ksu_uapi::KSU_IOCTL_GET_MANAGER_APPID, &raw mut cmd)?;
    Ok(cmd.appid)
}

fn verify_manager_appid(appid: u32) -> std::io::Result<()> {
    use std::io;

    match get_manager_appid() {
        Ok(current) if current == appid => Ok(()),
        Ok(current) => Err(io::Error::other(format!(
            "manager appid is {current}, expected {appid}"
        ))),
        Err(e) => Err(e),
    }
}

fn set_manager_appid_sysfs(appid: u32) -> std::io::Result<()> {
    use std::io;

    let mut errors = Vec::new();
    for module_name in ["kernelsu", "apkesu"] {
        for parameter_name in ["manager_appid", "ksu_debug_manager_appid"] {
            let path = format!("/sys/module/{module_name}/parameters/{parameter_name}");
            match std::fs::write(&path, appid.to_string())
                .and_then(|()| verify_manager_appid(appid))
            {
                Ok(()) => return Ok(()),
                Err(error) => errors.push(format!("{path}: {error}")),
            }
        }
    }

    Err(io::Error::other(format!(
        "manager appid sysfs fallback failed: {}",
        errors.join("; "),
    )))
}

pub fn set_manager_appid(appid: u32) -> std::io::Result<()> {
    use std::io;

    const FIRST_APPLICATION_APPID: u32 = 10_000;
    const LAST_APPLICATION_APPID: u32 = 19_999;
    if !(FIRST_APPLICATION_APPID..=LAST_APPLICATION_APPID).contains(&appid) {
        return Err(io::Error::new(
            io::ErrorKind::InvalidInput,
            format!("refusing non-application manager appid {appid}"),
        ));
    }

    let ioctl_err = match set_manager_appid_ioctl(appid).and_then(|()| verify_manager_appid(appid))
    {
        Ok(()) => return Ok(()),
        Err(e) => e,
    };
    let sysfs_err = match set_manager_appid_sysfs(appid) {
        Ok(()) => return Ok(()),
        Err(e) => e,
    };

    Err(io::Error::other(format!(
        "failed to set manager appid {appid}; ioctl: {ioctl_err}; sysfs: {sysfs_err}"
    )))
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct DynamicManagerKernelState {
    pub enabled: bool,
    pub active: bool,
    pub appid: u32,
    pub cert_size: u32,
    pub package_name: String,
    pub cert_sha256: String,
}

fn copy_dynamic_manager_field<const N: usize>(value: &str) -> std::io::Result<[u8; N]> {
    if value.is_empty() || value.len() >= N || value.as_bytes().contains(&0) {
        return Err(std::io::Error::new(
            std::io::ErrorKind::InvalidInput,
            "dynamic manager field is empty, too long, or contains a null byte",
        ));
    }
    let mut field = [0u8; N];
    field[..value.len()].copy_from_slice(value.as_bytes());
    Ok(field)
}

const fn dynamic_manager_command(operation: u32) -> ksu_uapi::ksu_dynamic_manager_cmd {
    ksu_uapi::ksu_dynamic_manager_cmd {
        operation,
        appid: 0,
        cert_size: 0,
        enabled: 0,
        active: 0,
        reserved: [0; 2],
        package_name: [0; ksu_uapi::KSU_MAX_PACKAGE_NAME as usize],
        cert_sha256: [0; ksu_uapi::KSU_DYNAMIC_MANAGER_CERT_SHA256_LEN as usize],
        reserved2: [0; 3],
    }
}

pub fn set_dynamic_manager(
    appid: u32,
    package_name: &str,
    cert_size: u32,
    cert_sha256: &str,
) -> std::io::Result<()> {
    let mut cmd = dynamic_manager_command(ksu_uapi::KSU_DYNAMIC_MANAGER_OP_SET);
    cmd.appid = appid;
    cmd.cert_size = cert_size;
    cmd.package_name = copy_dynamic_manager_field(package_name)?;
    cmd.cert_sha256 = copy_dynamic_manager_field(cert_sha256)?;
    ksuctl(ksu_uapi::KSU_IOCTL_DYNAMIC_MANAGER, &raw mut cmd)?;
    Ok(())
}

pub fn get_dynamic_manager() -> std::io::Result<DynamicManagerKernelState> {
    let mut cmd = dynamic_manager_command(ksu_uapi::KSU_DYNAMIC_MANAGER_OP_GET);
    ksuctl(ksu_uapi::KSU_IOCTL_DYNAMIC_MANAGER, &raw mut cmd)?;

    let decode = |bytes: &[u8]| {
        let end = bytes
            .iter()
            .position(|value| *value == 0)
            .unwrap_or(bytes.len());
        String::from_utf8_lossy(&bytes[..end]).into_owned()
    };
    Ok(DynamicManagerKernelState {
        enabled: cmd.enabled != 0,
        active: cmd.active != 0,
        appid: cmd.appid,
        cert_size: cmd.cert_size,
        package_name: decode(&cmd.package_name),
        cert_sha256: decode(&cmd.cert_sha256),
    })
}

pub fn clear_dynamic_manager() -> std::io::Result<()> {
    let mut cmd = dynamic_manager_command(ksu_uapi::KSU_DYNAMIC_MANAGER_OP_CLEAR);
    ksuctl(ksu_uapi::KSU_IOCTL_DYNAMIC_MANAGER, &raw mut cmd)?;
    Ok(())
}

/// Get mark status for a process (pid=0 returns total marked count)
pub fn mark_get(pid: i32) -> std::io::Result<u32> {
    let mut cmd = ksu_uapi::ksu_manage_mark_cmd {
        operation: ksu_uapi::KSU_MARK_GET,
        pid,
        result: 0,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(cmd.result)
}

/// Mark a process (pid=0 marks all processes)
pub fn mark_set(pid: i32) -> std::io::Result<()> {
    let mut cmd = ksu_uapi::ksu_manage_mark_cmd {
        operation: ksu_uapi::KSU_MARK_MARK,
        pid,
        result: 0,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

/// Unmark a process (pid=0 unmarks all processes)
pub fn mark_unset(pid: i32) -> std::io::Result<()> {
    let mut cmd = ksu_uapi::ksu_manage_mark_cmd {
        operation: ksu_uapi::KSU_MARK_UNMARK,
        pid,
        result: 0,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

/// Refresh mark for all running processes
pub fn mark_refresh() -> std::io::Result<()> {
    let mut cmd = ksu_uapi::ksu_manage_mark_cmd {
        operation: ksu_uapi::KSU_MARK_REFRESH,
        pid: 0,
        result: 0,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_MANAGE_MARK, &raw mut cmd)?;
    Ok(())
}

pub fn nuke_ext4_sysfs(mnt: &str) -> anyhow::Result<()> {
    let c_mnt = std::ffi::CString::new(mnt)?;
    let mut ioctl_cmd = ksu_uapi::ksu_nuke_ext4_sysfs_cmd {
        arg: c_mnt.as_ptr() as u64,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_NUKE_EXT4_SYSFS, &raw mut ioctl_cmd)?;
    Ok(())
}

/// Wipe all entries from umount list
pub fn umount_list_wipe() -> std::io::Result<()> {
    let mut cmd = ksu_uapi::ksu_add_try_umount_cmd {
        arg: 0,
        flags: 0,
        mode: ksu_uapi::KSU_UMOUNT_WIPE,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Add mount point to umount list
pub fn umount_list_add(path: &str, flags: u32) -> anyhow::Result<()> {
    let c_path = std::ffi::CString::new(path)?;
    let mut cmd = ksu_uapi::ksu_add_try_umount_cmd {
        arg: c_path.as_ptr() as u64,
        flags,
        mode: ksu_uapi::KSU_UMOUNT_ADD,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Delete mount point from umount list
pub fn umount_list_del(path: &str) -> anyhow::Result<()> {
    let c_path = std::ffi::CString::new(path)?;
    let mut cmd = ksu_uapi::ksu_add_try_umount_cmd {
        arg: c_path.as_ptr() as u64,
        flags: 0,
        mode: ksu_uapi::KSU_UMOUNT_DEL,
    };
    ksuctl(ksu_uapi::KSU_IOCTL_ADD_TRY_UMOUNT, &raw mut cmd)?;
    Ok(())
}

/// Set current process's process group to init_group (pgid = 0)
pub fn set_init_pgrp() -> std::io::Result<()> {
    ksuctl(
        ksu_uapi::KSU_IOCTL_SET_INIT_PGRP,
        std::ptr::null_mut::<u8>(),
    )?;
    Ok(())
}

pub fn set_ksu_no_new_privs() -> anyhow::Result<()> {
    let result = ksuctl(
        ksu_uapi::KSU_IOCTL_DISABLE_ESCAPE_TO_ROOT,
        std::ptr::null_mut::<u8>(),
    )?;
    if result != 0 {
        bail!("unexpected result: {result}");
    }
    Ok(())
}
