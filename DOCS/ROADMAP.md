<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# ROADMAP — Kinetic Android AI IDE

## Vision

Build the best tablet-first Android AI IDE: fast local editing, reliable AI agent tooling, deep
project awareness, and practical mobile workflows that still feel "real IDE", not "chat app with
text box."

## Product principles

- UX over elegance: fast, obvious interactions beat technically perfect but slow features.
- Offline-first where possible: editing and navigation should work without network.
- Agent-safe execution: every AI action should be reviewable, cancelable, and recoverable.
- Android-native constraints: battery, thermal, storage, and background limits are first-class.
- Ship in slices: each milestone should be independently usable on real tablets.

## Delivery checklist (living tracker)

- **Legend:** `[x]` landed in tree (may be partial / MVP)* · `[ ]` not started  
- *[AMENDED 2026-04-30]: Checklist appended; canonical narrative stays in sections below—do not drop checklist rows when syncing status.*

### Phase 1 · Foundation hardening

- **Epic 1.1 — Editor core**
  - [x] Robust undo/redo with grouped bursts (idle debounced)
  - [x] Auto-save (interval + tab-blur quiet save); conflict prompts **[partial — still TODO]**
  - [x] Large-file mode (>1 MB) + reduced UX (banner, no heavy gutter/highlight)
  - [x] Per-tab scroll + selection persisted (Compose + drafts)
  - [x] Dirty badges, close guards, Save all, crash-safe drafts + SAF restore
- **Epic 1.2 — Workspace + SAF**
  - [ ] Recursive tree virtualization (large dirs)
  - [x] File ops MVP: rename · duplicate · delete · new file / new folder (same-tree; cross-folder move still `[ ]`)
  - [x] Extension icons + grouping **[partial — icons by extension in explorer; grouped-by-type tree still `[ ]`]**
  - [ ] Favorites / recents / pin
  - [x] Fuzzy tree search **[MVP — explorer filter field, ordered-char fuzzy match + ancestor paths]**
- **Epic 1.3 — Tablet UX**
  - [ ] Split-pane gestures + snap presets
  - [ ] External keyboard shortcuts
  - [ ] Stylus-friendly selection tuning
  - [ ] Theme modes (studio high-contrast)
  - [ ] Command palette

### Phase 2 — Agent V1 + developer trust *(partial MVP; see granular § below)*

- **2.1** Tool router — **partial** (SAF tools through `rename_path`; trust/audit rows still `[ ]`).
- **2.2** Agent UX — **partial** (streaming, tool rows, **expandable tool payloads**); revert / telemetry remain `[ ]`.
- **2.3** Prompt/context — **partial** (static prompt + per-send tab/selection appendix); templates / git / diagnostics remain `[ ]`.
- *[AMENDED 2026-04-30]: Replaced “all `[ ]`” — granular truth lives in **Phase 2 · Tool router** + **Phase 2 · Agent UX & context**; narrative §Phase 2 epics below stay canonical for acceptance criteria.*

### Phase 3 — Intelligence · Phase 4 — DevOps · Phase 5 — Performance

- [ ] See epic lists below (`### Epic 3.x` … `### Epic 5.x`); promoted to granular boxes when shipped.

### Cross-cutting tracks

- [ ] Quality (tests + perf CI)
- [ ] Observability (latency metrics, debug panel)
- [ ] Security (sandbox tests, outbound secret scan, SBOM discipline)

### Milestone checklist (granular)

- [x] **M1:** undo/redo, autosave, dirty tabs, drafts + restore (+ large-file MVP, scroll/selection, file ops MVP)
- [ ] **M2:** tree virtualization, favorites/recents, palette (remaining file ops polish tracked in Epic 1.2)
- [ ] **M3:** split polish, keyboard shortcuts, large-file polish
- [ ] **M4–M11:** as in milestone map § below

### Phase 2 · Tool router *(granular, synced to codebase)*

*Source of truth:* `app/src/main/java/com/tabletaide/ide/agent/ToolRouter.kt` + `WorkspaceRepository`.

- **Epic 2.1 — tools & policy**
  - [x] **`list_files`** (capped workspace listing)
  - [x] **`read_file`** / **`write_file`**
  - [x] **`edit_file`** (unique substring replace — not full **`apply_patch` / unified diff**; treat as **[partial]** vs roadmap “apply_patch”)
  - [x] **`search_files`** (regex across files + scan/match caps)
  - [x] **`create_directory`** / **`delete_path`** / **`rename_path`** (same-parent leaf rename via SAF `DocumentFile`)
  - [ ] **`run_command`** / Termux-host bridge **[ ]**
  - [ ] Path sandbox **`[partial]`** — workspace is SAF subtree only; no formal allow-list / escape tests **[ ]**
  - [ ] Dry-run for destructive ops **[ ]**
  - [ ] Operation receipts + visible audit **[ ]**
  - [ ] Approval gates (**auto / ask / deny**) per tool class **[ ]**
