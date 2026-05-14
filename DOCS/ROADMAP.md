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

## Status snapshot — 2026-05-14

*Clean, scannable view of where we are right now. Detailed history lives in the **Delivery checklist** below; this section is a derived summary and is safe to rewrite each session.*

### Shipped (MVP or better)

- **Editor core** — undo/redo, autosave, dirty tabs, crash-safe drafts, large-file mode, per-tab scroll/selection.
- **Workspace + SAF** — file ops MVP (rename, duplicate, delete, create), extension icons, starred/recent pins, fuzzy tree filter.
- **[AMENDED 2026-05-14] Explorer tree browse path** — incremental root/subdirectory loading,
  folder expand/collapse, branch-local refresh after mutations, and explicit loading/empty/filter-empty
  states. Filter mode still does a bounded full-tree scan while a query is active.
- **Tablet UX** — split-pane with snap presets, command palette, keyboard shortcuts (palette / save / close / tab cycle), three theme modes, inline capability banners.
- **Agent tool router** — `list_files`, `read_file`, `write_file`, `edit_file`, `search_files`, `create_directory`, `delete_path`, `rename_path`, `run_command`.
- **Agent UX** — streaming text, expandable tool cards with full request/result, copy-JSON, receipts (provider, time, duration, target, status, risk, policy, decision).
- **[AMENDED 2026-05-14] Agent telemetry MVP** — durable local JSONL event log, per-session rollup strip in the AI panel, rough token/cost estimator with source/confidence, model/tool/approval/checkpoint spans, and focused unit coverage for telemetry codec math/round-trip.
- **Trust & safety**
  - Per-class policy modes (file changes · destructive ops · shell commands) persisted via `AgentTrustStore`.
  - Approval dialog before risky tool calls.
  - **Revert / Apply again** for `write_file` / `edit_file` (single-file snapshot).
  - **Command preview** (risk level · reasons · likely targets) shown in approval dialog for `run_command`.
  - **Revert / Reapply** for `run_command` via pre/post workspace snapshot diff, with per-path conflict detection.

### In flight / partial

- Explorer filtered search still materializes the full tree while a query is active; normal browse mode is incremental.
- Autosave conflict prompts — silent failures fixed; interactive resolution UX pending.
- Stylus-friendly selection tuning.
- **[AMENDED 2026-05-14] Persistent audit timeline** — `AgentAuditStore` persists every agent tool
  invocation to local JSONL; `AuditTimelinePanel` provides a dedicated dialog (command palette →
  Audit timeline) with expandable entry cards showing tool name, target, status, risk, policy,
  approval, duration, and mutation summary. Filter/sort and export are still `[ ]`.
- Agent telemetry (cost, tokens) and session timeline / checkpoints.
  - **[AMENDED 2026-05-14]** Expand via **Phase 2 · Agent telemetry, evaluation, and audit observability** below before implementation; treat as product-quality observability layer, not a counter widget.
  - **[AMENDED 2026-05-14]** First telemetry slice is shipped: local append-only `agent_telemetry.jsonl`, `TelemetryEvent`/`AgentTelemetryStore`, chat-panel summary metrics, rough token/cost estimates, tool/model/approval/checkpoint event capture, and codec tests. Remaining work: full trace drawer, export/redaction UI, pricing-table reconciliation, retention controls, dashboards, and benchmark runner.
- Prompt context — workspace appendix on send done; pinnable blocks, templates, relevance ranking pending.

### Not started (next horizons)

- LSP platform (Phase 3.1) — diagnostics, hover, go-to-definition, rename symbol.
- Project-wide ripgrep + symbol index (Phase 3.2).
- Inline completions / refactor intents (Phase 3.3).
- Read-only project structure tree view that reuses the explorer node model as a separate surface, not a
  replacement for Explorer.
- Git operations UI (Phase 4.1).
- Persistent terminal & task runner (Phase 4.2).
- Rust rope + tree-sitter core (Phase 5.1).
- Persistent audit log, command allowlist, full rules panel.

