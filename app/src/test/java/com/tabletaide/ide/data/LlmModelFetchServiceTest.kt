package com.tabletaide.ide.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmModelFetchServiceTest {

    @Test
    fun parseOpenRouterModels_mapsIdsAndFiltersTextOutput() {
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
                  "id": "vendor/image-only",
                  "name": "Image Only",
                  "architecture": { "output_modalities": ["image"] }
                }
              ]
            }
        """.trimIndent()

        val models = OpenRouterModelParser.parse(json)
        assertEquals(1, models.size)
        assertEquals("openai/gpt-4o-mini", models[0].id)
        assertEquals("GPT-4o Mini", models[0].displayName)
        assertTrue(models[0].supportsTools)
    }
}
