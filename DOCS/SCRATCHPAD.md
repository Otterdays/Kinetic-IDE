<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD — Kinetic

## MVP happy-path demo (documented — run on device)

1. `gradlew.bat :app:assembleDebug` → install APK on tablet/emulator.
2. Startup → **Grant All files access** → **Clone repository** → `Downloads/KineticTest` → HTTPS URL + GitHub PAT (`repo` scope) → save token.
3. AI panel → **API keys** → add Gemini or Claude key → tap model label → pick model.
4. Prompt: *Read README.md and add `<!-- kinetic mvp test -->` as the first line.* Approve write if Ask policy is on.
5. `Ctrl+P` → **Commit and push** → generate message → **Commit & push** → confirm on github.com.
6. 👤 **USER:** Log device model + Android version here after first successful run: `_______________`

## Session checkpoint — 2026-06-11 (GitHub OAuth clone)

- **Done:** GitHub OAuth (PKCE) + repo list in **Clone repository → GitHub** tab; clone + `openWorkspaceRoot` unchanged.
- **Setup:** GitHub OAuth App callback `com.tabletaide.ide://oauth/github`; `githubOAuthClientId` in `local.properties`.
- **Verify:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` **BUILD SUCCESSFUL** (`GitHubApiParserTest`).

## Session checkpoint — 2026-06-11 (P0 code-complete)

- **Done:** `GitPullService` + **Pull** in commit dialog; auto pull+retry on non-fast-forward push.
- **Done:** IDE `CapabilityBanner` All-files CTA; agent errors show `Provider · model: …`.
- **Done:** `AgentToolRoundTripTest`, `GitPushFailuresTest`; MVP_CHECKLIST marks code P0 complete vs 👤 USER device QA.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest` — run before push.
- **Next 👤 USER:** Manual QA script in `DOCS/MVP_CHECKLIST.md`.

## Session checkpoint — 2026-06-11 (multi-provider LLM)

- **Done:** Added OpenAI, Grok, OpenRouter clients (`OpenAiChatClientImpl` engine) + `LlmModelCatalog`
  + filterable `ModelPickerDialog` (grey out providers missing keys).
- **Done:** Expanded API keys dialog for all five providers; per-provider model persistence in
  `LlmProviderStore`.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest` **BUILD SUCCESSFUL**
  (includes `OpenAiMessageCodecTest`, `GeminiMessageCodecTest`).
- **Next:** Device smoke OpenRouter tool loop; optional custom OpenRouter slug field; fetch models
  from `GET /api/v1/models` later.

## Session checkpoint — 2026-06-11 (MVP hardening pass)

- **Done:** Fixed Gemini multi-turn agent tools via `GeminiMessageCodec` (`tool_use` → `functionCall`);
  4 unit tests in `GeminiMessageCodecTest`.
- **Done:** Git auth for opened (non-cloned) repos — `GitAuthDialog`, palette **Save git credentials**,
  commit dialog **Save git token** link, `GitRepoUiState.pushReady` gates push.
- **Done:** Startup gateway workspace tip + **Grant All files access** deep link when missing.
- **Done:** README 5-minute device demo; `MVP_CHECKLIST.md` updated with completed P0 items.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest` **BUILD SUCCESSFUL**.
- **Next (human/device):** Run `DOCS/MVP_CHECKLIST.md` manual QA script on tablet/emulator — especially
  Gemini `read_file` + `edit_file` loop and HTTPS push smoke.
- **Next (code):** `git pull` on non-fast-forward reject; optional `GitPushService` bare-repo test.

## Session checkpoint — 2026-05-14 (persistent audit timeline shipped)

- **Done:** Added persistent audit timeline UI as the next natural roadmap slice:
  `AuditTimelinePanel` is a Dialog-based composable showing `AgentAuditStore` entries in a
  scrollable, expandable timeline. Each card shows tool name, target, status (with color dot),
  risk class, policy mode, approval outcome, duration, and mutation summary — collapsed by
  default, expandable on tap.
- **Done:** Wired `AgentViewModel` with `loadAuditEntries()` and `clearAuditEntries()` state/actions;
  entries load on panel open via `LaunchedEffect`.
- **Done:** Added `Audit timeline` command palette entry (keywords: audit, history, log, agent,
  timeline, receipt) wired to the panel visibility state.
- **Done:** Panel keyboard-guard added so shortcuts do not fire while the audit dialog is open.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Docs:** `DOCS/ROADMAP.md` marks persistent audit timeline as partial MVP (filter/sort/export
  still `[ ]`); `DOCS/CHANGELOG.md` and `DOCS/SUMMARY.md` amended.
- **Known limitation:** No filter/sort controls in the panel yet — entries are shown newest-first
  with the full list loaded on open. No export/redaction for audit entries.
- **Next:** Either filter/sort for the audit panel, or the next roadmap slice (LSP MVP for Kotlin).

## Session checkpoint — 2026-05-14 (tree wrap-up shipped)

- **Done:** Replaced the explorer’s eager full-tree browse path with an incremental node model:
  `WorkspaceRepository.listDirectoryRows(...)`, `IdeViewModel` root/subtree loading, visible-row
  derivation, and branch-local refresh after file mutations.
- **Done:** `FileTreePane` now renders explorer-specific visible rows with folder expand/collapse plus
  explicit loading, empty-workspace, and filter-empty states without disturbing STARRED / RECENT pins.
- **Done:** Added focused local unit tests for explorer filtering and pure tree row visibility/sorting,
  then verified with `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest` from repo root
  (**BUILD SUCCESSFUL**).
- **Known limitation:** Query mode still materializes the full tree while a filter is active; that is
  now a follow-up optimization rather than the default browse-path behavior.
