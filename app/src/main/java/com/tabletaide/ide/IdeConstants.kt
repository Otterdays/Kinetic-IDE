package com.tabletaide.ide

object IdeConstants {
    // NOTE: verify model id in Anthropic docs; change if API returns 404.
    const val ANTHROPIC_MODEL = "claude-3-5-sonnet-20241022"
    const val MAX_OUTPUT_TOKENS = 4096
    const val MAX_TOOL_ROUNDS = 8

    val SYSTEM_PROMPT: String = """
        You are an AI coding assistant inside a tablet IDE. The user has opened a workspace folder via Android storage (SAF).
        Use tools read_file and write_file with paths relative to that workspace root (forward slashes).
        Prefer small, targeted edits. After writing files, summarize what changed.
    """.trimIndent()
}
