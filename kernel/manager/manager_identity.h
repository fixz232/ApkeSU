#ifndef __KSU_H_MANAGER_IDENTITY
#define __KSU_H_MANAGER_IDENTITY

#include <linux/cred.h>
#include <linux/types.h>

#include "feature/dynamic_manager.h"

#define KSU_INVALID_APPID -1
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
static inline bool ksu_is_manager_appid_valid()
{
    return true;
}

static inline bool is_manager()
{
    return current_uid().val == 0;
}

static inline bool is_primary_manager()
{
    return current_uid().val == 0;
}

static inline bool is_uid_manager(uid_t uid)
{
    return uid == 0;
}

static inline uid_t ksu_get_manager_appid()
{
    return 0;
}

static inline void ksu_set_manager_appid(uid_t appid)
{
    (void)appid;
}

static inline void ksu_invalidate_manager_uid()
{
}
#else
extern uid_t ksu_manager_appid; // DO NOT DIRECT USE

static inline bool ksu_is_manager_appid_valid()
{
    return ksu_manager_appid != KSU_INVALID_APPID;
}

static inline bool is_manager()
{
    uid_t appid = ksu_normalize_appid(current_uid().val);

    return unlikely(ksu_manager_appid == appid || ksu_is_dynamic_manager_appid(appid));
}

static inline bool is_primary_manager()
{
    return unlikely(ksu_manager_appid == ksu_normalize_appid(current_uid().val));
}

static inline bool is_uid_manager(uid_t uid)
{
    uid_t appid = ksu_normalize_appid(uid);

    return unlikely(ksu_manager_appid == appid || ksu_is_dynamic_manager_appid(appid));
}

static inline uid_t ksu_get_manager_appid()
{
    return ksu_manager_appid;
}

static inline void ksu_set_manager_appid(uid_t appid)
{
    ksu_manager_appid = ksu_normalize_appid(appid);
}

static inline void ksu_invalidate_manager_uid()
{
    ksu_manager_appid = KSU_INVALID_APPID;
}
#endif

#endif // __KSU_H_MANAGER_IDENTITY
