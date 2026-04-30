<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SBOM — Kinetic

**Last updated:** 2026-04-30

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
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.9.0 |

## Build-time / debug / test dependencies

| Dependency | Version | Scope |
|---|---|---|
| com.google.dagger:hilt-compiler | 2.59.2 | ksp |
| androidx.compose.ui:ui-tooling | via BOM | debugImplementation |
| androidx.compose.ui:ui-test-manifest | via BOM | debugImplementation |
| androidx.compose:compose-bom | 2024.12.01 | androidTestImplementation |

## Notes

- Source of truth: `settings.gradle.kts`, `app/build.gradle.kts`, `app/settings.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`.
- Root repo policy: `FAIL_ON_PROJECT_REPOS` with `google()` and `mavenCentral()`.
