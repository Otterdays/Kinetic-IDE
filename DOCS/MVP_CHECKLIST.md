<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# MVP Checklist — Kinetic Android AI IDE

**Goal:** A real tablet user can clone or open a repo, chat with an AI, edit files in-app, and push commits — without hitting silent failures.

**Last updated:** 2026-06-11 (P0 code-complete pass)

**Status:** All **code-side P0** items are done. What remains is **on-device verification** (marked 👤 **USER** below).

---

## Definition of done (MVP)

| Criterion | Code | 👤 USER |
|-----------|------|---------|
| Fresh install → workspace open &lt; 2 min | Onboarding tips + All-files deep links | Install APK, grant permissions, time yourself |
| AI reads + edits file in editor | Tool router + codec tests | Run manual QA prompt on device |
| Commit + push HTTPS tracked branch | Commit/push/pull + auth + auto pull-retry | Push smoke on real GitHub repo |
| Clear failure messages | Capability banner, git/auth errors, `Provider · model: err` | Spot-check failure cases in QA script |
| Documented happy path | `DOCS/SCRATCHPAD.md` demo block | Optional: record screen capture |

---

## P0 — Blockers

### AI agent

- [x] **Fix Gemini multi-turn tools** — `GeminiMessageCodec` + tests
- [x] **Verify tool round-trip (code)** — `AgentToolRoundTripTest` (Gemini + OpenAI codecs after tool result)
- [x] **Agent error surfacing** — failures show `Provider · modelId: message`
- [ ] 👤 **Smoke-test providers on device** — prompt: *Read README.md and add `<!-- kinetic mvp test -->` at the top* on **Claude** and **Gemini** (or OpenRouter equivalent)

### First-run / permissions

- [x] **All files access onboarding** — startup gateway + IDE `CapabilityBanner` with **Grant All files access** CTA
- [x] **Workspace location guidance** — Downloads/Documents tips on gateway + banner
- [x] **API key + model UX** — filterable picker; Claude / Gemini / OpenAI / Grok / OpenRouter; grey out missing keys

### Git push path

- [x] **Auth for non-cloned repos** — `GitAuthDialog`, palette entry, `pushReady` gate
- [x] **Pull on push reject** — `GitPullService`, **Pull** button when behind, auto pull+retry on non-fast-forward push
- [x] **Document PAT scopes** — README: GitHub HTTPS needs `repo` scope
- [ ] 👤 **Push-only smoke test** — clean repo, `aheadCount > 0`, tap **Push**; verify on GitHub web

---

## P1 — Fast polish (deferred / partial)

- [x] **Upstream missing copy** — commit dialog explains fix path
- [ ] **Trust defaults for demo** — file edits default **Ask**; set **Auto** in Settings for faster demos (documented in README)
- [x] **Editor refresh after agent write** — `WorkspaceMutationBus` reloads open non-dirty tab
- [ ] 👤 **Device matrix** — note Android version + device model in SCRATCHPAD after first QA run

---

## Build gate

```bat
gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

- [x] BUILD SUCCESSFUL (2026-06-11)
- [x] Unit tests: `GeminiMessageCodecTest`, `OpenAiMessageCodecTest`, `AgentToolRoundTripTest`, `GitPushFailuresTest`, telemetry/explorer tests

---

## 👤 USER — Manual device QA (~15 min)

Run on a **physical tablet or emulator** with network.

### Setup

- [ ] Install `app-debug.apk` (`gradlew.bat :app:assembleDebug`)
- [ ] Grant **All files access** (startup or IDE capability banner)
- [ ] Clone repo into `Downloads/KineticTest` (HTTPS + PAT, save token)
- [ ] AI panel → API keys → select model in picker

### AI edit loop

- [ ] Prompt: *“Read README.md and add a line at the top: `<!-- kinetic mvp test -->`”*
- [ ] Approve file mutation if Ask policy is on
- [ ] Editor shows change; revert from tool card works

### Git

- [ ] **Commit & push** → verify on GitHub
- [ ] **Push** only when clean + ahead
- [ ] (Optional) **Pull** when behind remote

### Failure cases

- [ ] No API key → send disabled, “Add API key”
- [ ] Non-git folder → commit UI explains
- [ ] Push without auth → clear error + save-token flow
- [ ] Airplane mode → network error with provider · model prefix

---

## Out of scope for this MVP slice

- Desktop/web build · LSP · PTY terminal · SSH git · Play Store release

See [`ROADMAP.md`](./ROADMAP.md) for full backlog.
