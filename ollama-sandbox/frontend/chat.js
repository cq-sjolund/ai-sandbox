/* ─────────────────────────────────────────────────────────────────────────────
   Ollama Sandbox — chat.js
   Handles: health polling, message rendering, SSE streaming, markdown
───────────────────────────────────────────────────────────────────────────── */

const API    = 'http://localhost:8080/api/chat';
const HEALTH = 'http://localhost:8080/api/health';

const chatEl    = document.getElementById('chat');
const inputEl   = document.getElementById('input');
const sendBtn   = document.getElementById('send');
const clearBtn  = document.getElementById('clear');
const statusDot = document.getElementById('statusDot');
const statusTxt = document.getElementById('statusText');

let history   = [];   // [{role, content}]
let streaming = false;

// ── Marked config ────────────────────────────────────────────────────────────
marked.setOptions({ breaks: true, gfm: true });

// ── Health polling ───────────────────────────────────────────────────────────
async function checkHealth() {
  try {
    const r = await fetch(HEALTH, { signal: AbortSignal.timeout(3000) });
    setStatus(r.ok ? 'online' : 'error');
  } catch {
    setStatus('error');
  }
}

function setStatus(state) {
  statusDot.className = 'status-indicator ' + state;
  statusTxt.className = 'status-text ' + state;
  const labels = { online: 'Online', error: 'Offline', checking: 'Checking…' };
  statusTxt.textContent = labels[state] ?? state;
}

checkHealth();
setInterval(checkHealth, 12_000);

// ── Auto-grow textarea ────────────────────────────────────────────────────────
inputEl.addEventListener('input', () => {
  inputEl.style.height = 'auto';
  inputEl.style.height = Math.min(inputEl.scrollHeight, 160) + 'px';
});

// ── Keyboard shortcut ─────────────────────────────────────────────────────────
inputEl.addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
});

sendBtn.addEventListener('click', send);

clearBtn.addEventListener('click', () => {
  history = [];
  chatEl.innerHTML = '<div class="sys-msg">Conversation cleared</div>';
  inputEl.focus();
});

// ── DOM helpers ───────────────────────────────────────────────────────────────
function addSysMsg(text) {
  const el = document.createElement('div');
  el.className = 'sys-msg';
  el.textContent = text;
  chatEl.appendChild(el);
  scrollBottom();
}

function addMessage(role, text = '') {
  const wrap = document.createElement('div');
  wrap.className = `msg ${role}`;

  const av = document.createElement('div');
  av.className = 'avatar';
  av.textContent = role === 'user' ? 'You' : 'AI';

  const bub = document.createElement('div');
  bub.className = 'bubble';

  if (role === 'user') {
    bub.textContent = text;
  } else {
    bub.innerHTML = text ? renderMarkdown(text) : '';
  }

  wrap.appendChild(av);
  wrap.appendChild(bub);
  chatEl.appendChild(wrap);
  scrollBottom();
  return bub;
}

function renderMarkdown(text) {
  return marked.parse(text);
}

function highlightCode(bub) {
  bub.querySelectorAll('pre code:not(.hljs)').forEach(el => hljs.highlightElement(el));
}

function appendCursor(bub) {
  const c = document.createElement('span');
  c.className = 'cursor';
  bub.appendChild(c);
  return c;
}

function scrollBottom() {
  chatEl.scrollTop = chatEl.scrollHeight;
}

// ── Send ──────────────────────────────────────────────────────────────────────
async function send() {
  const text = inputEl.value.trim();
  if (!text || streaming) return;

  inputEl.value = '';
  inputEl.style.height = 'auto';
  streaming = true;
  sendBtn.disabled = true;

  addMessage('user', text);
  history.push({ role: 'user', content: text });

  const botBub = addMessage('bot');
  const cursor = appendCursor(botBub);
  let reply = '';

  try {
    const res = await fetch(API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, history: history.slice(0, -1) }),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const reader  = res.body.getReader();
    const decoder = new TextDecoder();
    let   buf     = '';

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      buf += decoder.decode(value, { stream: true });
      const lines = buf.split('\n');
      buf = lines.pop(); // keep incomplete line

      for (const line of lines) {
        if (!line.startsWith('data: ')) continue;
        const payload = line.slice(6).trim();
        if (payload === '[DONE]') break;

        try {
          reply += JSON.parse(payload);
        } catch {
          reply += payload;
        }

        botBub.innerHTML = renderMarkdown(reply);
        botBub.appendChild(cursor);
        highlightCode(botBub);
        scrollBottom();
      }
    }

    history.push({ role: 'assistant', content: reply });

  } catch (err) {
    botBub.innerHTML = '';
    botBub.textContent = '⚠ ' + err.message;
    botBub.style.color = 'var(--danger)';
  } finally {
    cursor.remove();
    // Final clean render
    if (reply) {
      botBub.innerHTML = renderMarkdown(reply);
      highlightCode(botBub);
    }
    streaming = false;
    sendBtn.disabled = false;
    inputEl.focus();
    scrollBottom();
  }
}
