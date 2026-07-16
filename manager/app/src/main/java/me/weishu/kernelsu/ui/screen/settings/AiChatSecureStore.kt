package me.weishu.kernelsu.ui.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AiChatSecureStore(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
    private val stateFile = AtomicFile(File(appContext.filesDir, ENCRYPTED_STATE_FILE))
    private val attachmentDir = File(appContext.filesDir, ATTACHMENT_DIRECTORY).apply { mkdirs() }
    private val lock = Any()

    fun load(defaultSystemPrompt: String): AiPersistedState = synchronized(lock) {
        if (stateFile.baseFile.isFile) {
            val restored = runCatching {
                val plain = decrypt(stateFile.readFully(), STATE_AAD)
                parseAiPersistedState(plain.toString(Charsets.UTF_8), defaultSystemPrompt)
            }.onFailure { Log.e(TAG, "Unable to decrypt AI chat state", it) }
                .getOrNull()
            if (restored != null) return@synchronized restored
        }

        migrateLegacyState(defaultSystemPrompt)
    }

    fun save(state: AiPersistedState): Boolean = synchronized(lock) {
        runCatching {
            val plain = state.toJson().toString().toByteArray(Charsets.UTF_8)
            val output = stateFile.startWrite()
            try {
                output.write(encrypt(plain, STATE_AAD))
                stateFile.finishWrite(output)
            } catch (error: Throwable) {
                stateFile.failWrite(output)
                throw error
            }
            true
        }.onFailure { Log.e(TAG, "Unable to save AI chat state", it) }
            .getOrDefault(false)
    }

    fun writeImage(bytes: ByteArray): String = synchronized(lock) {
        require(bytes.isNotEmpty()) { "Image is empty" }
        val storageId = UUID.randomUUID().toString()
        val target = attachmentFile(storageId)
        val atomicFile = AtomicFile(target)
        val output = atomicFile.startWrite()
        try {
            output.write(encrypt(bytes, attachmentAad(storageId)))
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
        storageId
    }

    fun readImage(storageId: String): ByteArray? = synchronized(lock) {
        if (!isValidStorageId(storageId)) return@synchronized null
        val file = attachmentFile(storageId)
        if (!file.isFile) return@synchronized null
        runCatching { decrypt(file.readBytes(), attachmentAad(storageId)) }
            .onFailure { Log.e(TAG, "Unable to read encrypted AI attachment", it) }
            .getOrNull()
    }

    fun deleteImage(storageId: String) = synchronized(lock) {
        if (isValidStorageId(storageId)) {
            runCatching { attachmentFile(storageId).delete() }
        }
        Unit
    }

    fun pruneImages(referencedStorageIds: Set<String>) = synchronized(lock) {
        val validReferences = referencedStorageIds.filterTo(hashSetOf(), ::isValidStorageId)
        attachmentDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == ATTACHMENT_EXTENSION && file.nameWithoutExtension !in validReferences) {
                runCatching { file.delete() }
            }
        }
    }

    fun readEncryptedText(fileName: String): String? = synchronized(lock) {
        require(SAFE_DOCUMENT_NAME.matches(fileName)) { "Invalid encrypted document name" }
        val file = File(appContext.filesDir, fileName)
        if (!file.isFile || file.length() > MAX_ENCRYPTED_DOCUMENT_BYTES) return@synchronized null
        runCatching {
            decrypt(AtomicFile(file).readFully(), documentAad(fileName)).toString(Charsets.UTF_8)
        }.onFailure { Log.e(TAG, "Unable to read encrypted document: $fileName", it) }
            .getOrNull()
    }

    fun writeEncryptedText(fileName: String, text: String): Boolean = synchronized(lock) {
        require(SAFE_DOCUMENT_NAME.matches(fileName)) { "Invalid encrypted document name" }
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_ENCRYPTED_DOCUMENT_BYTES) { "Encrypted document is too large" }
        val atomicFile = AtomicFile(File(appContext.filesDir, fileName))
        runCatching {
            val output = atomicFile.startWrite()
            try {
                output.write(encrypt(bytes, documentAad(fileName)))
                atomicFile.finishWrite(output)
            } catch (error: Throwable) {
                atomicFile.failWrite(output)
                throw error
            }
            true
        }.onFailure { Log.e(TAG, "Unable to write encrypted document: $fileName", it) }
            .getOrDefault(false)
    }

    @SuppressLint("ApplySharedPref", "UseKtx")
    private fun migrateLegacyState(defaultSystemPrompt: String): AiPersistedState {
        val defaultConfig = defaultAiApiConfig(defaultSystemPrompt)
        val baseUrl = legacyPrefs.getString(LEGACY_BASE_URL, null)?.trim().orEmpty()
        val model = legacyPrefs.getString(LEGACY_MODEL, null)?.trim().orEmpty()
        val config = defaultConfig.copy(
            provider = inferProvider(baseUrl),
            baseUrl = baseUrl.ifBlank { defaultConfig.baseUrl },
            apiKey = legacyPrefs.getString(LEGACY_API_KEY, "").orEmpty(),
            model = model.ifBlank { defaultConfig.model },
            systemPrompt = legacyPrefs.getString(LEGACY_SYSTEM_PROMPT, defaultSystemPrompt)
                .orEmpty()
                .ifBlank { defaultSystemPrompt },
        )
        val messages = parseLegacyAiMessages(legacyPrefs.getString(LEGACY_HISTORY, null))
        val now = System.currentTimeMillis()
        val conversation = AiConversation(
            title = messages.firstOrNull { it.role == AiRole.User }
                ?.text
                ?.let(::deriveConversationTitle)
                ?: DEFAULT_CONVERSATION_TITLE,
            createdAt = messages.firstOrNull()?.createdAt ?: now,
            updatedAt = messages.lastOrNull()?.createdAt ?: now,
            messages = messages,
        )
        val migrated = AiPersistedState(
            config = config,
            conversations = listOf(conversation),
            activeConversationId = conversation.id,
        )
        if (save(migrated)) {
            legacyPrefs.edit()
                .remove(LEGACY_BASE_URL)
                .remove(LEGACY_API_KEY)
                .remove(LEGACY_MODEL)
                .remove(LEGACY_SYSTEM_PROMPT)
                .remove(LEGACY_HISTORY)
                .commit()
        }
        return migrated
    }

    private fun inferProvider(baseUrl: String): AiProviderPreset = when {
        "api.deepseek.com" in baseUrl -> AiProviderPreset.DeepSeek
        "api.openai.com" in baseUrl -> AiProviderPreset.OpenAi
        else -> AiProviderPreset.Compatible
    }

    private fun encrypt(plain: ByteArray, aad: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(plain)
        return ByteBuffer.allocate(1 + iv.size + encrypted.size)
            .put(PAYLOAD_VERSION)
            .put(iv)
            .put(encrypted)
            .array()
    }

    private fun decrypt(payload: ByteArray, aad: ByteArray): ByteArray {
        require(payload.size > 1 + GCM_IV_BYTES) { "Encrypted payload is too short" }
        val buffer = ByteBuffer.wrap(payload)
        require(buffer.get() == PAYLOAD_VERSION) { "Unsupported encrypted payload version" }
        val iv = ByteArray(GCM_IV_BYTES).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun attachmentFile(storageId: String): File =
        File(attachmentDir, "$storageId.$ATTACHMENT_EXTENSION")

    private fun attachmentAad(storageId: String): ByteArray =
        "attachment:$storageId".toByteArray(Charsets.UTF_8)

    private fun documentAad(fileName: String): ByteArray =
        "document:$fileName".toByteArray(Charsets.UTF_8)

    private fun isValidStorageId(storageId: String): Boolean = STORAGE_ID_REGEX.matches(storageId)

    private companion object {
        const val TAG = "AiChatSecureStore"
        const val LEGACY_PREFS = "ai_chat"
        const val ENCRYPTED_STATE_FILE = "ai_chat_state_v2.bin"
        const val KEY_ALIAS = "apkesu_ai_chat_aes_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val ATTACHMENT_DIRECTORY = "ai_chat_attachments"
        const val ATTACHMENT_EXTENSION = "bin"
        const val MAX_ENCRYPTED_DOCUMENT_BYTES = 16 * 1024 * 1024L
        const val LEGACY_BASE_URL = "base_url"
        const val LEGACY_API_KEY = "api_key"
        const val LEGACY_MODEL = "model"
        const val LEGACY_SYSTEM_PROMPT = "system_prompt"
        const val LEGACY_HISTORY = "history"
        val PAYLOAD_VERSION: Byte = 1
        val STATE_AAD: ByteArray = "state:v2".toByteArray(Charsets.UTF_8)
        val STORAGE_ID_REGEX = Regex("[0-9a-fA-F-]{36}")
        val SAFE_DOCUMENT_NAME = Regex("[a-zA-Z0-9._-]{1,80}")
    }
}
