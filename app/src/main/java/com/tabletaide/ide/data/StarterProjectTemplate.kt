package com.tabletaide.ide.data

enum class StarterProjectTemplate(
    val displayName: String,
    val description: String,
) {
    BLANK(
        displayName = "Blank workspace",
        description = "Minimal folders and docs for a clean project start.",
    ),
    KOTLIN_CONSOLE(
        displayName = "Kotlin console",
        description = "Simple Kotlin source layout with a runnable main entry point.",
    ),
    WEB_APP(
        displayName = "Web starter",
        description = "Tiny HTML/CSS/JS app for quick prototypes.",
    ),
}
