// SPDX-License-Identifier: GPL-2.0
#include <linux/kernel.h>
#include <linux/mutex.h>
#include <linux/string.h>

#include "feature/dynamic_manager.h"
#include "manager/manager_identity.h"

#define KSU_DYNAMIC_MANAGER_MIN_CERT_SIZE 0x100
#define KSU_DYNAMIC_MANAGER_MAX_CERT_SIZE 0x1000

struct dynamic_manager_config {
    unsigned size;
    char hash[65];
    bool is_set;
    u64 generation;
};

static DEFINE_MUTEX(dynamic_manager_lock);
static struct dynamic_manager_config dynamic_manager;

static bool valid_hash(const u8 hash[64])
{
    size_t index;

    for (index = 0; index < 64; index++) {
        u8 value = hash[index];

        if (!((value >= '0' && value <= '9') ||
              (value >= 'a' && value <= 'f')))
            return false;
    }
    return true;
}

bool ksu_is_dynamic_manager_enabled(void)
{
    return READ_ONCE(dynamic_manager.is_set);
}

apk_sign_key_t ksu_get_dynamic_manager_sign(void)
{
    apk_sign_key_t key;

    mutex_lock(&dynamic_manager_lock);
    key.size = dynamic_manager.size;
    key.sha256 = dynamic_manager.hash;
    mutex_unlock(&dynamic_manager_lock);
    return key;
}

bool ksu_dynamic_manager_matches(unsigned size, const char *sha256)
{
    bool matches;

    if (!sha256)
        return false;
    mutex_lock(&dynamic_manager_lock);
    matches = dynamic_manager.is_set && dynamic_manager.size == size &&
              !strcmp(dynamic_manager.hash, sha256);
    mutex_unlock(&dynamic_manager_lock);
    return matches;
}

u64 ksu_get_dynamic_manager_generation(void)
{
    u64 generation;

    mutex_lock(&dynamic_manager_lock);
    generation = dynamic_manager.generation;
    mutex_unlock(&dynamic_manager_lock);
    return generation;
}

int ksu_handle_dynamic_manager(struct ksu_dynamic_manager_cmd *cmd)
{
    if (!cmd)
        return -EINVAL;

    switch (cmd->operation) {
    case DYNAMIC_MANAGER_OP_SET_SYNCHRONOUS:
    case DYNAMIC_MANAGER_OP_SET:
        if (cmd->size < KSU_DYNAMIC_MANAGER_MIN_CERT_SIZE ||
            cmd->size > KSU_DYNAMIC_MANAGER_MAX_CERT_SIZE ||
            !valid_hash(cmd->hash))
            return -EINVAL;

        mutex_lock(&dynamic_manager_lock);
        dynamic_manager.size = cmd->size;
        memcpy(dynamic_manager.hash, cmd->hash, sizeof(cmd->hash));
        dynamic_manager.hash[64] = '\0';
        dynamic_manager.is_set = true;
        dynamic_manager.generation++;
        mutex_unlock(&dynamic_manager_lock);
        ksu_unregister_manager_by_signature_index(
            KSU_SIGNATURE_INDEX_DYNAMIC_MANAGER);
        pr_info("dynamic manager updated: size=0x%x, hash=%.16s\n",
                cmd->size, cmd->hash);
        return 0;

    case DYNAMIC_MANAGER_OP_GET:
        mutex_lock(&dynamic_manager_lock);
        if (!dynamic_manager.is_set) {
            mutex_unlock(&dynamic_manager_lock);
            return -ENODATA;
        }
        cmd->size = dynamic_manager.size;
        memcpy(cmd->hash, dynamic_manager.hash, sizeof(cmd->hash));
        mutex_unlock(&dynamic_manager_lock);
        return 0;

    case DYNAMIC_MANAGER_OP_WIPE:
        mutex_lock(&dynamic_manager_lock);
        dynamic_manager.size = 0;
        memset(dynamic_manager.hash, 0, sizeof(dynamic_manager.hash));
        dynamic_manager.is_set = false;
        dynamic_manager.generation++;
        mutex_unlock(&dynamic_manager_lock);
        ksu_unregister_manager_by_signature_index(
            KSU_SIGNATURE_INDEX_DYNAMIC_MANAGER);
        pr_info("dynamic manager kernel settings reset\n");
        return 0;

    default:
        return -EINVAL;
    }
}
