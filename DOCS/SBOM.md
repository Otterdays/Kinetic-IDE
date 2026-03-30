<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SBOM — Tablet AI IDE

**Last updated:** 2026-03-28 (session wrap — no new artifacts; row added for Hilt Compose integration). [AMENDED 2026-03-28]: Gradle wrapper 9.4.1; AGP 9.1.0; Kotlin 2.3.10 (aligned for Gradle 9.x).

## Application dependencies

_Android `:app` — versions from `app/build.gradle.kts` / Compose BOM; update rows when changing deps._

| Package | Version | Scope | Notes |
|---------|---------|-------|-------|
| Android Gradle Plugin | 9.1.0 | build | |
| Kotlin | 2.3.10 | build | |
| Gradle (wrapper) | 9.4.1 | build | distributionUrl in `gradle/wrapper/gradle-wrapper.properties` |
| Compose BOM | 2024.12.01 | implementation | platform |
| androidx.core:core-ktx | 1.15.0 | implementation | |
| activity-compose | 1.9.3 | implementation | |
| lifecycle (runtime-ktx, viewmodel-compose, runtime-compose) | 2.8.7 | implementation | |
| material3 / compose ui | (BOM) | implementation | |
| material-icons-extended | (BOM) | implementation | |
| documentfile | 1.1.0 | implementation | SAF |
| Hilt | 2.52 | implementation + kapt | |
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
