package com.tabletaide.ide.agent

import com.tabletaide.ide.IdeConstants
import com.tabletaide.ide.data.LlmProvider
import com.tabletaide.ide.data.LlmProviderStore
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitCommitMessageService @Inject constructor(
    private val anthropicClient: AnthropicClientImpl,
    private val geminiClient: GeminiClientImpl,
    private val providerStore: LlmProviderStore,
) {
    suspend fun generateCommitMessage(gitContext: String): Result<String> {
        val provider = providerStore.getProvider()
        val client = when (provider) {
            LlmProvider.ANTHROPIC -> anthropicClient
            LlmProvider.GEMINI -> geminiClient
        }
        val model = when (provider) {
            LlmProvider.ANTHROPIC -> IdeConstants.ANTHROPIC_MODEL
            LlmProvider.GEMINI -> IdeConstants.GEMINI_MODEL
        }
        val textBuffer = StringBuilder()
        var failure: String? = null
        client.streamMessage(
            model = model,
            systemPrompt = SYSTEM_PROMPT,
            messages = JSONArray().put(
                JSONObject().put("role", "user").put(
                    "content",
                    JSONArray().put(
                        JSONObject().put("type", "text").put("text", buildUserPrompt(gitContext)),
                    ),
                ),
            ),
            tools = null,
            maxTokens = IdeConstants.GIT_COMMIT_MESSAGE_MAX_TOKENS,
        ).collect { event ->
            when (event) {
                is StreamEvent.TextDelta -> textBuffer.append(event.text)
                is StreamEvent.Failure -> failure = event.message
                is StreamEvent.ToolUseComplete -> Unit
                is StreamEvent.Finished -> Unit
            }
        }
        failure?.let { return Result.failure(IllegalStateException(it)) }
        val normalized = normalizeResponse(textBuffer.toString())
        return if (normalized.isBlank()) {
            Result.failure(
                IllegalStateException("The AI provider returned an empty commit message."),
            )
        } else {
            Result.success(normalized)
        }
    }

    private fun buildUserPrompt(gitContext: String): String {
        return buildString {
            appendLine("Write a git commit message for these repository changes.")
            appendLine("Use Conventional Commits style.")
            appendLine("Return only the final message text.")
            appendLine()
            append(gitContext)
        }
    }

    private fun normalizeResponse(raw: String): String {
        val cleaned = raw
            .replace("```", "")
            .trim()
        val lines = cleaned.lines()
            .map { line ->
                line.trim()
                    .removePrefix("Subject:")
                    .removePrefix("subject:")
                    .trim()
                    .trim('"')
                    .trim('\'')
                    .trimStart('-', '*', '•')
                    .trim()
            }
            .filter { it.isNotBlank() }
        return lines.take(2).joinToString("\n")
    }

    private companion object {
        val SYSTEM_PROMPT: String = """
            You write concise git commit messages for a coding IDE.
            Use Conventional Commits style when the scope is obvious.
            Prefer one subject line. Add at most one short body line only when it materially helps.
            Do not return markdown, bullets, quotes, or explanations.
        """.trimIndent()
    }
}
