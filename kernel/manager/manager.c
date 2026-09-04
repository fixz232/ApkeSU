// SPDX-License-Identifier: GPL-2.0
#include <linux/errno.h>
#include <linux/kernel.h>
#include <linux/limits.h>
#include <linux/rculist.h>
#include <linux/slab.h>
#include <linux/spinlock.h>
#include <linux/uaccess.h>

#include "manager/manager_identity.h"

struct ksu_manager_node {
    u8 signature_index;
    uid_t appid;
    struct list_head list;
    struct rcu_head rcu;
};

static LIST_HEAD(ksu_manager_appid_list);
static DEFINE_SPINLOCK(ksu_manager_list_lock);
static uid_t primary_manager_appid = KSU_INVALID_APPID;

static struct ksu_manager_node *alloc_manager_node(uid_t uid, u8 signature_index, gfp_t flags)
{
    struct ksu_manager_node *node;
    uid_t appid = ksu_normalize_appid(uid);

    if (!ksu_is_normal_appid(appid))
        return NULL;

    node = kzalloc(sizeof(*node), flags);
    if (!node)
        return NULL;
    node->appid = appid;
    node->signature_index = signature_index;
    return node;
}

bool ksu_is_manager_appid(uid_t appid)
{
    struct ksu_manager_node *node;
    bool found = false;

    appid = ksu_normalize_appid(appid);
    rcu_read_lock();
    list_for_each_entry_rcu(node, &ksu_manager_appid_list, list) {
        if (node->appid == appid) {
            found = true;
            break;
        }
    }
    rcu_read_unlock();
    return found;
}

bool ksu_is_manager_uid(uid_t uid)
{
    return ksu_is_manager_appid(ksu_normalize_appid(uid));
}

bool is_manager(void)
{
    return ksu_is_manager_uid(current_uid().val);
}

bool is_primary_manager(void)
{
    return unlikely(READ_ONCE(primary_manager_appid) ==
                    ksu_normalize_appid(current_uid().val));
}

bool is_uid_manager(uid_t uid)
{
    return ksu_is_manager_uid(uid);
}

bool ksu_is_manager_appid_valid(void)
{
    return READ_ONCE(primary_manager_appid) != KSU_INVALID_APPID;
}

uid_t ksu_get_manager_appid(void)
{
    return READ_ONCE(primary_manager_appid);
}

void ksu_register_manager(uid_t uid, u8 signature_index)
{
    struct ksu_manager_node *node, *existing;
    unsigned long flags;

    node = alloc_manager_node(uid, signature_index, GFP_KERNEL);
    if (!node)
        return;

    spin_lock_irqsave(&ksu_manager_list_lock, flags);
    list_for_each_entry(existing, &ksu_manager_appid_list, list) {
        if (existing->appid == node->appid) {
            spin_unlock_irqrestore(&ksu_manager_list_lock, flags);
            kfree(node);
            return;
        }
    }
    list_add_tail_rcu(&node->list, &ksu_manager_appid_list);
    if (signature_index == KSU_SIGNATURE_INDEX_PRIMARY)
        WRITE_ONCE(primary_manager_appid, node->appid);
    spin_unlock_irqrestore(&ksu_manager_list_lock, flags);
}

void ksu_unregister_manager(uid_t uid)
{
    struct ksu_manager_node *node, *next;
    uid_t appid = ksu_normalize_appid(uid);
    unsigned long flags;

    spin_lock_irqsave(&ksu_manager_list_lock, flags);
    list_for_each_entry_safe(node, next, &ksu_manager_appid_list, list) {
        if (node->appid != appid)
            continue;
        if (node->signature_index == KSU_SIGNATURE_INDEX_PRIMARY)
            WRITE_ONCE(primary_manager_appid, KSU_INVALID_APPID);
        list_del_rcu(&node->list);
        kfree_rcu(node, rcu);
    }
    spin_unlock_irqrestore(&ksu_manager_list_lock, flags);
}

void ksu_unregister_manager_by_signature_index(u8 signature_index)
{
    struct ksu_manager_node *node, *next;
    unsigned long flags;

    spin_lock_irqsave(&ksu_manager_list_lock, flags);
    list_for_each_entry_safe(node, next, &ksu_manager_appid_list, list) {
        if (node->signature_index != signature_index)
            continue;
        if (signature_index == KSU_SIGNATURE_INDEX_PRIMARY)
            WRITE_ONCE(primary_manager_appid, KSU_INVALID_APPID);
        list_del_rcu(&node->list);
        kfree_rcu(node, rcu);
    }
    spin_unlock_irqrestore(&ksu_manager_list_lock, flags);
}

