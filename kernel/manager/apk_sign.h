#ifndef __KSU_H_APK_V2_SIGN
#define __KSU_H_APK_V2_SIGN

#include <linux/types.h>

bool is_manager_apk(char *path);
int get_pkg_from_apk_path(char *pkg, const char *path);
bool ksu_apk_matches_v2_signature(const char *path, unsigned expected_size, const char *expected_sha256);

#endif
