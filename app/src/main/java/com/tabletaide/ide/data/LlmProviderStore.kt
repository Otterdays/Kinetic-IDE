package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class LlmProvider(val id: String, val displayName: String) {
    ANTHROPIC("anthropic", "Claude (Anthropic)"),
    GEMINI("gemini", "Gemini (Google)"),
}

data class LlmCredentialState(
    val anthropicApiKey: String,
    val geminiApiKey: String,
) {
    fun hasKey(provider: LlmProvider): Boolean = when (provider) {
        LlmProvider.ANTHROPIC -> anthropicApiKey.isNotBlank()
        LlmProvider.GEMINI -> geminiApiKey.isNotBlank()
    }
}

@Singleton
class LlmProviderStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences("kinetic_llm_settings", Context.MODE_PRIVATE)
    private val providerKey = "llm_provider"
    private val anthropicApiKey = "anthropic_api_key"
    private val geminiApiKey = "gemini_api_key"

    fun getProvider(): LlmProvider {
        val id = prefs.getString(providerKey, null) ?: return LlmProvider.ANTHROPIC
        return LlmProvider.entries.find { it.id == id } ?: LlmProvider.ANTHROPIC
    }

    fun setProvider(provider: LlmProvider) {
        prefs.edit().putString(providerKey, provider.id).apply()
    }

    fun getCredentialState(): LlmCredentialState {
        return LlmCredentialState(
            anthropicApiKey = prefs.getString(anthropicApiKey, "").orEmpty(),
            geminiApiKey = prefs.getString(geminiApiKey, "").orEmpty(),
        )
    }

    fun setApiKey(provider: LlmProvider, apiKey: String) {
        val key = when (provider) {
            LlmProvider.ANTHROPIC -> anthropicApiKey
            LlmProvider.GEMINI -> geminiApiKey
        }
        prefs.edit().putString(key, apiKey.trim()).apply()
    }
}