### Natural next slice

1. **[AMENDED 2026-05-14 — shipped as partial MVP: `AgentAuditStore` + `AuditTimelinePanel` reachable via command palette; filter/sort and export still `[ ]`]**
2. LSP MVP for Kotlin (highest-leverage "real IDE" signal).
3. ~~Git status / diff / commit MVP (closes daily-workflow loop on-device).~~ *(shipped 2026-05-13)*

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
  - [x] Recursive tree virtualization **[AMENDED 2026-05-14 — shipped for the normal browse path: `WorkspaceRepository.listDirectoryRows(...)`, incremental root/subtree loading in `IdeViewModel`, expandable folders, branch-local refreshes, and explicit loading/empty/filter-empty states. Filter mode still falls back to a full-tree scan while query text is non-blank.]**
  - [x] File ops MVP: rename · duplicate · delete · new file / new folder (same-tree; cross-folder move still `[ ]`)
  - [x] Extension icons + grouping **[partial — icons by extension in explorer; grouped-by-type tree still `[ ]`]**
  - [x] Favorites / recents / pin **[partial — starred list + MRU recents per workspace, explorer sections + menu + palette toggle active file; pin-to-top chrome still `[ ]`]**
  - [x] Fuzzy tree search **[MVP — explorer filter field, ordered-char fuzzy match + ancestor paths]**
- **Epic 1.3 — Tablet UX**
  - [x] Split-pane + presets **[partial — draggable divider + snap-to-preset on drag end + palette fractions 70/30 · 50/50 · 30/70; fling snap gestures still `[ ]`]**
  - [x] External keyboard shortcuts **[partial — Ctrl+P palette; Ctrl+S / Ctrl+Shift+S / Ctrl+W; Ctrl+Tab / Ctrl+Shift+Tab tab cycle; full shortcut layer still `[ ]`]**
  - [ ] Stylus-friendly selection tuning
  - [x] Theme modes **[partial — Dark / Light / High Contrast via settings dialog + command palette; additional per-pane/accessibility polish still `[ ]`]**
  - [x] Command palette **[partial — filtered palette + primary actions; not yet “all primary actions”]**
  - [x] Capability/status banners **[MVP — inline shell banner now explains when editing/chat still work but git and/or command execution are unavailable in the current workspace]**

### Phase 2 — Agent V1 + developer trust *(partial MVP; see granular § below)*

- **2.1** Tool router — **partial** (SAF tools through `rename_path`; trust/audit rows still `[ ]`).
- **2.2** Agent UX — **partial** (streaming, tool rows, expandable payloads, **apply/revert for write/edit**); telemetry remain `[ ]`.
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
- [x] **M2:** tree virtualization **[AMENDED 2026-05-14 — incremental browse path shipped; query-mode full-tree scan remains a follow-up optimization rather than a blocker]** · favorites/recents **[partial — `ExplorerPinsStore` + explorer UI]** · palette **[palette MVP]**
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
  - [x] **`run_command`** **[partial — backed by the in-app `/system/bin/sh` workspace runner with trust-policy ask / deny controls plus approval UI; risk-classified command preview + pre/post workspace snapshot revert layer landed 2026-05-14; still no PTY, timeout policy, Termux bridge, background-job model, or command allowlist]**
  - [ ] Path sandbox **`[partial]`** — workspace is SAF subtree only; no formal allow-list / escape tests **[ ]**
  - [x] Dry-run for destructive ops **[partial — `CommandRiskClassifier` surfaces risk level, reasons, and likely targets in the run_command approval dialog; no command simulation/dry-execute yet]**
   - [x] Operation receipts + visible audit **[partial — expanded tool cards show provider, time, duration, target, status, risk, policy, and approval decision; persistent audit log now has a dedicated timeline panel (`AuditTimelinePanel`) reachable via command palette]**

   - [x] Approval gates (**auto / ask / deny**) per tool class **[partial — persisted policy modes now gate file changes and destructive ops with `auto / ask / deny`, while shell commands intentionally stay `ask / deny`; fine-grained per-tool/per-workspace policy remains `[ ]`]**
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
  - [x] Copy tool request JSON **[partial — clipboard from expanded row]**
  - [x] One-tap apply / revert **[partial — `write_file` / `edit_file` tool rows: disk snapshot Revert / Apply again + conflict state; `run_command` rows: pre/post workspace snapshot Revert / Reapply with per-path conflict detection (2026-05-14); unified diff / copy-patch still `[ ]`]**
  - [ ] Session timeline with checkpoints and rollback points
  - [ ] Cost and token telemetry per conversation
    - **[AMENDED 2026-05-14]** Scope is broader than cost display: token accounting, latency, tool-call traces, model/provider metadata, prompt/context composition, mutation diffs, checkpoint graph, privacy controls, exportable session packets, and eval-ready event streams. See **Phase 2 · Agent telemetry, evaluation, and audit observability**.
    - **[AMENDED 2026-05-14]** MVP shipped as **[partial]**: `AgentTelemetryStore` writes local JSONL events, `AgentViewModel` records turn/model/tool/approval/mutation/checkpoint/cost events, `AgentChatPanel` shows session rollup, and `AgentTelemetryCodecTest` verifies summaries, JSON round-trip, and estimator source. Full trace drawer/export/redaction/retention/dashboard still `[ ]`.
