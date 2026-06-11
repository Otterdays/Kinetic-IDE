package com.tabletaide.ide.data

import org.json.JSONArray
import org.json.JSONObject

internal object GeminiModelParser {
    private val EXCLUDED_ID_FRAGMENTS = listOf(
        "embedding",
        "imagen",
        "veo",
        "lyria",
        "tts",
        "live",
        "aqa",
        "computer-use",
        "robotics",
        "nano-banana",
        "deep-research",
        "antigravity",
    )

    fun parsePage(json: String): GeminiModelPage {
        val root = JSONObject(json)
        val modelsArray = root.optJSONArray("models") ?: JSONArray()
        val models = mutableListOf<LlmModelOption>()
        for (i in 0 until modelsArray.length()) {
            val item = modelsArray.optJSONObject(i) ?: continue
            toOption(item)?.let(models::add)
        }
        val nextPageToken = root.optString("nextPageToken").trim().ifEmpty { null }
        return GeminiModelPage(models = models, nextPageToken = nextPageToken)
    }

    private fun toOption(item: JSONObject): LlmModelOption? {
        val methods = item.optJSONArray("supportedGenerationMethods")?.toStringList().orEmpty()
        if ("generateContent" !in methods) return null

        val modelId = item.optString("baseModelId").trim().ifEmpty {
            item.optString("name").trim().removePrefix("models/")
        }
        if (modelId.isEmpty()) return null
        val normalizedId = modelId.lowercase()
        if (EXCLUDED_ID_FRAGMENTS.any { normalizedId.contains(it) }) return null
        if (!normalizedId.startsWith("gemini-")) return null

        val displayName = item.optString("displayName").trim().ifEmpty { modelId }
        return LlmModelOption(
            id = modelId,
            displayName = displayName,
            provider = LlmProvider.GEMINI,
            supportsTools = true,
        )
    }

    fun sortForPicker(models: List<LlmModelOption>): List<LlmModelOption> {
        return models.sortedWith(
            compareBy<LlmModelOption> { preferredRank(it.id) }
                .thenBy { it.displayName.lowercase() },
        )
    }

    fun preferredDefaultModelId(models: List<LlmModelOption>): String? {
        val ids = models.map { it.id }.toSet()
        for (candidate in PREFERRED_DEFAULT_IDS) {
            if (candidate in ids) return candidate
        }
        return models.firstOrNull()?.id
    }

    private fun preferredRank(modelId: String): Int {
        val normalized = modelId.lowercase()
        for ((index, preferred) in PREFERRED_DEFAULT_IDS.withIndex()) {
            if (normalized == preferred) return index
        }
        return PREFERRED_DEFAULT_IDS.size + when {
            normalized.contains("flash-lite") -> 10
            normalized.contains("flash") -> 20
            normalized.contains("pro") -> 30
            else -> 40
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        val out = mutableListOf<String>()
        for (i in 0 until length()) {
            val value = optString(i).trim()
            if (value.isNotEmpty()) out.add(value)
        }
        return out
    }

    private val PREFERRED_DEFAULT_IDS = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-3-flash-preview",
        "gemini-2.5-flash",
        "gemini-2.0-flash",
    )
}

internal data class GeminiModelPage(
    val models: List<LlmModelOption>,
    val nextPageToken: String?,
)
