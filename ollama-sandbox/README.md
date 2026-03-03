# 🦙 Ollama Sandbox — Local LLM on Docker (Apple Silicon)

A fully offline AI chat stack running in Docker. All inference happens locally — no data leaves your machine after the initial image and model download.

```
Browser (localhost:3000)
  └── nginx (static frontend — index.html + styles.css + chat.js)
        └── Java HTTP server (localhost:8080)
              └── Ollama + llama3.2:3b (localhost:11434)
```

---

## Prerequisites

| Tool | Install |
|------|---------|
| Docker Desktop (Apple Silicon) | https://www.docker.com/products/docker-desktop/ |

> **GPU note:** Docker on macOS cannot expose Apple Metal to containers.
> The LLM runs on CPU — llama3.2:3b is comfortable for conversation at ~10–20 tok/s.

---

## Quick Start

```bash
# 1. Unzip / enter the project
cd ollama-sandbox

# 2. Build and start all services
docker compose up --build

# First run pulls the llama3.2:3b model (~2 GB).
# Watch the ollama-init container logs for download progress.

# 3. Open the chat UI
open http://localhost:3000
```

---

## Services

| Container | Port | Purpose |
|-----------|------|---------|
| `ollama` | 11434 | LLM inference engine |
| `ollama-init` | — | One-shot model downloader (exits after pull) |
| `java-backend` | 8080 | Plain Java 21 HTTP proxy + CORS bridge |
| `frontend` | 3000 | Static UI served by nginx |

---

## Frontend

The UI is split into three files under `frontend/`:

| File | Purpose |
|------|---------|
| `index.html` | Markup and script tags only |
| `styles.css` | Adobe Granite light theme, full responsive layout |
| `chat.js` | SSE streaming, markdown rendering, health polling |

**Design highlights:**
- Adobe Granite light theme — white/grey surfaces, `#1473e6` Adobe blue accent
- Adobe Fonts via Typekit: Source Sans 3 (UI) + Source Code Pro (code blocks)
- Fully responsive and full-width — fluid `clamp()` padding, adaptive bubble widths
- Responsive breakpoints at 768px (tablet) and 480px (mobile)
- Syntax-highlighted code blocks via highlight.js (Atom One Light theme)
- Markdown rendering via marked.js — headings, lists, tables, blockquotes all supported
- Streaming tokens rendered incrementally with a blinking cursor

To update the Typekit font kit, replace the kit ID in the `<link>` tag in `index.html`:
```html
<link rel="stylesheet" href="https://use.typekit.net/YOUR_KIT_ID.css"/>
```
Create a free kit at https://fonts.adobe.com with Source Sans 3 and Source Code Pro.

---

## Changing the Model

Edit `docker-compose.yml` and update both references:

```yaml
# In ollama-init — the pull command:
-d '{"name":"mistral"}'

# In java-backend — the inference target:
- MODEL_NAME=mistral
```

Also update the model badge in `frontend/index.html`:
```html
<div class="header-model">mistral</div>
```

Popular small models that run well on CPU:

| Model | Size | Notes |
|-------|------|-------|
| `llama3.2:3b` | ~2 GB | Default — fast, well-rounded |
| `mistral` | ~4 GB | Excellent reasoning |
| `phi3:mini` | ~2 GB | Very fast |
| `gemma2:2b` | ~1.5 GB | Smallest option |

---

## Useful Commands

```bash
# View logs
docker compose logs -f java-backend
docker compose logs -f ollama

# Rebuild only the Java backend (after code changes)
docker compose up --build java-backend

# Restart only the frontend (after HTML/CSS/JS changes — no rebuild needed)
docker compose restart frontend

# Stop everything
docker compose down

# Stop and delete downloaded model data
docker compose down -v
```

---

## Privacy & Data Isolation

All chat data stays on your machine. The request path is:

```
Browser → nginx (local) → Java backend (local) → Ollama (local)
```

No chat content is sent to any external server. The only outbound network calls are:

| When | What | Contains chat data? |
|------|------|-------------------|
| First `docker compose up` | Docker pulls `ollama/ollama` image | No |
| First run of `ollama-init` | Model weights downloaded from Ollama servers | No |
| Every page load | Adobe Fonts (Typekit) + highlight.js + marked.js loaded from CDN | No |

If you need a fully air-gapped setup after first build, the CDN assets (fonts and JS libraries) can be bundled into the Docker image on request.

---

## Project Structure

```
ollama-sandbox/
├── docker-compose.yml
├── README.md
├── frontend/
│   ├── index.html          # Markup shell — links CSS/JS, no inline styles or scripts
│   ├── styles.css          # Adobe Granite light theme + responsive layout
│   └── chat.js             # Chat logic — streaming, markdown, health check
└── java-backend/
    ├── Dockerfile           # Eclipse Temurin 21, two-stage build
    └── src/main/java/com/sandbox/
        ├── Main.java        # HTTP server entry point (port 8080, virtual threads)
        ├── ChatHandler.java # POST /api/chat → SSE stream from Ollama
        └── HealthHandler.java  # GET /api/health → liveness probe
```

---

## Troubleshooting

**`ollama is unhealthy` on startup**
The healthcheck uses a bash TCP probe (`/dev/tcp/localhost/11434`) which requires no external tools. If it still fails, increase `start_period` in `docker-compose.yml`:
```yaml
healthcheck:
  start_period: 60s
```

**Garbled / missing spaces in responses**
Ensure you are using the latest `ChatHandler.java` — earlier versions used a naive JSON string parser that dropped spaces and mishandled escape sequences. The current version walks the string character-by-character and handles all `\n`, `\t`, `\\`, `\"`, and `\uXXXX` escapes correctly.

**Fonts not loading**
The Typekit kit ID (`aot8dfm`) in `index.html` is a shared public kit. If it stops working, create your own free kit at https://fonts.adobe.com and replace the ID. The UI falls back to `system-ui` / `monospace` if fonts fail to load.
