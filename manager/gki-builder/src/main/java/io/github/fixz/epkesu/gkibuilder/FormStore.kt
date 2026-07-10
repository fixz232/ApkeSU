package io.github.fixz.apkesu.gkibuilder

import android.content.Context
import android.content.SharedPreferences

private const val PREF_NAME = "gki_builder"
private const val KEY_OWNER = "owner"
private const val KEY_REPO = "repo"
private const val KEY_REF = "ref"
private const val KEY_WORKFLOW = "workflow"
private const val KEY_TOKEN = "token"
private const val KEY_TOKEN_ENCRYPTED = "token_encrypted"
private const val KEY_TOKEN_IV = "token_iv"
private const val KEY_GITHUB_CLIENT_ID = "github_client_id"
private const val KEY_ANDROID_VERSION = "android_version"
private const val KEY_KERNEL_VERSION = "kernel_version"
private const val KEY_SUBLEVEL = "sublevel"
private const val KEY_OS_PATCH_LEVEL = "os_patch_level"
private const val KEY_FEATURE_SET = "feature_set"
private const val KEY_USE_REPO = "use_repo"
private const val KEY_USE_LATEST_SUSFS = "use_latest_susfs"
private const val KEY_BUILD_BYPASS = "build_bypass"
private const val KEY_BUILD_TIME = "build_time"
private const val KEY_USE_ZRAM = "use_zram"
private const val KEY_ZRAM_FULL_ALGO = "zram_full_algo"
private const val KEY_ZRAM_EXTRA_ALGOS = "zram_extra_algos"
private const val KEY_USE_NTSYNC = "use_ntsync"
private const val KEY_USE_NETWORKING = "use_networking"
private const val KEY_VIRTUALIZATION_SUPPORT = "virtualization_support"
private const val KEY_UPLOAD_AUX_ARTIFACTS = "upload_aux_artifacts"
private const val KEY_OPLUS_PATCH_MODE = "oplus_patch_mode"
private const val KEY_OPLUS_REFERENCE_REF = "oplus_reference_ref"

class FormStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun read(): BuilderForm = prefs.readBuilderForm()

    fun save(form: BuilderForm) {
        prefs.saveBuilderForm(form)
    }
}

private fun SharedPreferences.readBuilderForm(): BuilderForm {
    val defaults = BuilderForm()
    return BuilderForm(
        owner = getString(KEY_OWNER, defaults.owner) ?: defaults.owner,
        repo = getString(KEY_REPO, defaults.repo) ?: defaults.repo,
        ref = getString(KEY_REF, defaults.ref) ?: defaults.ref,
        workflowFile = getString(KEY_WORKFLOW, defaults.workflowFile) ?: defaults.workflowFile,
        token = readToken(defaults.token),
        githubClientId = getString(KEY_GITHUB_CLIENT_ID, defaults.githubClientId) ?: defaults.githubClientId,
        androidVersion = getString(KEY_ANDROID_VERSION, defaults.androidVersion) ?: defaults.androidVersion,
        kernelVersion = getString(KEY_KERNEL_VERSION, defaults.kernelVersion) ?: defaults.kernelVersion,
        sublevel = getString(KEY_SUBLEVEL, defaults.sublevel) ?: defaults.sublevel,
        osPatchLevel = getString(KEY_OS_PATCH_LEVEL, defaults.osPatchLevel) ?: defaults.osPatchLevel,
        featureSet = getString(KEY_FEATURE_SET, defaults.featureSet) ?: defaults.featureSet,
        useRepo = getBoolean(KEY_USE_REPO, defaults.useRepo),
        useLatestSusfs = getBoolean(KEY_USE_LATEST_SUSFS, defaults.useLatestSusfs),
        buildBypass = getBoolean(KEY_BUILD_BYPASS, defaults.buildBypass),
        buildTime = getString(KEY_BUILD_TIME, defaults.buildTime) ?: defaults.buildTime,
        useZram = getBoolean(KEY_USE_ZRAM, defaults.useZram),
        zramFullAlgo = getBoolean(KEY_ZRAM_FULL_ALGO, defaults.zramFullAlgo),
        zramExtraAlgos = getString(KEY_ZRAM_EXTRA_ALGOS, defaults.zramExtraAlgos) ?: defaults.zramExtraAlgos,
        useNtsync = getBoolean(KEY_USE_NTSYNC, defaults.useNtsync),
        useNetworking = getBoolean(KEY_USE_NETWORKING, defaults.useNetworking),
        virtualizationSupport = getString(KEY_VIRTUALIZATION_SUPPORT, defaults.virtualizationSupport)
            ?: defaults.virtualizationSupport,
        uploadAuxArtifacts = getBoolean(KEY_UPLOAD_AUX_ARTIFACTS, defaults.uploadAuxArtifacts),
        oplusPatchMode = getString(KEY_OPLUS_PATCH_MODE, defaults.oplusPatchMode) ?: defaults.oplusPatchMode,
        oplusReferenceRef = getString(KEY_OPLUS_REFERENCE_REF, defaults.oplusReferenceRef) ?: defaults.oplusReferenceRef,
    )
}

