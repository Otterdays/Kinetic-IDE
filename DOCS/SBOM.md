<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SBOM — Kinetic

**Last updated:** 2026-05-14

## Build toolchain

| Component | Version |
|---|---|
| Gradle wrapper | 9.4.1 |
| Android Gradle Plugin | 9.2.0 |
| Kotlin Compose plugin | 2.3.10 |
| KSP plugin | 2.3.6 |
| Hilt Gradle plugin | 2.59.2 |
| Foojay resolver convention plugin | 1.0.0 |

## Runtime dependencies (`implementation`)

| Dependency | Version |
|---|---|
| androidx.compose:compose-bom | 2024.12.01 |
| androidx.core:core-ktx | 1.15.0 |
| androidx.activity:activity-compose | 1.9.3 |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.7 |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7 |
| androidx.lifecycle:lifecycle-runtime-compose | 2.8.7 |
| androidx.compose.ui:ui | via BOM |
| androidx.compose.ui:ui-tooling-preview | via BOM |
| androidx.compose.material3:material3 | via BOM |
| androidx.compose.material:material-icons-extended | via BOM |
| androidx.documentfile:documentfile | 1.1.0 |
| com.google.dagger:hilt-android | 2.59.2 |
| androidx.hilt:hilt-navigation-compose | 1.2.0 |
| com.squareup.okhttp3:okhttp | 4.12.0 |
| org.eclipse.jgit:org.eclipse.jgit | 7.6.0.202603022253-r |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.9.0 |

## Build-time / debug / test dependencies

| Dependency | Version | Scope |
|---|---|---|
| com.google.dagger:hilt-compiler | 2.59.2 | ksp |
| androidx.compose.ui:ui-tooling | via BOM | debugImplementation |
| androidx.compose.ui:ui-test-manifest | via BOM | debugImplementation |
| androidx.compose:compose-bom | 2024.12.01 | androidTestImplementation |
| junit:junit | 4.13.2 | testImplementation |
| org.json:json | 20250517 | testImplementation |

## Notes

- 2026-05-14 tree wrap-up slice: added `junit:junit:4.13.2` as a minimal local unit-test dependency
  for new explorer tree model/filter coverage. No runtime dependency or plugin version changes.
- 2026-05-14 agent telemetry MVP: added `org.json:json:20250517` as a local unit-test dependency so
  telemetry codec tests exercise real JVM `JSONObject` behavior instead of Android's mocked test stub.
  Runtime telemetry uses the existing Android platform JSON classes; no new runtime dependency.
- 2026-05-14 capability-banner + docs-truth pass: no dependency or plugin version changes.
- Source of truth: `settings.gradle.kts`, `app/build.gradle.kts`, `app/settings.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`.
- 2026-05-14 trust policy layer: no dependency or plugin version changes. Adds a SharedPreferences-
  backed policy store and broader approval/receipt UI on top of the existing agent/runtime stack.
- 2026-05-13 agent `run_command` tool slice: no dependency or plugin version changes. Reuses the
  existing in-app runner/runtime and adds approval-gated AI execution on top.
- Root repo policy: `FAIL_ON_PROJECT_REPOS` with `google()` and `mavenCentral()`.
- 2026-04-30 theme-mode UI pass: no dependency or plugin version changes.
- 2026-05-13 startup gateway MVP: no dependency or plugin version changes.
- 2026-05-13 git clone auth MVP: added `org.eclipse.jgit:org.eclipse.jgit` for on-device clone support.
- 2026-05-13 git clone auth MVP: shared-folder clone path currently depends on Android `MANAGE_EXTERNAL_STORAGE`; saved git auth is excluded from backup/data-transfer via XML rules.
- 2026-05-13 git commit / push MVP: no dependency or plugin version changes. Reuses existing JGit runtime plus saved host-scoped HTTPS auth and adds local git author-name/email persistence in app prefs.
- 2026-05-13 prompt enhancer UX: no dependency or plugin version changes. Reuses the existing Anthropic/Gemini provider stack for one-shot draft rewriting in the AI composer.
- 2026-05-13 ship-readiness sprint: no dependency or plugin version changes. Adds an in-app command runner using Android-accessible `/system/bin/sh` plus shared-storage workspace path resolution; still bounded by filesystem-backed workspace access.