- **Epic 2.3 — Prompt & context**
  - [x] Static **`SYSTEM_PROMPT`** (workspace-relative paths, `list_files` guidance) **[partial — not assembled from live workspace]**
  - [x] Workspace context builder **[partial — open tabs, active file, optional selection snippet appended to system prompt on send]**
  - [ ] Pinnable context blocks and reusable task templates
  - [ ] Rules/policies panel (style, safety, platform constraints)
  - [ ] Fast truncation + relevance ranking before send
  - [ ] “Explain before change” mode for sensitive tasks

*[AMENDED 2026-04-30]: New subsection — folds former “2.2/2.3 backlog” lines into the same checkbox style as Epic 2.1.*

### Phase 2 · Agent telemetry, evaluation, and audit observability *(lab-grade target spec)*

*Purpose:* make every agent session measurable, debuggable, reversible, reproducible, and safe enough for serious coding work. This is not a decorative token counter; it is the evidence layer for trust, cost control, quality evaluation, regression analysis, and future AI-lab-style experimentation on-device.

#### Product surface

- [ ] **Conversation telemetry header** — provider, model, effective context window, prompt tokens, completion tokens, cache-read/write tokens if provider exposes them, estimated cost, wall-clock duration, first-token latency, total tool time, retry count, and stop reason.
- [ ] **Per-turn trace drawer** — exact message envelope summary, context blocks included/excluded, truncation decisions, pinned files, selected range, open tabs, git/diff/diagnostic context once available, policy mode, approval decisions, and final model/tool outcome.
- [ ] **Tool-call timeline** — ordered spans for tool request, approval wait, execution, result serialization, UI render, apply/revert/reapply actions, conflicts, and user cancellations.
- [ ] **Session checkpoints** — named rollback points before risky mutations, automatic checkpoints before multi-tool turns, checkpoint diff summary, restore confidence, and conflict explanation.
- [ ] **Cost budget controls** — per-session budget, warning threshold, hard-stop threshold, model downgrade suggestion, context trimming suggestion, and “expensive turn” explanation.
- [ ] **Export packet** — redacted JSONL trace, prompt/context manifest, tool receipts, mutation diffs, model/provider versions, app version, workspace fingerprint, and replay notes for debugging.
- [ ] **Debug panel** — aggregate local metrics by day/workspace/model: p50/p95 latency, failure rate, average tokens/turn, average tool calls/turn, approval rate, revert rate, command risk distribution, and top failure classes.

