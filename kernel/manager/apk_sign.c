#include <linux/err.h>
#include <linux/fs.h>
#include <linux/gfp.h>
#include <linux/kernel.h>
#include <linux/limits.h>
#include <linux/slab.h>
#include <linux/version.h>
#ifdef CONFIG_KSU_DEBUG
#include <linux/moduleparam.h>
#endif
#include <crypto/hash.h>
#if LINUX_VERSION_CODE >= KERNEL_VERSION(5, 11, 0)
#include <crypto/sha2.h>
#else
#include <crypto/sha.h>
#endif
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 4, 0)
#include <linux/hex.h>
#endif

#include "manager/apk_sign.h"
#include "feature/dynamic_manager.h"
#include "manager/manager_identity.h"
#include "manager/manager_sign.h"
#include "uapi/app_profile.h"
#include "klog.h" // IWYU pragma: keep

struct sdesc {
    struct shash_desc shash;
    char ctx[];
};

static struct sdesc *init_sdesc(struct crypto_shash *alg)
{
    struct sdesc *sdesc;
    int size;

    size = sizeof(struct shash_desc) + crypto_shash_descsize(alg);
    sdesc = kzalloc(size, GFP_KERNEL);
    if (!sdesc)
        return ERR_PTR(-ENOMEM);
    sdesc->shash.tfm = alg;
    return sdesc;
}

static int calc_hash(struct crypto_shash *alg, const unsigned char *data, unsigned int datalen, unsigned char *digest)
{
    struct sdesc *sdesc;
    int ret;

    sdesc = init_sdesc(alg);
    if (IS_ERR(sdesc)) {
        pr_info("can't alloc sdesc\n");
        return PTR_ERR(sdesc);
    }

    ret = crypto_shash_digest(&sdesc->shash, data, datalen, digest);
    kfree(sdesc);
    return ret;
}

static int ksu_sha256(const unsigned char *data, unsigned int datalen, unsigned char *digest)
{
    struct crypto_shash *alg;
    char *hash_alg_name = "sha256";
    int ret;

    alg = crypto_alloc_shash(hash_alg_name, 0, 0);
    if (IS_ERR(alg)) {
        pr_info("can't alloc alg %s\n", hash_alg_name);
        return PTR_ERR(alg);
    }
    ret = calc_hash(alg, data, datalen, digest);
    crypto_free_shash(alg);
    return ret;
}

static bool read_exact(struct file *fp, void *buffer, size_t size, loff_t *pos, loff_t end)
{
    if (*pos < 0 || *pos > end || size > (size_t)(end - *pos))
        return false;

    return kernel_read(fp, buffer, size, pos) == (ssize_t)size;
}

static bool read_length_prefixed_end(struct file *fp, loff_t *pos, loff_t container_end, loff_t *value_end)
{
    u32 length;

    if (!read_exact(fp, &length, sizeof(length), pos, container_end))
        return false;
    if (length > INT_MAX || length > (u64)(container_end - *pos))
        return false;

    *value_end = *pos + length;
    return true;
}

struct zip_central_directory_header {
    u32 signature;
    u16 version_made_by;
    u16 version_needed;
    u16 flags;
    u16 compression;
    u16 mod_time;
    u16 mod_date;
    u32 crc32;
    u32 compressed_size;
    u32 uncompressed_size;
    u16 file_name_length;
    u16 extra_field_length;
    u16 file_comment_length;
    u16 disk_number_start;
    u16 internal_attributes;
    u32 external_attributes;
    u32 local_header_offset;
} __attribute__((packed));

/* A v1-signed APK necessarily contains this entry. */
static int has_v1_signature_file(struct file *fp, loff_t directory_offset,
                                 u32 directory_size)
{
    static const char manifest[] = "META-INF/MANIFEST.MF";
    struct zip_central_directory_header header;
    loff_t pos = directory_offset;
    loff_t end = directory_offset + directory_size;

    if (directory_offset < 0 || end < directory_offset)
        return -EINVAL;

    while (pos < end) {
        char file_name[sizeof(manifest)];
        u64 variable_size;

        if (!read_exact(fp, &header, sizeof(header), &pos, end) ||
            header.signature != 0x02014b50)
            return -EINVAL;

        variable_size = (u64)header.file_name_length +
                        header.extra_field_length +
                        header.file_comment_length;
        if (variable_size > (u64)(end - pos))
            return -EINVAL;

        if (header.file_name_length == sizeof(manifest) - 1) {
            if (!read_exact(fp, file_name, sizeof(manifest) - 1, &pos, end))
                return -EINVAL;
            file_name[sizeof(manifest) - 1] = '\0';
            if (!memcmp(file_name, manifest, sizeof(manifest)))
                return 1;
        } else {
            pos += header.file_name_length;
        }
        pos += header.extra_field_length + header.file_comment_length;
    }

    return pos == end ? 0 : -EINVAL;
}

