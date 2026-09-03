// SPDX-License-Identifier: GPL-2.0
#ifndef __KSU_DYNAMIC_MANAGER_H
#define __KSU_DYNAMIC_MANAGER_H

#include <linux/errno.h>
#include <linux/types.h>

#include "uapi/supercall.h"

#ifdef CONFIG_KSU_DISABLE_MANAGER
static inline int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd)
{
    (void)cmd;
    return -EOPNOTSUPP;
}

static inline bool ksu_is_dynamic_manager_appid(uid_t appid)
{
    (void)appid;
    return false;
}

static inline bool ksu_dynamic_manager_get_identity(char *package_name, size_t package_size, uid_t *appid,
                                                    u64 *revision)
{
    (void)package_name;
    (void)package_size;
    (void)appid;
    (void)revision;
    return false;
}

static inline bool ksu_dynamic_manager_validate_apk(const char *path)
{
    (void)path;
    return false;
}

static inline void ksu_dynamic_manager_set_active(uid_t appid, u64 revision, bool active)
{
    (void)appid;
    (void)revision;
    (void)active;
}
#else
int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd);
bool ksu_is_dynamic_manager_appid(uid_t appid);
bool ksu_dynamic_manager_get_identity(char *package_name, size_t package_size, uid_t *appid, u64 *revision);
bool ksu_dynamic_manager_validate_apk(const char *path);
void ksu_dynamic_manager_set_active(uid_t appid, u64 revision, bool active);
#endif

#endif
