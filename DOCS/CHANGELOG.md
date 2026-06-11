<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Changelog

All notable changes to this project will be documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- **Git pull MVP:** `GitPullService`, commit-dialog **Pull** when behind, automatic pull+retry when
  push is rejected non-fast-forward.

- **P0 polish:** IDE capability banner All-files CTA; agent stream errors include provider + model id;
  `AgentToolRoundTripTest` + `GitPushFailuresTest`.

- **Multi-provider LLM routing:** OpenAI, Grok (xAI), and OpenRouter clients (OpenAI-compatible
  streaming + tools); `LlmClientResolver`, `LlmModelCatalog`, filterable `ModelPickerDialog` with
  per-provider sections; providers without API keys greyed out; expanded API keys dialog.

- **[MVP_CHECKLIST P0]** `GeminiMessageCodec` — maps assistant `tool_use` blocks to Gemini `functionCall`
  parts so multi-turn agent file edits work; unit tests in `GeminiMessageCodecTest`.

- **[MVP_CHECKLIST P0]** **Save git credentials** flow: `GitAuthDialog`, command-palette entry, commit
  dialog link, and `GitRepoUiState.pushReady` (tracked upstream + saved HTTPS token).

- **[MVP_CHECKLIST]** Startup gateway tip for shared-storage workspaces and All files access, with
  settings deep link when permission is missing.

- **[MVP_CHECKLIST]** README 5-minute device demo path and link to `DOCS/MVP_CHECKLIST.md`.

- **[ROADMAP Natural next slice / Persistent audit timeline]** Added a dedicated `AuditTimelinePanel`
  that surfaces the `AgentAuditStore` JSONL entries in a browsable dialog: expandable cards show tool
  name, target, status, risk, policy, approval, duration, and mutation summary. Accessible from the
  command palette with fuzzy filtering. Entries load on open and can be cleared from the panel.

- **[ROADMAP Epic 2.2 / Agent telemetry]** Expanded `DOCS/ROADMAP.md` with a lab-grade agent
  telemetry, evaluation, and audit observability spec: product surfaces, canonical event schema,
  token/cost accounting contracts, privacy/redaction rules, eval/regression loop, engineering
  architecture, acceptance criteria, and first implementation slice.

- **[ROADMAP Epic 2.2 / Agent telemetry]** Added first in-app telemetry slice:
  `AgentTelemetryStore`, `TelemetryEvent`, `AgentTelemetryCodec`, and `agent_telemetry.jsonl`
  persistence with bounded local retention and summary rollups.

- **[ROADMAP Epic 2.2 / Agent telemetry]** Instrumented `AgentViewModel` to emit session, turn,
  context, prompt, model-completion, tool, approval, checkpoint, mutation, cost-estimate, and error
  events around the existing agent/tool loop.

- **[ROADMAP Epic 2.2 / Agent telemetry]** Added an AI-panel telemetry strip that surfaces turn/event
  counts, rough token/cost estimates, average first-token latency, p95 tool latency, failure count,
  and last telemetry event without requiring a separate debug screen.

- **[ROADMAP Epic 2.2 / Agent telemetry]** Added focused telemetry codec tests for summary
  aggregation, JSON round-trip, and explicit rough-estimator source.

- **[ROADMAP Epic 1.2 / tree wrap-up]** Added an incremental explorer tree browse path:
  `WorkspaceRepository.listDirectoryRows(...)`, `ExplorerTreeModels`, branch-local refresh logic in
  `IdeViewModel`, folder expand/collapse in `FileTreePane`, and explicit loading / empty /
  filter-empty explorer states. Normal browsing no longer depends on eagerly materializing the full
  SAF tree up front.

- **[ROADMAP Epic 1.2 / tree wrap-up]** Added focused local unit coverage for the explorer tree slice:
  fuzzy filter ancestor retention plus pure explorer row sorting/visibility behavior for collapsed vs
  expanded trees.

- **[ROADMAP Epic 1.3 / capability clarity]** Added inline IDE-shell capability banners so users can
  see when editing + AI workspace context are available but git and/or command execution remain
  unavailable for the current workspace location.

- **[ROADMAP Epic 2.1 / Agent trust]** Added a persisted agent trust-policy layer with configurable
  **Auto / Ask / Deny** modes for file changes and destructive operations, plus **Ask / Deny**
  gating for shell commands, a generalized approval dialog, and richer tool receipts showing risk,
  policy, and decision metadata.

- **[ROADMAP Epic 2.1 / Agent trust]** Added a real agent `run_command` tool path on top of the
  in-app runner, plus an explicit approval dialog before shell execution so AI-triggered commands
  are reviewable, denyable, and return structured stdout/stderr/exit-code results into the chat.

- **[ROADMAP Ship-readiness sprint]** Added a first real in-app command runner:
  `CommandRunnerModels`, `WorkspaceExecutionResolver`, `InAppCommandRunner`, and
  `RunCommandDialog`, giving Kinetic a bounded workspace-root shell execution path with output,
  cancellation, rerun, and clear-output support.

- **[ROADMAP Agent UX]** Added a one-shot `PromptEnhancementService` so the AI panel can rewrite the
  current composer draft into a clearer prompt without using the full chat/tool loop or sending the
  message immediately.

- **[ROADMAP Git commit/push MVP]** Added real local-repo workflow services:
  `GitRepositoryResolver`, `GitStatusService`, `GitCommitService`, `GitPushService`,
  `GitRepoModels`, and `GitIdentityStore` so Kinetic can resolve repo roots, inspect status,
  commit on-device, and push the current tracked HTTPS branch with saved auth.

- **[ROADMAP Git commit/push MVP]** Added one-shot AI commit-message generation via
  `GitCommitMessageService`, using the currently selected provider plus bounded real git
  status/diff context instead of the full agent chat/tool loop.

