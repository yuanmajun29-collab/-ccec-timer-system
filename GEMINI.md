# Gemini CLI / Antigravity instructions

This project uses **Agent Coordinator** to share state across AI tools.
The full instructions live in `AGENTS.md`. Read that file first, then
follow the steps below.

## Mandatory startup step

```bash
node .agent-coordinator/hooks/gemini-cli.js
```

For Antigravity sessions use `antigravity.js` / `--tool antigravity`.

## When you change shared state

```bash
coordinator state set <KEY> <OLD> <NEW> \
  --tool gemini-cli \
  --reason "<why>"
```

See `AGENTS.md` for the full reference.