- **Epic 2.2 — Agent UX** — see **Phase 2 · Agent UX & context** for row-level status (partial vs backlog).
- **Epic 2.3 — Prompt & context builder** — partial: `SYSTEM_PROMPT` + `IdeViewModel.buildAgentWorkspaceContext()` on each send (tabs/selection); templates/rules still `[ ]`.

*[AMENDED]*: When adding tools, bump **Phase 2 · Tool router** and the Phase 2 header pointer above — keep narrative §Phase 2 epics intact.

### Phase 2 · Agent UX & context *(granular, synced to `AgentChatPanel` / `AgentViewModel` / `IdeConstants`)*

- **Epic 2.2 — Agent UX**
  - [x] Streaming assistant text (model deltas → `AssistantStreaming` / `AssistantDone`)
  - [x] Inline **tool** rows after each round (name + clipped result preview)
  - [x] Busy / idle chrome + error surface in panel
  - [ ] Token-aware chunk rendering / rich markdown (beyond plain `Text`)
  - [x] Expandable tool cards (full **request JSON** + **full tool result** in panel; large results capped for UI)
  - [ ] One-tap apply / revert / copy-patch for edits
  - [ ] Session timeline with checkpoints and rollback points
  - [ ] Cost and token telemetry per conversation
- **Epic 2.3 — Prompt & context**
  - [x] Static **`SYSTEM_PROMPT`** (workspace-relative paths, `list_files` guidance) **[partial — not assembled from live workspace]**
  - [x] Workspace context builder **[partial — open tabs, active file, optional selection snippet appended to system prompt on send]**
  - [ ] Pinnable context blocks and reusable task templates
  - [ ] Rules/policies panel (style, safety, platform constraints)
  - [ ] Fast truncation + relevance ranking before send
  - [ ] “Explain before change” mode for sensitive tasks

*[AMENDED 2026-04-30]: New subsection — folds former “2.2/2.3 backlog” lines into the same checkbox style as Epic 2.1.*

---
**Footnotes**
- *[2026-04-30]*: M1 checklist items shipped (session drafts, editing stack in `IdeViewModel`, explorer SAF file ops MVP). *[AMENDED session]* Sync checkboxes whenever scope changes—**never delete** historical rows; supersede only with `[superseded YYYY-MM-DD]` beside the line.
- *[AMENDED]: **Phase 2 · Tool router** granular block reflects current Anthropic-tool surface (`list_files` … `rename_path`). Trust/audit/approvals rows stay `[ ]` until implemented.*
- *[AMENDED 2026-04-30]: **Phase 2 · Agent UX & context** — checkboxes reflect `AgentChatPanel` / `AgentViewModel` streaming + tool rows, expandable tool payloads, `IdeConstants.SYSTEM_PROMPT`, and `IdeViewModel.buildAgentWorkspaceContext()`.*

## Release themes

1. Foundation UX + trustable file editing.
2. Agent capability + project intelligence.
3. Power-user developer workflows.
4. Performance engine + ecosystem expansion.

## Phase 1 — Foundation hardening (0-8 weeks)

Goal: make daily editing stable and pleasant before adding heavy intelligence.

### Epic 1.1: Editor core reliability

- Add robust undo/redo stack with grouped operations and cross-tab state recovery.
- Implement auto-save policy (on blur, interval, manual override) with conflict prompts.
- Add large-file mode (>1 MB) with reduced features and explicit UX indicator.
- Add persistent cursor/scroll restoration per file and per tab.
- Add file dirty-state badges, close guards, and "save all".

Acceptance:
- No data loss in forced app restart test.
- Undo/redo survives orientation change.
- 95th percentile open-to-edit latency under 300 ms for 2,000-line files.

### Epic 1.2: Workspace + SAF quality

- Add recursive tree virtualization for large directories.
- Add file operations: rename, move, duplicate, delete, create file/folder.
- Add extension-based icons and file-type grouping.
- Add favorites and recent files with pinning.
- Add search in tree (name/path fuzzy match).

Acceptance:
- 5,000-file workspace remains responsive at 60 fps while scrolling tree.
- File operations are atomic and error surfaced with retry.

### Epic 1.3: Tablet UX polish

- Improve split pane gestures and snap presets (50/50, 70/30, 30/70).
- Add keyboard shortcut layer (external keyboard first-class).
- Add stylus-friendly selection handles and tap target tuning.
- Add theme modes: dark/light/high-contrast "studio" variants.
- Add command palette for all primary actions.

