# Agent instructions for this workspace

This project uses **Agent Coordinator** (`.agent-coordinator/`) to share
state, decisions, and context across Cursor / Claude Code / Codex /
Gemini CLI / Antigravity / Hermes.

## 1. Before you start — run the matching hook once

| Tool          | Command                                                       |
| ------------- | ------------------------------------------------------------- |
| Cursor        | `node .agent-coordinator/hooks/cursor.js`                     |
| Claude Code   | `node .agent-coordinator/hooks/claude-code.js`                |
| Codex         | `node .agent-coordinator/hooks/codex.js`                      |
| Gemini CLI    | `node .agent-coordinator/hooks/gemini-cli.js`                 |
| Antigravity   | `node .agent-coordinator/hooks/antigravity.js`                |
| Hermes        | `node .agent-coordinator/hooks/hermes.js` *(also auto-runs via shell hook)* |

Equivalent: `npx coordinator context inject <tool>`.

## 2. When you change shared state

```bash
coordinator state set <KEY> <OLD> <NEW> \
  --tool <your-tool-name> \
  --reason "<one-line explanation>"
```

Canonical tool names: `cursor`, `claude-code`, `codex`, `gemini-cli`,
`antigravity`, `hermes`.

## 3. Quick reference

| Action                       | Command                          |
| ---------------------------- | -------------------------------- |
| Inspect current shared state | `coordinator status`             |
| Get one key                  | `coordinator state get <KEY>`    |
| List recent decisions        | `coordinator decisions`          |
| Check conflicts              | `coordinator conflicts`          |
| Health-check the whole setup | `acp-doctor` (if installed)      |
