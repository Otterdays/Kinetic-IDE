package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class LlmProvider(val id: String, val displayName: String) {
    ANTHROPIC("anthropic", "Claude (Anthropic)"),
    GEMINI("gemini", "Gemini (Google)"),
}

@Singleton
class LlmProviderStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences("kinetic_llm_settings", Context.MODE_PRIVATE)
    private val providerKey = "llm_provider"

    fun getProvider(): LlmProvider {
        val id = prefs.getString(providerKey, null) ?: return LlmProvider.ANTHROPIC
        return LlmProvider.entries.find { it.id == id } ?: LlmProvider.ANTHROPIC
    }

    fun setProvider(provider: LlmProvider) {
        prefs.edit().putString(providerKey, provider.id).apply()
    }
}
