package com.tabletaide.ide.data

data class LlmModelOption(
    val id: String,
    val displayName: String,
    val provider: LlmProvider,
    val supportsTools: Boolean = true,
)

data class ModelPickerUiState(
    val loading: Boolean = false,
    val models: List<LlmModelOption> = emptyList(),
    val loaded: Boolean = false,
    val errorMessage: String? = null,
) {
    fun modelsFor(provider: LlmProvider): List<LlmModelOption> =
        models.filter { it.provider == provider }

    fun hasAnyModels(): Boolean = models.isNotEmpty()
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