- **[ROADMAP Git clone auth MVP]** Added real startup **HTTPS token** clone support with **JGit**,
  typed clone runtime models, clone target resolution for supported shared-storage folders, and
  startup-gateway wiring that opens a cloned repo directly into the IDE shell.

- **[ROADMAP Git clone auth MVP]** Added `GitAuthStore` with Android Keystore-backed encryption for
  saved host-scoped git tokens and backup exclusions via `res/xml/backup_rules.xml` and
  `res/xml/data_extraction_rules.xml`.

- **[ROADMAP Startup Gateway MVP]** Added a conditional launch route: `MainActivity` now shows a
  dedicated startup dashboard only when no restorable session/workspace exists, while returning users
  still resume directly into the IDE shell. `StartupGatewayScreen` ships `New Project`, `Open Folder`,
  `Clone Repository`, and `Recent Workspaces` entry points.

- **[ROADMAP Startup Gateway MVP]** Added `RecentWorkspacesStore` (`SharedPreferences`) so opened or
  restored workspaces appear in a launch-time recent list independent of editor-tab session restore.

- **[ROADMAP Startup Gateway MVP]** Added starter project creation in `WorkspaceRepository` with
  minimal templates (`Blank workspace`, `Kotlin console`, `Web starter`) plus a dedicated new-project
  dialog that opens the created workspace immediately.

- **[ROADMAP Startup Gateway MVP]** Added a validated clone-repository placeholder dialog with staged
  messaging and destination picking, intentionally deferring real git clone execution to a later phase.
  *[AMENDED 2026-05-14]: Superseded later in this same Unreleased section by the shipped real JGit
  clone/auth flow. Retained here as historical trace of the earlier gateway slice.*

- **[ROADMAP Epic 1.3] Theme modes:** Added persisted app appearance modes
  (**Dark / Light / High Contrast**) via `IdeViewModel` + `KineticTheme` selector.
  Settings gear opens a theme dialog; command palette adds direct theme actions.

- **[ROADMAP Epic 1.3] Tab-cycle UX:** Added keyboard tab cycling
  (**Ctrl+Tab / Ctrl+Shift+Tab**) and matching command-palette actions
  (**Next tab** / **Previous tab**).

- **Gradle memory fix for standalone app import:** added `app/gradle.properties` with
  `org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8` (plus standard
  Android/Kotlin flags) so IDE runs targeting `app/` do not use the default 512 MiB heap and trigger
  daemon GC-thrashing warnings.

- **Git hygiene:** `.gitignore` now excludes nested app-module Gradle artifacts (`app/.gradle/`,
  `app/gradle/`, `app/gradlew*`) and local heap dumps (`*.hprof`), and previously tracked cache files
  were untracked to keep `git status` clean after local IDE sync/build runs.

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

- **Docs roadmap cleanup:** Rewrote `DOCS/ROADMAP.md` into a concise current-status roadmap with
  clear shipped/partial/not-started lists, next slices, phases, milestones, and quality gates.
  Removed amendment-note clutter from the roadmap itself.

- **[ROADMAP Epic 1.2 / tree wrap-up]** Explorer filtering is still available, but it now sits on top
  of the incremental browse model: normal navigation loads root/expanded folders only, while query mode
  intentionally falls back to a full-tree scan for simpler fuzzy-match semantics.

- **Docs truth pass:** `README.md`, `DOCS/SUMMARY.md`, and `DOCS/ROADMAP.md` now describe the current
  MVP accurately: real startup clone/auth flow, real git commit/push, trust controls, inline
  capability banners, and the remaining shared-storage / All files access / non-PTY limitations.

- AI tool reliability was hardened: `ToolRouter` now emits the correct `input_schema` for
  `edit_file`, `AgentViewModel` validates tool definitions before sending tool-enabled requests, and
  `GeminiClientImpl` now preserves tool names in `functionResponse` and sends `toolConfig`.

- Execute surfaces are no longer placeholders: `TabletIdeScreen`, `KineticShell`, and
  `TerminalPanel` now route through the in-app runner, and the command palette adds real run/rerun/
  cancel/clear terminal actions. `Debug` was explicitly downgraded to a visible `Debug Soon` state
  instead of a no-op button.

- Autosave failures now surface visible status feedback in `IdeViewModel` instead of being silently
  swallowed during background writes.

- `AgentChatPanel` now supports an `Enhance prompt` composer action that rewrites the typed draft
  in place for review before send, using the selected provider and current workspace context.

- Kinetic shell git surfaces are now real: `KineticTopBar`, `KineticStatusBar`,
  `IdeCommandPalette`, and new `GitCommitDialog` now reflect actual repo branch/change state and
  expose `Generate message`, `Commit`, and `Commit & push` flows against the current tracked branch.

- Startup clone UI is no longer a placeholder: `CloneRepositoryDialog` now validates HTTPS URLs,
  retains the picked destination `Uri`, supports saved-token reuse/clear, masks PAT entry, shows
  progress/errors, and requests shared-storage access when needed for real clone writes.

- Explorer and agent file tooling now hide or reject `.git` paths by default once repos are cloned
  locally (`WorkspaceRepository.listTreeRows()`, `ToolRouter`).

- Android manifest now declares `MANAGE_EXTERNAL_STORAGE` for first-version shared-folder git clone
  support and references backup/data-extraction rules to exclude saved git auth from backup flows.

- Updated machine-local Gradle / Java IDE config to use `C:/Program Files/Java/jdk-25.0.2`
  in `gradle.properties` and `.vscode/settings.json`, fixing invalid `org.gradle.java.home`
  failures during root Gradle runs and Android Studio sync on this machine.

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