static bool check_block(struct file *fp, loff_t *pos, loff_t block_end,
                        unsigned *certificate_size_out, char hash_out[65])
{
    loff_t signers_end, signer_end, signed_data_end, digests_end, certificates_end;
    unsigned char *cert;
    unsigned char digest[SHA256_DIGEST_SIZE];
    u32 certificate_size;
    bool valid = false;

    // v2 block: signers sequence -> first signer -> signed data -> digests
    if (!read_length_prefixed_end(fp, pos, block_end, &signers_end) ||
        !read_length_prefixed_end(fp, pos, signers_end, &signer_end) ||
        !read_length_prefixed_end(fp, pos, signer_end, &signed_data_end) ||
        !read_length_prefixed_end(fp, pos, signed_data_end, &digests_end))
        return false;
    if (signer_end != signers_end)
        return false;

    *pos = digests_end;
    if (!read_length_prefixed_end(fp, pos, signed_data_end, &certificates_end) ||
        !read_exact(fp, &certificate_size, sizeof(certificate_size), pos, certificates_end))
        return false;

    if (certificate_size > INT_MAX || certificate_size > (u64)(certificates_end - *pos))
        return false;

#define CERT_MAX_LENGTH 0x1000
    if (certificate_size < 0x100 || certificate_size > CERT_MAX_LENGTH) {
        pr_info("cert length overlimit\n");
        return false;
    }

    cert = kmalloc(certificate_size, GFP_KERNEL);
    if (!cert)
        return false;
    if (!read_exact(fp, cert, certificate_size, pos, certificates_end))
        goto out;
    if (ksu_sha256(cert, certificate_size, digest)) {
        pr_info("sha256 error\n");
        goto out;
    }

    bin2hex(hash_out, digest, SHA256_DIGEST_SIZE);
    hash_out[SHA256_DIGEST_SIZE * 2] = '\0';
    *certificate_size_out = certificate_size;
    valid = true;
out:
    kfree(cert);
    return valid;
}

