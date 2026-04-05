<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD — Kinetic

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

1. **2026-03-29:** Rebrand **Kinetic**; `origin` → [Kinetic-IDE](https://github.com/Otterdays/Kinetic-IDE); push `main`.
2. **2026-03-29:** README GitHub polish + DOCS sync; push `main`.
3. **2026-03-29:** Gradle: `org.gradle.java.home` + `.vscode` Java home for Cursor/Red Hat JRE + `JdkImageTransform`.
4. **2026-03-29:** Docs: README + SUMMARY/ARCHITECTURE/STYLE_GUIDE/My_Thoughts/SBOM/CHANGELOG updates (AGP 9 / KSP narrative).
5. **2026-03-29:** Gradle: AGP built-in Kotlin migration (remove `kotlin.android`, KSP+Hilt 2.59.2, remove `kotlinOptions`); `assembleDebug` OK.
6. **2026-03-29:** Pushed local repo to GitHub `Otterdays/Tablet-IDE` (`main`); added `.kotlin/` to `.gitignore`.
7. **2026-03-28:** Gradle wrapper → **9.4.1** (latest stable per gradle.org/releases); AGP **9.1.0** + Kotlin **2.3.10** in `settings.gradle.kts` (AGP 9.1 requires Gradle ≥9.3.1 per Android docs). User asked no test run.
8. **2026-03-28:** UI aligned to `stitch_sample_1` (Kinetic Syntax): nav rail, tabbed editor, breadcrumbs + line gutter, AI Architect panel, terminal tabs, status bar; Material colorScheme from DESIGN tokens; multi-file tabs in `IdeViewModel`. `.\gradlew.bat :app:compileDebugKotlin` OK.
9. **2026-03-28:** Phase 1 app scaffold: `:app` Compose + Material3, SAF workspace + file tree, split-pane editor + agent panel, `AnthropicClient` SSE streaming, `ToolRouter` (`read_file` / `write_file`), simple Kotlin keyword highlight, terminal stub + Termux link. Gradle wrapper + `local.properties.example`. Hilt via **kapt**.
10. **2026-03-28:** Created `DOCS/` core set: SUMMARY, SBOM, SCRATCHPAD, STYLE_GUIDE, My_Thoughts, CHANGELOG, ARCHITECTURE. Aligned with `claude_ide_recommendation.html` blueprint.
11. **2026-03-28:** Confirmed repo contents: blueprint HTML only.

## Next steps

- Open in Android Studio, sync Gradle, set `anthropicApiKey` in `local.properties`, deploy to tablet/emulator.
- Optional: bundle **Space Grotesk / Inter / JetBrains Mono** (`res/font/`) to match `stitch_sample_1` typography exactly.
- [AMENDED 2026-03-29]: KSP+Hilt is required for built-in Kotlin; watch CI if prior KSP filesystem/DNS issues return.

## Out-of-scope observations

- If a TypeScript/WebView prototype exists elsewhere, blueprint suggests keeping it for agent/tool-loop experiments and mapping WebView UI → Compose L1.

---

*[2026-03-28]: Scratchpad initialized.*
