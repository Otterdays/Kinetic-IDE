package com.tabletaide.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmModelFetchService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val providerStore: LlmProviderStore,
) {
    suspend fun fetchForProvider(provider: LlmProvider): Result<List<LlmModelOption>> =
        withContext(Dispatchers.IO) {
            val apiKey = providerStore.resolveApiKey(provider)
            if (apiKey.isBlank()) {
                return@withContext Result.success(emptyList())
            }
            when (provider) {
                LlmProvider.OPENROUTER -> fetchOpenRouterModels(apiKey)
                LlmProvider.ANTHROPIC,
                LlmProvider.GEMINI,
                LlmProvider.OPENAI,
                LlmProvider.GROK,
                -> Result.success(emptyList())
            }
        }

    private fun fetchOpenRouterModels(apiKey: String): Result<List<LlmModelOption>> {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        IllegalStateException("OpenRouter models HTTP ${response.code}"),
                    )
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return Result.failure(IllegalStateException("OpenRouter models response was empty"))
                }
                Result.success(OpenRouterModelParser.parse(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