static bool get_v2_signature(const char *path, unsigned *certificate_size,
                             char certificate_sha256[65])
{
    unsigned char buffer[0x10] = { 0 };
    u32 cd_offset, cd_size;
    u32 zip64_locator_magic;
    u64 size_of_block, size_of_block_at_head;

    loff_t pos, pairs_end, file_size, eocd_offset;

    bool v2_signing_valid = false;
    int v2_signing_blocks = 0;
    bool v3_signing_exist = false;
    bool v3_1_signing_exist = false;

    int i;
    struct file *fp = filp_open(path, O_RDONLY, 0);
    if (IS_ERR(fp)) {
        pr_err("open %s error.\n", path);
        return false;
    }

    // disable inotify for this file
    fp->f_mode |= FMODE_NONOTIFY;

    file_size = generic_file_llseek(fp, 0, SEEK_END);
    if (file_size < 0)
        goto clean;

    // https://en.wikipedia.org/wiki/Zip_(file_format)#End_of_central_directory_record_(EOCD)
    for (i = 0;; ++i) {
        unsigned short comment_size;
        u32 magic;
        pos = file_size - i - 2;
        if (!read_exact(fp, &comment_size, sizeof(comment_size), &pos, file_size))
            goto clean;
        if (comment_size == i) {
            pos -= 22;
            if (!read_exact(fp, &magic, sizeof(magic), &pos, file_size))
                goto clean;
            if (magic == 0x06054b50) {
                eocd_offset = pos - sizeof(magic);
                break;
            }
        }
        if (i == 0xffff) {
            pr_info("error: cannot find eocd\n");
            goto clean;
        }
    }

    // reject ZIP64 before looking for a signing block
    if (eocd_offset >= 20) {
        pos = eocd_offset - 20;
        if (!read_exact(fp, &zip64_locator_magic, sizeof(zip64_locator_magic), &pos, file_size))
            goto clean;
        if (zip64_locator_magic == 0x07064b50)
            goto clean;
    }

    pos = eocd_offset + 12;
    // size of central directory
    if (!read_exact(fp, &cd_size, sizeof(cd_size), &pos, file_size))
        goto clean;
    // offset of central directory
    if (!read_exact(fp, &cd_offset, sizeof(cd_offset), &pos, file_size))
        goto clean;
    if ((u64)cd_offset > (u64)eocd_offset || (u64)cd_size != (u64)eocd_offset - cd_offset)
        goto clean;
    if (cd_offset < 0x20)
        goto clean;

    pairs_end = (loff_t)cd_offset - 0x18;
    pos = pairs_end;

    if (!read_exact(fp, &size_of_block, sizeof(size_of_block), &pos, cd_offset))
        goto clean;
    if (!read_exact(fp, buffer, sizeof(buffer), &pos, cd_offset))
        goto clean;
    if (memcmp((char *)buffer, "APK Sig Block 42", sizeof(buffer)))
        goto clean;

    if (size_of_block < 0x18 || size_of_block > INT_MAX - 0x8 || size_of_block > (u64)cd_offset - 0x8)
        goto clean;

    pos = (loff_t)cd_offset - (loff_t)size_of_block - 0x8;
    if (!read_exact(fp, &size_of_block_at_head, sizeof(size_of_block_at_head), &pos, pairs_end))
        goto clean;
    if (size_of_block_at_head != size_of_block)
        goto clean;

    // Scan every length-prefixed pair, matching AOSP's signing block parser
    // Each valid pair consumes an 8-byte length plus at least a 4-byte ID, so
    // malformed entries fail below instead of spinning in place.
    while (pos < pairs_end) {
        uint32_t id;
        u64 size_of_pair;
        loff_t pair_end;

        if (!read_exact(fp, &size_of_pair, sizeof(size_of_pair), &pos, pairs_end))
            goto invalid;
        if (size_of_pair < sizeof(id) || size_of_pair > INT_MAX || size_of_pair > (u64)(pairs_end - pos))
            goto invalid;

        pair_end = pos + (loff_t)size_of_pair;
        if (!read_exact(fp, &id, sizeof(id), &pos, pair_end))
            goto invalid;

        if (id == 0x7109871au) {
            v2_signing_blocks++;
            v2_signing_valid = check_block(fp, &pos, pair_end, certificate_size,
                                           certificate_sha256);
        } else if (id == 0xf05368c0u) {
            // http://aospxref.com/android-14.0.0_r2/xref/frameworks/base/core/java/android/util/apk/ApkSignatureSchemeV3Verifier.java#73
            v3_signing_exist = true;
        } else if (id == 0x1b93ad61u) {
            // http://aospxref.com/android-14.0.0_r2/xref/frameworks/base/core/java/android/util/apk/ApkSignatureSchemeV3Verifier.java#74
            v3_1_signing_exist = true;
        } else {
#ifdef CONFIG_KSU_DEBUG
            pr_info("Unknown id: 0x%08x\n", id);
#endif
        }
        pos = pair_end;
    }

    if (v2_signing_blocks != 1) {
#ifdef CONFIG_KSU_DEBUG
        pr_err("Unexpected v2 signature count: %d\n", v2_signing_blocks);
#endif
        v2_signing_valid = false;
    }

    if (v2_signing_valid) {
        int v1_status = has_v1_signature_file(fp, cd_offset, cd_size);

        if (v1_status != 0) {
            if (v1_status > 0)
                pr_err("Unexpected v1 signature scheme found!\n");
            else
                pr_err("Invalid ZIP central directory\n");
            goto invalid;
        }
    }

    goto clean;

invalid:
    v2_signing_valid = false;
clean:
    filp_close(fp, 0);

    if (v2_signing_valid && (v3_signing_exist || v3_1_signing_exist)) {
        pr_err("Unexpected v3 signature scheme found!\n");
        return false;
    }

    return v2_signing_valid;
}

