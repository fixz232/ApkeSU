#ifndef __KSU_H_MANAGER_IDENTITY
#define __KSU_H_MANAGER_IDENTITY

#include <linux/cred.h>
#include <linux/errno.h>
#include <linux/types.h>

#include "uapi/supercall.h"

#define KSU_SIGNATURE_INDEX_PRIMARY 0
#define KSU_SIGNATURE_INDEX_DYNAMIC_MANAGER 255
#define KSU_SIGNATURE_INDEX_KSU_DEBUG 254

#define KSU_INVALID_APPID ((uid_t)-1)
#define KSU_PER_USER_RANGE 100000
#define KSU_FIRST_APPLICATION_APPID 10000
#define KSU_LAST_APPLICATION_APPID 19999

static inline uid_t ksu_normalize_appid(uid_t uid)
{
    return uid % KSU_PER_USER_RANGE;
}

static inline bool ksu_is_normal_appid(uid_t appid)
{
    return appid >= KSU_FIRST_APPLICATION_APPID && appid <= KSU_LAST_APPLICATION_APPID;
}

#ifdef CONFIG_KSU_DISABLE_MANAGER
static inline bool ksu_is_manager_appid_valid(void)
{
    return true;
}

static inline bool is_manager(void)
{
    return current_uid().val == 0;
}

static inline bool is_primary_manager(void)
{
    return current_uid().val == 0;
}

static inline bool ksu_is_manager_appid(uid_t appid)
{
    return appid == 0;
}

static inline bool ksu_is_manager_uid(uid_t uid)
{
    return uid == 0;
}

static inline bool is_uid_manager(uid_t uid)
{
    return uid == 0;
}

static inline uid_t ksu_get_manager_appid(void)
{
    return 0;
}

static inline void ksu_set_manager_appid(uid_t appid)
{
    (void)appid;
}

static inline void ksu_invalidate_manager_uid(void)
{
}

static inline void ksu_register_manager(uid_t uid, u8 signature_index)
{
    (void)uid;
    (void)signature_index;
}

static inline void ksu_unregister_manager(uid_t uid)
{
    (void)uid;
}

static inline void ksu_unregister_manager_by_signature_index(u8 signature_index)
{
    (void)signature_index;
}

static inline int ksu_get_manager_signature_index_by_appid(uid_t appid)
{
    (void)appid;
    return -EOPNOTSUPP;
}

static inline bool ksu_has_manager(void)
{
    return true;
}

static inline int ksu_replace_managers(const struct ksu_manager_entry *entries, u16 count)
{
    (void)entries;
    (void)count;
    return 0;
}

static inline int ksu_handle_get_managers_cmd(
    struct ksu_get_managers_cmd __user *arg,
    struct ksu_get_managers_cmd *cmd)
{
    (void)arg;
    (void)cmd;
    return -EOPNOTSUPP;
}

static inline void ksu_manager_registry_exit(void)
{
}
#else
bool is_manager(void);
bool is_primary_manager(void);
bool is_uid_manager(uid_t uid);
bool ksu_is_manager_appid(uid_t appid);
bool ksu_is_manager_uid(uid_t uid);
bool ksu_is_manager_appid_valid(void);
uid_t ksu_get_manager_appid(void);
void ksu_set_manager_appid(uid_t appid);
void ksu_invalidate_manager_uid(void);
void ksu_register_manager(uid_t uid, u8 signature_index);
void ksu_unregister_manager(uid_t uid);
void ksu_unregister_manager_by_signature_index(u8 signature_index);
int ksu_get_manager_signature_index_by_appid(uid_t appid);
bool ksu_has_manager(void);
int ksu_replace_managers(const struct ksu_manager_entry *entries, u16 count);
int ksu_handle_get_managers_cmd(struct ksu_get_managers_cmd __user *arg,
                                struct ksu_get_managers_cmd *cmd);
void ksu_manager_registry_exit(void);
#endif

#endif // __KSU_H_MANAGER_IDENTITY
