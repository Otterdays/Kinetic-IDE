<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Changelog

All notable changes to this project will be documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- **Build alignment:** root `settings.gradle.kts` now uses **AGP 9.2.0** so plugin management matches
  the app module plugin version.

- **Gradle app-module import compatibility:** `app/build.gradle.kts` declares plugin versions directly
  and `app/settings.gradle.kts` supplies plugin/dependency repositories, so IDEs that accidentally
  import `app/` as the build root can still resolve AGP / KSP / Hilt / Compose plugins.

- **[ROADMAP Epic 1.2 / Epic 2.2]** Explorer filter hoisted to **`IdeViewModel`** (`explorerTreeFilterQuery`, **`filteredTree`** via `filterTreeRows`); **`FileTreePane`** uses discrete lazy items + **content types** for STARRED / RECENT / tree rows. **`AgentViewModel`** + **`WorkspaceRepository`**: **`ToolMutationAction`** on successful **`write_file`** / **`edit_file`** tool rows — expanded card **Revert file** / **Apply again** with compare-and-swap and **CONFLICT** messaging when disk diverges.

- **Gradle IDE sync compatibility:** root `build.gradle.kts` conditionally registers a no-op
  `prepareKotlinBuildScriptModel` for projects missing it, satisfying IDE tooling that still asks for
  that legacy Kotlin script model task under AGP 9 built-in Kotlin.

- **[ROADMAP Phase 2 / Agent trust] Tool receipt MVP:** Expanded agent tool cards now show a visible
  receipt with provider, timestamp, duration, target summary, and OK/FAILED status. This is the
  first audit surface before persistent logs / approval gates.

- **[ROADMAP Epic 1.2 / Epic 1.3]** **`ExplorerPinsStore`**: workspace-scoped **recents** + **starred favorites**
  (`SharedPreferences`); explorer **STARRED** / **RECENT** lists + file context menu; **`IdeViewModel`**
  path hygiene on rename/delete. **Split divider** **snap-to-preset** on drag end (30/50/70%). **Shortcuts:**
  Ctrl+S, Ctrl+Shift+S, Ctrl+W (palette / API-keys dialog suppresses).

- **[ROADMAP Phase 2 / Agent V2] Runtime API key input:** AI Architect now has an **API keys**
  dialog (key icon + command palette command) for Anthropic and Gemini keys stored on-device in
  `LlmProviderStore` SharedPreferences. `AnthropicClientImpl` / `GeminiClientImpl` prefer runtime
  keys and keep `local.properties` / `BuildConfig` keys as fallback. Agent status shows **NEEDS KEY**
  when the selected provider has no runtime key.

- **[ROADMAP Epic 1.3 / Epic 2.2]** Tablet UX: **command palette** (`IdeCommandPalette.kt`) —
  Search rail + **Ctrl+P / ⌘P** (hardware keyboard), fuzzy filter, shortcuts for workspace, explorer
  focus, save/save all, undo/redo, clear agent chat, **editor/agent split presets** (70/30 · 50/50 ·
  30/70), Execute stub. **Draggable split** between editor stack and AI panel (`EditorAgentSplitDivider.kt`,
  fraction persisted with `rememberSaveable`). Agent **expandable tool** rows: **Copy JSON** copies tool
  request payload to clipboard (`AgentChatPanel`). Nav rail **Search** opens palette only (no snackbar stub).