#### Event model and schema

- [ ] **Canonical event log** — append-only local JSONL or SQLite table with stable `schemaVersion`, monotonic sequence, wall time, session id, turn id, span id, parent span id, event type, redaction class, and payload hash.
- [ ] **Required event types** — `session_started`, `turn_started`, `context_built`, `prompt_sent`, `model_delta`, `model_completed`, `tool_requested`, `approval_requested`, `approval_decided`, `tool_started`, `tool_completed`, `mutation_snapshot_created`, `mutation_applied`, `mutation_reverted`, `checkpoint_created`, `checkpoint_restored`, `cost_estimated`, `error_recorded`, `session_exported`.
- [ ] **Trace/span semantics** — every model call and tool call is a span with start/end time, status, error code, retry metadata, parent-child linkage, and deterministic correlation id.
- [ ] **Token accounting contract** — store provider-reported usage when available; otherwise store estimator name/version, tokenizer assumption, estimated count, and confidence level.
- [ ] **Cost accounting contract** — store pricing table version, input/output/cache token rates, currency, estimate source, and provider invoice reconciliation flag when available.
- [ ] **Context manifest** — record every included block by type, path/symbol/range, token estimate, rank score, reason included, reason excluded for near misses, truncation boundary, and redaction state.
- [ ] **Mutation manifest** — record pre/post hashes, byte size, line count delta, diff summary, snapshot id, restore method, conflict state, and whether mutation came from file tool, command, or future patch tool.

#### Privacy, safety, and retention

- [ ] **Local-first storage** — telemetry remains on-device by default; export/share requires explicit user action and visible redaction review.
- [ ] **Redaction pipeline** — classify payload fields as safe metadata, path metadata, code/content, secret candidate, credential, prompt text, model output, command output, or binary/unsupported.
- [ ] **Secret scanning** — run outbound/export scans for common API keys, tokens, private keys, `.env` values, auth headers, and Android keystore metadata leaks.
- [ ] **Retention policy** — user-configurable retention by event class: keep metadata longer, prune model deltas/tool payloads sooner, preserve audit receipts for mutations unless user purges workspace history.
- [ ] **Workspace isolation** — telemetry namespace per workspace root fingerprint; no cross-workspace leakage in search, dashboard, export, or context ranking.
- [ ] **Tamper-evident mode** — optional hash chain for audit events: each event stores previous event hash so mutation history can be trusted during review/export.

#### Evaluation and quality loop

- [ ] **Golden task suite** — at least 10 benchmark prompts covering read-only analysis, single-file edit, multi-file edit, risky command refusal, test-fix loop, context truncation, revert flow, and long-session continuity.
- [ ] **Run comparison view** — compare model/provider/prompt/template across benchmark runs: pass/fail, diff correctness, tool count, latency, token cost, approvals, reversions, and user intervention count.
- [ ] **Failure taxonomy** — classify failures as tool schema, provider transport, hallucinated path, bad edit, unsafe command, context miss, token overflow, timeout, UI freeze, permission, SAF failure, or user cancellation.
- [ ] **Regression gates** — no release should degrade benchmark pass rate, p95 first-token latency, p95 tool-call latency, mutation rollback success, or secret-redaction accuracy beyond defined thresholds.
- [ ] **Human review hooks** — mark turn as good/bad, tag root cause, attach note, and add anonymized/redacted turn to local benchmark corpus.
- [ ] **Prompt/version tracking** — bind every turn to system prompt version, tool schema version, context-builder version, policy version, and model alias resolution.

#### Engineering architecture

