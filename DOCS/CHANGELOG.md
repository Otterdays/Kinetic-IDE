<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Changelog

All notable changes to this project will be documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed

- `README.md`: GitHub-oriented layout (shields, feature table, Kinetic Syntax preview image, stack/contributing/license sections); documents `.vscode` JDK settings for Cursor/VS Code.
- `gradle.properties`: set **`org.gradle.java.home`** to a full JDK so AGP’s `JdkImageTransform` can run `jlink` when the IDE would otherwise use a JRE without it (e.g. Cursor + Red Hat Java). Adjust path per machine or override in `~/.gradle/gradle.properties`.
- Gradle: AGP 9 **built-in Kotlin** — removed `org.jetbrains.kotlin.android`; Hilt **2.59.2** with **KSP** 2.3.6 (replaces kapt, required with built-in Kotlin); removed `android.kotlinOptions` (JVM target follows `compileOptions`).

### Added

- `.vscode/settings.json`: `java.import.gradle.java.home` / `java.jdt.ls.java.home` (full JDK for Red Hat Java + Gradle; paths machine-specific — adjust per developer).
- Root `README.md`: quick start, prerequisites, documentation index, current Gradle/Kotlin/Hilt tooling summary.
- **Kinetic Syntax** shell aligned with `stitch_sample_1` (nav rail, tabbed editor, breadcrumbs, line gutter, AI Architect panel, terminal tabs, status bar); `KineticColors` / extended Material 3 `ColorScheme`; multi-file tabs in `IdeViewModel`.
- Android `:app` module (Phase 1 per blueprint): Jetpack Compose UI, SAF workspace, editor with basic Kotlin highlighting, Anthropic streaming client, read/write file tools, agent chat, Termux placeholder; Hilt DI with **kapt**; Gradle wrapper; `local.properties.example`. *[AMENDED 2026-03-29]: Hilt now uses **KSP** under AGP 9 built-in Kotlin — see **Changed** in this section.*
- `DOCS/` core documentation: SUMMARY, SBOM, SCRATCHPAD, STYLE_GUIDE, My_Thoughts, CHANGELOG, ARCHITECTURE — aligned with tablet IDE blueprint (`claude_ide_recommendation.html`).

---

## [0.0.0] - 2026-03-28

### Added

- Architecture blueprint as standalone HTML reference in repository root.