- **Next:** If we want a richer “tree visualization” later, build it as a separate read-only surface
  backed by the same explorer node model instead of replacing the main Explorer pane.

## Session checkpoint — 2026-05-14 (tree wrap-up in progress)

- **In progress:** Replacing the explorer’s eager full-tree materialization with an incremental
  node model: root/subdirectory loading, visible-row derivation, and branch-local refreshes now live
  in `IdeViewModel` on top of a new `WorkspaceRepository.listDirectoryRows(...)` API.
- **In progress:** `FileTreePane` is being moved from raw `TreeRow` rendering to explorer-specific
  visible rows so folder expand/collapse, loading states, and empty/filter-empty messaging can ship
  without changing agent/file-tool internals.
- **In progress:** Added minimal local unit-test scaffolding (`junit:junit`) plus RED tests for the
  new explorer model/filter helpers before finishing the production implementation.
- **Next:** Run the new unit target, fix compile/runtime issues from the model/UI refactor, then update
  `DOCS/ROADMAP.md` + `DOCS/CHANGELOG.md` once the tree slice is green.

## Session checkpoint — 2026-05-14 (docs truth pass shipped)

- **Done:** Updated `README.md` to reflect the current MVP instead of older Phase 1 / terminal-stub
  wording: real startup clone/auth flow, git commit/push, trust controls, and bounded command-runner
  constraints are now called out explicitly.
- **Done:** Synced `DOCS/SUMMARY.md`, `DOCS/ROADMAP.md`, and `DOCS/CHANGELOG.md` to the real app state,
  including the new capability-banner UX and the fact that startup clone is no longer a placeholder.
- **Done:** Annotated the stale historical changelog/summary wording rather than deleting it, keeping
  the append-only doc trail intact while making the current status clear.
- **Done:** Updated `DOCS/SBOM.md` with a no-dependency-change note for this slice.
- **Next:** Optional follow-up is a small UI copy pass in `StartupGatewayScreen.kt` so the gateway card
  text matches the now-correct docs and shipped clone behavior.

## Session checkpoint — 2026-05-14 (capability banners shipped)

- **Done:** Added a dedicated `CapabilityBanner` surface in the IDE shell so workspace limitations are
  visible inline instead of only surfacing as disabled actions or dialog errors.
- **Done:** Wired banner messaging from existing `GitRepoUiState.message` and
  `CommandRunnerUiState.availabilityMessage`, keeping the capability copy truthful to the current
  workspace/path constraints without introducing duplicate resolver logic.
- **Done:** Banner copy explicitly tells the user that editing + AI workspace context still work while
  git and/or command execution may remain unavailable in unsupported locations.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Next:** Do the broader docs truth pass separately (startup gateway wording, README status, roadmap
  sync) once the current UI slice is accepted.

## Session checkpoint — 2026-05-14 (trust policy layer shipped)

- **Done:** Added persisted agent trust policy state via `AgentTrustStore` with configurable
  **Auto / Ask / Deny** modes for file changes, destructive ops, and shell commands.
- **Done:** Generalized the approval gate from command-only handling to broader risky-tool handling in
  `AgentViewModel`, so `write_file`, `edit_file`, `create_directory`, `rename_path`, `delete_path`,
  and `run_command` now flow through centralized policy decisions.
- **Done:** Replaced the command-only approval dialog with a generic tool approval dialog that shows
  risk class, target, policy mode, assistant rationale, and request JSON before execution.
- **Done:** Expanded tool receipts in the chat UI to show risk class, policy mode, and approval
  decision alongside the existing provider/time/status metadata.
- **Done:** Added a lightweight settings surface so trust modes can be changed in-app without editing
  code, while keeping theme controls in the same settings entry point.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Known limitation:** Trust policy is still app-wide, not per-workspace or per-tool; audit history
  is still session-local chat UI only; dry-run and explain-before-change remain future work.
- **Next:** Highest-value follow-up is either destructive-op dry-run/preview or a durable audit
  timeline so approvals and tool actions survive beyond the live conversation session.

## Session checkpoint — 2026-05-14 (trust policy layer in progress)

- **In progress:** Broadening the agent trust layer beyond command-only approval by adding a small
  persisted policy store, generic risky-tool approval state, and richer receipt metadata.
- **Direction:** Keep the existing `AgentViewModel` orchestration choke point and reuse the current
  modal approval slot in `TabletIdeScreen` so the safety logic stays centralized and low-churn.
- **Next:** Land trust-policy models/store first, then generalize approval across file-mutation and
  destructive tools, then add a minimal trust settings dialog and verify compile/docs.

## Session checkpoint — 2026-05-13 (agent run command shipped)

- **Done:** Added a real `run_command` agent tool to `ToolRouter`, backed by the existing
  `InAppCommandRunner` instead of a disconnected placeholder path.
- **Done:** Added explicit approval UX for agent command execution with
  `AgentCommandApprovalState` and `AgentCommandApprovalDialog`, so shell commands pause for user
  review before they run.
- **Done:** Extended the runner with tool-oriented execution results (`stdout`, `stderr`, exit code,
  cancellation) so agent tool receipts correctly report success vs failure and return a useful
  command transcript back into the chat loop.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Known limitation:** Agent command execution still inherits the runner MVP limits: one foreground
  workspace-root command at a time, `/system/bin/sh` only, no PTY, no timeout presets, and no
  broader per-tool approval policy matrix yet.
- **Next:** Highest-value follow-up is either richer tool policy controls (auto/ask/deny classes,
  dry-run rules) or the next trust surface from roadmap Phase 2/3 such as diagnostics/LSP plumbing.

## Session checkpoint — 2026-05-13 (agent run command in progress)

- **In progress:** Beginning the next roadmap slice from Phase 2 tool router: real `run_command`
  support for the agent, backed by the new in-app runner instead of leaving execution disconnected
  from AI tool calls.