- [ ] **TelemetryCollector** — single ingestion API used by `AgentViewModel`, `ToolRouter`, provider clients, approval UI, snapshot store, and future LSP/git systems.
- [ ] **TelemetryStore** — bounded local persistence with backpressure, batch writes, schema migration, corruption recovery, and workspace-level purge.
- [ ] **UsageEstimator** — provider-aware token/cost module with pluggable pricing tables and explicit fallback when provider usage is missing.
- [ ] **TraceRenderer** — Compose UI state derived from stored events, not transient chat row state, so timeline survives process death.
- [ ] **ExportRedactor** — deterministic redaction pass that emits manifest of removed/hashed fields and never silently includes high-risk payloads.
- [ ] **MetricsAggregator** — incremental rollups for dashboard so large histories do not jank UI.
- [ ] **Test harness** — fake provider + fake tool router replay scripts to verify event ordering, cost math, redaction, checkpoint restore, and export determinism.

#### Acceptance criteria

- Every agent turn can be reconstructed from telemetry without relying on in-memory chat state.
- Every tool mutation links to a pre-mutation checkpoint or explicit reason why no checkpoint exists.
- Token/cost numbers show source and confidence, never pretending estimates are provider invoices.
- User can identify “why was this expensive / slow / wrong?” from UI in under 3 taps.
- Exported session packet can reproduce model/tool sequence shape with secrets redacted and hashes stable.
- Telemetry capture adds under 5 ms median overhead per event and never blocks editor typing.
- Dashboard handles 1,000 turns / 10,000 events in one workspace without UI freeze.
- Redaction tests catch representative API keys, bearer tokens, private keys, `.env` secrets, and command-output secrets.

#### First implementation slice

1. [x] Add `TelemetryEvent` model + `TelemetryStore` append/query APIs. **[AMENDED 2026-05-14 — `AgentTelemetryStore` writes bounded local `agent_telemetry.jsonl`; query path returns recent events and rollup summary.]**
2. [x] Instrument provider request/response usage, latency, model id, stop reason, and errors. **[AMENDED 2026-05-14 — model spans capture provider/model/status/duration/first-token/stop reason plus rough token estimates; provider-reported usage still `[ ]` because current streaming parsers do not expose usage objects.]**
3. [x] Instrument tool-call spans, approval waits, mutation snapshot ids, revert/reapply outcomes. **[AMENDED 2026-05-14 — tool requested/started/completed, approval requested/decided, checkpoint created/restored, mutation applied/reverted events now emit from `AgentViewModel`.]**
4. [x] Add conversation header metrics and per-turn trace drawer in agent panel. **[partial — session rollup strip shipped in `AgentChatPanel`; full per-turn trace drawer still `[ ]`.]**
5. [ ] Add local debug metrics screen with p50/p95 latency, token/cost totals, failure classes. **[partial — rollup computes average first-token, p95 tool latency, total events/turns/tokens/cost/failures; separate debug screen still `[ ]`.]**
6. [ ] Add redacted JSONL export with schema version and payload hash manifest.
7. [x] Add fake-provider tests for event ordering, token/cost fallback, and export redaction. **[partial — `AgentTelemetryCodecTest` covers summary aggregation, JSON round-trip, and rough estimator source; fake-provider ordering/export-redaction tests still `[ ]`.]**

#### Open design decisions

- Decide SQLite vs JSONL for long-term store; SQLite fits dashboards/querying, JSONL fits export/replay simplicity.
- Decide exact pricing table source/update path; avoid network dependency unless user opts in.
- Decide whether raw model deltas are stored by default or reconstructed from final assistant text to reduce privacy risk.
- Decide retention defaults for command output because logs can contain secrets despite not being prompts.
- Decide whether tamper-evident audit mode is always on for mutations or only enterprise/debug mode.

---
**Footnotes**
- *[2026-04-30]*: M1 checklist items shipped (session drafts, editing stack in `IdeViewModel`, explorer SAF file ops MVP). *[AMENDED session]* Sync checkboxes whenever scope changes—**never delete** historical rows; supersede only with `[superseded YYYY-MM-DD]` beside the line.
- *[AMENDED]: **Phase 2 · Tool router** granular block reflects current Anthropic-tool surface (`list_files` … `rename_path`). Trust/audit/approvals rows stay `[ ]` until implemented.*
- *[AMENDED 2026-04-30]: Epic 1.2 explorer **`filteredTree`** + lazy pin rows; Epic 2.2 **`ToolMutationAction`** apply/revert for `write_file`/`edit_file`.*

