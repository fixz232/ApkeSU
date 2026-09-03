// SPDX-License-Identifier: GPL-2.0
#include <linux/ctype.h>
#include <linux/kernel.h>
#include <linux/mutex.h>
#include <linux/string.h>

#include "feature/dynamic_manager.h"
#include "manager/apk_sign.h"
#include "manager/manager_identity.h"

#define KSU_DYNAMIC_MANAGER_MIN_CERT_SIZE 0x100
#define KSU_DYNAMIC_MANAGER_MAX_CERT_SIZE 1024

struct dynamic_manager_config {
    uid_t appid;
    u32 cert_size;
    char package_name[KSU_MAX_PACKAGE_NAME];
    char cert_sha256[KSU_DYNAMIC_MANAGER_CERT_SHA256_LEN];
    bool enabled;
};

static DEFINE_MUTEX(dynamic_manager_lock);
static struct dynamic_manager_config dynamic_manager;
static uid_t dynamic_manager_active_appid = KSU_INVALID_APPID;
static u64 dynamic_manager_revision;

static bool valid_package_name(const u8 *package_name)
{
    size_t index;
    size_t length = strnlen((const char *)package_name, KSU_MAX_PACKAGE_NAME);

    if (!length || length >= KSU_MAX_PACKAGE_NAME || package_name[0] == '.' || package_name[length - 1] == '.')
        return false;

    for (index = 0; index < length; index++) {
        u8 value = package_name[index];

        if (!(isalnum(value) || value == '_' || value == '.'))
            return false;
        if (value == '.' && index > 0 && package_name[index - 1] == '.')
            return false;
    }
    return true;
}

static bool valid_cert_sha256(const u8 *hash)
{
    size_t index;

    if (hash[KSU_DYNAMIC_MANAGER_CERT_SHA256_LEN - 1] != '\0')
        return false;

    for (index = 0; index < KSU_DYNAMIC_MANAGER_CERT_SHA256_LEN - 1; index++) {
        u8 value = hash[index];

        if (!((value >= '0' && value <= '9') || (value >= 'a' && value <= 'f')))
            return false;
    }
    return true;
}

bool ksu_is_dynamic_manager_appid(uid_t appid)
{
    return unlikely(READ_ONCE(dynamic_manager_active_appid) == appid);
}

bool ksu_dynamic_manager_get_identity(char *package_name, size_t package_size, uid_t *appid, u64 *revision)
{
    bool enabled;

    if (!package_name || !package_size || !appid || !revision)
        return false;

    mutex_lock(&dynamic_manager_lock);
    enabled = dynamic_manager.enabled;
    if (enabled) {
        strscpy(package_name, dynamic_manager.package_name, package_size);
        *appid = dynamic_manager.appid;
        *revision = dynamic_manager_revision;
    }
    mutex_unlock(&dynamic_manager_lock);
    return enabled;
}

bool ksu_dynamic_manager_validate_apk(const char *path)
{
    struct dynamic_manager_config snapshot;
    char package_name[KSU_MAX_PACKAGE_NAME];

    if (!path)
        return false;

    mutex_lock(&dynamic_manager_lock);
    snapshot = dynamic_manager;
    mutex_unlock(&dynamic_manager_lock);

    if (!snapshot.enabled || get_pkg_from_apk_path(package_name, path) < 0 ||
        strcmp(package_name, snapshot.package_name) != 0)
        return false;

    return ksu_apk_matches_v2_signature(path, snapshot.cert_size, snapshot.cert_sha256);
}

void ksu_dynamic_manager_set_active(uid_t appid, u64 revision, bool active)
{
    uid_t next = KSU_INVALID_APPID;

    mutex_lock(&dynamic_manager_lock);
    if (revision == dynamic_manager_revision) {
        if (active && dynamic_manager.enabled && dynamic_manager.appid == appid)
            next = appid;
        WRITE_ONCE(dynamic_manager_active_appid, next);
    }
    mutex_unlock(&dynamic_manager_lock);
}

int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd)
{
    if (!cmd)
        return -EINVAL;

    switch (cmd->operation) {
    case KSU_DYNAMIC_MANAGER_OP_SET:
        if (!ksu_is_normal_appid(cmd->appid) || cmd->cert_size < KSU_DYNAMIC_MANAGER_MIN_CERT_SIZE ||
            cmd->cert_size > KSU_DYNAMIC_MANAGER_MAX_CERT_SIZE || !valid_package_name(cmd->package_name) ||
            !valid_cert_sha256(cmd->cert_sha256))
            return -EINVAL;

        mutex_lock(&dynamic_manager_lock);
        dynamic_manager.appid = cmd->appid;
        dynamic_manager.cert_size = cmd->cert_size;
        strscpy(dynamic_manager.package_name, (const char *)cmd->package_name, sizeof(dynamic_manager.package_name));
        strscpy(dynamic_manager.cert_sha256, (const char *)cmd->cert_sha256, sizeof(dynamic_manager.cert_sha256));
        dynamic_manager.enabled = true;
        dynamic_manager_revision++;
        WRITE_ONCE(dynamic_manager_active_appid, KSU_INVALID_APPID);
        mutex_unlock(&dynamic_manager_lock);
        pr_info("dynamic manager configured for %s (appid=%u)\n", (const char *)cmd->package_name, cmd->appid);
        return 0;

    case KSU_DYNAMIC_MANAGER_OP_GET:
        memset(cmd, 0, sizeof(*cmd));
        cmd->operation = KSU_DYNAMIC_MANAGER_OP_GET;
        mutex_lock(&dynamic_manager_lock);
        cmd->enabled = dynamic_manager.enabled;
        cmd->active = dynamic_manager.enabled && READ_ONCE(dynamic_manager_active_appid) == dynamic_manager.appid;
        if (dynamic_manager.enabled) {
            cmd->appid = dynamic_manager.appid;
            cmd->cert_size = dynamic_manager.cert_size;
            strscpy((char *)cmd->package_name, dynamic_manager.package_name, sizeof(cmd->package_name));
            strscpy((char *)cmd->cert_sha256, dynamic_manager.cert_sha256, sizeof(cmd->cert_sha256));
        }
        mutex_unlock(&dynamic_manager_lock);
        return 0;

    case KSU_DYNAMIC_MANAGER_OP_CLEAR:
        mutex_lock(&dynamic_manager_lock);
        memset(&dynamic_manager, 0, sizeof(dynamic_manager));
        dynamic_manager.appid = KSU_INVALID_APPID;
        dynamic_manager_revision++;
        WRITE_ONCE(dynamic_manager_active_appid, KSU_INVALID_APPID);
        mutex_unlock(&dynamic_manager_lock);
        pr_info("dynamic manager configuration cleared\n");
        return 0;

    default:
        return -EINVAL;
    }
}
