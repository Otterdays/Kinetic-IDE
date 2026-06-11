package com.tabletaide.ide.agent

import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmProviderStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmClientResolver @Inject constructor(
    private val anthropicClient: AnthropicClientImpl,
    private val geminiClient: GeminiClientImpl,
    private val openAiClient: OpenAiClientImpl,
    private val grokClient: GrokClientImpl,
    private val openRouterClient: OpenRouterClientImpl,
    private val providerStore: LlmProviderStore,
) {
    fun clientFor(provider: LlmProvider): LlmClient = when (provider) {
        LlmProvider.ANTHROPIC -> anthropicClient
        LlmProvider.GEMINI -> geminiClient
        LlmProvider.OPENAI -> openAiClient
        LlmProvider.GROK -> grokClient
        LlmProvider.OPENROUTER -> openRouterClient
    }

    fun modelFor(provider: LlmProvider): String = providerStore.getSelectedModel(provider)

    fun currentClient(): LlmClient = clientFor(providerStore.getProvider())

    fun currentModel(): String = modelFor(providerStore.getProvider())
}
