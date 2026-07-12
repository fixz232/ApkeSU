#include "feature/hook_status.h"

#include <linux/init.h>

#include "hook/lsm_hook.h"
#include "hook/syscall_hook_manager.h"
#include "klog.h" // IWYU pragma: keep
#include "policy/feature.h"
#include "uapi/feature.h"

static int hook_status_feature_get(u64 *value)
{
    u64 status = 0;

    if (ksu_syscall_hook_manager_is_ready())
        status |= KSU_HOOK_STATUS_MANAGER_READY;
    if (ksu_syscall_hook_manager_has_syscall_handlers())
        status |= KSU_HOOK_STATUS_SYSCALL_HANDLERS;
    if (ksu_syscall_hook_manager_tracepoint_registered())
        status |= KSU_HOOK_STATUS_TRACEPOINT;
    if (ksu_syscall_hook_manager_kretprobes_registered())
        status |= KSU_HOOK_STATUS_KRETPROBES;
    if (ksu_lsm_hook_active_count() > 0)
        status |= KSU_HOOK_STATUS_LSM;

    *value = status;
    return 0;
}

static const struct ksu_feature_handler hook_status_handler = {
    .feature_id = KSU_FEATURE_HOOK_STATUS,
    .name = "hook_status",
    .get_handler = hook_status_feature_get,
};

void __init ksu_hook_status_init(void)
{
    if (ksu_register_feature_handler(&hook_status_handler))
        pr_err("hook_status: failed to register feature handler\n");
}

void __exit ksu_hook_status_exit(void)
{
    ksu_unregister_feature_handler(KSU_FEATURE_HOOK_STATUS);
}