bool ksu_apk_matches_v2_signature(const char *path, unsigned expected_size,
                                  const char *expected_sha256)
{
    unsigned certificate_size = 0;
    char certificate_sha256[65] = { 0 };

    if (!expected_sha256 ||
        !get_v2_signature(path, &certificate_size, certificate_sha256))
        return false;
    return certificate_size == expected_size &&
           !strcmp(certificate_sha256, expected_sha256);
}

#ifdef CONFIG_KSU_DEBUG

int ksu_debug_manager_appid = -1;

#include "manager/manager_identity.h"

static int set_debug_manager_appid(const char *val,
                                  const struct kernel_param *kp)
{
    int appid;
    int rv = kstrtoint(val, 0, &appid);
    if (rv) {
        return rv;
    }
    if (!ksu_is_normal_appid((uid_t)appid)) {
        pr_err("ksu_manager_appid rejected invalid appid %d\n", appid);
        return -EINVAL;
    }
    *(int *)kp->arg = appid;
    ksu_set_manager_appid((uid_t)appid);
    pr_info("ksu_manager_appid set to %d\n", appid);
    return 0;
}

static const struct kernel_param_ops debug_manager_appid_param_ops = {
    .set = set_debug_manager_appid,
    .get = param_get_int,
};

module_param_cb(ksu_debug_manager_appid, &debug_manager_appid_param_ops,
                &ksu_debug_manager_appid, S_IRUSR | S_IWUSR);

#endif

int get_pkg_from_apk_path(char *pkg, const char *path)
{
    const char *last_slash;
    const char *parent_start;
    const char *hyphen;
    size_t len;
    size_t parent_len;
    size_t pkg_len;

    if (!pkg || !path)
        return -1;

    len = strnlen(path, PATH_MAX);
    if (!len || len >= PATH_MAX)
        return -1;

    last_slash = path + len;
    while (last_slash > path) {
        last_slash--;
        if (*last_slash == '/')
            break;
    }
    if (*last_slash != '/' || last_slash == path || !last_slash[1])
        return -1;

    parent_start = last_slash;
    while (parent_start > path && parent_start[-1] != '/')
        parent_start--;
    parent_len = last_slash - parent_start;
    if (!parent_len)
        return -1;

    hyphen = memchr(parent_start, '-', parent_len);
    pkg_len = hyphen ? (size_t)(hyphen - parent_start) : parent_len;
    if (!pkg_len || pkg_len >= KSU_MAX_PACKAGE_NAME)
        return -1;

    memcpy(pkg, parent_start, pkg_len);
    pkg[pkg_len] = '\0';

    return 0;
}

static int manager_signature_index(unsigned size, const char *sha256)
{
    if (size == EXPECTED_SIZE && !strcmp(sha256, EXPECTED_HASH))
        return KSU_SIGNATURE_INDEX_PRIMARY;
#ifdef EXPECTED_SIZE2
    if (size == EXPECTED_SIZE2 && !strcmp(sha256, EXPECTED_HASH2))
        return 1;
#endif
    if (ksu_dynamic_manager_matches(size, sha256))
        return KSU_SIGNATURE_INDEX_DYNAMIC_MANAGER;
    return -ENODATA;
}

bool is_manager_apk(char *path, u8 *signature_index)
{
    unsigned certificate_size = 0;
    char certificate_sha256[65] = { 0 };
    int matched_index;

    if (!get_v2_signature(path, &certificate_size, certificate_sha256))
        return false;

    matched_index = manager_signature_index(certificate_size,
                                            certificate_sha256);
    if (matched_index < 0)
        return false;

#if defined(KSU_MANAGER_PACKAGE)
    if (matched_index != KSU_SIGNATURE_INDEX_DYNAMIC_MANAGER) {
    char pkg[KSU_MAX_PACKAGE_NAME];

    if (get_pkg_from_apk_path(pkg, path) < 0) {
        pr_err("Failed to get package name from apk path: %s\n", path);
        return false;
    }

    if (strcmp(pkg, KSU_MANAGER_PACKAGE) != 0) {
        return false;
    }
    }
#endif

    if (signature_index)
        *signature_index = matched_index;
    return true;
}