- *[AMENDED 2026-04-30]: Epic 1.2 **ExplorerPinsStore** + explorer STARRED/RECENT; Epic 1.3 divider snap + Ctrl+S / Ctrl+Shift+S / Ctrl+W.*
- *[AMENDED 2026-04-30]: Epic 2.1 visible tool receipts: expanded tool cards show provider, time, duration, target, and OK/FAILED status.*
- *[AMENDED 2026-05-13]: Ship-readiness sprint landed outside the original checkbox rows: AI tool schema/provider reliability was hardened, `Execute` is now backed by a first in-app runner (`/system/bin/sh`, one foreground command, workspace-root execution), and autosave failures now surface status feedback. Full PTY shell, debugger integration, and agent `run_command` tool support remain pending.*
- *[AMENDED 2026-05-13]: Epic 1.1 conflict handling is still **partial**; silent autosave failures are fixed, but interactive conflict prompts/resolution UX remain TODO.*
- *[AMENDED 2026-05-13]: Agent `run_command` now exists as a real tool path using the runner MVP plus an explicit approval dialog before execution. It remains intentionally bounded to one foreground workspace-root command at a time, with no PTY/Termux bridge or generalized tool policy matrix yet.*
- *[AMENDED 2026-05-14]: Agent trust policy broadened: file changes and destructive ops now use persisted **auto / ask / deny** settings, while shell commands intentionally stay **ask / deny** only; approval receipts also surface risk class, policy mode, and decision outcome. This is still a lightweight trust layer, not yet a persistent audit log, command allowlist, or full rules panel.*
- *[AMENDED 2026-05-14]: Capability banners now surface workspace/git/runner availability inline in the shell, making the shared-storage + All files access constraints visible before the user hits a disabled action or error dialog.*
- *[AMENDED 2026-05-14]: Epic 2.1 — `run_command` gained a **risk-classified preview** (`CommandRiskClassifier`: rm / mv / dd / chmod -R / git reset --hard / `>` redirect / sudo / pipe-to-shell heuristics) shown in the approval dialog, plus a **`WorkspaceSnapshotStore`-backed Revert / Reapply** flow on `run_command` chat receipts. Snapshots are bounded (256 KiB / file, 5 000 files, excluding `.git`, `build`, `.gradle`, `.idea`, `node_modules`, `dist`, `out`) with per-path conflict detection on restore. Persistent audit timeline still `[ ]`.*
- *[AMENDED 2026-05-14]: Explorer tree wrap-up: the normal browse path now uses an incremental node model (`WorkspaceRepository.listDirectoryRows(...)` + `IdeViewModel` visible-row derivation) with folder expand/collapse and branch-local refreshes. Fuzzy filter remains intentionally simpler and still materializes the full tree only while a query is active.*
- *[AMENDED 2026-05-14]: Agent telemetry first slice shipped: `AgentTelemetryStore` / `TelemetryEvent` local JSONL, `AgentViewModel` instrumentation for model/tool/approval/mutation/checkpoint/cost events, `AgentChatPanel` rollup strip, and focused `AgentTelemetryCodecTest` coverage. Still not full lab-grade export/redaction/dashboard/eval system.*

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

*[AMENDED 2026-04-30]: Agent tool receipt MVP: expanded tool cards now expose visible operation receipts (provider, timestamp, duration, target, status) as a stepping stone toward persistent audit logs and approval gates.*

*[AMENDED 2026-05-14]: Added top-of-file **Status snapshot** section as a derived, rewritable summary view (clean lists for Shipped / In flight / Not started / Next slice). Annotated Epic 2.1 (`run_command`, dry-run row) and Epic 2.2 (one-tap apply/revert) to reflect the command preview + workspace snapshot revert layer landing today.*
