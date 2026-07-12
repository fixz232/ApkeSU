#ifndef __KSU_H_HOOK_MANAGER
#define __KSU_H_HOOK_MANAGER

#include <asm/ptrace.h>
#include <linux/types.h>

// Hook manager initialization and cleanup
void ksu_syscall_hook_manager_init(void);
void ksu_syscall_hook_manager_exit(void);

/* Runtime status for the read-only Hook status feature. */
bool ksu_syscall_hook_manager_is_ready(void);
bool ksu_syscall_hook_manager_has_syscall_handlers(void);
bool ksu_syscall_hook_manager_tracepoint_registered(void);
bool ksu_syscall_hook_manager_kretprobes_registered(void);

#endif
