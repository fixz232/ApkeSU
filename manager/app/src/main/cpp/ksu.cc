//
// Created by weishu on 2022/12/9.
//

#include <sys/prctl.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <mutex>
#include <unistd.h>
#include <utility>
#include <android/log.h>
#include <dirent.h>
#include <cstdlib>
#include <sys/syscall.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/wait.h>

#include <climits>
#include <fcntl.h>
#include <cerrno>
#include "ksu.h"

static int fd = -1;
static std::mutex fd_mutex;

static inline void set_fd_recv_timeout(int socket_fd) {
    struct timeval timeout = {};
    timeout.tv_sec = 1;
    timeout.tv_usec = 500000;
    setsockopt(socket_fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
}

static inline void close_driver_fd() {
    if (fd >= 0) {
        close(fd);
        fd = -1;
    }
}

static inline void keep_driver_fd_on_exec(int driver_fd) {
    int flags = fcntl(driver_fd, F_GETFD);
    if (flags >= 0) {
        fcntl(driver_fd, F_SETFD, flags & ~FD_CLOEXEC);
    }
}

static inline int scan_driver_fd() {
    const char *kName = "[ksu_driver]";
    DIR *dir = opendir("/proc/self/fd");
    if (!dir) {
        return -1;
    }

    int found = -1;
    struct dirent *de;
    char path[64];
    char target[PATH_MAX];

    while ((de = readdir(dir)) != NULL) {
        if (de->d_name[0] == '.') {
            continue;
        }

        char *endptr = NULL;
        long fd_long = strtol(de->d_name, &endptr, 10);
        if (!de->d_name[0] || *endptr != '\0' || fd_long < 0 || fd_long > INT_MAX) {
            continue;
        }

        snprintf(path, sizeof(path), "/proc/self/fd/%s", de->d_name);
        ssize_t n = readlink(path, target, sizeof(target) - 1);
        if (n < 0) {
            continue;
        }
        target[n] = '\0';

        const char *base = strrchr(target, '/');
        base = base ? base + 1 : target;

        if (strstr(base, kName)) {
            found = (int)fd_long;
            keep_driver_fd_on_exec(found);
            break;
        }
    }

    closedir(dir);
    return found;
}

static bool send_driver_fd(int socket_fd, int driver_fd) {
    char payload = 0;
    struct iovec iov = {};
    iov.iov_base = &payload;
    iov.iov_len = sizeof(payload);
    char control[CMSG_SPACE(sizeof(int))] = {};
    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control;
    msg.msg_controllen = sizeof(control);

    struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = CMSG_LEN(sizeof(int));
    memcpy(CMSG_DATA(cmsg), &driver_fd, sizeof(driver_fd));

    return sendmsg(socket_fd, &msg, 0) == sizeof(payload);
}

static int recv_driver_fd(int socket_fd) {
    char payload = 0;
    struct iovec iov = {};
    iov.iov_base = &payload;
    iov.iov_len = sizeof(payload);
    char control[CMSG_SPACE(sizeof(int))] = {};
    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control;
    msg.msg_controllen = sizeof(control);

    ssize_t n = recvmsg(socket_fd, &msg, 0);
    if (n <= 0) {
        return -1;
    }

    struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
    if (!cmsg || cmsg->cmsg_level != SOL_SOCKET || cmsg->cmsg_type != SCM_RIGHTS) {
        return -1;
    }

    int driver_fd = -1;
    memcpy(&driver_fd, CMSG_DATA(cmsg), sizeof(driver_fd));
    return driver_fd;
}

static inline int install_driver_fd() {
    int sockets[2] = {-1, -1};
    if (socketpair(AF_UNIX, SOCK_STREAM, 0, sockets) < 0) {
        return -1;
    }
    set_fd_recv_timeout(sockets[0]);

    pid_t pid = fork();
    if (pid < 0) {
        int saved_errno = errno;
        close(sockets[0]);
        close(sockets[1]);
        errno = saved_errno;
        return -1;
    }

    if (pid == 0) {
        close(sockets[0]);
        int child_fd = -1;
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, KSU_INSTALL_MAGIC2, 0, &child_fd);
        bool sent = child_fd >= 0 && send_driver_fd(sockets[1], child_fd);
        if (child_fd >= 0) {
            close(child_fd);
        }
        close(sockets[1]);
        _exit(sent ? 0 : 1);
    }

    close(sockets[1]);
    int driver_fd = recv_driver_fd(sockets[0]);
    close(sockets[0]);

    int status = 0;
    while (waitpid(pid, &status, 0) < 0 && errno == EINTR) {
    }

    if (driver_fd >= 0) {
        keep_driver_fd_on_exec(driver_fd);
    } else {
        errno = ENODEV;
    }
    return driver_fd;
}

