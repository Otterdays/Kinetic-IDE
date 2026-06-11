package com.tabletaide.ide.agent

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiMessageCodecTest {

    @Test
    fun convertMessages_mapsAssistantToolUseToOpenAiToolCalls() {
        val history = JSONArray().put(
            JSONObject().put("role", "assistant").put(
                "content",
                JSONArray()
                    .put(JSONObject().put("type", "text").put("text", "Reading."))
                    .put(
                        JSONObject()
                            .put("type", "tool_use")
                            .put("id", "call_1")
                            .put("name", "read_file")
                            .put("input", JSONObject().put("path", "README.md")),
                    ),
            ),
        )

        val converted = OpenAiMessageCodec.convertMessages(history)
        assertEquals(1, converted.length())
        val assistant = converted.getJSONObject(0)
        assertEquals("assistant", assistant.optString("role"))
        assertEquals("Reading.", assistant.optString("content"))
        val toolCall = assistant.getJSONArray("tool_calls").getJSONObject(0)
        assertEquals("call_1", toolCall.optString("id"))
        assertEquals("read_file", toolCall.getJSONObject("function").optString("name"))
    }

    @Test
    fun convertMessages_mapsToolResultToToolRole() {
        val history = JSONArray().put(
            JSONObject().put("role", "user").put(
                "content",
                JSONArray().put(
                    JSONObject()
                        .put("type", "tool_result")
                        .put("tool_use_id", "call_1")
                        .put("tool_name", "read_file")
                        .put("content", "hello"),
                ),
            ),
        )

        val converted = OpenAiMessageCodec.convertMessages(history)
        val toolMessage = converted.getJSONObject(0)
        assertEquals("tool", toolMessage.optString("role"))
        assertEquals("call_1", toolMessage.optString("tool_call_id"))
        assertEquals("hello", toolMessage.optString("content"))
    }

    @Test
    fun convertTools_mapsAnthropicSchemaToOpenAiFunctions() {
        val tools = JSONArray().put(
            JSONObject().apply {
                put("name", "read_file")
                put("description", "Read a file")
                put(
                    "input_schema",
                    JSONObject().apply {
                        put("type", "object")
                        put(
                            "properties",
                            JSONObject().put(
                                "path",
                                JSONObject().put("type", "string"),
                            ),
                        )
                    },
                )
            },
        )

        val converted = OpenAiMessageCodec.convertTools(tools)!!
        val function = converted.getJSONObject(0).getJSONObject("function")
        assertEquals("read_file", function.optString("name"))
        assertTrue(function.has("parameters"))
    }
}
