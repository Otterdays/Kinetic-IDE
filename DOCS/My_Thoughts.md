<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# My_Thoughts — decisions & rationale

## 2026-03-28 — Adopt Claude blueprint as north star

**Decision:** Treat `claude_ide_recommendation.html` as the authoritative architecture narrative for v1.

**Rationale:**

- Four layers with clear ownership reduce thrash: UI (Compose), agent/LSP (Kotlin), perf core (Rust later), system (NDK/Termux).
- **Phase 1 Kotlin-only** avoids JNI/Rust complexity until editor and agent loop prove valuable on device.
- Explicit module names (`AnthropicClient`, `ToolRouter`, `LSPManager`, `ProcessHost`) map cleanly to Gradle modules later.
- Tablet-first (stylus, split panes, large touch targets) is a different UX problem than phone; Compose + WindowManager fits.

**Risks to watch:**

- Termux / NDK process and storage story varies by device and Android version — validate early on target hardware.
- LSP over stdio on Android is finicky (packaging servers, ABI); may need staged rollout after bare agent + file tools work.

**Alternatives considered:** Single-language Electron-style shell on tablet (heavier, worse integration); full Rust UI (higher cost, smaller Android ecosystem). Sticking with Kotlin UI + optional Rust core matches blueprint and team Android defaults.

## 2026-03-28 — Kinetic Syntax as product chrome

**Decision:** Implement the shipped IDE shell using tokens and layout from `stitch_sample_1` (DESIGN.md + HTML reference), not generic Material defaults.

**Rationale:** Gives a coherent “tablet instrument” identity, matches stakeholder mockups, and keeps blueprint architecture unchanged underneath (still Compose L1, same agent/data layers).

**Note:** Display fonts in the spec are not yet bundled; system monospace/sans are placeholders until `res/font/` is added.

## 2026-03-29 — AGP 9 built-in Kotlin + KSP for Hilt

**Decision:** Align the `:app` module with **Android Gradle Plugin 9** defaults: no `org.jetbrains.kotlin.android` plugin; **Hilt 2.59.x** for compatibility with AGP 9’s **new DSL** (no `BaseExtension`); **KSP** instead of **kapt** because kapt is incompatible with built-in Kotlin.

**Rationale:**

- Matches official [migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) guidance and avoids fighting the toolchain.
- KSP is the supported path for Hilt codegen alongside built-in Kotlin; `com.android.legacy-kapt` was avoided to reduce long-term debt.
- Hilt 2.52-era plugins fail on AGP 9 until upgraded — staying current on Hilt avoids silent breakage on sync.

**Trade-offs:** CI or agent environments that previously failed KSP (filesystem/DNS) need to be fixed at the environment layer; reverting to kapt without opting out of built-in Kotlin is not supported.

## 2026-03-29 — Product name: Kinetic

**Decision:** Ship the app and Gradle project under the name **Kinetic**; GitHub repo **Kinetic-IDE** (`Otterdays/Kinetic-IDE`).

**Rationale:** Aligns public name with the **Kinetic Syntax** UI system; shorter launcher and README identity. **`applicationId`** / Kotlin packages stay **`com.tabletaide.ide`** to avoid a disruptive package move and Play reinstall semantics until a deliberate migration.

---

*[2026-03-28]: First entry.*

*[2026-03-29]: [AMENDED] AGP 9 / KSP / Hilt decision log.*

*[2026-03-29]: [AMENDED] Product name Kinetic + Kinetic-IDE repo.*