void ksu_set_manager_appid(uid_t appid)
{
    appid = ksu_normalize_appid(appid);
    if (!ksu_is_normal_appid(appid))
        return;
    ksu_unregister_manager_by_signature_index(KSU_SIGNATURE_INDEX_PRIMARY);
    ksu_register_manager(appid, KSU_SIGNATURE_INDEX_PRIMARY);
}

void ksu_invalidate_manager_uid(void)
{
    ksu_unregister_manager_by_signature_index(KSU_SIGNATURE_INDEX_PRIMARY);
}

int ksu_get_manager_signature_index_by_appid(uid_t appid)
{
    struct ksu_manager_node *node;
    int signature_index = -ENODATA;

    appid = ksu_normalize_appid(appid);
    rcu_read_lock();
    list_for_each_entry_rcu(node, &ksu_manager_appid_list, list) {
        if (node->appid == appid) {
            signature_index = node->signature_index;
            break;
        }
    }
    rcu_read_unlock();
    return signature_index;
}

bool ksu_has_manager(void)
{
    return !list_empty_careful(&ksu_manager_appid_list);
}

int ksu_replace_managers(const struct ksu_manager_entry *entries, u16 count)
{
    LIST_HEAD(replacement);
    struct ksu_manager_node *node, *next, *existing;
    uid_t next_primary = KSU_INVALID_APPID;
    unsigned long flags;
    u16 index;

    if (count > KSU_LAST_APPLICATION_APPID - KSU_FIRST_APPLICATION_APPID + 1)
        return -E2BIG;

    for (index = 0; index < count; index++) {
        bool duplicate = false;

        node = alloc_manager_node(entries[index].uid, entries[index].signature_index,
                                  GFP_KERNEL);
        if (!node)
            goto nomem;
        list_for_each_entry(existing, &replacement, list) {
            if (existing->appid == node->appid) {
                duplicate = true;
                break;
            }
        }
        if (duplicate) {
            kfree(node);
            continue;
        }
        if (node->signature_index == KSU_SIGNATURE_INDEX_PRIMARY)
            next_primary = node->appid;
        list_add_tail(&node->list, &replacement);
    }

    spin_lock_irqsave(&ksu_manager_list_lock, flags);
    list_for_each_entry_safe(node, next, &ksu_manager_appid_list, list) {
        list_del_rcu(&node->list);
        kfree_rcu(node, rcu);
    }
    list_for_each_entry_safe(node, next, &replacement, list) {
        list_del(&node->list);
        list_add_tail_rcu(&node->list, &ksu_manager_appid_list);
    }
    WRITE_ONCE(primary_manager_appid, next_primary);
    spin_unlock_irqrestore(&ksu_manager_list_lock, flags);
    return 0;

nomem:
    list_for_each_entry_safe(node, next, &replacement, list) {
        list_del(&node->list);
        kfree(node);
    }
    return -ENOMEM;
}

int ksu_handle_get_managers_cmd(struct ksu_get_managers_cmd __user *arg,
                                struct ksu_get_managers_cmd *cmd)
{
    struct ksu_manager_entry *entries = NULL;
    struct ksu_manager_node *node;
    u16 capacity = cmd->count;
    u16 copied = 0;
    u16 total = 0;

    if (capacity > KSU_LAST_APPLICATION_APPID - KSU_FIRST_APPLICATION_APPID + 1)
        return -E2BIG;
    if (capacity) {
        entries = kmalloc_array(capacity, sizeof(*entries), GFP_KERNEL);
        if (!entries)
            return -ENOMEM;
    }

    rcu_read_lock();
    list_for_each_entry_rcu(node, &ksu_manager_appid_list, list) {
        if (copied < capacity) {
            entries[copied].uid = node->appid;
            entries[copied].signature_index = node->signature_index;
            copied++;
        }
        if (total != USHRT_MAX)
            total++;
    }
    rcu_read_unlock();

    cmd->count = copied;
    cmd->total_count = total;
    if (copied && copy_to_user((char __user *)arg + sizeof(*cmd), entries,
                               copied * sizeof(*entries))) {
        kfree(entries);
        return -EFAULT;
    }
    kfree(entries);
    return 0;
}

void ksu_manager_registry_exit(void)
{
    (void)ksu_replace_managers(NULL, 0);
    synchronize_rcu();
}
