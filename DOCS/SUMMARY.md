<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Kinetic — Project summary

## Snapshot — 2026-05-14

- **Build:** `:app:compileDebugKotlin` verified locally after capability-banner polish; current toolchain
  is **AGP 9.2.0** + built-in Kotlin + **Hilt 2.59.2 / KSP**.
- **Status:** Partial MVP is now usable for the main happy path: startup gateway → open/create/clone
  workspace → edit/chat with tools → run bounded commands → commit/push tracked HTTPS branches.
- **Reality check:** Clone, git, and command execution still depend on supported shared-storage folders
  that resolve to real filesystem paths and usually require Android **All files access**. There is
  still no PTY/debugger stack or LSP/diagnostics layer.
- **[AMENDED 2026-05-14]:** [`README.md`](../README.md) now reflects the current MVP instead of older
  Phase 1 / terminal-stub wording.
- **[AMENDED 2026-05-14]:** The IDE shell now surfaces inline capability banners when git and/or
  command execution are unavailable for the current workspace, so those constraints are visible before
  the user hits a dialog or disabled action.
- **[AMENDED 2026-05-14]:** `DOCS/ROADMAP.md` now expands agent telemetry into a lab-grade observability
  plan covering trace schema, token/cost accounting, privacy/redaction, eval loops, dashboards, export,
  checkpoints, and first implementation slice.
- **[AMENDED 2026-05-14]:** Agent telemetry first slice shipped in-app: local JSONL telemetry store,
  model/tool/approval/mutation/checkpoint/cost event capture, AI-panel rollup strip, rough token/cost
  estimator with explicit source, and focused codec tests.

## Snapshot — 2026-03-29