- **Direction:** Add the tool schema plus a user approval step before execution so shell commands are
  reviewable and cancelable rather than silently executed by the model.
- **Next:** Extend the runner for agent-awaitable command results, wire approval state into
  `AgentViewModel` / `TabletIdeScreen`, and verify the tool round-trip with a full Kotlin compile.

## Session checkpoint — 2026-05-13 (ship-readiness sprint shipped)

- **Done:** Fixed the highest-confidence AI tool-loop breakpoints: `ToolRouter.toolDefinitions()`
  now emits the correct `input_schema` for `edit_file`, `AgentViewModel` validates tool schema
  before sending tool-enabled requests, and `GeminiClientImpl` now preserves tool names in
  `functionResponse` plus actually sends `toolConfig`.
- **Done:** Added a first real in-app runner stack:
  `CommandRunnerModels`, `WorkspaceExecutionResolver`, `InAppCommandRunner`, and `RunCommandDialog`.
  The shell now supports one foreground workspace-root command at a time with output capture,
  cancellation, rerun, clear-output, and debug/event lines.
- **Done:** Replaced execute stubs across `TabletIdeScreen`, `KineticShell`, and `TerminalPanel`
  with real runner-backed flows. Command palette now exposes run/rerun/cancel/clear actions.
- **Done:** Autosave failures are no longer swallowed silently; `IdeViewModel.autosaveTabIfDirty()`
  now surfaces visible status feedback when background writes fail.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Known limitation:** The first in-app runner still depends on a workspace that resolves to a real
  shared-storage filesystem path with **All files access**. It is not a PTY shell, only supports one
  foreground command at a time, and does not provide real debugger integration yet.

## Session checkpoint — 2026-05-13 (prompt enhancer shipped)

- **Done:** Added a Trae-style `Enhance prompt` flow to `AgentChatPanel` so the current draft can be
  rewritten in-place for review before send.
- **Done:** Added `PromptEnhancementService` as a one-shot provider-backed rewrite path that preserves
  user intent, does not answer the prompt, and stays separate from the full chat/tool loop.
- **Done:** Hoisted chat composer draft state into `AgentViewModel` with explicit `composerDraft` and
  `enhancingPrompt` state, plus send/enhance actions so the input can be safely replaced after rewrite.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-05-13 (prompt enhancer in progress)

- **In progress:** Adding a Trae-style composer enhancement flow for `AgentChatPanel` so the user can
  click an `Enhance prompt` action, let the provider rewrite the current draft, review the result,
  and only then hit send.
- **In progress:** Added `PromptEnhancementService` as a one-shot provider-backed rewrite path
  separate from the main chat/tool loop, plus `AgentViewModel` composer draft + enhancing state so
  enhanced text can replace the current input cleanly.
- **Next:** Wire the compose UI button/states in the chat panel, then verify `:app:compileDebugKotlin`
  and amend summary/changelog if the feature ships cleanly.

## Session checkpoint — 2026-05-13 (git commit/push MVP shipped)

- **Done:** Added real repo workflow services for opened local repos:
  `GitRepositoryResolver`, `GitStatusService`, `GitCommitService`, `GitPushService`,
  `GitRepoModels`, and `GitIdentityStore`.
- **Done:** Added one-shot AI commit-message generation in
  `app/src/main/java/com/tabletaide/ide/agent/GitCommitMessageService.kt`, using current provider
  selection plus bounded real git status/diff input instead of the full agent chat/tool loop.
- **Done:** Added repo state + commit dialog flow in `IdeViewModel` and UI wiring across
  `TabletIdeScreen`, `KineticShell`, `IdeCommandPalette`, and new `GitCommitDialog`.
  The shell now shows real branch/status counts, supports AI draft generation, commit, and
  tracked-branch commit-and-push, with retryable push when the repo is already ahead.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Known limitation:** Push remains HTTPS-only for MVP and depends on saved host-scoped auth from
  the clone/auth flow; branch creation, remote picking, SSH, and force-push remain out of scope.

## Session checkpoint — 2026-05-13 (git commit/push MVP in progress)

- **In progress:** Added the first real commit/push runtime layer under `app/src/main/java/com/tabletaide/ide/data/`:
  `GitRepoModels`, `GitRepositoryResolver`, `GitStatusService`, `GitCommitService`, `GitPushService`,
  plus `GitIdentityStore` for author name/email persistence across commit attempts.
- **In progress:** Added a one-shot provider-backed AI path in
  `app/src/main/java/com/tabletaide/ide/agent/GitCommitMessageService.kt` so commit-message generation
  can use real git status + bounded diff context without the full tool/chat loop.
- **Constraint:** Git workflow is intentionally scoped to repo roots opened from resolvable shared-storage
  paths, current checked-out branch, and tracked HTTPS upstreams with saved host-scoped auth.
- **Next:** Wire repo state into `IdeViewModel`, add commit dialog + top-bar/status-bar/palette actions,
  then verify compile/build and update summary/changelog/SBOM for the shipped git workflow.

## Session checkpoint — 2026-05-13 (git clone auth MVP shipped)

- **Done:** Real startup clone flow now exists for **HTTPS token auth** using **JGit**. Added:
  `GitCloneModels`, `GitAuthStore` (Keystore-backed token encryption), `CloneTargetResolver`,
  `GitCloneService`, `GitCloneUiState`, and `IdeViewModel.cloneRepository(...)`.
- **Done:** Startup clone dialog upgraded from placeholder to real flow: retains destination `Uri`,
  validates HTTPS repo URLs, supports saved-token reuse/clear, masks PAT entry, shows progress,
  and opens the cloned repo into the IDE on success.
