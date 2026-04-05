<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SBOM — Kinetic

**Last updated:** 2026-03-29 ([AMENDED]: README/GitHub + `.vscode` JDK settings documented; no dependency version changes this pass). Prior: AGP built-in Kotlin — no `kotlin.android` plugin; Hilt 2.59.2 + KSP 2.3.6; Gradle wrapper 9.4.1; AGP 9.1.0; Kotlin 2.3.10 (Compose compiler plugin).

## Application dependencies

_Android `:app` — versions from `app/build.gradle.kts` / Compose BOM; update rows when changing deps._

**Maintenance:** After changing `settings.gradle.kts` plugin versions or `app/build.gradle.kts` dependencies, update the table below and bump **Last updated** in this file’s header. Primary sources: [`settings.gradle.kts`](../settings.gradle.kts), [`app/build.gradle.kts`](../app/build.gradle.kts), [`gradle/wrapper/gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties).

| Package | Version | Scope | Notes |
|---------|---------|-------|-------|
| Android Gradle Plugin | 9.1.0 | build | |
| Kotlin (Compose compiler plugin) | 2.3.10 | build | Built-in Kotlin via AGP 9; no `org.jetbrains.kotlin.android` |
| KSP | 2.3.6 | build | `com.google.devtools.ksp`; Hilt code gen |
| Gradle (wrapper) | 9.4.1 | build | distributionUrl in `gradle/wrapper/gradle-wrapper.properties` |
| Compose BOM | 2024.12.01 | implementation | platform |
| androidx.core:core-ktx | 1.15.0 | implementation | |
| activity-compose | 1.9.3 | implementation | |
| lifecycle (runtime-ktx, viewmodel-compose, runtime-compose) | 2.8.7 | implementation | |
| material3 / compose ui | (BOM) | implementation | |
| material-icons-extended | (BOM) | implementation | |
| documentfile | 1.1.0 | implementation | SAF |
| Hilt | 2.59.2 | implementation + ksp | AGP 9 new DSL |
| androidx.hilt:hilt-navigation-compose | 1.2.0 | implementation | `hiltViewModel()` |
| OkHttp | 4.12.0 | implementation | SSE |
| kotlinx-coroutines-android | 1.9.0 | implementation | |
| — | — | — | Remove stale rows when amending; do not delete historical block above |

## Tooling / CI

_Not configured._

## Policy

- Audit before adding dependencies; prefer well-maintained, minimal transitive trees.
- No known-vulnerable versions; pin versions when the build exists.

---

*[2026-03-28]: Initialized SBOM. No bill of materials entries until first dependency manifest exists.*

*[2026-03-28]: [AMENDED] Gradle deps from `:app` reflected in table above.*

*[2026-03-29]: [AMENDED] Maintenance blurb (where to edit when versions change).*

*[2026-03-29]: [AMENDED] Header note — docs/README refresh; SBOM table unchanged.*