Acceptance:
- Top 30 actions reachable via keyboard and command palette.
- All primary panes usable in landscape and portrait tablets.

## Phase 2 — Agent V1 + developer trust (8-16 weeks)

Goal: move from "chat with tools" to dependable coding copilot.

### Epic 2.1: Tool router expansion

- Add tools: list_dir, grep/semantic search, mkdir, rename, delete, apply_patch, run_command.
- Add path sandboxing and allow-list policy per workspace root.
- Add dry-run mode for destructive operations.
- Add operation receipts (who, what, when, diff summary).
- Add user approval gates per tool class (auto/ask/deny).

Acceptance:
- Every tool call has deterministic result envelope and visible audit trail.
- No tool can escape workspace scope.

### Epic 2.2: Agent UX and control surface

- Streaming response with token-aware chunk rendering.
- Inline tool cards with expandable request/response payloads.
- One-tap "apply", "revert", and "copy patch" actions.
- Session timeline with checkpoints and rollback points.
- Cost and token telemetry per conversation.

Acceptance:
- User can understand and reverse any agent action in under 2 taps.
- No UI freeze during 3+ simultaneous tool events.

### Epic 2.3: Prompt and context system

- Workspace context builder (open files, symbols, diagnostics, git diff).
- Pin-able context blocks and reusable task templates.
- Rules/policies panel (coding style, safety rules, platform constraints).
- Fast context truncation strategy with relevance ranking.
- "Explain before change" mode for sensitive tasks.

Acceptance:
- Context assembly under 150 ms median for normal workspace.
- Agent output quality improves on benchmark prompts across 10 scenarios.

## Phase 3 — Intelligence features (16-28 weeks)

Goal: match core desktop IDE expectations for understanding code.

### Epic 3.1: LSP platform

- Integrate LSP manager lifecycle (start/stop/restart server per project).
- Diagnostics pipeline (errors, warnings, hints in editor + panel).
- Hover, definition, references, rename symbol.
- Code actions and formatting hooks.
- Multi-language server presets (Kotlin, TS/JS, Python, Rust baseline).

Acceptance:
- End-to-end "go to definition" under 200 ms on warm session.
- Diagnostics update incrementally on edit without blocking typing.

### Epic 3.2: Project-wide search and navigation

- Ripgrep-backed text search with filters and preview.
- Symbol index with fuzzy jump.
- Breadcrumb enrichment by syntax scope.
- Cross-file references graph (lightweight).
- Search results to split view quick-open.

Acceptance:
- Query results for medium project (<100k LOC) in <1 s typical.

### Epic 3.3: AI coding accelerators

- Inline code completion ghost text (agent-backed, latency budgeted).
- Refactor intents ("extract method", "rename safely", "add tests").
- Test generation scaffolds with per-language templates.
- Explain selection/file/project modes.
- Bug-fix loop: detect failing tests -> propose patch -> rerun.

Acceptance:
- Completion median latency under 250 ms for cached contexts.
- Refactor actions always produce preview before apply.

## Phase 4 — DevOps and team workflows (28-40 weeks)

Goal: make the app viable for serious project iteration, not just prototyping.

### Epic 4.1: Git operations

- Status, diff, stage/unstage, discard hunk, commit composer.
- Branch create/switch and upstream tracking.
- Conflict viewer with three-way merge assist.
- Commit message AI helper with repo style hints.
- Basic PR draft generation (title/body/checklist).

Acceptance:
- Common single-branch workflow done without leaving app.
- Conflict resolution supports at least text-based merge flows.

### Epic 4.2: Terminal and task runner

- Replace stub with stable process host bridge.
- Persistent terminal sessions with reconnection.
- Task presets (build/test/lint/deploy local variants).
- Output parser for clickable stack traces.
- Resource governor (CPU/thermal) for long tasks.

Acceptance:
- Build/test logs stream reliably across app background/foreground transitions.

### Epic 4.3: Team-grade collaboration

- Session export/share (prompt, tool logs, diffs).
- Work notes tied to files/symbols.
- Review mode for staged changes with AI risk highlights.
- "handoff packet" generation for async teammates.

Acceptance:
- Handoff artifact can reconstruct intent and exact edits.

## Phase 5 — Performance engine + Android depth (40-56 weeks)

Goal: support very large projects while keeping tablet UX smooth.

### Epic 5.1: Rust core integration (optional but strategic)

- Rope-backed document buffer via JNI bridge.
- Tree-sitter incremental parse pipeline.
- Tokenization and fold regions from Rust service.
- Incremental diff engine for faster patch previews.

