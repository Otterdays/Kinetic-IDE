package com.tabletaide.ide.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogBuilderTest {

    @Test
    fun sectionAfterFetch_successWithModels_isReady() {
        val models = listOf(
            LlmModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", LlmProvider.GEMINI),
        )
        val section = ModelCatalogBuilder.sectionAfterFetch(
            provider = LlmProvider.GEMINI,
            hasKey = true,
            fetchResult = Result.success(models),
        )
        assertEquals(ProviderModelStatus.Ready, section.status)
        assertEquals(1, section.models.size)
    }

    @Test
    fun sectionAfterFetch_emptyListedProvider_isFailed() {
        val section = ModelCatalogBuilder.sectionAfterFetch(
            provider = LlmProvider.OPENROUTER,
            hasKey = true,
            fetchResult = Result.success(emptyList()),
        )
        assertEquals(ProviderModelStatus.Failed, section.status)
        assertTrue(!section.errorMessage.isNullOrBlank())
    }

    @Test
    fun sectionAfterFetch_unlistedProvider_isNotListed() {
        val section = ModelCatalogBuilder.sectionAfterFetch(
            provider = LlmProvider.ANTHROPIC,
            hasKey = true,
            fetchResult = Result.success(emptyList()),
        )
        assertEquals(ProviderModelStatus.NotListed, section.status)
    }

    @Test
    fun sectionAfterFetch_failure_isFailed() {
        val section = ModelCatalogBuilder.sectionAfterFetch(
            provider = LlmProvider.GEMINI,
            hasKey = true,
            fetchResult = Result.failure(IllegalStateException("HTTP 401")),
        )
        assertEquals(ProviderModelStatus.Failed, section.status)
        assertEquals("HTTP 401", section.errorMessage)
    }

    @Test
    fun resolveUsableProvider_prefersCurrentWhenKeyed() {
        val creds = LlmCredentialState(geminiApiKey = "g", openRouterApiKey = "o")
        assertEquals(
            LlmProvider.GEMINI,
            LlmProviderResolution.resolveUsableProvider(creds, LlmProvider.GEMINI),
        )
    }

    @Test
    fun resolveUsableProvider_fallsBackToPreferredOrder() {
        val creds = LlmCredentialState(openRouterApiKey = "o")
        assertEquals(
            LlmProvider.OPENROUTER,
            LlmProviderResolution.resolveUsableProvider(creds, LlmProvider.ANTHROPIC),
        )
    }
}
