<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# STYLE_GUIDE — Kinetic

## Trace tags

Link non-obvious code to docs:

```text
// [TRACE: DOCS/ARCHITECTURE.md]
```

## Kotlin / Android (primary)

- **Naming:** Follow Android Kotlin style — `PascalCase` types, `camelCase` members, `SCREAMING_SNAKE` only where platform-idiomatic.
- **Architecture:** MVVM; `ViewModel` + `StateFlow` / `SharedFlow`; business logic not in composables beyond UI mapping; use `viewModelScope` / `lifecycleScope`, not `GlobalScope`.
- **Comments:** WHY and non-obvious constraints; use `TODO:` / `FIXME:` / `NOTE:` prefixes when needed.
- **Line length:** Prefer ≤ 100 characters where practical.

## Paths in repo

- Use forward slashes in documentation and cross-platform scripts.

## Limits (from team rules)

- Target ≤ 50 lines per function and ≤ 400 lines per file where reasonable; split when it improves clarity.

## Gradle / AGP 9 (project-specific)

- **Built-in Kotlin:** Do not add `org.jetbrains.kotlin.android`; Kotlin Android compilation is provided by AGP ≥ 9 when the Android plugin is applied.
- **Annotation processors:** Use **KSP** (`ksp(...)`) for Hilt and other KSP-capable processors; **kapt** is incompatible with built-in Kotlin unless you adopt `com.android.legacy-kapt` (not used here).
- **Versions:** Declare Android plugin, KSP, Compose compiler plugin, and Hilt plugin in `settings.gradle.kts` inside `pluginManagement { plugins { ... } }` so the whole build resolves consistently.
- **Paths:** In Gradle Kotlin DSL on Windows, prefer forward slashes in string paths inside repo-owned scripts (e.g. `"app/proguard-rules.pro"`).

---

*[2026-03-28]: Initial style guide for Kotlin-first tablet IDE. Extend when Rust/NDK modules land.*

*[2026-03-29]: [AMENDED] Gradle / AGP 9 subsection (built-in Kotlin, KSP, plugin management).*
