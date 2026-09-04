// SPDX-License-Identifier: GPL-2.0
#include <linux/err.h>
#include <linux/fs.h>
#include <linux/kernel.h>
#include <linux/list.h>
#include <linux/moduleparam.h>
#include <linux/mutex.h>
#include <linux/pid.h>
#include <linux/sched/task.h>
#include <linux/slab.h>
#include <linux/spinlock.h>
#include <linux/string.h>
#include <linux/task_work.h>
#include <linux/types.h>
#include <linux/version.h>

#include "feature/dynamic_manager.h"
#include "klog.h" // IWYU pragma: keep
#include "manager/apk_sign.h"
#include "manager/manager_identity.h"
#include "manager/throne_tracker.h"
#include "policy/allowlist.h"

#define SYSTEM_PACKAGES_LIST_PATH "/data/system/packages.list"
#define DATA_PATH_LEN 384

static int ksu_manager_appid_param = KSU_INVALID_APPID;
static DEFINE_MUTEX(throne_scan_lock);
static DEFINE_SPINLOCK(throne_request_lock);
static unsigned int throne_pending_flags;
static bool throne_scan_pending;
static bool throne_scan_queued;

struct uid_data {
    struct list_head list;
    u32 uid;
    char package[KSU_MAX_PACKAGE_NAME];
};

struct data_path {
    char dirpath[DATA_PATH_LEN];
    int depth;
    struct list_head list;
};

struct manager_scan {
    struct ksu_manager_entry *entries;
    u16 count;
    u16 capacity;
    bool complete;
};

struct manager_dir_context {
    struct dir_context ctx;
    struct list_head *data_path_list;
    const char *parent_dir;
    char *candidate_path;
    int depth;
    bool *complete;
};

struct throne_scan_request {
    struct callback_head callback;
};

#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 1, 0)
#define FILLDIR_RETURN_TYPE bool
#define FILLDIR_ACTOR_CONTINUE true
#define FILLDIR_ACTOR_STOP false
#else
#define FILLDIR_RETURN_TYPE int
#define FILLDIR_ACTOR_CONTINUE 0
#define FILLDIR_ACTOR_STOP -EINVAL
#endif

static int set_manager_appid_param(const char *val,
                                   const struct kernel_param *kp)
{
    int appid;
    int ret = kstrtoint(val, 0, &appid);

    if (ret)
        return ret;
    if (!ksu_is_normal_appid((uid_t)appid)) {
        pr_err("manager_appid rejected invalid appid %d\n", appid);
        return -EINVAL;
    }

    *(int *)kp->arg = appid;
    ksu_set_manager_appid((uid_t)appid);
    pr_info("manager appid set from module param: %d\n", appid);
    return 0;
}

static const struct kernel_param_ops manager_appid_param_ops = {
    .set = set_manager_appid_param,
    .get = param_get_int,
};

module_param_cb(manager_appid, &manager_appid_param_ops,
                &ksu_manager_appid_param, S_IRUSR | S_IWUSR);

static bool append_manager(struct manager_scan *scan, uid_t uid,
                           u8 signature_index)
{
    struct ksu_manager_entry *entries;
    uid_t appid = ksu_normalize_appid(uid);
    u16 index;
    u16 capacity;

    if (!ksu_is_normal_appid(appid))
        return true;
    for (index = 0; index < scan->count; index++) {
        if (scan->entries[index].uid != appid)
            continue;
        if (signature_index == KSU_SIGNATURE_INDEX_PRIMARY)
            scan->entries[index].signature_index = signature_index;
        return true;
    }

    if (scan->count == scan->capacity) {
        capacity = scan->capacity ? scan->capacity * 2 : 4;
        if (capacity > KSU_LAST_APPLICATION_APPID -
                           KSU_FIRST_APPLICATION_APPID + 1)
            capacity = KSU_LAST_APPLICATION_APPID -
                       KSU_FIRST_APPLICATION_APPID + 1;
        if (capacity <= scan->capacity)
            return false;
        entries = krealloc(scan->entries,
                           sizeof(*entries) * capacity, GFP_KERNEL);
        if (!entries)
            return false;
        scan->entries = entries;
        scan->capacity = capacity;
    }

    scan->entries[scan->count].uid = appid;
    scan->entries[scan->count].signature_index = signature_index;
    scan->count++;
    return true;
}