Acceptance:
- 100k+ LOC workspace operations without jank in core interactions.
- Syntax highlighting cost reduced versus pure Kotlin baseline.

### Epic 5.2: Advanced Android platform features

- Foldable posture-aware layouts.
- Multi-window task handoff and drag/drop improvements.
- Scoped storage edge-case handling matrix per API level.
- Better background behavior under Doze/app standby constraints.
- Optional on-device model inference gateway for small tasks.

Acceptance:
- Consistent behavior across API 26-35 test matrix and foldable profiles.

### Epic 5.3: Quality, security, and resilience

- Structured crash/error reporting with redaction.
- Permission and secrets scanner before commit/share.
- Immutable audit log mode for enterprise users.
- Backup/restore for sessions and workspace metadata.

Acceptance:
- Zero high-severity security issues in periodic audit.
- Recovery flow can restore recent state after crash.

## Cross-cutting tracks (run every phase)

### Quality track

- Unit tests for core editor/agent/tool routing.
- Integration tests for file operations and LSP lifecycle.
- UI tests for key flows (open/edit/save, run command, apply patch).
- Performance CI checks (startup, typing latency, memory).

Target:
- 80% coverage on business logic modules.

### Observability track

- Latency metrics: editor actions, tool calls, network round trips.
- Failure taxonomy and retry strategy dashboards.
- On-device debug panel for advanced users.

### Security track

- Workspace sandbox enforcement tests.
- Secret detection on outbound prompts/tools.
- Dependency hygiene with SBOM-first updates.

## Feature backlog (high-value add-ons)

- Voice coding + dictation command mode.
- Camera-to-code (OCR from whiteboard/mockup into notes/snippets).
- Plugin/extension API (safe sandboxed commands).
- Snippet marketplace synced with repo templates.
- AI pair "roles" (architect, reviewer, debugger) with specialized prompts.
- Offline knowledge packs (language docs, API refs, style guides).
- Project memory graph (decisions, architecture notes, change history).
- Smart battery mode that downshifts AI/analysis features automatically.

## Suggested milestone map (granular)

- M1 (Weeks 1-2): undo/redo, autosave, dirty tabs, crash-safe draft recovery.
- M2 (Weeks 3-4): tree virtualization, file ops, favorites/recents, command palette.
- M3 (Weeks 5-8): split-view polish, keyboard shortcuts, large-file mode.
- M4 (Weeks 9-12): tool router v1 + audit trail + approvals.
- M5 (Weeks 13-16): agent timeline, reversible apply/revert, cost telemetry.
- M6 (Weeks 17-22): LSP diagnostics + go-to-definition + hover.
- M7 (Weeks 23-28): search/index, inline AI completion, refactor previews.
- M8 (Weeks 29-34): git status/diff/commit + terminal persistence.
- M9 (Weeks 35-40): merge helper + collaboration handoff packets.
- M10 (Weeks 41-48): Rust rope + tree-sitter opt-in path.
- M11 (Weeks 49-56): foldable optimization, security hardening, resilience suite.

## Prioritization scorecard (for feature intake)

Score features 1-5 on:

- User pain reduced
- Dev effort
- Risk
- Performance impact
- Revenue/retention potential

Prioritize high pain reduction + low/medium risk first.

## 90-day execution starter (recommended next)

1. Ship Phase 1 Epics 1.1 + 1.2 minimum usable reliability baseline.
2. Build Tool Router v1 receipts/approvals before adding more powerful tools.
3. Deliver LSP diagnostics + definition early to prove "real IDE" value.
4. Add terminal persistence and git status/commit to close daily workflow loop.

---

*[2026-04-30]: Initial roadmap created after docs review request; aligned to existing
Phase 1-3 architecture and extended to feature-complete Android AI IDE trajectory.*

*[AMENDED 2026-04-30]: § Delivery checklist (living tracker) + milestone granularity; status synced to app implementation.*

*[AMENDED]: § Phase 2 tool-router **granular** checklist appended (Epic 2.1 MVP vs backlog); aligns `ToolRouter` tool definitions/dispatch.*

*[AMENDED 2026-04-30]: § **Phase 2 · Agent UX & context** — granular Epic 2.2/2.3 rows synced to `AgentChatPanel.kt`, `AgentViewModel.kt`, `IdeConstants.SYSTEM_PROMPT`; Phase 2 checklist header corrected from “all `[ ]`” to partial MVP.*

*[AMENDED 2026-04-30 (session)]: Explorer **filter** (`TreeFilter.kt`), **extension icons** (`ExplorerIcons.kt`), agent **expandable tool cards** + **workspace context appendix** on send.*
