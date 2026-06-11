package com.tabletaide.ide.agent

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract: after assistant tool_use + user tool_result, provider codecs must emit a valid
 * second-round request shape (model functionCall before user functionResponse for Gemini;
 * assistant tool_calls before tool role for OpenAI).
 */
class AgentToolRoundTripTest {

    @Test
    fun geminiRoundTrip_preservesFunctionCallBeforeFunctionResponse() {
        val history = buildPostToolHistory()
        val contents = GeminiMessageCodec.convertMessages(history)
        assertEquals(3, contents.length())
        val modelTurn = contents.getJSONObject(1)
        assertEquals("model", modelTurn.optString("role"))
        assertTrue(modelTurn.getJSONArray("parts").getJSONObject(1).has("functionCall"))
        val userTurn = contents.getJSONObject(2)
        assertTrue(userTurn.getJSONArray("parts").getJSONObject(0).has("functionResponse"))
    }

    @Test
    fun openAiRoundTrip_preservesToolCallsBeforeToolMessages() {
        val history = buildPostToolHistory()
        val messages = OpenAiMessageCodec.convertMessages(history)
        assertEquals(3, messages.length())
        val assistant = messages.getJSONObject(1)
        assertEquals("assistant", assistant.optString("role"))
        assertTrue(assistant.has("tool_calls"))
        assertEquals("tool", messages.getJSONObject(2).optString("role"))
    }

    private fun buildPostToolHistory(): JSONArray {
        return JSONArray()
            .put(JSONObject().put("role", "user").put("content", "edit README"))
            .put(
                JSONObject().put("role", "assistant").put(
                    "content",
                    JSONArray()
                        .put(JSONObject().put("type", "text").put("text", "Reading file."))
                        .put(
                            JSONObject()
                                .put("type", "tool_use")
                                .put("id", "call_1")
                                .put("name", "read_file")
                                .put("input", JSONObject().put("path", "README.md")),
                        ),
                ),
            )
            .put(
                JSONObject().put("role", "user").put(
                    "content",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "tool_result")
                            .put("tool_use_id", "call_1")
                            .put("tool_name", "read_file")
                            .put("content", "# Title"),
                    ),
                ),
            )
    }
}