- Root **`build.bat`**: runs `gradlew :app:assembleDebug` and copies the APK to **`BUILT\`**; optional argument **`release`** runs `assembleRelease` (add a `signingConfig` if AGP requires it). **`BUILT/`** in `.gitignore`.

- **[ROADMAP Epic 1.2 / Epic 2.2–2.3] Explorer + agent UX:** File tree **filter** (`TreeFilter.kt` —
  ordered-character fuzzy match, keeps ancestor folders). **Extension-based icons** in explorer
  (`ExplorerIcons.kt`). Agent panel **expandable tool cards** (tap to show full result + indented
  request JSON; large results capped for UI). **`IdeViewModel.buildAgentWorkspaceContext()`** —
  open tab paths, active file, optional selection snippet — appended to the LLM system prompt on
  each send via `AgentViewModel.sendUserMessage(..., systemPromptAppendix)`.

- **[ROADMAP Phase 2 / Agent V2] Multi-provider LLM support:** `LlmClient` interface replaces
  `AnthropicClient`; `AnthropicClientImpl` and new `GeminiClientImpl` both implement it.
  `GeminiClientImpl` translates the Anthropic tool-use wire format ↔ Gemini `functionDeclarations` /
  `functionCall` / `functionResponse` parts. `LlmProviderStore` persists selection via
  SharedPreferences. `AgentViewModel` injects both clients and selects per-message. Provider picker
  dropdown added to **AI Architect** panel header (tap to switch). `IdeConstants` gains `GEMINI_MODEL`
  (`"gemini-2.0-flash"`). `app/build.gradle.kts` reads `geminiApiKey` from `local.properties` and
  exposes it as `BuildConfig.GEMINI_API_KEY`.

- **[ROADMAP Phase 2 / Agent V1] Expanded tool router:** `ToolRouter` extended from 3 tools to 8:
  added `edit_file` (unique-string replace, fails on missing or duplicate match), `search_files`
  (regex grep with optional path glob, capped at 500 matches / 2000 files), `create_directory`,
  `delete_path` (recursive), and `rename_path`. All map to existing `WorkspaceRepository` ops.
  `SYSTEM_PROMPT` updated to enumerate all 8 tools with descriptions and a 4-step usage strategy.

- **[ROADMAP Phase 2 / Agent V1] Android agent tools:** Added `list_files` so the on-device
  Anthropic agent can inspect the opened SAF workspace before using existing `read_file` /
  `write_file` tools.

- **[ROADMAP Phase 1 / M1] Editor foundations:** Debounced grouped **undo/redo** per buffer (capped depth),
  persisted **autosave** (quiet interval + blur on tab switch for dirty buffers), **Save all**, dirty-tab
  **close guard** (save / discard / cancel), **`EditorSessionStore` + SAF session restore on cold start**
  when persistable URI permission is still granted, `persistDraftIfPossible()` on `ON_STOP`, and **dirty badge**
  (middle dot beside tab titles). Wired `WorkspaceRepository.rootTreeUriOrNull()` for session snapshots.
- **Epic 1.1 + Epic 1.2 (M1 extension):** Large-buffer banner + simplified editor chrome (>1M chars / 8k+
  lines), per-tab **scroll + text selection** persisted in `EditorSessionStore` (prefs schema v2). Explorer
  **SAF file ops MVP** (`createDirectory`, empty file, rename, duplicate UTF-8 files, recursive delete) with
  long-press menu + root **New file / New folder**; `WorkspaceRepository.writeText` now owns
  `WorkspaceMutationBus.notifyFileWritten` emission (agent tools no longer double-notify).

### Fixed

- `ToolRouter` `search_files` match cap loop: `break@outer` replaces invalid `return@outer` (compile error).

### Changed

- Product/repo identity **Kinetic**: launcher label, `rootProject.name`, `Theme.Kinetic`, `KineticApp` / `KineticTheme`; docs/README titles. Package id remains `com.tabletaide.ide`. Remote: [Otterdays/Kinetic-IDE](https://github.com/Otterdays/Kinetic-IDE).
- `README.md`: GitHub-oriented layout (shields, feature table, Kinetic Syntax preview image, stack/contributing/license sections); documents `.vscode` JDK settings for Cursor/VS Code.
- `gradle.properties`: set **`org.gradle.java.home`** to a full JDK so AGP’s `JdkImageTransform` can run `jlink` when the IDE would otherwise use a JRE without it (e.g. Cursor + Red Hat Java). Adjust path per machine or override in `~/.gradle/gradle.properties`.
- Gradle: AGP 9 **built-in Kotlin** — removed `org.jetbrains.kotlin.android`; Hilt **2.59.2** with **KSP** 2.3.6 (replaces kapt, required with built-in Kotlin); removed `android.kotlinOptions` (JVM target follows `compileOptions`).
- `DOCS/ROADMAP.md`: appended **living delivery checklist** (Markdown task boxes per phase/epic/M1–M3) synced to current implementation notes (original narrative unchanged).
- `DOCS/ROADMAP.md`: **§ Phase 2 · Tool router (granular)** addendum synced to `ToolRouter` (`list_files` … `rename_path`); Epic 2.2/2.3 row-level checklist under **§ Phase 2 · Agent UX & context** (streaming + tool rows + static `SYSTEM_PROMPT` = partial; expandable cards / context builder / telemetry = `[ ]`). Phase 2 checklist header corrected to partial MVP.

### Added

- `DOCS/ROADMAP.md`: granular Android AI IDE roadmap (Phases 1-5), epics, acceptance criteria,
  milestone map (M1-M11), cross-cutting quality/security/observability tracks, and a 90-day
  execution starter.
- `.vscode/settings.json`: `java.import.gradle.java.home` / `java.jdt.ls.java.home` (full JDK for Red Hat Java + Gradle; paths machine-specific — adjust per developer).
- Root `README.md`: quick start, prerequisites, documentation index, current Gradle/Kotlin/Hilt tooling summary.
- **Kinetic Syntax** shell aligned with `stitch_sample_1` (nav rail, tabbed editor, breadcrumbs, line gutter, AI Architect panel, terminal tabs, status bar); `KineticColors` / extended Material 3 `ColorScheme`; multi-file tabs in `IdeViewModel`.
- Android `:app` module (Phase 1 per blueprint): Jetpack Compose UI, SAF workspace, editor with basic Kotlin highlighting, Anthropic streaming client, read/write file tools, agent chat, Termux placeholder; Hilt DI with **kapt**; Gradle wrapper; `local.properties.example`. *[AMENDED 2026-03-29]: Hilt now uses **KSP** under AGP 9 built-in Kotlin — see **Changed** in this section.*
- `DOCS/` core documentation: SUMMARY, SBOM, SCRATCHPAD, STYLE_GUIDE, My_Thoughts, CHANGELOG, ARCHITECTURE — aligned with tablet IDE blueprint (`claude_ide_recommendation.html`).

---

## [0.0.0] - 2026-03-28

### Added

- Architecture blueprint as standalone HTML reference in repository root.
