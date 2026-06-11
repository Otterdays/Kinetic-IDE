package com.tabletaide.ide.data

import org.json.JSONArray
import org.json.JSONObject

internal object OpenRouterModelParser {
    fun parse(json: String): List<LlmModelOption> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: return emptyList()
        val models = mutableListOf<LlmModelOption>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id").trim()
            if (id.isEmpty()) continue
            val name = item.optString("name").trim().ifEmpty { id }
            val supported = item.optJSONArray("supported_parameters")?.toStringSet().orEmpty()
            val architecture = item.optJSONObject("architecture")
            val outputModalities = architecture
                ?.optJSONArray("output_modalities")
                ?.toStringSet()
                .orEmpty()
            if (outputModalities.isNotEmpty() && "text" !in outputModalities) continue
            val supportsTools = "tools" in supported || "tool_choice" in supported
            models.add(
                LlmModelOption(
                    id = id,
                    displayName = name,
                    provider = LlmProvider.OPENROUTER,
                    supportsTools = supportsTools,
                ),
            )
        }
        return models.sortedBy { it.displayName.lowercase() }
    }

    private fun JSONArray.toStringSet(): Set<String> {
        val out = mutableSetOf<String>()
        for (i in 0 until length()) {
            val value = optString(i).trim()
            if (value.isNotEmpty()) out.add(value)
        }
        return out
    }
}