- **Done:** Repo guardrails added: explorer hides `.git` by default via `WorkspaceRepository.listTreeRows()`,
  and agent file tools now reject `.git` paths in `ToolRouter`.
- **Done:** Backup hardening for stored git auth: `kinetic_git_auth.xml` excluded from Android
  cloud backup/device transfer via `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.
- **Known limitation:** Shared-folder clone currently requires **All files access** and only supports
  resolvable primary shared-storage folders for the first real git slice.

## Session checkpoint — 2026-05-13 (git clone auth MVP in progress)

- **In progress:** Real git clone/auth slice started with new runtime files:
  `GitCloneModels`, `GitAuthStore`, `CloneTargetResolver`, `GitCloneService`.
- **Direction:** HTTPS token auth first, JGit runtime, shared-folder clone target restricted to
  resolvable primary shared storage, with explicit rejection for unsupported destinations.
- **Security:** Token storage is being implemented with Android Keystore-backed encryption rather
  than embedding credentials in URLs or recent-workspace state.
- **Next:** Wire the resolver + clone service into `IdeViewModel` and replace the startup clone
  placeholder with a real auth/progress flow.

## Session checkpoint — 2026-05-13 (root JDK path corrected)

- **Issue:** Root Gradle / Android Studio sync still pointed at stale JDK path
  `C:/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot`, causing
  `org.gradle.java.home` invalid-path failure.
- **Done:** Updated `gradle.properties` `org.gradle.java.home` and
  `.vscode/settings.json` Java homes to `C:/Program Files/Java/jdk-25.0.2`
  / `C:\\Program Files\\Java\\jdk-25.0.2`.
- **Verify:** `.\gradlew.bat :app:compileDebugKotlin` from repo root **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-05-13 (startup gateway MVP shipped)

- **Done:** Added a conditional startup route for `MainActivity` / `IdeViewModel`: restoreable sessions
  still enter the IDE shell directly; otherwise the app now lands on a dedicated startup gateway.
- **Done:** Added `RecentWorkspacesStore` plus gateway recent-workspace reopen flow; successful
  restore/open actions now refresh recent workspaces independently of editor session snapshots.
- **Done:** Added startup UI surface (`StartupGatewayScreen`) with `New Project`, `Open Folder`,
  `Clone Repository`, and recent workspaces, plus shared SAF picker plumbing reusable by the gateway
  and the shell.
- **Done:** Added starter project creation (`Blank workspace`, `Kotlin console`, `Web starter`) via
  `WorkspaceRepository.createStarterProject`, creating a real folder structure then opening it.
- **Done:** Added validated clone placeholder dialog with repository URL + destination selection and
  explicit staged messaging for later git implementation.
- **Verify:** `..\gradlew.bat compileDebugKotlin` from `app/` **BUILD SUCCESSFUL**.
- **Note:** Root build path still inherits a machine-specific stale `org.gradle.java.home` from
  `gradle.properties`; app-module standalone verification succeeded without changing repo config.

## Session checkpoint — 2026-05-13 (startup gateway MVP in progress)

- **In progress:** Startup Gateway MVP implementation from attached plan. Current sequence:
  launch routing/state first, then recent workspaces, then gateway UI, then new-project wizard,
  then clone placeholder polish.
- **Intent:** Keep cold-start resume behavior for existing sessions and only show the welcome
  gateway when no restorable workspace/session exists.
- **Next:** Add shared workspace picker plumbing plus `IdeViewModel` startup route state before
  building the dedicated gateway surface.

## Session checkpoint — 2026-04-30 (AI IDE QoL + edge hardening)

- **Done:** AI Architect send affordance is now key-aware and less brittle:
  `sendEnabled = !busy && draft.isNotBlank() && hasSelectedProviderKey` in `AgentChatPanel`.
- **Done:** Status header now includes current neutral/provider label (`ASSISTANT STATUS · ...`) so the user
  sees context even before first message; no forced provider branding when no key exists.
- **Done:** `AgentViewModel.sendUserMessage` wrapped with `try/catch/finally` so unexpected failures cannot
  leave the panel stuck in busy state; explicit fallback error emitted.
- **Done:** Clearing stale errors on provider/key updates (`setProvider`, `setApiKey`) so credential fixes
  immediately recover UX without requiring extra actions.
- **Done:** Compose deprecation cleanup in `AgentChatPanel` (`Icons.AutoMirrored.Filled.Send`).
- **Verify:** `./gradlew.bat :app:compileDebugKotlin` **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-04-30 (theme modes UX)

- **Done:** Implemented roadmap Epic 1.3 theme modes: `KineticThemeMode` (Dark/Light/High Contrast),
  `KineticTheme(mode=...)` color-scheme switching, persisted `IdeViewModel.themeMode` in
  `kinetic_ui_settings` prefs, Settings-gear theme dialog, and command-palette theme actions.
  `MainActivity` now binds theme from `IdeViewModel` so mode applies app-wide. Added editor tab
  cycling shortcuts (`Ctrl+Tab` / `Ctrl+Shift+Tab`) and palette actions (`Next tab`, `Previous tab`).
- **Verify:** `./gradlew.bat :app:compileDebugKotlin` **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-04-30 (AI Architect provider label + key gate)

- **Issue:** AI Architect header showed provider branding ("Claude") on first open, which looked hardcoded
  before users configured credentials.
- **Fix:** `AgentChatPanel` now shows neutral header text (`Add API key`) until the selected provider
  has a key, adds inline guidance (`Add an API key with the key icon to start chatting.`), and keeps
  status at **NEEDS KEY**.
- **Guardrail:** `AgentViewModel.sendUserMessage` now fails fast with explicit error when selected provider
  has no key (`API key required. Tap the key icon in AI Architect to add one.`).
- **Verify:** `./gradlew.bat :app:compileDebugKotlin` **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-04-30 (Gradle incremental cache corruption recovery)

- **Issue:** Multi-task assemble run failed with missing generated intermediates (`mergeExtDexDebug`,
  `compileDebugJavaWithJavac`) and Kotlin daemon incremental cache close/assertion errors (`*.tab`
  storage already registered) under `app/build/kotlin/.../cacheable/caches-jvm`.
- **Fix:** Reset daemon + ephemeral build outputs, then rerun full targets from repo root:
  `./gradlew.bat --stop` → delete `app/build` → `./gradlew.bat :app:assembleDebug :app:assembleDebugUnitTest :app:assembleDebugAndroidTest --no-build-cache --rerun-tasks`.
- **Result:** **BUILD SUCCESSFUL** (all three targets assembled). No source changes required.

## Session checkpoint — 2026-04-30 (standalone app Gradle memory)

- **Issue:** Running Gradle tasks from `app/` showed daemon GC thrashing with default 512 MiB heap.
- **Fix:** Added `app/gradle.properties` with `org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m`
  plus Android/Kotlin defaults for the standalone app-import path. Verified with
  `..\gradlew.bat clean compileDebugKotlin` from `app/` (**BUILD SUCCESSFUL** after clean rerun).

## Session checkpoint — 2026-04-30 (git noise cleanup)

- **Done:** Added `.gitignore` coverage for nested app Gradle artifacts (`app/.gradle/`, `app/gradle/`,
  `app/gradlew*`) and local heap dumps (`*.hprof`); untracked previously tracked cache lock/bin files
  under `app/.gradle` to stop recurring dirty status after local sync/build.

## Session checkpoint — 2026-04-30 (AGP bump)

- **Done:** Aligned root plugin management to **AGP 9.2.0** in `settings.gradle.kts` (app module was
  already on 9.2.0). Updated `DOCS/SBOM.md` build toolchain row accordingly.

## Session checkpoint — 2026-04-30 (SBOM clarity pass)

- **Done:** Reviewed build sources (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
  `app/settings.gradle.kts`, wrapper properties) and amended `DOCS/SBOM.md` with a canonical,
  grouped inventory: build toolchain/plugins, runtime deps, build-time codegen, debug/test scopes,
  and repository/import notes.
- **Next:** Keep canonical SBOM section as the first update target when any Gradle plugin/dependency changes.

## Session checkpoint — 2026-04-30 (app-module standalone Gradle import)

- **Issue:** Importing/running Gradle from `app/` made `com.android.application` unresolved because
  plugin versions/repos lived only in root `settings.gradle.kts`.
- **Fix:** `app/build.gradle.kts` now declares plugin versions explicitly and `app/settings.gradle.kts`
  supplies `google()`, `mavenCentral()`, and `gradlePluginPortal()` for standalone IDE import.
  Verified both root `.\gradlew.bat :app:prepareKotlinBuildScriptModel :app:compileDebugKotlin`
  and app-dir `..\gradlew.bat prepareKotlinBuildScriptModel compileDebugKotlin` **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-04-30 (explorer filteredTree + agent apply/revert)

- **Done:** **`IdeViewModel`**: `explorerTreeFilterQuery`, `setExplorerTreeFilterQuery`, **`filteredTree`**
  recomputed in `refreshTree`. **`FileTreePane`**: filter wired from VM; STARRED / RECENT / divider / tree as
  separate **`LazyColumn`** items with **`contentType`**. **`AgentViewModel`**: pre/post disk snapshots for
  **`write_file`** / **`edit_file`**; **`revertToolMutation`** / **`applyToolMutation`**; **`ToolMutationPhase`**
  (**APPLIED_ON_DISK**, **REVERTED**, **CONFLICT**). **`AgentChatPanel`**: **Revert file** / **Apply again** when expanded.
  **`TabletIdeScreen`** passes filtered rows + mutation callbacks. `:app:compileDebugKotlin` OK. ROADMAP/CHANGELOG/SUMMARY amended.
- **Next:** Incremental SAF tree listing; dirty-tab coordination for revert; markdown in chat.

## Session checkpoint — 2026-04-30 (Gradle IDE sync compatibility)

- **Issue:** IDE Gradle sync requested `:app:prepareKotlinBuildScriptModel`, but AGP 9 built-in
  Kotlin does not guarantee that legacy Kotlin script model task exists on every project.
- **Fix:** Root `build.gradle.kts` conditionally registers a no-op `prepareKotlinBuildScriptModel`
  only on projects missing it, preserving AGP built-in Kotlin and avoiding `org.jetbrains.kotlin.android`.
  Verified `.\gradlew.bat :app:prepareKotlinBuildScriptModel :app:compileDebugKotlin` **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-04-30 (tool receipt MVP)

- **Done:** Phase 2 trust slice: agent tool cards now carry visible receipts from `AgentViewModel`
  (`ToolReceipt`) into `AgentChatPanel`: provider, clock time, duration, target summary, and
  OK/FAILED status. `DOCS/ROADMAP.md` marks operation receipts as partial (persistent audit log
  still pending). CHANGELOG updated.
- **Next:** Persist receipts across sessions and add approval gates / dry-run policies for destructive tools.

## Session checkpoint — 2026-04-30 (runtime API keys)

- **Done:** API keys are now part of the app flow, not just build setup. `LlmProviderStore` stores
  Anthropic/Gemini runtime keys in SharedPreferences; `AgentViewModel` exposes credential state;
  `AnthropicClientImpl` / `GeminiClientImpl` prefer runtime keys with `BuildConfig` fallback.
  `AgentChatPanel` adds a key action and **NEEDS KEY** status; command palette opens the same
  `ApiKeysDialog`. `local.properties.example`, README, CHANGELOG updated.
- **Next:** Consider masking/toggle visibility and Android Keystore-backed encryption before public beta.

## Session checkpoint — 2026-04-30 (Epic 1.2 pins + 1.3 snap / shortcuts)

- **Done:** **`ExplorerPinsStore`** (`ExplorerPinsStore.kt`): per-workspace SharedPreferences **recents** (opened files,
  capped) + **starred favorites**; bind on workspace open / restore; track opens on tab/file select;
  rename/delete/sync paths on explorer SAF rename/delete. **`FileTreePane`**: STARRED + RECENT blocks above tree,
  long-press **Add/remove favorite** on files. **`IdeViewModel`**: `openExplorerPinnedPath`, `toggleExplorerFavorite`,
  `toggleFavoriteActiveTab`. **Split divider:** **snap on drag end** to nearest 30/50/70% preset within threshold
  (`snapEditorAgentFraction`). **Shortcuts:** Ctrl+Shift+S save all, Ctrl+S save, Ctrl+W close tab (when palette/API
  dialog closed); palette footer updated. `:app:compileDebugKotlin` OK.
- **Next:** LazyColumn virtualization for huge trees; tab-cycle shortcuts; conflict prompts.

## Session checkpoint — 2026-04-30 (Epic 1.3 UI: palette + split + tool copy)

- **Done:** **Command palette** (`IdeCommandPalette.kt`): rail Search + Ctrl/⌘P, filter field,
  actions (workspace, explorer focus, save/save all, undo/redo, clear chat, split presets, Execute stub).
  **Editor/agent split:** draggable divider (`EditorAgentSplitDivider.kt`), fractional widths via
  `rememberSaveable`, presets from palette. **Agent:** expanded tool row **Copy JSON** → clipboard.
  Nav rail Search no longer toggles a stub snackbar / phantom rail section.
  `:app:compileDebugKotlin` OK.
- **Next:** Gesture snap on divider release; richer shortcuts; Epic 2.2 apply/revert for edits.

## Session checkpoint — 2026-04-30 (multi-provider LLM + tool router expansion)

- **Done:**
  - `AnthropicClient` interface renamed → `LlmClient`; `GeminiClientImpl` added (Anthropic ↔ Gemini tool-format adapter); `LlmProviderStore` (SharedPreferences)
  - `AgentViewModel` injects both impls, selects per `_provider` state; provider saved across sessions
  - `AgentChatPanel` header gains clickable provider name → `DropdownMenu` (Claude / Gemini)
  - `TabletIdeScreen` threads `provider` state + `onProviderChange` through to panel
  - `app/build.gradle.kts` + `IdeConstants`: `GEMINI_MODEL` + `BuildConfig.GEMINI_API_KEY`
  - `ToolRouter` expanded: 3 → 8 tools (`edit_file`, `search_files`, `create_directory`, `delete_path`, `rename_path`)
  - `SYSTEM_PROMPT` rewritten to enumerate all 8 tools + 4-step usage strategy
- **Next:** Add `geminiApiKey` to `local.properties` to enable Gemini; test tool-use round-trips on both providers; consider write-preview / confirm gate before `delete_path`.

## Session checkpoint — 2026-04-30 (build.bat)

- **Done:** Root `build.bat`: `gradlew :app:assembleDebug` (default), copy `*.apk` to `BUILT\`; optional arg `release` → `assembleRelease` + `apk\release`. `.gitignore`: `/BUILT/`.
- **Next:** If `release` fails, add `signingConfig` in `app/build.gradle.kts` or use debug builds only.

## Session checkpoint — 2026-04-30 (checklist implementation: explorer + agent)

- **Done:** Roadmap-aligned features: explorer **filter** (`TreeFilter.kt`), **icons by extension**
  (`ExplorerIcons.kt`), agent **expandable tool rows** (full JSON input + full/capped result in
  `AgentChatPanel` / `AgentViewModel`), **workspace context appendix** on send
  (`IdeViewModel.buildAgentWorkspaceContext` → `AgentViewModel`). `:app:compileDebugKotlin` OK.
  `DOCS/ROADMAP.md` checklist + footnotes, `CHANGELOG`, this block.
- **Next:** Tree virtualization / favorites, apply-revert for agent edits, or conflict prompts.

## Session checkpoint — roadmap Phase 2 · Agent UX granular

- **Done:** `DOCS/ROADMAP.md` — Phase 2 delivery header set to **partial MVP** (was stale “all `[ ]`”);
  new **§ Phase 2 · Agent UX & context** with Epic 2.2/2.3 checkbox rows synced to
  `AgentChatPanel.kt`, `AgentViewModel.kt`, `IdeConstants.SYSTEM_PROMPT`; Epic 2.1 block now points
  readers to that subsection; footnotes + appendix line amended.
- **Next:** When shipping expandable tool cards or context builder, tick rows here and in CHANGELOG.

## Session checkpoint — roadmap Phase 2 sync

- **Done:** `DOCS/ROADMAP.md` checklist addendum — **§ Phase 2 · Tool router (granular)** (`list_files`
  … `rename_path`), partial vs backlog rows; Phase 2 header pointer + appendix amendment (preserves narrative).
- **Next:** Optionally fold Epic 2.2/2.3 into same-format sub-bullets when first UI ships. *[Superseded 2026-04-30]: 2.2/2.3 granular subsection added — “when first UI ships” already met for baseline chat surface.*

## Session checkpoint — 2026-04-30 (Android agent tools)

- **Done:** Verified Android agents already support Anthropic tool-use rounds with local
  `read_file` / `write_file` SAF tools. Added `list_files` to let agents inspect the opened
  workspace before choosing files to read or edit.
- **Next:** Consider richer trust controls for write previews / approvals before expanding to
  rename, delete, shell, or git tools.

## Session checkpoint — 2026-04-30 (Roadmap checklist + M1/M2 continuation)

- **Done:** `DOCS/ROADMAP.md` **Delivery checklist** (checkbox-style rows, footnotes, appendix); Epic 1.1 polish
  (large-file chrome banner, capped undo on huge buffers/lines), per-tab scroll + selection persisted (prefs
  schema v2); Epic **1.2 file-ops MVP** — `WorkspaceRepository` create/rename/dup/delete + explorer long-press
  menu + root new file/folder dialogs; centralized mutation notify on `writeText`; **`ToolRouter`**
  `search_files` compile fix (`break@outer`).
- **Next:** SAF move/cross-folder ops, tree virtualization, conflict prompts, directory delete guard if dirty tabs.

## Session checkpoint — 2026-04-30 (Roadmap M1 implementation start)

- **Done:** Implemented first **Epic 1.1 / M1** slice from [`ROADMAP.md`](./ROADMAP.md):
  debounced undo/redo, autosave interval + autosave-on-tab-switch for dirty buffers, Save all, dirty-tab
  close dialog, persisted session drafts (`EditorSessionStore` + `WORKSPACE_REPOSITORY` URI), lifecycle
  `ON_STOP` persist, SAF restore when persistable URI still valid (see CHANGELOG **[Unreleased]**).
- **Next:** Larger-file safeguards, conflict prompts on external change, grouped undo UI hints; Epic 1.2 tree
  virtualization / file ops when ready.

## Session checkpoint — 2026-04-30 (Roadmap planning)

- **Done:** Reviewed `README.md` + DOCS core (`SUMMARY`, `SBOM`, `SCRATCHPAD`,
  `STYLE_GUIDE`, `ARCHITECTURE`, `My_Thoughts`, `CHANGELOG`) and authored
  [`ROADMAP.md`](./ROADMAP.md) with granular, feature-packed Android AI IDE plan
  (Phases 1-5, epics, acceptance criteria, milestone map, and 90-day starter plan).
- **Next:** Validate milestone sequencing against engineering capacity and split M1-M3 into
  implementation issues.

## Session checkpoint — 2026-03-29 (Kinetic + Kinetic-IDE remote)

- **Done:** Rebrand **Kinetic** (`app_name`, `KineticApp`, `KineticTheme`, `Theme.Kinetic`, `rootProject.name`); removed `TabletAiIdeApp` / `TabletAiIdeTheme`. **`git remote set-url origin`** → `https://github.com/Otterdays/Kinetic-IDE.git`; **`git push -u origin main`** (initial publish to empty repo).