static bool crown_manager(const char *apk, struct list_head *uid_list,
                          u8 signature_index, struct manager_scan *scan)
{
    struct uid_data *entry;
    char package[KSU_MAX_PACKAGE_NAME];

    if (get_pkg_from_apk_path(package, apk) < 0) {
        pr_err("Failed to get package name from apk path: %s\n", apk);
        return false;
    }

    list_for_each_entry(entry, uid_list, list) {
        if (strncmp(entry->package, package, KSU_MAX_PACKAGE_NAME))
            continue;
        if (!append_manager(scan, entry->uid, signature_index))
            return false;
        pr_info("Crowning manager: %s(uid=%u, signature_index=%u)\n",
                package, entry->uid, signature_index);
        return true;
    }

    pr_warn("Manager APK %s has no matching packages.list entry\n", package);
    return true;
}

static FILLDIR_RETURN_TYPE manager_actor(struct dir_context *ctx,
                                         const char *name, int namelen,
                                         loff_t off, u64 ino,
                                         unsigned int d_type)
{
    struct manager_dir_context *scan_ctx =
        container_of(ctx, struct manager_dir_context, ctx);
    char path[DATA_PATH_LEN];

    (void)off;
    (void)ino;
    if (!scan_ctx || !*scan_ctx->complete)
        return FILLDIR_ACTOR_STOP;
    if ((namelen == 1 && name[0] == '.') ||
        (namelen == 2 && name[0] == '.' && name[1] == '.'))
        return FILLDIR_ACTOR_CONTINUE;
    if (d_type == DT_DIR && namelen >= 8 &&
        !strncmp(name, "vmdl", 4) &&
        !strncmp(name + namelen - 4, ".tmp", 4))
        return FILLDIR_ACTOR_CONTINUE;
    if (snprintf(path, sizeof(path), "%s/%.*s", scan_ctx->parent_dir,
                 namelen, name) >= sizeof(path)) {
        *scan_ctx->complete = false;
        return FILLDIR_ACTOR_STOP;
    }

    if (d_type == DT_DIR && scan_ctx->depth > 0) {
        struct data_path *data = kzalloc(sizeof(*data), GFP_KERNEL);

        if (!data) {
            *scan_ctx->complete = false;
            return FILLDIR_ACTOR_STOP;
        }
        strscpy(data->dirpath, path, sizeof(data->dirpath));
        data->depth = scan_ctx->depth - 1;
        list_add_tail(&data->list, scan_ctx->data_path_list);
    } else if (namelen == 8 && !memcmp(name, "base.apk", 8)) {
        strscpy(scan_ctx->candidate_path, path, DATA_PATH_LEN);
    }
    return FILLDIR_ACTOR_CONTINUE;
}

static bool search_managers(const char *root, int depth,
                            struct list_head *uid_list,
                            struct manager_scan *scan)
{
    LIST_HEAD(paths);
    struct data_path *path, *next;
    unsigned long data_app_magic = 0;

    path = kzalloc(sizeof(*path), GFP_KERNEL);
    if (!path)
        return false;
    strscpy(path->dirpath, root, sizeof(path->dirpath));
    path->depth = depth;
    list_add_tail(&path->list, &paths);
    scan->complete = true;

    while (!list_empty(&paths) && scan->complete) {
        char candidate_path[DATA_PATH_LEN] = { 0 };
        struct manager_dir_context context;
        struct file *file;
        int iterate_result;

        path = list_first_entry(&paths, struct data_path, list);
        list_del(&path->list);
        context.ctx.actor = manager_actor;
        context.data_path_list = &paths;
        context.parent_dir = path->dirpath;
        context.candidate_path = candidate_path;
        context.depth = path->depth;
        context.complete = &scan->complete;

        file = filp_open(path->dirpath, O_RDONLY | O_NOFOLLOW, 0);
        if (IS_ERR(file)) {
            pr_err("Failed to open directory: %s, err: %ld\n",
                   path->dirpath, PTR_ERR(file));
            scan->complete = false;
            kfree(path);
            break;
        }
        if (!data_app_magic)
            data_app_magic = file->f_inode->i_sb->s_magic;
        if (!data_app_magic || file->f_inode->i_sb->s_magic != data_app_magic) {
            scan->complete = false;
            filp_close(file, NULL);
            kfree(path);
            break;
        }

        iterate_result = iterate_dir(file, &context.ctx);
        filp_close(file, NULL);
        kfree(path);
        if (iterate_result < 0 || !scan->complete) {
            scan->complete = false;
            break;
        }

        if (candidate_path[0]) {
            u8 signature_index = 0;

            if (is_manager_apk(candidate_path, &signature_index) &&
                !crown_manager(candidate_path, uid_list, signature_index, scan)) {
                scan->complete = false;
                break;
            }
        }
    }

