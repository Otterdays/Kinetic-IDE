package com.tabletaide.ide.data

data class LlmModelOption(
    val id: String,
    val displayName: String,
    val provider: LlmProvider,
    val supportsTools: Boolean = true,
)

object LlmModelCatalog {

    private val anthropicModels = listOf(
        LlmModelOption("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", LlmProvider.ANTHROPIC),
        LlmModelOption("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", LlmProvider.ANTHROPIC),
        LlmModelOption("claude-3-opus-20240229", "Claude 3 Opus", LlmProvider.ANTHROPIC),
    )

    private val geminiModels = listOf(
        LlmModelOption("gemini-2.0-flash", "Gemini 2.0 Flash", LlmProvider.GEMINI),
        LlmModelOption("gemini-2.5-pro-preview-03-25", "Gemini 2.5 Pro Preview", LlmProvider.GEMINI),
        LlmModelOption("gemini-1.5-pro", "Gemini 1.5 Pro", LlmProvider.GEMINI),
    )

    private val openAiModels = listOf(
        LlmModelOption("gpt-4o", "GPT-4o", LlmProvider.OPENAI),
        LlmModelOption("gpt-4o-mini", "GPT-4o Mini", LlmProvider.OPENAI),
        LlmModelOption("gpt-4.1", "GPT-4.1", LlmProvider.OPENAI),
        LlmModelOption("gpt-4.1-mini", "GPT-4.1 Mini", LlmProvider.OPENAI),
    )

    private val grokModels = listOf(
        LlmModelOption("grok-2-latest", "Grok 2 Latest", LlmProvider.GROK),
        LlmModelOption("grok-2-1212", "Grok 2 (1212)", LlmProvider.GROK),
        LlmModelOption("grok-beta", "Grok Beta", LlmProvider.GROK),
    )

  /** Curated OpenRouter slugs — any `provider/model` works when entered as custom later. */
    private val openRouterModels = listOf(
        LlmModelOption("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", LlmProvider.OPENROUTER),
        LlmModelOption("google/gemini-2.0-flash-001", "Gemini 2.0 Flash", LlmProvider.OPENROUTER),
        LlmModelOption("openai/gpt-4o-mini", "GPT-4o Mini", LlmProvider.OPENROUTER),
        LlmModelOption("x-ai/grok-2-1212", "Grok 2", LlmProvider.OPENROUTER),
        LlmModelOption("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B", LlmProvider.OPENROUTER),
        LlmModelOption("deepseek/deepseek-chat", "DeepSeek Chat", LlmProvider.OPENROUTER),
    )

    fun allModels(): List<LlmModelOption> =
        anthropicModels + geminiModels + openAiModels + grokModels + openRouterModels

    fun modelsFor(provider: LlmProvider): List<LlmModelOption> = when (provider) {
        LlmProvider.ANTHROPIC -> anthropicModels
        LlmProvider.GEMINI -> geminiModels
        LlmProvider.OPENAI -> openAiModels
        LlmProvider.GROK -> grokModels
        LlmProvider.OPENROUTER -> openRouterModels
    }

    fun defaultModel(provider: LlmProvider): String = modelsFor(provider).first().id

    fun findModel(modelId: String): LlmModelOption? =
        allModels().find { it.id == modelId }

    fun filter(query: String, credentials: LlmCredentialState): List<LlmModelOption> {
        val normalized = query.trim().lowercase()
        val base = if (normalized.isEmpty()) {
            allModels()
        } else {
            allModels().filter { option ->
                option.displayName.lowercase().contains(normalized) ||
                    option.id.lowercase().contains(normalized) ||
                    option.provider.displayName.lowercase().contains(normalized) ||
                    option.provider.brandLabel.lowercase().contains(normalized)
            }
        }
        return base
    }
}