static int ensure_driver_fd(bool allow_install) {
    if (fd < 0) {
        fd = scan_driver_fd();
    }
    if (fd < 0 && allow_install) {
        fd = install_driver_fd();
    }

    return fd;
}

template<typename... Args>
static int ksuctl_impl(bool allow_install, unsigned long op, Args &&... args) {
    std::lock_guard<std::mutex> lock(fd_mutex);
    int driver_fd = ensure_driver_fd(allow_install);
    if (driver_fd < 0) {
        errno = ENODEV;
        return -1;
    }

    static_assert(sizeof...(Args) <= 1, "ioctl expects at most one extra argument");

    int ret = ioctl(driver_fd, op, std::forward<Args>(args)...);
    if (ret < 0 && (errno == EBADF || errno == ENOTTY)) {
        close_driver_fd();
        driver_fd = ensure_driver_fd(allow_install);
        if (driver_fd < 0) {
            errno = ENODEV;
            return -1;
        }
        ret = ioctl(driver_fd, op, std::forward<Args>(args)...);
    }
    return ret;
}

template<typename... Args>
static int ksuctl(unsigned long op, Args &&... args) {
    return ksuctl_impl(true, op, std::forward<Args>(args)...);
}

template<typename... Args>
static int ksuctl_scan_only(unsigned long op, Args &&... args) {
    return ksuctl_impl(false, op, std::forward<Args>(args)...);
}

static struct ksu_get_info_cmd g_version {};
static std::mutex info_mutex;

struct ksu_get_info_cmd get_info() {
    std::lock_guard<std::mutex> lock(info_mutex);
    if (!g_version.version) {
        if (ksuctl(KSU_IOCTL_GET_INFO, &g_version) < 0) {
            auto legacy = legacy_get_info();
            g_version.version = legacy.first;
            g_version.flags = legacy.second;
            g_version.features = 0;
            g_version.uapi_version = 0;
        }
    }
    return g_version;
}

void refresh_info() {
    {
        std::lock_guard<std::mutex> lock(info_mutex);
        g_version = {};
    }
    get_info();
}

uint32_t get_kernel_uapi_version() {
    auto info = get_info();
    return info.uapi_version;
}

uint32_t get_manager_uapi_version() {
    return KERNEL_SU_UAPI_VERSION;
}

uint32_t get_version() {
    auto info = get_info();
    return info.version;
}

bool get_allow_list(struct ksu_new_get_allow_list_cmd *cmd) {
    return ksuctl_scan_only(KSU_IOCTL_NEW_GET_ALLOW_LIST, cmd) == 0;
}

bool is_safe_mode() {
    struct ksu_check_safemode_cmd cmd = {};
    ksuctl_scan_only(KSU_IOCTL_CHECK_SAFEMODE, &cmd);
    return cmd.in_safe_mode;
}

bool is_lkm_mode() {
    auto info = get_info();
    if (info.version > 0) {
        return (info.flags & KSU_GET_INFO_FLAG_LKM) != 0;
    }
    return (legacy_get_info().second & KSU_GET_INFO_FLAG_LKM) != 0;
}

bool is_late_load_mode() {
    auto info = get_info();
    if (info.version > 0) {
        return (info.flags & KSU_GET_INFO_FLAG_LATE_LOAD) != 0;
    }
    return false;
}

bool is_manager() {
    auto info = get_info();
    if (info.version > 0) {
        return (info.flags & KSU_GET_INFO_FLAG_MANAGER) != 0;
    }
    return legacy_get_info().first > 0;
}

bool is_pr_build() {
    auto info = get_info();
    if (info.version > 0) {
        return (info.flags & KSU_GET_INFO_FLAG_PR_BUILD) != 0;
    }
    return false;
}

bool uid_should_umount(int uid) {
    struct ksu_uid_should_umount_cmd cmd = {};
    cmd.uid = uid;
    ksuctl_scan_only(KSU_IOCTL_UID_SHOULD_UMOUNT, &cmd);
    return cmd.should_umount;
}

bool set_app_profile(const app_profile *profile) {
    struct ksu_set_app_profile_cmd cmd = {};
    cmd.profile = *profile;
    return ksuctl(KSU_IOCTL_SET_APP_PROFILE, &cmd) == 0;
}

