package com.tabletaide.ide.agent

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiMessageCodecTest {

    @Test
    fun convertMessages_mapsAssistantToolUseToFunctionCall() {
        val history = JSONArray().put(
            JSONObject().put("role", "assistant").put(
                "content",
                JSONArray().put(
                    JSONObject()
                        .put("type", "tool_use")
                        .put("id", "call_1")
                        .put("name", "read_file")
                        .put("input", JSONObject().put("path", "README.md")),
                ),
            ),
        )

        val contents = GeminiMessageCodec.convertMessages(history)
        assertEquals(1, contents.length())
        val modelTurn = contents.getJSONObject(0)
        assertEquals("model", modelTurn.optString("role"))
        val part = modelTurn.getJSONArray("parts").getJSONObject(0)
        val functionCall = part.getJSONObject("functionCall")
        assertEquals("call_1", functionCall.optString("id"))
        assertEquals("read_file", functionCall.optString("name"))
        assertEquals("README.md", functionCall.getJSONObject("args").optString("path"))
    }

    @Test
    fun convertMessages_mapsUserToolResultToFunctionResponse() {
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

        val contents = GeminiMessageCodec.convertMessages(history)
        val userTurn = contents.getJSONObject(0)
        assertEquals("user", userTurn.optString("role"))
        val response = userTurn.getJSONArray("parts")
            .getJSONObject(0)
            .getJSONObject("functionResponse")
        assertEquals("read_file", response.optString("name"))
        assertEquals("call_1", response.optString("id"))
        assertEquals("hello", response.getJSONObject("response").optString("result"))
    }

    @Test
    fun convertMessages_preservesMultiTurnToolConversation() {
        val history = JSONArray()
            .put(
                JSONObject().put("role", "user").put("content", "edit README"),
            )
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

        val contents = GeminiMessageCodec.convertMessages(history)
        assertEquals(3, contents.length())
        assertEquals("user", contents.getJSONObject(0).optString("role"))
        assertEquals("model", contents.getJSONObject(1).optString("role"))
        assertTrue(
            contents.getJSONObject(1).getJSONArray("parts")
                .getJSONObject(1)
                .has("functionCall"),
        )
        assertEquals("user", contents.getJSONObject(2).optString("role"))
    }

    @Test
    fun convertMessages_skipsEmptyAssistantTurns() {
        val history = JSONArray().put(
            JSONObject().put("role", "assistant").put("content", JSONArray()),
        )

        val contents = GeminiMessageCodec.convertMessages(history)
        assertEquals(0, contents.length())
    }
}