## Session checkpoint — 2026-03-29 (README + Git push)

- **Done:** [`README.md`](../README.md) GitHub refresh (badges, preview `stitch_sample_1/screen.png`, feature table, stack, docs index). DOCS: [`SUMMARY.md`](./SUMMARY.md), [`CHANGELOG.md`](./CHANGELOG.md), [`SBOM.md`](./SBOM.md), [`SCRATCHPAD.md`](./SCRATCHPAD.md). **`git push`** `origin/main` — commit `b57773c`.

## Session checkpoint — 2026-03-29 (Gradle jlink / JDK)

- **Issue:** `:app:compileDebugJavaWithJavac` failed — `JdkImageTransform` looked for `jlink.exe` under Cursor’s Red Hat Java **JRE** (no jlink). **Fix:** `org.gradle.java.home` in [`gradle.properties`](../gradle.properties) + [`.vscode/settings.json`](../.vscode/settings.json) (`java.import.gradle.java.home`, `java.jdt.ls.java.home`) so the extension does not spawn Gradle on the bundled JRE; then `gradlew --stop` / clean Java LS workspace. See [`README.md`](../README.md) § Gradle JVM.

## Session checkpoint — 2026-03-29 (docs modernization)

- **Done:** Root [`README.md`](../README.md) added (quick start, tooling, doc index). [`SUMMARY.md`](./SUMMARY.md) snapshot + README link + L2 KSP note. [`ARCHITECTURE.md`](./ARCHITECTURE.md) build pipeline (AGP 9 / KSP). [`STYLE_GUIDE.md`](./STYLE_GUIDE.md) Gradle conventions. [`My_Thoughts.md`](./My_Thoughts.md) AGP 9 decision. [`SBOM.md`](./SBOM.md) maintenance blurb. [`CHANGELOG.md`](./CHANGELOG.md) stale kapt line annotated.

