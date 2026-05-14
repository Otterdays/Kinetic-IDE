package com.tabletaide.ide.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "kinetic_git_auth"
private const val KEY_ALIAS = "kinetic_git_auth_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_SIZE_BYTES = 12
private const val GCM_TAG_LENGTH_BITS = 128

@Singleton
class GitAuthStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(host: String): GitAuthEntry? {
        val normalizedHost = host.trim().lowercase()
        if (normalizedHost.isEmpty()) return null
        val username = prefs.getString(usernameKey(normalizedHost), null)?.trim().orEmpty()
        val encrypted = prefs.getString(tokenKey(normalizedHost), null)?.trim().orEmpty()
        if (encrypted.isEmpty()) return null
        val token = decrypt(encrypted) ?: return null
        return GitAuthEntry(
            host = normalizedHost,
            username = username.ifEmpty { defaultGitUsernameForHost(normalizedHost) },
            token = token,
        )
    }

    fun peek(host: String): GitSavedAuthState? {
        val normalizedHost = host.trim().lowercase()
        if (normalizedHost.isEmpty()) return null
        val encrypted = prefs.getString(tokenKey(normalizedHost), null)?.trim().orEmpty()
        if (encrypted.isEmpty()) return null
        val username = prefs.getString(usernameKey(normalizedHost), null)?.trim().orEmpty()
        return GitSavedAuthState(
            host = normalizedHost,
            suggestedUsername = username.ifEmpty { defaultGitUsernameForHost(normalizedHost) },
            hasSavedToken = true,
        )
    }

    fun save(entry: GitAuthEntry) {
        val normalizedHost = entry.host.trim().lowercase()
        if (normalizedHost.isEmpty()) return
        val encrypted = encrypt(entry.token.trim())
        prefs.edit()
            .putString(usernameKey(normalizedHost), entry.username.trim())
            .putString(tokenKey(normalizedHost), encrypted)
            .apply()
    }

    fun clear(host: String) {
        val normalizedHost = host.trim().lowercase()
        prefs.edit()
            .remove(usernameKey(normalizedHost))
            .remove(tokenKey(normalizedHost))
            .apply()
    }

    private fun usernameKey(host: String): String = "username_$host"

    private fun tokenKey(host: String): String = "token_$host"

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(cipherText: String): String? = try {
        val payload = Base64.decode(cipherText, Base64.NO_WRAP)
        if (payload.size <= IV_SIZE_BYTES) return null
        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
