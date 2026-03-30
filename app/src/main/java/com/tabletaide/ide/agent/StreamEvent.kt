package com.tabletaide.ide.agent

import org.json.JSONObject

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data class ToolUseComplete(val id: String, val name: String, val input: JSONObject) : StreamEvent
    data object Finished : StreamEvent
    data class Failure(val message: String) : StreamEvent
}
