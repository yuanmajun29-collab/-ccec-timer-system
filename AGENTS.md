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

## Cursor Cloud specific instructions

### Product & services (dev stack)

Primary dev path is **Docker Compose** (`docker-compose.yml`). See `README.md` and `make help`.

| Service | Role | Required for browser E2E |
| ------- | ---- | ------------------------ |
| redis, oracle, timer-backend, plc-collector, edgebox-gate | Core pipeline + UI | Yes |
| mosquitto | MQTT for Android kiosk | No (browser uses WebSocket) |

First-time setup: `make setup` (wraps `deploy/scripts/setup-local.sh`). Daily: `make up`.

### Docker in Cloud Agent VMs

Docker is **not** pre-installed. One-time VM bootstrap (already done in this environment):

1. Install Docker CE + `docker-compose-plugin`, `fuse-overlayfs`, set `storage-driver: fuse-overlayfs` in `/etc/docker/daemon.json`, use `iptables-legacy`.
2. Start the daemon: `sudo dockerd > /tmp/dockerd.log 2>&1 &` (systemd may not auto-start in nested VMs).
3. Allow the agent user to talk to the socket: `sudo chmod 666 /var/run/docker.sock` (or `sudo usermod -aG docker $USER` and re-login).

### Startup caveats

- **Oracle** (`gvenzl/oracle-free:23-slim`) can take **5–15 minutes** on first boot; backend health has a 120s `start_period`.
- **`make setup` / `docker compose up --build` may fail** on current `main` due to two build issues (work around before upstream fixes):
  - **timer-backend**: `spring-boot-maven-plugin` does not bind `repackage` on `package`, so the image JAR lacks `Main-Class`. Rebuild locally:  
    `cd java/timer-backend && mvn -B -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.5:repackage`  
    then build/run a fixed image (see session notes) or run `mvn spring-boot:run` against infra on `localhost`.
  - **plc-collector**: runtime image missing `libhiredis1.1.0`. Add `RUN apt-get install -y libhiredis1.1.0` to the final stage or use a locally rebuilt image.
- After infra is healthy, verify: `curl -sf http://127.0.0.1:8080/actuator/health`, `./deploy/scripts/wait-for-backend.sh`.
- Default admin (empty DB, docker profile): `admin` / `Admin123!` — see `README.md`.

### Lint / test / build (without full stack)

| Component | Command | Notes |
| --------- | ------- | ----- |
| Compose validate | `docker compose -f docker-compose.yml config --quiet` | CI parity |
| Java backend | `cd java/timer-backend && mvn -B test package` | JDK 17+; repackage step needed for runnable JAR |
| C++ collector image | `docker build -t ccec-plc-collector:test ./cpp/plc-collector` | Needs Docker |
| Android kiosk | `cd android/station-kiosk && ./gradlew :app:assembleDebug` | Optional; needs Android SDK |

Frontends are static files served by **edgebox-gate** (no separate npm dev server).

### Agent Coordinator hook

`node .agent-coordinator/hooks/cursor.js` requires the `coordinator` CLI (`npx coordinator`). If unavailable, skip — it does not block running the stack.
