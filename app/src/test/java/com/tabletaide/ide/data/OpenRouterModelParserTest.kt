package com.tabletaide.ide.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterModelParserTest {

    @Test
    fun parse_default_includesOnlyToolCapableModels() {
        val json = """
            {
              "data": [
                {
                  "id": "openai/gpt-4o-mini",
                  "name": "GPT-4o Mini",
                  "supported_parameters": ["tools", "max_tokens"],
                  "architecture": { "output_modalities": ["text"] }
                },
                {
                  "id": "vendor/no-tools",
                  "name": "No Tools",
                  "supported_parameters": ["max_tokens"],
                  "architecture": { "output_modalities": ["text"] }
                }
              ]
            }
        """.trimIndent()

        val models = OpenRouterModelParser.parse(json)
        assertEquals(1, models.size)
        assertEquals("openai/gpt-4o-mini", models[0].id)
        assertTrue(models[0].supportsTools)
    }

    @Test
    fun parse_includeNonToolModels_true_listsAllTextModels() {
        val json = """
            {
              "data": [
                {
                  "id": "openai/gpt-4o-mini",
                  "name": "GPT-4o Mini",
                  "supported_parameters": ["tools"],
                  "architecture": { "output_modalities": ["text"] }
                },
                {
                  "id": "vendor/no-tools",
                  "name": "No Tools",
                  "supported_parameters": ["max_tokens"],
                  "architecture": { "output_modalities": ["text"] }
                }
              ]
            }
        """.trimIndent()

        val models = OpenRouterModelParser.parse(json, includeNonToolModels = true)
        assertEquals(2, models.size)
    }
}
