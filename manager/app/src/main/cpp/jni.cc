#include <jni.h>

#include <sys/prctl.h>
#include <linux/capability.h>
#include <pwd.h>
#include <unistd.h>
#include <sys/wait.h>
#include <cstdlib>

#include <android/log.h>
#include <cstring>

#include "ksu.h"
#include "logging.h"

static bool copyJStringToFixed(JNIEnv *env, jstring src, char *dst, size_t dst_size) {
    if (!src || !dst || dst_size == 0) {
        return false;
    }

    jsize len = env->GetStringUTFLength(src);
    if (len < 0 || static_cast<size_t>(len) >= dst_size) {
        return false;
    }

    auto chars = env->GetStringUTFChars(src, nullptr);
    if (!chars) {
        return false;
    }

    memcpy(dst, chars, static_cast<size_t>(len) + 1);
    env->ReleaseStringUTFChars(src, chars);
    return true;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_weishu_kernelsu_Natives_getVersion(JNIEnv *env, jobject) {
    int version = get_version();
    if (version > 0) {
        return version;
    }
    // try legacy method as fallback
    return legacy_get_info().first;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_weishu_kernelsu_Natives_refreshInfo(JNIEnv *env, jobject) {
    refresh_info();
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_weishu_kernelsu_Natives_getKernelUAPIVersion(JNIEnv *env, jobject) {
    return get_kernel_uapi_version();
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_weishu_kernelsu_Natives_getManagerUAPIVersion(JNIEnv *env, jobject) {
    return get_manager_uapi_version();
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_weishu_kernelsu_Natives_getSuperuserCount(JNIEnv *env, jobject) {
    struct ksu_new_get_allow_list_cmd cmd = {
        .count = 0
    };
    bool result = get_allow_list(&cmd);
    return result ? cmd.total_count : 0;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_me_weishu_kernelsu_Natives_getAllowList(JNIEnv *env, jobject) {
    struct ksu_new_get_allow_list_cmd header = {
        .count = 0
    };

    if (!get_allow_list(&header) || header.total_count == 0) {
        return env->NewIntArray(0);
    }

    size_t cmd_size = sizeof(ksu_new_get_allow_list_cmd) + sizeof(__u32) * header.total_count;
    auto *cmd = static_cast<ksu_new_get_allow_list_cmd *>(calloc(1, cmd_size));
    if (!cmd) {
        return env->NewIntArray(0);
    }

    cmd->count = header.total_count;
    if (!get_allow_list(cmd)) {
        free(cmd);
        return env->NewIntArray(0);
    }

    jintArray result = env->NewIntArray(cmd->count);
    if (result) {
        env->SetIntArrayRegion(result, 0, cmd->count, reinterpret_cast<jint *>(cmd->uids));
    }
    free(cmd);
    return result ? result : env->NewIntArray(0);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isSafeMode(JNIEnv *env, jclass clazz) {
    return is_safe_mode();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isLkmMode(JNIEnv *env, jclass clazz) {
    return is_lkm_mode();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isLateLoadMode(JNIEnv *env, jclass clazz) {
    return is_late_load_mode();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isManager(JNIEnv *env, jclass clazz) {
    return is_manager();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isPrBuild(JNIEnv *env, jclass clazz) {
    return is_pr_build();
}

static void fillIntArray(JNIEnv *env, jobject list, int *data, int count) {
    auto cls = env->GetObjectClass(list);
    auto add = env->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");
    auto integerCls = env->FindClass("java/lang/Integer");
    auto constructor = env->GetMethodID(integerCls, "<init>", "(I)V");
    for (int i = 0; i < count; ++i) {
        auto integer = env->NewObject(integerCls, constructor, data[i]);
        env->CallBooleanMethod(list, add, integer);
    }
}

static void addIntToList(JNIEnv *env, jobject list, int ele) {
    auto cls = env->GetObjectClass(list);
    auto add = env->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");
    auto integerCls = env->FindClass("java/lang/Integer");
    auto constructor = env->GetMethodID(integerCls, "<init>", "(I)V");
    auto integer = env->NewObject(integerCls, constructor, ele);
    env->CallBooleanMethod(list, add, integer);
}

static uint64_t capListToBits(JNIEnv *env, jobject list) {
    auto cls = env->GetObjectClass(list);
    auto get = env->GetMethodID(cls, "get", "(I)Ljava/lang/Object;");
    auto size = env->GetMethodID(cls, "size", "()I");
    auto listSize = env->CallIntMethod(list, size);
    auto integerCls = env->FindClass("java/lang/Integer");
    auto intValue = env->GetMethodID(integerCls, "intValue", "()I");
    uint64_t result = 0;
    for (int i = 0; i < listSize; ++i) {
        auto integer = env->CallObjectMethod(list, get, i);
        int data = env->CallIntMethod(integer, intValue);

        if (cap_valid(data)) {
            result |= (1ULL << data);
        }
    }

    return result;
}

static int getListSize(JNIEnv *env, jobject list) {
    auto cls = env->GetObjectClass(list);
    auto size = env->GetMethodID(cls, "size", "()I");
    return env->CallIntMethod(list, size);
}

static void fillArrayWithList(JNIEnv *env, jobject list, int *data, int count) {
    auto cls = env->GetObjectClass(list);
    auto get = env->GetMethodID(cls, "get", "(I)Ljava/lang/Object;");
    auto integerCls = env->FindClass("java/lang/Integer");
    auto intValue = env->GetMethodID(integerCls, "intValue", "()I");
    for (int i = 0; i < count; ++i) {
        auto integer = env->CallObjectMethod(list, get, i);
        data[i] = env->CallIntMethod(integer, intValue);
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_weishu_kernelsu_Natives_getAppProfile(JNIEnv *env, jobject, jstring pkg, jint uid) {
    app_profile profile = {};
    profile.version = KSU_APP_PROFILE_VER;

    if (!copyJStringToFixed(env, pkg, profile.key, sizeof(profile.key))) {
        return nullptr;
    }
    profile.curr_uid = uid;

    bool useDefaultProfile = get_app_profile(&profile) != 0;

    auto cls = env->FindClass("me/weishu/kernelsu/Natives$Profile");
    auto constructor = env->GetMethodID(cls, "<init>", "()V");
    auto obj = env->NewObject(cls, constructor);
    auto keyField = env->GetFieldID(cls, "name", "Ljava/lang/String;");
    auto currentUidField = env->GetFieldID(cls, "currentUid", "I");
    auto allowSuField = env->GetFieldID(cls, "allowSu", "Z");

    auto rootUseDefaultField = env->GetFieldID(cls, "rootUseDefault", "Z");
    auto rootTemplateField = env->GetFieldID(cls, "rootTemplate", "Ljava/lang/String;");

    auto uidField = env->GetFieldID(cls, "uid", "I");
    auto gidField = env->GetFieldID(cls, "gid", "I");
    auto groupsField = env->GetFieldID(cls, "groups", "Ljava/util/List;");
    auto capabilitiesField = env->GetFieldID(cls, "capabilities", "Ljava/util/List;");
    auto domainField = env->GetFieldID(cls, "context", "Ljava/lang/String;");
    auto namespacesField = env->GetFieldID(cls, "namespace", "I");
    jfieldID flagsField = env->GetFieldID(cls, "flags", "J");

    auto nonRootUseDefaultField = env->GetFieldID(cls, "nonRootUseDefault", "Z");
    auto umountModulesField = env->GetFieldID(cls, "umountModules", "Z");

    env->SetObjectField(obj, keyField, env->NewStringUTF(profile.key));
    env->SetIntField(obj, currentUidField, profile.curr_uid);

    if (useDefaultProfile) {
        // no profile found, so just use default profile:
        // don't allow root and use default profile!
        LOGD("use default profile for: %s, %d", profile.key, uid);

        // allow_su = false
        // non root use default = true
        env->SetBooleanField(obj, allowSuField, false);
        env->SetBooleanField(obj, nonRootUseDefaultField, true);

        return obj;
    }

    auto allowSu = profile.allow_su;

    if (allowSu) {
        env->SetBooleanField(obj, rootUseDefaultField, (jboolean) profile.rp_config.use_default);
        if (strlen(profile.rp_config.template_name) > 0) {
            env->SetObjectField(obj, rootTemplateField,
                    env->NewStringUTF(profile.rp_config.template_name));
        }

        env->SetIntField(obj, uidField, profile.rp_config.profile.uid);
        env->SetIntField(obj, gidField, profile.rp_config.profile.gid);

        jobject groupList = env->GetObjectField(obj, groupsField);
        int groupCount = profile.rp_config.profile.groups_count;
        if (groupCount > KSU_MAX_GROUPS) {
            LOGD("kernel group count too large: %d???", groupCount);
            groupCount = KSU_MAX_GROUPS;
        }
        fillIntArray(env, groupList, profile.rp_config.profile.groups, groupCount);

        jobject capList = env->GetObjectField(obj, capabilitiesField);
        for (int i = 0; i <= CAP_LAST_CAP; i++) {
            if (profile.rp_config.profile.capabilities.effective & (1ULL << i)) {
                addIntToList(env, capList, i);
            }
        }

        env->SetObjectField(obj, domainField,
                env->NewStringUTF(profile.rp_config.profile.selinux_domain));
        env->SetIntField(obj, namespacesField, profile.rp_config.profile.namespaces);
        env->SetBooleanField(obj, allowSuField, profile.allow_su);
        env->SetLongField(obj, flagsField, (jlong) profile.rp_config.profile.flags);
    } else {
        env->SetBooleanField(obj, nonRootUseDefaultField,
                (jboolean) profile.nrp_config.use_default);
        env->SetBooleanField(obj, umountModulesField, profile.nrp_config.profile.umount_modules);
    }

    return obj;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_setAppProfile(JNIEnv *env, jobject clazz, jobject profile) {
    if (!profile) {
        return false;
    }

    auto cls = env->FindClass("me/weishu/kernelsu/Natives$Profile");

    auto keyField = env->GetFieldID(cls, "name", "Ljava/lang/String;");
    auto currentUidField = env->GetFieldID(cls, "currentUid", "I");
    auto allowSuField = env->GetFieldID(cls, "allowSu", "Z");

    auto rootUseDefaultField = env->GetFieldID(cls, "rootUseDefault", "Z");
    auto rootTemplateField = env->GetFieldID(cls, "rootTemplate", "Ljava/lang/String;");

    auto uidField = env->GetFieldID(cls, "uid", "I");
    auto gidField = env->GetFieldID(cls, "gid", "I");
    auto groupsField = env->GetFieldID(cls, "groups", "Ljava/util/List;");
    auto capabilitiesField = env->GetFieldID(cls, "capabilities", "Ljava/util/List;");
    auto domainField = env->GetFieldID(cls, "context", "Ljava/lang/String;");
    auto namespacesField = env->GetFieldID(cls, "namespace", "I");
    jfieldID flagsField = env->GetFieldID(cls, "flags", "J");

    auto nonRootUseDefaultField = env->GetFieldID(cls, "nonRootUseDefault", "Z");
    auto umountModulesField = env->GetFieldID(cls, "umountModules", "Z");

    auto key = env->GetObjectField(profile, keyField);
    if (!key) {
        return false;
    }

    auto currentUid = env->GetIntField(profile, currentUidField);

    auto uid = env->GetIntField(profile, uidField);
    auto gid = env->GetIntField(profile, gidField);
    auto groups = env->GetObjectField(profile, groupsField);
    auto capabilities = env->GetObjectField(profile, capabilitiesField);
    auto domain = env->GetObjectField(profile, domainField);
    auto allowSu = env->GetBooleanField(profile, allowSuField);
    auto umountModules = env->GetBooleanField(profile, umountModulesField);

    app_profile p = {};
    p.version = KSU_APP_PROFILE_VER;

    if (!copyJStringToFixed(env, (jstring) key, p.key, sizeof(p.key))) {
        return false;
    }
    p.allow_su = allowSu;
    p.curr_uid = currentUid;

    if (allowSu) {
        p.rp_config.use_default = env->GetBooleanField(profile, rootUseDefaultField);
        auto templateName = env->GetObjectField(profile, rootTemplateField);
        if (templateName) {
            if (!copyJStringToFixed(env, (jstring) templateName, p.rp_config.template_name,
                                    sizeof(p.rp_config.template_name))) {
                return false;
            }
        }

        p.rp_config.profile.uid = uid;
        p.rp_config.profile.gid = gid;

        int groups_count = getListSize(env, groups);
        if (groups_count > KSU_MAX_GROUPS) {
            LOGD("groups count too large: %d", groups_count);
            return false;
        }
        p.rp_config.profile.groups_count = groups_count;
        fillArrayWithList(env, groups, p.rp_config.profile.groups, groups_count);

        p.rp_config.profile.capabilities.effective = capListToBits(env, capabilities);

        if (!copyJStringToFixed(env, (jstring) domain, p.rp_config.profile.selinux_domain,
                                sizeof(p.rp_config.profile.selinux_domain))) {
            return false;
        }

        p.rp_config.profile.namespaces = env->GetIntField(profile, namespacesField);

        p.rp_config.profile.flags = env->GetLongField(profile, flagsField);
    } else {
        p.nrp_config.use_default = env->GetBooleanField(profile, nonRootUseDefaultField);
        p.nrp_config.profile.umount_modules = umountModules;
    }

    return set_app_profile(&p);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_uidShouldUmount(JNIEnv *env, jobject thiz, jint uid) {
    return uid_should_umount(uid);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isSuEnabled(JNIEnv *env, jobject thiz) {
    return is_su_enabled();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_setSuEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    return set_su_enabled(enabled);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isKernelUmountEnabled(JNIEnv *env, jobject thiz) {
    return is_kernel_umount_enabled();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_setKernelUmountEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    return set_kernel_umount_enabled(enabled);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isSelinuxHideEnabled(JNIEnv *env, jobject thiz) {
    return is_selinux_hide_enabled();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isSelinuxHideSupported(JNIEnv *env, jobject thiz) {
    return is_selinux_hide_supported();
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_weishu_kernelsu_Natives_setSelinuxHideEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    return set_selinux_hide_enabled(enabled);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_isAvcSpoofEnabled(JNIEnv *env, jobject thiz) {
    return is_avc_spoof_enabled();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_weishu_kernelsu_Natives_setAvcSpoofEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
    return set_avc_spoof_enabled(enabled);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_me_weishu_kernelsu_Natives_getKernelHookStatus(JNIEnv *env, jobject thiz) {
    return static_cast<jlong>(get_kernel_hook_status());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_me_weishu_kernelsu_Natives_getUserName(JNIEnv *env, jobject thiz, jint uid) {
    struct passwd *pw = getpwuid((uid_t) uid);
    if (pw && pw->pw_name && pw->pw_name[0] != '\0') {
        return env->NewStringUTF(pw->pw_name);
    }
    return nullptr;
}

int fork_dont_care_and_exec_ksud(const char *path, const char *pkg, int manager_uid) {
    int pid = fork();
    if (pid < 0) {
        PLOGE("fork");
        return pid;
    } else if (pid > 0) {
        int status = 0;
        if (TEMP_FAILURE_RETRY(waitpid(pid, &status, 0)) < 0) {
            PLOGE("waitpid");
            return -1;
        }
        if (!WIFEXITED(status) || WEXITSTATUS(status) != 0) {
            LOGE("magica bootstrap child failed, status=%d", status);
        }
        return pid;
    }

    if (setgid(0) != 0) {
        PLOGE("setgid");
        _exit(1);
    }

    if (setuid(0) != 0) {
        PLOGE("setuid");
        _exit(1);
    }

    pid = fork();
    if (pid < 0) {
        PLOGE("fork 2");
        _exit(1);
    } else if (pid > 0) {
        _exit(0);
    }

    char manager_uid_arg[16];
    snprintf(manager_uid_arg, sizeof(manager_uid_arg), "%d", manager_uid);
    execl(path, "ksud", "late-load", "--magica", "5555", "--package-name", pkg,
          "--manager-uid", manager_uid_arg, nullptr);
    PLOGE("exec magica");
    _exit(1);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_weishu_kernelsu_magica_AppZygotePreload_forkDontCareAndExecKsud(JNIEnv *env, jclass clazz,
                                                                        jstring ksud_path,
                                                                        jstring pkg_name,
                                                                        jint manager_uid) {
    auto path = env->GetStringUTFChars(ksud_path, nullptr);
    auto pkg = env->GetStringUTFChars(pkg_name, nullptr);
    LOGD("executing magica %s (pkg %s, uid %d)", path, pkg, manager_uid);
    fork_dont_care_and_exec_ksud(path, pkg, manager_uid);
    env->ReleaseStringUTFChars(ksud_path, path);
    env->ReleaseStringUTFChars(pkg_name, pkg);
}
