# Tablet AI IDE

**Tablet-first Android IDE shell** with an AI agent loop: split editor, SAF workspace, streaming tools, and a **Kinetic Syntax** UI built in Jetpack Compose.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![AGP](https://img.shields.io/badge/AGP-9.1-4285F4?logo=androidstudio&logoColor=white)](https://developer.android.com/build)
[![minSdk](https://img.shields.io/badge/minSdk-26-2E7D32)](app/build.gradle.kts)
![License](https://img.shields.io/badge/license-TBD-lightgrey)

---

## Preview

Design reference (Kinetic Syntax — `stitch_sample_1`):

<p align="center">
  <img src="stitch_sample_1/screen.png" alt="Tablet IDE UI concept — Kinetic Syntax" width="720" />
</p>

<p align="center">
  <sub>Concept mock · Compose implementation lives in <code>app/</code></sub>
</p>

---

## Features (Phase 1)

| Area | What’s in the app |
|------|-------------------|
| **Workspace** | SAF-backed tree, open folders, read/write via tool router |
| **Editor** | Multi-tab surface, Kotlin keyword highlight, line gutter + breadcrumbs |
| **Agent** | Anthropic SSE client, chat panel, `read_file` / `write_file` tools |
| **Shell** | Nav rail, AI sidebar, terminal strip (Termux-oriented stub) |
| **DI** | Hilt + **KSP** (AGP 9 built-in Kotlin) |

Roadmap: LSP, richer diagnostics, git, Rust/NDK core — see [`claude_ide_recommendation.html`](claude_ide_recommendation.html) and [`DOCS/ARCHITECTURE.md`](DOCS/ARCHITECTURE.md).

---

## Quick start

```bash
# 1. Secrets & SDK path
cp local.properties.example local.properties   # or copy manually on Windows
# Edit local.properties: sdk.dir=..., anthropicApiKey=... (optional)

# 2. Debug APK
./gradlew :app:assembleDebug
```

Windows:

```bat
copy local.properties.example local.properties
gradlew.bat :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Requirements

| Requirement | Notes |
|-------------|--------|
| **JDK 17+** | Must be a **full JDK** (`jlink` on `PATH` / `bin`) — AGP 9 uses it for `androidJdkImage` |
| **Android SDK** | e.g. Android Studio or `sdkmanager`; `compileSdk` / `targetSdk` **35** |
| **Network** | First resolve of Maven/Google artifacts |

### Cursor / VS Code and “`jlink` does not exist”

The Red Hat Java extension can run Gradle on a **bundled JRE** without `jlink`. This repo sets:

- [`gradle.properties`](gradle.properties) → `org.gradle.java.home` (adjust path for your machine; Linux/macOS/CI may need a different JDK path)
- [`.vscode/settings.json`](.vscode/settings.json) → `java.import.gradle.java.home` and `java.jdt.ls.java.home`

After changing JDK settings: **Java: Clean Java Language Server Workspace** (or reload window) and `gradlew --stop`.

**Android Studio:** **Settings → Build Tools → Gradle → Gradle JDK** → full JDK (e.g. embedded **jbr**).

---

## Stack

- **UI:** Jetpack Compose, Material 3, Compose BOM
- **Async:** Kotlin coroutines, OkHttp (SSE)
- **Build:** Gradle **9.4.1**, AGP **9.1**, built-in Kotlin, Compose compiler plugin **2.3.10**, **KSP 2.3.6**, Hilt **2.59.2**

Full table: [`DOCS/SBOM.md`](DOCS/SBOM.md)

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [`DOCS/SUMMARY.md`](DOCS/SUMMARY.md) | Status, phases, quick links |
| [`DOCS/ARCHITECTURE.md`](DOCS/ARCHITECTURE.md) | Layers, modules, Mermaid |
| [`DOCS/SCRATCHPAD.md`](DOCS/SCRATCHPAD.md) | Active work & session log |
| [`DOCS/SBOM.md`](DOCS/SBOM.md) | Dependency versions |
| [`DOCS/STYLE_GUIDE.md`](DOCS/STYLE_GUIDE.md) | Code & Gradle conventions |
| [`DOCS/CHANGELOG.md`](DOCS/CHANGELOG.md) | History |
| [`DOCS/My_Thoughts.md`](DOCS/My_Thoughts.md) | Architecture decisions |

Blueprint narrative: [`claude_ide_recommendation.html`](claude_ide_recommendation.html) · UI tokens: [`stitch_sample_1/DESIGN.md`](stitch_sample_1/DESIGN.md)

---

## Contributing

Issues and PRs welcome. Match [`DOCS/STYLE_GUIDE.md`](DOCS/STYLE_GUIDE.md); keep `DOCS/` append-only per preservation headers.

---

## License

No `LICENSE` file yet — add one before redistribution.
