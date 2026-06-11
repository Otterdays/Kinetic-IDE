package com.tabletaide.ide.data

import android.content.Context
import com.tabletaide.ide.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class LlmProvider(
    val id: String,
    val brandLabel: String,
    val displayName: String = brandLabel,
) {
    ANTHROPIC("anthropic", "Claude"),
    GEMINI("gemini", "Gemini"),
    OPENAI("openai", "OpenAI"),
    GROK("grok", "Grok"),
    OPENROUTER("openrouter", "OpenRouter"),
}

data class LlmCredentialState(
    val anthropicApiKey: String = "",
    val geminiApiKey: String = "",
    val openAiApiKey: String = "",
    val grokApiKey: String = "",
    val openRouterApiKey: String = "",
) {
    fun storedKey(provider: LlmProvider): String = when (provider) {
        LlmProvider.ANTHROPIC -> anthropicApiKey
        LlmProvider.GEMINI -> geminiApiKey
        LlmProvider.OPENAI -> openAiApiKey
        LlmProvider.GROK -> grokApiKey
        LlmProvider.OPENROUTER -> openRouterApiKey
    }

    fun hasKey(provider: LlmProvider): Boolean = storedKey(provider).isNotBlank()

    fun hasAnyKey(): Boolean = LlmProvider.entries.any(::hasKey)
}

data class LlmSelectionState(
    val provider: LlmProvider,
    val modelId: String,
    val modelDisplayName: String? = null,
) {
    val label: String
        get() = when {
            !modelDisplayName.isNullOrBlank() -> modelDisplayName
            modelId.isNotBlank() -> modelId
            else -> "Coming soon"
        }

    val hasModel: Boolean get() = modelId.isNotBlank()
}

@Singleton
class LlmProviderStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences("kinetic_llm_settings", Context.MODE_PRIVATE)
    private val providerKey = "llm_provider"
    private val anthropicApiKey = "anthropic_api_key"
    private val geminiApiKey = "gemini_api_key"
    private val openAiApiKey = "openai_api_key"
    private val grokApiKey = "grok_api_key"
    private val openRouterApiKey = "openrouter_api_key"

    fun getProvider(): LlmProvider {
        val id = prefs.getString(providerKey, null) ?: return LlmProvider.ANTHROPIC
        return LlmProvider.entries.find { it.id == id } ?: LlmProvider.ANTHROPIC
    }

    fun setProvider(provider: LlmProvider) {
        prefs.edit().putString(providerKey, provider.id).apply()
    }

    fun getSelectedModel(provider: LlmProvider = getProvider()): String =
        prefs.getString(modelKey(provider), null).orEmpty().trim()

    fun setSelectedModel(provider: LlmProvider, modelId: String) {
        prefs.edit().putString(modelKey(provider), modelId.trim()).apply()
    }

    fun getSelection(): LlmSelectionState {
        val provider = getProvider()
        return LlmSelectionState(provider = provider, modelId = getSelectedModel(provider))
    }

    fun setSelection(provider: LlmProvider, modelId: String) {
        setProvider(provider)
        setSelectedModel(provider, modelId)
    }

    fun getCredentialState(): LlmCredentialState {
        return LlmCredentialState(
            anthropicApiKey = prefs.getString(anthropicApiKey, "").orEmpty(),
            geminiApiKey = prefs.getString(geminiApiKey, "").orEmpty(),
            openAiApiKey = prefs.getString(openAiApiKey, "").orEmpty(),
            grokApiKey = prefs.getString(grokApiKey, "").orEmpty(),
            openRouterApiKey = prefs.getString(openRouterApiKey, "").orEmpty(),
        )
    }

    fun resolveApiKey(provider: LlmProvider): String {
        val stored = getCredentialState().storedKey(provider)
        if (stored.isNotBlank()) return stored
        return when (provider) {
            LlmProvider.ANTHROPIC -> BuildConfig.ANTHROPIC_API_KEY
            LlmProvider.GEMINI -> BuildConfig.GEMINI_API_KEY
            LlmProvider.OPENAI -> BuildConfig.OPENAI_API_KEY
            LlmProvider.GROK -> BuildConfig.GROK_API_KEY
            LlmProvider.OPENROUTER -> BuildConfig.OPENROUTER_API_KEY
        }
    }

    fun setApiKey(provider: LlmProvider, apiKey: String) {
        val key = when (provider) {
            LlmProvider.ANTHROPIC -> anthropicApiKey
            LlmProvider.GEMINI -> geminiApiKey
            LlmProvider.OPENAI -> openAiApiKey
            LlmProvider.GROK -> grokApiKey
            LlmProvider.OPENROUTER -> openRouterApiKey
        }
        prefs.edit().putString(key, apiKey.trim()).apply()
    }

    private fun modelKey(provider: LlmProvider): String = "llm_model_${provider.id}"
}