## Session checkpoint — 2026-03-29 (AGP 9 built-in Kotlin)

- **Done:** Removed `org.jetbrains.kotlin.android` (AGP 9.1 built-in Kotlin); migrated Hilt **kapt** → **KSP** (`com.google.devtools.ksp` 2.3.6); Hilt **2.59.2** (AGP 9 new DSL); dropped `android.kotlinOptions` (JVM target follows `compileOptions`). `.\gradlew.bat :app:assembleDebug` **BUILD SUCCESSFUL**.

## Session checkpoint — 2026-03-29 (git + GitHub)

- **Done:** `git init` (prior); initial commit `chore: initial commit`; `origin` → [Otterdays/Tablet-IDE](https://github.com/Otterdays/Tablet-IDE); default branch `main` pushed; `.kotlin/` ignored (tooling logs).

## Session checkpoint — 2026-03-28 (wrap-up)

- **Done this session:** Phase 1 app + **Kinetic Syntax** UI from `stitch_sample_1`; multi-tab editor; docs under `DOCS/` maintained; `:app:compileDebugKotlin` green.
- **Carry forward:** run `assembleDebug`, install on tablet; add API key in `local.properties`; Phase 2 (LSP, Termux bridge) when you pick it up.
- **Clean close:** no open code blockers; [AMENDED 2026-03-29]: Hilt on **KSP** (built-in Kotlin incompatible with kapt).

## Active tasks

- [ ] `gradlew :app:assembleDebug` + install on device/emulator ([AMENDED 2026-03-29]: assembleDebug verified locally; user confirmed all working).
- [ ] Phase 2: LSP client, richer editor (Phase 3 Canvas), Termux:API bridge for real terminal.

## Blockers

- [AMENDED 2026-03-28]: CI/agent environment saw KSP+Hilt `failed to make parent directories` and/or DNS failures resolving Maven/Google — project switched to **kapt** for Hilt. Re-run build locally; if KSP is preferred later, align Hilt/KSP versions and retry.
- [AMENDED 2026-03-29]: AGP 9 **built-in Kotlin** requires **KSP** (not `org.jetbrains.kotlin.kapt`). Project restored **KSP** for Hilt; re-verify CI/agents if directory/DNS issues recur.

## Last actions (most recent first)

1. **2026-04-30:** `IdeViewModel.filteredTree` + lazy explorer pin rows; agent **`write_file`/`edit_file`** Revert/Apply + conflict state; ROADMAP/CHANGELOG/SUMMARY/SCRATCHPAD.
2. **2026-04-30:** Explorer recents/favorites (`ExplorerPinsStore`), divider snap + Ctrl+S / Ctrl+Shift+S / Ctrl+W; ROADMAP/CHANGELOG/SUMMARY.
2. **2026-03-29:** Rebrand **Kinetic**; `origin` → [Kinetic-IDE](https://github.com/Otterdays/Kinetic-IDE); push `main`.
3. **2026-03-29:** README GitHub polish + DOCS sync; push `main`.
4. **2026-03-29:** Gradle: `org.gradle.java.home` + `.vscode` Java home for Cursor/Red Hat JRE + `JdkImageTransform`.
5. **2026-03-29:** Docs: README + SUMMARY/ARCHITECTURE/STYLE_GUIDE/My_Thoughts/SBOM/CHANGELOG updates (AGP 9 / KSP narrative).
6. **2026-03-29:** Gradle: AGP built-in Kotlin migration (remove `kotlin.android`, KSP+Hilt 2.59.2, remove `kotlinOptions`); `assembleDebug` OK.
7. **2026-03-29:** Pushed local repo to GitHub `Otterdays/Tablet-IDE` (`main`); added `.kotlin/` to `.gitignore`.
8. **2026-03-28:** Gradle wrapper → **9.4.1** (latest stable per gradle.org/releases); AGP **9.1.0** + Kotlin **2.3.10** in `settings.gradle.kts` (AGP 9.1 requires Gradle ≥9.3.1 per Android docs). User asked no test run.
9. **2026-03-28:** UI aligned to `stitch_sample_1` (Kinetic Syntax): nav rail, tabbed editor, breadcrumbs + line gutter, AI Architect panel, terminal tabs, status bar; Material colorScheme from DESIGN tokens; multi-file tabs in `IdeViewModel`. `.\gradlew.bat :app:compileDebugKotlin` OK.
10. **2026-03-28:** Phase 1 app scaffold: `:app` Compose + Material3, SAF workspace + file tree, split-pane editor + agent panel, `AnthropicClient` SSE streaming, `ToolRouter` (`read_file` / `write_file`), simple Kotlin keyword highlight, terminal stub + Termux link. Gradle wrapper + `local.properties.example`. Hilt via **kapt**.
11. **2026-03-28:** Created `DOCS/` core set: SUMMARY, SBOM, SCRATCHPAD, STYLE_GUIDE, My_Thoughts, CHANGELOG, ARCHITECTURE. Aligned with `claude_ide_recommendation.html` blueprint.
12. **2026-03-28:** Confirmed repo contents: blueprint HTML only.

## Next steps

- Open in Android Studio, sync Gradle, set `anthropicApiKey` in `local.properties`, deploy to tablet/emulator.
- Optional: bundle **Space Grotesk / Inter / JetBrains Mono** (`res/font/`) to match `stitch_sample_1` typography exactly.
- [AMENDED 2026-03-29]: KSP+Hilt is required for built-in Kotlin; watch CI if prior KSP filesystem/DNS issues return.

## Out-of-scope observations

- If a TypeScript/WebView prototype exists elsewhere, blueprint suggests keeping it for agent/tool-loop experiments and mapping WebView UI → Compose L1.

---

*[2026-03-28]: Scratchpad initialized.*

## Session checkpoint — 2026-05-14 (agent telemetry MVP shipped)

- **Done:** Added `AgentTelemetryStore`, `TelemetryEvent`, and `AgentTelemetryCodec` with bounded
  local `agent_telemetry.jsonl` persistence, stable schema version, sequence ids, payload hashes,
  rough token/cost estimator, and summary rollups.
- **Done:** Instrumented `AgentViewModel` around the existing agent loop: session/turn start, context
  build, prompt send, model completion, tool request/start/complete, approval wait/decision,
  checkpoint create/restore, mutation apply/revert, cost estimate, and errors.
- **Done:** Added an AI-panel telemetry strip in `AgentChatPanel` showing turns, events, estimated
  tokens/cost, average first-token latency, p95 tool latency, failures, and last event.
- **Done:** Added focused `AgentTelemetryCodecTest` coverage plus `org.json:json` test dependency so
  local unit tests exercise real JSON behavior.
- **Verify:** `.\gradlew.bat :app:testDebugUnitTest --tests com.tabletaide.ide.data.AgentTelemetryCodecTest`
  from repo root **BUILD SUCCESSFUL**.
- **Known limitation:** Usage/cost remains rough estimator only; full trace drawer, redacted export,
  retention controls, dashboard screen, pricing-table reconciliation, and benchmark runner remain
  roadmap work.
