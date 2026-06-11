package com.tabletaide.ide

object IdeConstants {
    /** [ROADMAP Epic 1.1] Large buffer: simplified editor chrome + capped undo depth. */
    const val LARGE_FILE_CHAR_THRESHOLD = 1_000_000
    const val LARGE_FILE_LINE_SOFT_THRESHOLD = 8_000

    const val MAX_OUTPUT_TOKENS = 4096
    const val MAX_TOOL_ROUNDS = 8
    const val GIT_COMMIT_PATH_LIMIT = 24
    const val GIT_COMMIT_DIFF_FILE_LIMIT = 12
    const val GIT_COMMIT_DIFF_CHAR_LIMIT = 6_000
    const val GIT_COMMIT_PROMPT_MAX_CHARS = 10_000
    const val GIT_COMMIT_MESSAGE_MAX_TOKENS = 120
    const val PROMPT_ENHANCEMENT_INPUT_MAX_CHARS = 6_000
    const val PROMPT_ENHANCEMENT_CONTEXT_MAX_CHARS = 2_000
    const val PROMPT_ENHANCEMENT_MAX_TOKENS = 320

    val SYSTEM_PROMPT: String = """
        You are an AI coding assistant inside a tablet IDE. The user has opened a workspace folder via Android storage (SAF).
        All file paths are relative to the workspace root and use forward slashes.

        Available tools:
        - list_files: list all files and folders in the workspace (use first to orient yourself)
        - read_file: read the full text of a file
        - write_file: overwrite a file entirely (use for new files or full rewrites)
        - edit_file: replace a unique substring in a file (prefer over write_file for targeted edits; fails if match is missing or non-unique — add more surrounding context)
        - search_files: grep a regex pattern across workspace files, optionally filtered by path glob (e.g. "*.kt")
        - create_directory: create a folder and any missing parents
        - delete_path: permanently delete a file or folder (recursive) — use with caution
        - rename_path: rename the leaf name of a file or folder in place

        Strategy:
        1. Call list_files to understand the project structure before making changes.
        2. Use search_files to locate symbols or strings across the codebase.
        3. Prefer edit_file over write_file when changing only part of a file.
        4. Make small targeted changes. After writing files, summarize what changed and why.
    """.trimIndent()
}