- **Build:** `:app:assembleDebug` verified with **AGP 9.1** built-in Kotlin, **Hilt 2.59** + **KSP** (see `DOCS/CHANGELOG.md` *Unreleased*).
- **Docs index:** root [`README.md`](../README.md) for prerequisites and commands; this file for depth and blueprint links.
- **[AMENDED 2026-03-29]:** [`README.md`](../README.md) refreshed for GitHub (badges, feature table, preview image, Cursor JDK notes); [`.vscode/settings.json`](../.vscode/settings.json) documents Red Hat Java / Gradle JVM fix.
- **[AMENDED 2026-03-29]:** Product name **Kinetic**; canonical repo [Otterdays/Kinetic-IDE](https://github.com/Otterdays/Kinetic-IDE) (was Tablet-IDE).

**Status:** Phase 1 `:app` in progress — **Kinetic Syntax** UI (`stitch_sample_1`) applied; `:app:compileDebugKotlin` verified locally. Full APK: `gradlew :app:assembleDebug` (needs SDK + network for deps).

**Goal:** Android tablet–first IDE with AI agent loop, aligned with the Kotlin-native stack blueprint.

## Quick links

| Resource | Purpose |
|----------|---------|
| [README.md](../README.md) | Clone → build quick start, tooling summary |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Layer diagram, data flow, module map |
| [SCRATCHPAD.md](./SCRATCHPAD.md) | Active tasks, blockers, recent actions |
| [SBOM.md](./SBOM.md) | Dependencies and supply-chain notes |
| [ROADMAP.md](./ROADMAP.md) | Granular Android AI IDE execution roadmap |
| [STYLE_GUIDE.md](./STYLE_GUIDE.md) | Conventions for this repo |
| [My_Thoughts.md](./My_Thoughts.md) | Decisions and rationale |
| [CHANGELOG.md](./CHANGELOG.md) | Version history |
| Blueprint (HTML) | `../claude_ide_recommendation.html` — source architecture narrative |
| App module | `../app/` — Compose IDE shell, SAF, Anthropic + tools |
| Secrets template | `../local.properties.example` → copy to `local.properties` (gitignored) |
| UI spec (Kinetic) | `../stitch_sample_1/DESIGN.md`, `../stitch_sample_1/code.html`, `../stitch_sample_1/screen.png` |

## Stack (target)

- **L1:** Jetpack Compose + Material You (UI)
- **L2:** Kotlin coroutines, Anthropic API (SSE), LSP client, Hilt (runtime) + **KSP** (compile-time DI codegen)
- **L3:** Rust NDK (Phase 3) — tree-sitter, ropey, JNI; optional in Phase 1
- **L4:** NDK / storage / process host / Termux-oriented integration

**[AMENDED 2026-03-29]:** L2 build stack uses **KSP** for Hilt, not kapt, under AGP 9 **built-in Kotlin** (see [migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)).

## Phases (from blueprint)

1. **Foundation** — Kotlin-only: split pane, SAF file tree, basic editor, Anthropic client, read/write tools, Termux:API terminal, simple highlighting.
2. **Intelligence** — Full tool router, LSP, diagnostics, git (JGit), agent UX polish.
3. **Performance** — Rust core, custom canvas editor, fuzzy search, layouts for tablet/foldable.

---

*[2026-03-28]: Initial summary. Project folder contains blueprint HTML only; Android project to be added when implementation starts.*

*[2026-03-28]: [AMENDED] `:app` module added — Phase 1 foundation per blueprint.*

*[2026-03-28]: [AMENDED] Product UI tokens and shell layout follow **Kinetic Syntax** (`stitch_sample_1`); see `app/.../theme/KineticColors.kt` and `KineticShell.kt`.*

*[2026-03-28]: [AMENDED] Build toolchain: Gradle wrapper **9.4.1**, Android Gradle Plugin **9.1.0**, Kotlin **2.3.10** (see `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`).*

*[2026-03-29]: [AMENDED] Added snapshot block and README link; L2 stack note for KSP + built-in Kotlin.*

*[2026-03-29]: [AMENDED] README GitHub polish + .vscode JDK settings called out in snapshot.*

*[2026-04-30]: [AMENDED] Added ROADMAP quick link for phased execution planning.*

*[2026-04-30]: [AMENDED] `DOCS/ROADMAP.md` Phase 2 tracker: tool router + agent panel granular checklists (partial MVP); Epic 2.2/2.3 broken out from single “backlog” lines.*

*[2026-04-30]: [AMENDED] Ship: explorer tree filter + extension icons; agent expandable tool cards; per-send workspace context (tabs + selection) in system prompt.*

*[2026-04-30]: [AMENDED] Epic 1.2 explorer **recents + starred favorites** (`ExplorerPinsStore`, `FileTreePane`); Epic 1.3 **divider snap** + **Ctrl+S / Ctrl+Shift+S / Ctrl+W**.*

*[2026-04-30]: [AMENDED] Epic 1.3 MVP: command palette (Search rail + Ctrl/⌘P), draggable editor/agent split + palette presets, tool-row Copy JSON. See `DOCS/ROADMAP.md` Epic 1.3.*

*[2026-04-30]: [AMENDED] Runtime API key input: AI Architect key dialog + command palette action save Anthropic/Gemini keys on-device; clients prefer runtime keys and keep `local.properties` / `BuildConfig` fallback.*

*[2026-04-30]: [AMENDED] Agent trust: expanded tool cards now include visible operation receipts (provider, time, duration, target, OK/FAILED status); persistent audit log remains roadmap work.*

*[2026-04-30]: [AMENDED] Explorer **`filteredTree`** + lazy pin **content types**; agent tool **`ToolMutationAction`** (Revert / Apply again) for **`write_file`** / **`edit_file`**.**

*[2026-04-30]: [AMENDED] Gradle IDE sync compatibility: root `build.gradle.kts` adds conditional no-op `prepareKotlinBuildScriptModel` for tooling that requests the legacy task under AGP 9 built-in Kotlin.*

*[2026-04-30]: [AMENDED] App-module Gradle import compatibility: `app/build.gradle.kts` declares plugin versions and `app/settings.gradle.kts` provides repositories so importing `app/` directly resolves Android/KSP/Hilt/Compose plugins.*

*[2026-04-30]: [AMENDED] Standalone `app/` Gradle memory guard: `app/gradle.properties` sets `org.gradle.jvmargs` to 2G heap + 512m metaspace to avoid daemon GC-thrashing under IDE app-module runs.*

*[2026-04-30]: [AMENDED] UI polish: theme modes shipped (Dark / Light / High Contrast), persisted in `IdeViewModel`, selectable from Settings gear and command palette.*

*[2026-04-30]: [AMENDED] Editor workflow polish: tab-cycle shortcuts (`Ctrl+Tab` / `Ctrl+Shift+Tab`) and command-palette actions for next/previous tab.*

*[2026-05-13]: [AMENDED] Startup Gateway MVP shipped: launch now routes to a dedicated welcome dashboard only when no restorable session/workspace exists; returning sessions still resume directly into the IDE shell. Gateway adds `New Project`, `Open Folder`, `Clone Repository` placeholder flow, and recent workspaces via `RecentWorkspacesStore`.*

*[AMENDED 2026-05-14]: The earlier startup-gateway note calling clone a placeholder is superseded by the later real JGit/auth entries below. Current canonical status: gateway clone is real, opens the repo directly into the IDE when the selected destination is supported, and still inherits the shared-storage + All files access constraints called out elsewhere in this summary.*

*[2026-05-13]: [AMENDED] New starter project setup flow added in `WorkspaceRepository.createStarterProject` with three minimal templates (`Blank workspace`, `Kotlin console`, `Web starter`) opened immediately after creation.*

*[2026-05-13]: [AMENDED] Git Clone Auth MVP shipped: startup clone flow now performs real **HTTPS token** clones via **JGit**, stores saved tokens with Android Keystore-backed encryption, opens cloned repos directly into the IDE, and hides `.git` from explorer/agent traversal by default. First version is intentionally constrained to primary shared-storage folders that can be resolved to filesystem paths and requires Android **All files access** for shared-folder clone writes.*

*[2026-05-13]: [AMENDED] Git Commit / Push MVP shipped: the IDE now detects opened repo roots, shows real branch/change state in the shell, generates AI commit messages from bounded real git status + diff context, and supports commit plus tracked-branch push using the saved HTTPS auth flow. The commit dialog also stores author name/email locally for repeat commits on device.*

*[2026-05-13]: [AMENDED] Ship-readiness sprint shipped: AI tool-call reliability was hardened (tool schema validation + Gemini function-response fixes), the shell now has a first real in-app command runner with `Execute`, rerun, cancel, and live terminal/output/debug panes, and autosave failures now surface visible status feedback instead of failing silently. Runner MVP is still intentionally bounded to resolvable shared-storage workspaces with All files access and is not yet a PTY/debugger stack.*

*[2026-05-14]: [AMENDED] Agent trust controls broadened: persisted **Auto / Ask / Deny** policies now gate file changes and destructive ops, while shell commands intentionally stay **Ask / Deny** only; the approval dialog is generic across risky tools, and chat receipts now include risk, policy, and decision metadata. This remains app-wide/settings-driven rather than a full per-tool or persistent audit system.*

*[2026-05-14]: [AMENDED] Capability clarity pass shipped: `CapabilityBanner` now exposes workspace/git/run limitations inline inside the IDE shell so the user can see that editing + AI context still work even when git and command execution are unavailable for the current location.*

*[2026-05-14]: [AMENDED] Roadmap agent telemetry scope upgraded from cost/token placeholder to lab-grade observability spec: canonical event log, span model, cost/token contracts, redaction/export, benchmark loop, dashboard metrics, acceptance criteria, and first build slice.*

*[2026-05-14]: [AMENDED] Agent telemetry MVP shipped: `AgentTelemetryStore` persists bounded `agent_telemetry.jsonl`, `AgentViewModel` emits session/turn/model/tool/approval/mutation/checkpoint/cost events, `AgentChatPanel` shows session rollup metrics, and `AgentTelemetryCodecTest` verifies summary math, JSON round-trip, and estimator source.

*[2026-05-14]: [AMENDED] Persistent audit timeline shipped: `AgentAuditStore` persists every agent tool invocation to `agent_audit.jsonl`, and `AuditTimelinePanel` provides a dedicated dialog (command palette → Audit timeline) with expandable entry cards showing tool name, target, status, risk, policy, approval, duration, and mutation summary. This closes the "persistent audit log" slice from ROADMAP item #1 of the natural next slices; filter/sort and export remain future work.*

*[2026-05-14]: Roadmap cleanup pass: `DOCS/ROADMAP.md` is now a concise front-and-center roadmap without amendment history, footnotes, or duplicated checklist clutter.*
