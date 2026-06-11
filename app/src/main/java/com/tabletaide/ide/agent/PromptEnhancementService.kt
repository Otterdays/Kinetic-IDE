package com.tabletaide.ide.agent

import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.data.LlmProviderStore
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptEnhancementService @Inject constructor(
    private val llmClientResolver: LlmClientResolver,
    private val providerStore: LlmProviderStore,
) {
    suspend fun enhancePrompt(
        draft: String,
        workspaceContext: String = "",
    ): Result<String> {
        val provider = providerStore.getProvider()
        val client = llmClientResolver.clientFor(provider)
        val model = llmClientResolver.modelFor(provider)
        val textBuffer = StringBuilder()
        var failure: String? = null
        client.streamMessage(
            model = model,
            systemPrompt = SYSTEM_PROMPT,
            messages = JSONArray().put(
                JSONObject().put("role", "user").put(
                    "content",
                    JSONArray().put(
                        JSONObject().put(
                            "type",
                            "text",
                        ).put(
                            "text",
                            buildUserPrompt(
                                draft = draft.take(IdeConstants.PROMPT_ENHANCEMENT_INPUT_MAX_CHARS),
                                workspaceContext = workspaceContext.take(
                                    IdeConstants.PROMPT_ENHANCEMENT_CONTEXT_MAX_CHARS,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            tools = null,
            maxTokens = IdeConstants.PROMPT_ENHANCEMENT_MAX_TOKENS,
        ).collect { event ->
            when (event) {
                is StreamEvent.TextDelta -> textBuffer.append(event.text)
                is StreamEvent.Failure -> failure = event.message
                is StreamEvent.ToolUseComplete -> Unit
                is StreamEvent.Finished -> Unit
            }
        }
        failure?.let { return Result.failure(IllegalStateException(it)) }
        val enhanced = normalizeResponse(textBuffer.toString())
        return if (enhanced.isBlank()) {
            Result.failure(
                IllegalStateException("The AI provider returned an empty enhanced prompt."),
            )
        } else {
            Result.success(enhanced)
        }
    }

    private fun buildUserPrompt(
        draft: String,
        workspaceContext: String,
    ): String {
        return buildString {
            appendLine("Rewrite this draft into a stronger prompt for the same coding assistant.")
            appendLine("Keep the user's original intent, tone, and language.")
            appendLine("Make it clearer, more actionable, and easier for the assistant to execute.")
            appendLine("Do not answer the prompt. Only return the improved prompt text.")
            appendLine()
            appendLine("Draft:")
            appendLine(draft)
            if (workspaceContext.isNotBlank()) {
                appendLine()
                appendLine("Optional workspace context:")
                append(workspaceContext)
            }
        }
    }

    private fun normalizeResponse(raw: String): String {
        val cleaned = raw.replace("```", "").trim()
        return cleaned
            .removePrefix("Enhanced prompt:")
            .removePrefix("Improved prompt:")
            .trim()
            .trim('"')
            .trim('\'')
            .trim()
    }

    private companion object {
        val SYSTEM_PROMPT: String = """
            You improve draft prompts for a coding assistant inside an IDE.
            Preserve the user's true goal and constraints.
            Make the prompt clearer and more specific without inventing requirements.
            Keep the same language as the input when possible.
            Return only the improved prompt text with no markdown fences, labels, bullets, or commentary.
        """.trimIndent()
    }
}
