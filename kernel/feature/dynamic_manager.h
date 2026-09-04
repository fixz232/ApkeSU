// SPDX-License-Identifier: GPL-2.0
#ifndef __KSU_H_DYNAMIC_MANAGER
#define __KSU_H_DYNAMIC_MANAGER

#include <linux/errno.h>
#include <linux/types.h>

#include "manager/manager_sign.h"
#include "uapi/supercall.h"

#ifdef CONFIG_KSU_DISABLE_MANAGER
static inline int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd)
{
    (void)cmd;
    return -EOPNOTSUPP;
}

static inline bool ksu_is_dynamic_manager_enabled(void)
{
    return false;
}

static inline apk_sign_key_t ksu_get_dynamic_manager_sign(void)
{
    apk_sign_key_t key = { 0 };

    return key;
}

static inline bool ksu_dynamic_manager_matches(unsigned size, const char *sha256)
{
    (void)size;
    (void)sha256;
    return false;
}

static inline u64 ksu_get_dynamic_manager_generation(void)
{
    return 0;
}
#else
int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd);
bool ksu_is_dynamic_manager_enabled(void);
apk_sign_key_t ksu_get_dynamic_manager_sign(void);
bool ksu_dynamic_manager_matches(unsigned size, const char *sha256);
u64 ksu_get_dynamic_manager_generation(void);
#endif

#endif // __KSU_H_DYNAMIC_MANAGER
