<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Kinetic — Project summary

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
