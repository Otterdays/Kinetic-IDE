package com.tabletaide.ide.data

data class LlmModelOption(
    val id: String,
    val displayName: String,
    val provider: LlmProvider,
    val supportsTools: Boolean = true,
)

enum class ProviderModelStatus {
    NoKey,
    NotListed,
    Loading,
    Ready,
    Failed,
}

data class ProviderModelSection(
    val provider: LlmProvider,
    val status: ProviderModelStatus,
    val models: List<LlmModelOption> = emptyList(),
    val errorMessage: String? = null,
)

data class ModelPickerUiState(
    val loading: Boolean = false,
    val sections: Map<LlmProvider, ProviderModelSection> = emptyMap(),
    val loaded: Boolean = false,
) {
    val models: List<LlmModelOption>
        get() = sections.values.flatMap { it.models }

    fun sectionFor(provider: LlmProvider): ProviderModelSection =
        sections[provider] ?: ProviderModelSection(provider = provider, status = ProviderModelStatus.NoKey)

    fun modelsFor(provider: LlmProvider): List<LlmModelOption> =
        sectionFor(provider).models

    fun hasAnyModels(): Boolean = models.isNotEmpty()
}

object ModelCatalogBuilder {
    private val LISTED_PROVIDERS = setOf(
        LlmProvider.GEMINI,
        LlmProvider.OPENROUTER,
    )

    fun initialSections(credentials: LlmCredentialState, loading: Boolean): Map<LlmProvider, ProviderModelSection> =
        LlmProvider.entries.associateWith { provider ->
            when {
                !credentials.hasKey(provider) -> ProviderModelSection(
                    provider = provider,
                    status = ProviderModelStatus.NoKey,
                )
                loading -> ProviderModelSection(
                    provider = provider,
                    status = ProviderModelStatus.Loading,
                )
                provider in LISTED_PROVIDERS -> ProviderModelSection(
                    provider = provider,
                    status = ProviderModelStatus.Loading,
                )
                else -> ProviderModelSection(
                    provider = provider,
                    status = ProviderModelStatus.NotListed,
                )
            }
        }

    fun sectionAfterFetch(
        provider: LlmProvider,
        hasKey: Boolean,
        fetchResult: Result<List<LlmModelOption>>,
    ): ProviderModelSection {
        if (!hasKey) {
            return ProviderModelSection(provider = provider, status = ProviderModelStatus.NoKey)
        }
        if (provider !in LISTED_PROVIDERS) {
            return ProviderModelSection(provider = provider, status = ProviderModelStatus.NotListed)
        }
        return fetchResult.fold(
            onSuccess = { models ->
                if (models.isEmpty()) {
                    ProviderModelSection(
                        provider = provider,
                        status = ProviderModelStatus.Failed,
                        errorMessage = "No models returned for ${provider.displayName}.",
                    )
                } else {
                    ProviderModelSection(
                        provider = provider,
                        status = ProviderModelStatus.Ready,
                        models = models,
                    )
                }
            },
            onFailure = { error ->
                ProviderModelSection(
                    provider = provider,
                    status = ProviderModelStatus.Failed,
                    errorMessage = error.message ?: "Failed to load models for ${provider.displayName}.",
                )
            },
        )
    }
}

object LlmModelCatalog {
    fun filter(models: List<LlmModelOption>, query: String): List<LlmModelOption> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return models
        return models.filter { option ->
            option.displayName.lowercase().contains(normalized) ||
                option.id.lowercase().contains(normalized) ||
                option.provider.displayName.lowercase().contains(normalized) ||
                option.provider.brandLabel.lowercase().contains(normalized)
        }
    }

    fun displayNameFor(models: List<LlmModelOption>, modelId: String): String? =
        models.find { it.id == modelId }?.displayName
}

object LlmProviderResolution {
    private val PREFERRED_PROVIDER_ORDER = listOf(
        LlmProvider.GEMINI,
        LlmProvider.OPENROUTER,
        LlmProvider.ANTHROPIC,
        LlmProvider.OPENAI,
        LlmProvider.GROK,
    )

    fun resolveUsableProvider(
        credentials: LlmCredentialState,
        current: LlmProvider,
    ): LlmProvider? {
        if (credentials.hasKey(current)) return current
        return PREFERRED_PROVIDER_ORDER.firstOrNull { credentials.hasKey(it) }
    }
}
