# ROADMAP — Kinetic Android AI IDE

## North Star

Build a tablet-first Android IDE that feels like a real development environment: fast local editing, dependable AI agents, project intelligence, reversible automation, and mobile-native workflows.

## Product Principles

- **Fast first:** editing, navigation, and common actions stay responsive on real tablets.
- **Agent-safe:** every AI action is inspectable, approval-gated when risky, and reversible when possible.
- **Offline-capable:** file editing and navigation work without network; AI and git degrade clearly.
- **Android-native:** storage, battery, thermal, background limits, and permissions are first-class constraints.
- **Shipped slices:** every milestone should be independently useful.

## Current Status

### Shipped

- **Editor core:** undo/redo, autosave, dirty tabs, crash-safe drafts, large-file mode, scroll/selection restore.
- **Workspace explorer:** SAF workspace open/create, file ops, extension icons, starred/recent pins, fuzzy filter, incremental browse tree.
- **Tablet shell:** split editor/agent layout, draggable presets, command palette, keyboard shortcuts, light/dark/high-contrast themes, capability banners.
- **AI provider setup:** runtime Anthropic/Gemini API key entry, provider switching, prompt enhancement.
- **Agent tool router:** `list_files`, `read_file`, `write_file`, `edit_file`, `search_files`, `create_directory`, `delete_path`, `rename_path`, `run_command`.
- **Agent trust controls:** persisted Auto/Ask/Deny policies for file/destructive tools, Ask/Deny for shell commands, approval dialogs, risk preview.
- **Agent reversibility:** file Revert/Apply again, command snapshot Revert/Reapply, conflict detection.
- **Agent observability:** tool receipts, persistent audit timeline, telemetry JSONL, token/cost estimates, latency/failure rollups.
- **Command runner MVP:** bounded `/system/bin/sh` execution for resolvable workspaces, live output, rerun, cancel.
- **Git MVP:** HTTPS clone/auth, saved tokens, repo detection, status/diff context, commit, tracked-branch push, AI commit message draft.
- **Starter projects:** blank workspace, Kotlin console, web starter.
- **Testing foundation:** focused unit tests for explorer tree/filter and telemetry codec.

### Partial / Needs Hardening

- **Autosave conflicts:** failures surface, but interactive conflict resolution is not complete.
- **Explorer search:** normal browse is incremental; active fuzzy query still scans full tree.
- **Command runner:** no PTY, Termux bridge, timeout policy, background jobs, allowlist, or task presets.
- **Agent telemetry:** rollup exists; full trace drawer, export/redaction UI, retention controls, pricing-table reconciliation, and eval runner remain.
- **Audit timeline:** persisted and viewable; filter/sort/export remain.
- **Prompt context:** open tabs/selection appendix exists; pinnable blocks, templates, ranking, diagnostics/git context expansion remain.
- **Trust policy:** app-wide today; per-workspace/per-tool rules remain.
- **Git UI:** commit/push exists; in-app **Save git credentials** for HTTPS push on opened repos;
  stage/unstage, hunk discard, branch UI, merge/conflict UI, and pull-on-reject remain.

### Not Started

- LSP diagnostics, hover, go-to-definition, references, rename symbol.
- Project-wide ripgrep UI and symbol index.
- Inline AI completion and refactor intents.
- Persistent terminal sessions and task runner.
- Test runner integration and failure-fix loop.
- Collaboration handoff packets and review mode.
- Rust rope/tree-sitter core.
- Foldable/multi-window depth.
- Secret scanner before prompt/export/commit.
- Enterprise audit hardening.

## Next Slices

1. **LSP MVP for Kotlin**
   - Add LSP lifecycle manager.
   - Surface diagnostics in editor and panel.
   - Support hover and go-to-definition.
   - Acceptance: warm go-to-definition under 200 ms; diagnostics update without typing jank.

2. **Agent Trace Drawer + Export**
   - Show per-turn model/tool/approval/checkpoint timeline.
   - Add redacted JSONL export with payload hashes.
   - Add retention controls for prompts, outputs, command logs, and metadata.
   - Acceptance: user can answer “why was this slow, expensive, or wrong?” in under 3 taps.

3. **Git Daily Workflow**
   - Add stage/unstage, hunk discard, branch create/switch, and diff review.
   - Add commit validation and push error recovery.
   - Acceptance: common single-branch workflow completed without leaving app.