    list_for_each_entry_safe(path, next, &paths, list) {
        list_del(&path->list);
        kfree(path);
    }
    return scan->complete;
}

static bool is_uid_exist(uid_t uid, char *package, void *data)
{
    struct list_head *list = data;
    struct uid_data *entry;

    list_for_each_entry(entry, list, list) {
        if (ksu_normalize_appid(entry->uid) == ksu_normalize_appid(uid) &&
            !strncmp(entry->package, package, KSU_MAX_PACKAGE_NAME))
            return true;
    }
    return false;
}

static bool load_packages_list(struct list_head *uid_list)
{
    struct file *file;
    char chr = 0;
    loff_t pos = 0;
    loff_t line_start = 0;
    char buf[KSU_MAX_PACKAGE_NAME];
    bool valid = true;

    file = filp_open(SYSTEM_PACKAGES_LIST_PATH, O_RDONLY, 0);
    if (IS_ERR(file)) {
        pr_err("%s: open " SYSTEM_PACKAGES_LIST_PATH " failed: %ld\n",
               __func__, PTR_ERR(file));
        return false;
    }

    for (;;) {
        struct uid_data *data;
        char *temporary;
        char *package;
        char *uid;
        u32 parsed_uid;
        ssize_t count = kernel_read(file, &chr, sizeof(chr), &pos);

        if (!count) {
            if (pos != line_start)
                valid = false;
            break;
        }
        if (count != sizeof(chr)) {
            valid = false;
            break;
        }
        if (chr != '\n')
            continue;

        count = kernel_read(file, buf, sizeof(buf) - 1, &line_start);
        if (count <= 0) {
            valid = false;
            break;
        }
        buf[count] = '\0';
        temporary = buf;
        package = strsep(&temporary, " ");
        uid = strsep(&temporary, " ");
        if (!package || !uid || kstrtou32(uid, 10, &parsed_uid)) {
            valid = false;
            break;
        }

        data = kzalloc(sizeof(*data), GFP_KERNEL);
        if (!data) {
            valid = false;
            break;
        }
        data->uid = parsed_uid;
        strscpy(data->package, package, sizeof(data->package));
        list_add_tail(&data->list, uid_list);
        line_start = pos;
    }
    filp_close(file, NULL);
    return valid;
}

static void do_track_throne(unsigned int flags)
{
    LIST_HEAD(uid_list);
    struct uid_data *entry, *next;
    struct manager_scan scan = { 0 };
    bool package_list_valid;
    u64 dynamic_manager_generation;

    mutex_lock(&throne_scan_lock);
    dynamic_manager_generation = ksu_get_dynamic_manager_generation();
    package_list_valid = load_packages_list(&uid_list);
    if (!package_list_valid)
        goto out;

    if (flags & TRACK_THRONE_FORCE_SEARCH_MGR) {
        pr_info("Searching for manager(s)...\n");
        if (search_managers("/data/app", 2, &uid_list, &scan)) {
            int ret;

            if (dynamic_manager_generation !=
                ksu_get_dynamic_manager_generation()) {
                pr_warn("Dynamic manager changed during scan; keeping previous registry\n");
                goto prune;
            }

            ret = ksu_replace_managers(scan.entries, scan.count);

            if (ret)
                pr_err("Failed to replace manager registry: %d\n", ret);
            else if (dynamic_manager_generation !=
                     ksu_get_dynamic_manager_generation()) {
                ksu_unregister_manager_by_signature_index(
                    KSU_SIGNATURE_INDEX_DYNAMIC_MANAGER);
                pr_warn("Dynamic manager changed while publishing scan; revoked stale entries\n");
            } else
                pr_info("Manager search finished: %u manager(s)\n", scan.count);
        } else {
            pr_warn("Manager search incomplete; keeping previous registry\n");
        }
    }

prune:
    ksu_prune_allowlist(is_uid_exist, &uid_list);
out:
    kfree(scan.entries);
    list_for_each_entry_safe(entry, next, &uid_list, list) {
        list_del(&entry->list);
        kfree(entry);
    }
    mutex_unlock(&throne_scan_lock);
}