private fun SharedPreferences.saveBuilderForm(form: BuilderForm) {
    val token = form.token.trim()
    val encryptedToken = if (token.isBlank()) null else TokenCipher.encrypt(token)
    edit()
        .putString(KEY_OWNER, form.owner)
        .putString(KEY_REPO, form.repo)
        .putString(KEY_REF, form.ref)
        .putString(KEY_WORKFLOW, form.workflowFile)
        .remove(KEY_TOKEN)
        .apply {
            if (encryptedToken == null) {
                remove(KEY_TOKEN_ENCRYPTED)
                remove(KEY_TOKEN_IV)
            } else {
                putString(KEY_TOKEN_ENCRYPTED, encryptedToken.cipherText)
                putString(KEY_TOKEN_IV, encryptedToken.iv)
            }
        }
        .putString(KEY_GITHUB_CLIENT_ID, form.githubClientId)
        .putString(KEY_ANDROID_VERSION, form.androidVersion)
        .putString(KEY_KERNEL_VERSION, form.kernelVersion)
        .putString(KEY_SUBLEVEL, form.sublevel)
        .putString(KEY_OS_PATCH_LEVEL, form.osPatchLevel)
        .putString(KEY_FEATURE_SET, form.featureSet)
        .putBoolean(KEY_USE_REPO, form.useRepo)
        .putBoolean(KEY_USE_LATEST_SUSFS, form.useLatestSusfs)
        .putBoolean(KEY_BUILD_BYPASS, form.buildBypass)
        .putString(KEY_BUILD_TIME, form.buildTime)
        .putBoolean(KEY_USE_ZRAM, form.useZram)
        .putBoolean(KEY_ZRAM_FULL_ALGO, form.zramFullAlgo)
        .putString(KEY_ZRAM_EXTRA_ALGOS, form.zramExtraAlgos)
        .putBoolean(KEY_USE_NTSYNC, form.useNtsync)
        .putBoolean(KEY_USE_NETWORKING, form.useNetworking)
        .putString(KEY_VIRTUALIZATION_SUPPORT, form.virtualizationSupport)
        .putBoolean(KEY_UPLOAD_AUX_ARTIFACTS, form.uploadAuxArtifacts)
        .putString(KEY_OPLUS_PATCH_MODE, form.oplusPatchMode)
        .putString(KEY_OPLUS_REFERENCE_REF, form.oplusReferenceRef)
        .apply()
}

private fun SharedPreferences.readToken(defaultValue: String): String {
    val encrypted = getString(KEY_TOKEN_ENCRYPTED, "").orEmpty()
    val iv = getString(KEY_TOKEN_IV, "").orEmpty()
    if (encrypted.isNotBlank() && iv.isNotBlank()) {
        return runCatching {
            TokenCipher.decrypt(EncryptedValue(cipherText = encrypted, iv = iv))
        }.getOrDefault(defaultValue)
    }

    val legacy = getString(KEY_TOKEN, defaultValue) ?: defaultValue
    if (legacy.isNotBlank()) {
        runCatching {
            val payload = TokenCipher.encrypt(legacy)
            edit()
                .putString(KEY_TOKEN_ENCRYPTED, payload.cipherText)
                .putString(KEY_TOKEN_IV, payload.iv)
                .remove(KEY_TOKEN)
                .apply()
        }
    }
    return legacy
}
