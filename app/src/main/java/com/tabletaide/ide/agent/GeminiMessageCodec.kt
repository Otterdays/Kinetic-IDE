package com.tabletaide.ide.agent

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts Anthropic-shaped agent history into Gemini `contents` parts.
 * [TRACE: DOCS/MVP_CHECKLIST.md] P0 Gemini multi-turn tool fix.
 */
object GeminiMessageCodec {

    fun convertMessages(messages: JSONArray): JSONArray {
        val contents = JSONArray()
        for (i in 0 until messages.length()) {
            val msg = messages.getJSONObject(i)
            val role = when (msg.optString("role")) {
                "user" -> "user"
                "assistant" -> "model"
                else -> continue
            }
            val content = msg.opt("content")
            val parts = when (content) {
                is JSONArray -> partsFromBlocks(content)
                is String -> JSONArray().put(JSONObject().put("text", content))
                else -> JSONArray()
            }
            if (parts.length() == 0) continue
            contents.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", parts)
                },
            )
        }
        return contents
    }

    private fun partsFromBlocks(blocks: JSONArray): JSONArray {
        val parts = JSONArray()
        for (j in 0 until blocks.length()) {
            val part = blocks.getJSONObject(j)
            when (part.optString("type")) {
                "text" -> {
                    val text = part.optString("text", "")
                    if (text.isNotEmpty()) {
                        parts.put(JSONObject().put("text", text))
                    }
                }
                "tool_use" -> {
                    val toolUseId = part.optString("id", "unknown")
                    val name = part.optString("name", "unknown")
                    val input = part.optJSONObject("input") ?: JSONObject()
                    parts.put(
                        JSONObject().apply {
                            put(
                                "functionCall",
                                JSONObject().apply {
                                    put("id", toolUseId)
                                    put("name", name)
                                    put("args", input)
                                },
                            )
                        },
                    )
                }
                "tool_result" -> {
                    val toolUseId = part.optString("tool_use_id", "unknown")
                    val toolName = part.optString("tool_name").ifBlank { toolUseId }
                    parts.put(
                        JSONObject().apply {
                            put(
                                "functionResponse",
                                JSONObject().apply {
                                    put("id", toolUseId)
                                    put("name", toolName)
                                    put(
                                        "response",
                                        JSONObject().apply {
                                            put("result", part.optString("content", ""))
                                            put("toolUseId", toolUseId)
                                        },
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
        return parts
    }
}