4. **Terminal Hardening**
   - Add timeout policy, command allowlist, task presets, output parsing, and background-safe lifecycle.
   - Evaluate Termux/process-host bridge for PTY-like behavior.
   - Acceptance: build/test logs stream across foreground/background transitions.

5. **Prompt Context System**
   - Add pinnable context blocks, reusable task templates, relevance ranking, and truncation budget UI.
   - Add git diff and diagnostics context once available.
   - Acceptance: context assembly under 150 ms median for normal workspaces.

## Phase Roadmap

### Phase 1 — Foundation UX

- Complete autosave conflict resolution.
- Polish large-file UX and stylus selection.
- Expand keyboard shortcut layer.
- Finish explorer search optimization.
- Add accessibility pass for panes, dialogs, and high-contrast mode.

### Phase 2 — Agent Trust + Observability

- Add full `apply_patch` / unified diff tool.
- Formalize workspace path sandbox tests.
- Add command allowlist and destructive dry-run simulation.
- Complete per-turn trace drawer.
- Complete telemetry export/redaction/retention controls.
- Add eval harness with golden tasks and regression gates.
- Move trust settings from app-wide to workspace/per-tool rules.

### Phase 3 — IDE Intelligence

- LSP lifecycle, diagnostics, hover, definition, references, rename.
- Code actions and formatting hooks.
- Multi-language presets: Kotlin, TS/JS, Python, Rust.
- Ripgrep-backed search with filters and previews.
- Symbol index, fuzzy jump, breadcrumbs, lightweight reference graph.
- Inline AI completion with latency budget.
- Refactor intents with preview-before-apply.
- Test generation and test-fix loop.

### Phase 4 — DevOps Workflow

- Git stage/unstage/discard hunk.
- Branch create/switch and upstream tracking.
- Conflict viewer with merge assist.
- Persistent terminal sessions.
- Task presets for build/test/lint/deploy.
- Output parser for clickable stack traces.
- Resource governor for CPU/thermal/battery.
- PR draft and review workflow.

### Phase 5 — Performance + Platform Depth

- Rust rope-backed document buffer.
- Tree-sitter incremental parse pipeline.
- Incremental diff engine for patch previews.
- Foldable posture-aware layouts.
- Multi-window handoff and drag/drop.
- Scoped-storage edge-case matrix across API 26–35.
- Optional on-device model gateway for small tasks.

### Phase 6 — Collaboration + Enterprise

- Session export/share with prompts, tool logs, diffs, and decisions.
- Handoff packets for async teammates.
- Work notes tied to files/symbols.
- Review mode with AI risk highlights.
- Immutable audit mode.
- Secret scanner before prompt/export/commit/share.
- Backup/restore for sessions and workspace metadata.

## Milestones

- **M1 — Foundation editor:** shipped.
- **M2 — Explorer + palette:** shipped, with query-mode optimization remaining.
- **M3 — Tablet polish:** partial; finish stylus, shortcut coverage, large-file polish.
- **M4 — Tool router + trust:** partial; add path sandbox tests, `apply_patch`, allowlists, dry-run simulation.
- **M5 — Agent observability:** partial; add trace drawer, export/redaction, retention, eval harness.
- **M6 — LSP MVP:** next major IDE milestone.
- **M7 — Search + symbols:** ripgrep UI, symbol index, reference graph.
- **M8 — Git + terminal hardening:** stage/hunks/branches plus persistent terminal/task runner.
- **M9 — Collaboration:** handoff packets, review mode, PR drafting.
- **M10 — Performance engine:** Rust rope/tree-sitter path.
- **M11 — Platform/security hardening:** foldables, API matrix, secret scanning, resilience.

## Quality Gates

- **No data loss:** forced restart and conflict tests pass.
- **No workspace escape:** tool/router path sandbox tests pass.
- **No silent risky action:** destructive tools and shell commands are policy-gated.
- **No UI jank:** editor typing, explorer scroll, and agent event rendering stay responsive.
- **No fake precision:** estimated token/cost values always show source/confidence.
- **No secret leakage:** exports and outbound prompt/tool payloads pass scanner.
- **No unreversible mutation without receipt:** file/command changes have audit entries and rollback story.

## Feature Backlog

- Voice coding and dictation command mode.
- Camera-to-code OCR for whiteboards/mockups.
- Plugin/extension API with sandboxed commands.
- Snippet marketplace synced with repo templates.
- AI pair roles: architect, reviewer, debugger.
- Offline knowledge packs for docs/API refs/style guides.
- Project memory graph for decisions and architecture notes.
- Smart battery mode that downshifts AI/analysis work.
