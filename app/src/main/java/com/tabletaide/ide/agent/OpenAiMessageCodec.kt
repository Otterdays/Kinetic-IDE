package com.tabletaide.ide.agent

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts Anthropic-shaped agent history and tool defs to OpenAI Chat Completions format.
 * Used by OpenAI, Grok, and OpenRouter clients.
 */
object OpenAiMessageCodec {

    fun convertTools(tools: JSONArray?): JSONArray? {
        if (tools == null || tools.length() == 0) return null
        val openAiTools = JSONArray()
        for (i in 0 until tools.length()) {
            val tool = tools.getJSONObject(i)
            openAiTools.put(
                JSONObject().apply {
                    put("type", "function")
                    put(
                        "function",
                        JSONObject().apply {
                            put("name", tool.optString("name"))
                            put("description", tool.optString("description", ""))
                            put("parameters", tool.optJSONObject("input_schema") ?: JSONObject())
                        },
                    )
                },
            )
        }
        return openAiTools
    }

    fun convertMessages(messages: JSONArray): JSONArray {
        val converted = JSONArray()
        for (i in 0 until messages.length()) {
            val msg = messages.getJSONObject(i)
            val role = msg.optString("role")
            val content = msg.opt("content")
            when {
                role == "user" && content is String -> {
                    converted.put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", content)
                        },
                    )
                }
                role == "user" && content is JSONArray -> {
                    appendUserBlocks(converted, content)
                }
                role == "assistant" && content is JSONArray -> {
                    appendAssistantBlocks(converted, content)
                }
                role == "assistant" && content is String -> {
                    converted.put(
                        JSONObject().apply {
                            put("role", "assistant")
                            put("content", content)
                        },
                    )
                }
            }
        }
        return converted
    }

    private fun appendUserBlocks(converted: JSONArray, blocks: JSONArray) {
        val textParts = StringBuilder()
        for (j in 0 until blocks.length()) {
            val part = blocks.getJSONObject(j)
            when (part.optString("type")) {
                "text" -> textParts.append(part.optString("text", ""))
                "tool_result" -> {
                    if (textParts.isNotEmpty()) {
                        converted.put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", textParts.toString())
                            },
                        )
                        textParts.clear()
                    }
                    converted.put(
                        JSONObject().apply {
                            put("role", "tool")
                            put("tool_call_id", part.optString("tool_use_id", "unknown"))
                            put("content", part.optString("content", ""))
                        },
                    )
                }
            }
        }
        if (textParts.isNotEmpty()) {
            converted.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", textParts.toString())
                },
            )
        }
    }

    private fun appendAssistantBlocks(converted: JSONArray, blocks: JSONArray) {
        val textParts = StringBuilder()
        val toolCalls = JSONArray()
        for (j in 0 until blocks.length()) {
            val part = blocks.getJSONObject(j)
            when (part.optString("type")) {
                "text" -> textParts.append(part.optString("text", ""))
                "tool_use" -> {
                    toolCalls.put(
                        JSONObject().apply {
                            put("id", part.optString("id", "call_${j}"))
                            put("type", "function")
                            put(
                                "function",
                                JSONObject().apply {
                                    put("name", part.optString("name", "unknown"))
                                    put("arguments", (part.optJSONObject("input") ?: JSONObject()).toString())
                                },
                            )
                        },
                    )
                }
            }
        }
        if (textParts.isEmpty() && toolCalls.length() == 0) return
        converted.put(
            JSONObject().apply {
                put("role", "assistant")
                if (textParts.isNotEmpty()) {
                    put("content", textParts.toString())
                } else {
                    put("content", JSONObject.NULL)
                }
                if (toolCalls.length() > 0) {
                    put("tool_calls", toolCalls)
                }
            },
        )
    }
}
