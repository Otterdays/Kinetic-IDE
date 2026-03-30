<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# STYLE_GUIDE — Tablet AI IDE

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

---

*[2026-03-28]: Initial style guide for Kotlin-first tablet IDE. Extend when Rust/NDK modules land.*
