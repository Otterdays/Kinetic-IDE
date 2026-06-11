package com.tabletaide.ide.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelParserTest {

    @Test
    fun parsePage_keepsGenerateContentGeminiModels() {
        val page = GeminiModelParser.parsePage(
            """
            {
              "models": [
                {
                  "name": "models/gemini-3.5-flash",
                  "baseModelId": "gemini-3.5-flash",
                  "displayName": "Gemini 3.5 Flash",
                  "supportedGenerationMethods": ["generateContent", "countTokens"]
                },
                {
                  "name": "models/gemini-3.1-flash-lite",
                  "baseModelId": "gemini-3.1-flash-lite",
                  "displayName": "Gemini 3.1 Flash-Lite",
                  "supportedGenerationMethods": ["generateContent", "countTokens"]
                },
                {
                  "name": "models/text-embedding-004",
                  "baseModelId": "text-embedding-004",
                  "displayName": "Embedding",
                  "supportedGenerationMethods": ["embedContent"]
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, page.models.size)
        assertTrue(page.models.any { it.id == "gemini-3.5-flash" })
        assertTrue(page.models.any { it.id == "gemini-3.1-flash-lite" })
    }

    @Test
    fun preferredDefaultModelId_prefersGemini35Flash() {
        val models = listOf(
            LlmModelOption("gemini-2.5-flash", "2.5 Flash", LlmProvider.GEMINI),
            LlmModelOption("gemini-3.1-flash-lite", "3.1 Lite", LlmProvider.GEMINI),
            LlmModelOption("gemini-3.5-flash", "3.5 Flash", LlmProvider.GEMINI),
        )
        assertEquals("gemini-3.5-flash", GeminiModelParser.preferredDefaultModelId(models))
    }
}
