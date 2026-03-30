<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD — Tablet AI IDE

## Session checkpoint — 2026-03-28 (wrap-up)

- **Done this session:** Phase 1 app + **Kinetic Syntax** UI from `stitch_sample_1`; multi-tab editor; docs under `DOCS/` maintained; `:app:compileDebugKotlin` green.
- **Carry forward:** run `assembleDebug`, install on tablet; add API key in `local.properties`; Phase 2 (LSP, Termux bridge) when you pick it up.
- **Clean close:** no open code blockers; Hilt remains on **kapt** (see Blockers).

## Active tasks

- [ ] `gradlew :app:assembleDebug` + install on device/emulator (compile path already OK).
- [ ] Phase 2: LSP client, richer editor (Phase 3 Canvas), Termux:API bridge for real terminal.

## Blockers

- [AMENDED 2026-03-28]: CI/agent environment saw KSP+Hilt `failed to make parent directories` and/or DNS failures resolving Maven/Google — project switched to **kapt** for Hilt. Re-run build locally; if KSP is preferred later, align Hilt/KSP versions and retry.

## Last actions (most recent first)

1. **2026-03-28:** Gradle wrapper → **9.4.1** (latest stable per gradle.org/releases); AGP **9.1.0** + Kotlin **2.3.10** in `settings.gradle.kts` (AGP 9.1 requires Gradle ≥9.3.1 per Android docs). User asked no test run.
2. **2026-03-28:** UI aligned to `stitch_sample_1` (Kinetic Syntax): nav rail, tabbed editor, breadcrumbs + line gutter, AI Architect panel, terminal tabs, status bar; Material colorScheme from DESIGN tokens; multi-file tabs in `IdeViewModel`. `.\gradlew.bat :app:compileDebugKotlin` OK.
3. **2026-03-28:** Phase 1 app scaffold: `:app` Compose + Material3, SAF workspace + file tree, split-pane editor + agent panel, `AnthropicClient` SSE streaming, `ToolRouter` (`read_file` / `write_file`), simple Kotlin keyword highlight, terminal stub + Termux link. Gradle wrapper + `local.properties.example`. Hilt via **kapt**.
4. **2026-03-28:** Created `DOCS/` core set: SUMMARY, SBOM, SCRATCHPAD, STYLE_GUIDE, My_Thoughts, CHANGELOG, ARCHITECTURE. Aligned with `claude_ide_recommendation.html` blueprint.
5. **2026-03-28:** Confirmed repo contents: blueprint HTML only.

## Next steps

- Open in Android Studio, sync Gradle, set `anthropicApiKey` in `local.properties`, deploy to tablet/emulator.
- Optional: bundle **Space Grotesk / Inter / JetBrains Mono** (`res/font/`) to match `stitch_sample_1` typography exactly.
- Optional: migrate Hilt back to KSP after verifying toolchain (see Blockers).

## Out-of-scope observations

- If a TypeScript/WebView prototype exists elsewhere, blueprint suggests keeping it for agent/tool-loop experiments and mapping WebView UI → Compose L1.

---

*[2026-03-28]: Scratchpad initialized.*
