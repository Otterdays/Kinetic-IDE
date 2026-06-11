package com.tabletaide.ide.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

internal object GitHubPkce {
    private const val VERIFIER_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    fun generateVerifier(length: Int = 64): String {
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) {
                append(VERIFIER_CHARS[random.nextInt(VERIFIER_CHARS.length)])
            }
        }
    }

    fun generateState(length: Int = 32): String = generateVerifier(length)

    fun challengeForVerifier(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