static void run_deferred_throne_scan(struct callback_head *callback)
{
    struct throne_scan_request *request =
        container_of(callback, struct throne_scan_request, callback);
    unsigned long irq_flags;
    unsigned int flags;
    bool requeue;

    kfree(request);

    spin_lock_irqsave(&throne_request_lock, irq_flags);
    flags = throne_pending_flags;
    throne_pending_flags = 0;
    throne_scan_pending = false;
    spin_unlock_irqrestore(&throne_request_lock, irq_flags);

    do_track_throne(flags);

    spin_lock_irqsave(&throne_request_lock, irq_flags);
    requeue = throne_scan_pending;
    if (!requeue)
        throne_scan_queued = false;
    spin_unlock_irqrestore(&throne_request_lock, irq_flags);

    if (requeue) {
        struct task_struct *init_task;
        int ret;

        rcu_read_lock();
        init_task = get_pid_task(find_vpid(1), PIDTYPE_PID);
        rcu_read_unlock();
        if (!init_task) {
            pr_err("Unable to requeue manager scan: init task is unavailable\n");
            goto requeue_failed;
        }

        request = kzalloc(sizeof(*request), GFP_ATOMIC);
        if (!request) {
            pr_err("Unable to requeue manager scan: allocation failed\n");
            put_task_struct(init_task);
            goto requeue_failed;
        }

        request->callback.func = run_deferred_throne_scan;
        ret = task_work_add(init_task, &request->callback, TWA_RESUME);
        put_task_struct(init_task);
        if (!ret)
            return;

        pr_warn("Unable to requeue manager scan: task_work_add failed: %d\n",
                ret);
        kfree(request);

requeue_failed:
        spin_lock_irqsave(&throne_request_lock, irq_flags);
        throne_scan_queued = false;
        spin_unlock_irqrestore(&throne_request_lock, irq_flags);
    }
}

void track_throne(unsigned int flags)
{
    struct throne_scan_request *request;
    struct task_struct *init_task;
    unsigned long irq_flags;
    int ret;

    if (flags & TRACK_THRONE_FORCE_SYNCHRONOUS) {
        do_track_throne(flags);
        return;
    }

    spin_lock_irqsave(&throne_request_lock, irq_flags);
    throne_pending_flags |= flags;
    throne_scan_pending = true;
    if (throne_scan_queued) {
        spin_unlock_irqrestore(&throne_request_lock, irq_flags);
        return;
    }
    throne_scan_queued = true;
    spin_unlock_irqrestore(&throne_request_lock, irq_flags);

    /* fsnotify may call this while filesystem locks are held. */
    request = kzalloc(sizeof(*request), GFP_ATOMIC);
    if (!request) {
        pr_err("Unable to defer manager scan: allocation failed\n");
        goto queue_failed;
    }

    request->callback.func = run_deferred_throne_scan;
    rcu_read_lock();
    init_task = get_pid_task(find_vpid(1), PIDTYPE_PID);
    rcu_read_unlock();
    if (!init_task) {
        pr_err("Unable to defer manager scan: init task is unavailable\n");
        kfree(request);
        goto queue_failed;
    }

    ret = task_work_add(init_task, &request->callback, TWA_RESUME);
    put_task_struct(init_task);
    if (!ret)
        return;

    pr_warn("Unable to defer manager scan: task_work_add failed: %d\n", ret);
    kfree(request);

queue_failed:
    spin_lock_irqsave(&throne_request_lock, irq_flags);
    throne_scan_queued = false;
    spin_unlock_irqrestore(&throne_request_lock, irq_flags);
}

void __init ksu_throne_tracker_init(void)
{
    if (ksu_manager_appid_param >= 0 &&
        ksu_is_normal_appid((uid_t)ksu_manager_appid_param)) {
        ksu_set_manager_appid(ksu_manager_appid_param);
        pr_info("manager appid initialized from module param: %d\n",
                ksu_manager_appid_param);
    }
}

void __exit ksu_throne_tracker_exit(void)
{
    mutex_lock(&throne_scan_lock);
    ksu_manager_registry_exit();
    mutex_unlock(&throne_scan_lock);
}