int get_app_profile(app_profile *profile) {
    struct ksu_get_app_profile_cmd cmd = {.profile = *profile};
    int ret = ksuctl_scan_only(KSU_IOCTL_GET_APP_PROFILE, &cmd);
    *profile = cmd.profile;
    return ret;
}

bool set_su_enabled(bool enabled) {
    struct ksu_set_feature_cmd cmd = {};
    cmd.feature_id = KSU_FEATURE_SU_COMPAT;
    cmd.value = enabled ? 1 : 0;
    return ksuctl(KSU_IOCTL_SET_FEATURE, &cmd) == 0;
}

bool is_su_enabled() {
    struct ksu_get_feature_cmd cmd = {};
    cmd.feature_id = KSU_FEATURE_SU_COMPAT;
    if (ksuctl_scan_only(KSU_IOCTL_GET_FEATURE, &cmd) != 0) {
        return false;
    }
    if (!cmd.supported) {
        return false;
    }
    return cmd.value != 0;
}

static inline bool get_feature(uint32_t feature_id, uint64_t *out_value, bool *out_supported) {
    struct ksu_get_feature_cmd cmd = {};
    cmd.feature_id = feature_id;
    if (ksuctl_scan_only(KSU_IOCTL_GET_FEATURE, &cmd) != 0) {
        return false;
    }
    if (out_value) *out_value = cmd.value;
    if (out_supported) *out_supported = cmd.supported;
    return true;
}

static inline bool set_feature(uint32_t feature_id, uint64_t value) {
    struct ksu_set_feature_cmd cmd = {};
    cmd.feature_id = feature_id;
    cmd.value = value;
    return ksuctl(KSU_IOCTL_SET_FEATURE, &cmd) == 0;
}

bool set_kernel_umount_enabled(bool enabled) {
    return set_feature(KSU_FEATURE_KERNEL_UMOUNT, enabled ? 1 : 0);
}

bool is_kernel_umount_enabled() {
    uint64_t value = 0;
    bool supported = false;
    if (!get_feature(KSU_FEATURE_KERNEL_UMOUNT, &value, &supported)) {
        return false;
    }
    if (!supported) {
        return false;
    }
    return value != 0;
}

bool set_webview_zygote_umount_enabled(bool enabled) {
    return set_feature(KSU_FEATURE_WEBVIEW_ZYGOTE_UMOUNT, enabled ? 1 : 0);
}

bool is_webview_zygote_umount_enabled() {
    uint64_t value = 0;
    bool supported = false;
    if (!get_feature(KSU_FEATURE_WEBVIEW_ZYGOTE_UMOUNT, &value, &supported)) {
        return false;
    }
    if (!supported) {
        return false;
    }
    return value != 0;
}

int set_selinux_hide_enabled(bool enabled) {
    if (!set_feature(KSU_FEATURE_SELINUX_HIDE, enabled ? 1 : 0)) {
        return -errno;
    }
    return 0;
}

bool is_selinux_hide_enabled() {
    uint64_t value = 0;
    bool supported = false;
    if (!get_feature(KSU_FEATURE_SELINUX_HIDE, &value, &supported)) {
        return false;
    }
    if (!supported) {
        return false;
    }
    return value != 0;
}

bool is_selinux_hide_supported() {
    uint64_t value = 0;
    bool supported = false;
    if (!get_feature(KSU_FEATURE_SELINUX_HIDE, &value, &supported)) {
        return false;
    }
    return supported;
}

bool set_avc_spoof_enabled(bool enabled) {
    uint64_t value = enabled ? 1 : 0;
    return set_feature(KSU_FEATURE_AVC_SPOOF, value) ||
           set_feature(KSU_FEATURE_AVC_SPOOF_LEGACY, value);
}

bool is_avc_spoof_enabled() {
    uint64_t value = 0;
    bool supported = false;
    if (get_feature(KSU_FEATURE_AVC_SPOOF, &value, &supported) && supported) {
        return value != 0;
    }

    if (!get_feature(KSU_FEATURE_AVC_SPOOF_LEGACY, &value, &supported)) {
        return false;
    }
    return supported && value != 0;
}

uint64_t get_kernel_hook_status() {
    uint64_t value = 0;
    bool supported = false;
    if (!get_feature(KSU_FEATURE_HOOK_STATUS, &value, &supported) || !supported) {
        return UINT64_MAX;
    }
    return value;
}
