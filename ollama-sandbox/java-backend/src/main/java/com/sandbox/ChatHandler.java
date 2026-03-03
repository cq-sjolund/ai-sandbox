package com.sandbox;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles POST /api/chat
 *
 * Expected request body (JSON):
 *   { "message": "Hello!", "history": [ {"role":"user","content":"..."}, ... ] }
 *
 * Streams the Ollama response back to the browser as Server-Sent Events (SSE)
 * so the frontend can render tokens incrementally.
 */
public class ChatHandler implements HttpHandler {

    private final String ollamaUrl;
    private final String modelName;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ChatHandler(String ollamaUrl, String modelName) {
        this.ollamaUrl = ollamaUrl;
        this.modelName = modelName;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // ── CORS pre-flight ───────────────────────────────────────────────
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // ── Read request body ─────────────────────────────────────────────
        String requestBody = new String(
                exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        // ── Build Ollama payload ──────────────────────────────────────────
        String ollamaPayload = buildOllamaPayload(requestBody);

        // ── Call Ollama with streaming ────────────────────────────────────
        HttpRequest ollamaRequest = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ollamaPayload))
                .build();

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream out = exchange.getResponseBody()) {
            httpClient.send(ollamaRequest,
                    HttpResponse.BodyHandlers.ofLines())
                    .body()
                    .forEach(line -> {
                        try {
                            String delta = extractDelta(line);
                            if (delta != null) {
                                // Send the delta as a JSON string value so the
                                // frontend can safely parse it with JSON.parse()
                                String payload = toJsonString(delta);
                                String sseEvent = "data: " + payload + "\n\n";
                                out.write(sseEvent.getBytes(StandardCharsets.UTF_8));
                                out.flush();
                            }
                            if (line.contains("\"done\":true")) {
                                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                                out.flush();
                            }
                        } catch (IOException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Wrap the incoming {message, history} into an Ollama /api/chat payload. */
    private String buildOllamaPayload(String body) {
        String message = extractJsonString(body, "message");
        String history = extractJsonArray(body, "history");

        String userMsg = "{\"role\":\"user\",\"content\":\"" + escapeJson(message) + "\"}";
        String messages = history.equals("[]")
                ? "[" + userMsg + "]"
                : history.substring(0, history.length() - 1) + "," + userMsg + "]";

        return "{\"model\":\"" + modelName + "\",\"messages\":" + messages + ",\"stream\":true}";
    }

    /**
     * Pull the content delta from an Ollama streaming JSON line.
     * Properly walks the JSON string respecting backslash escapes,
     * so spaces, newlines, tabs and code characters are preserved.
     */
    private String extractDelta(String line) {
        int idx = line.indexOf("\"content\":\"");
        if (idx == -1) return null;
        int i = idx + 11; // position of first char inside the string value

        StringBuilder sb = new StringBuilder();
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"') break; // unescaped closing quote — end of value
            if (c == '\\' && i + 1 < line.length()) {
                char esc = line.charAt(i + 1);
                switch (esc) {
                    case 'n':  sb.append('\n'); i += 2; continue;
                    case 't':  sb.append('\t'); i += 2; continue;
                    case 'r':  sb.append('\r'); i += 2; continue;
                    case '"':  sb.append('"');  i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case '/':  sb.append('/');  i += 2; continue;
                    case 'u':
                        if (i + 5 < line.length()) {
                            try {
                                int cp = Integer.parseInt(line.substring(i + 2, i + 6), 16);
                                sb.append((char) cp);
                            } catch (NumberFormatException ignored) {}
                            i += 6;
                            continue;
                        }
                        break;
                    default: sb.append(esc); i += 2; continue;
                }
            }
            sb.append(c);
            i++;
        }
        // Return null only if there was genuinely no content key with a value
        if (sb.length() == 0 && !line.contains("\"content\":\"\"")) return null;
        return sb.toString();
    }

    /** Encode a string as a JSON string literal (with surrounding quotes). */
    private String toJsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        int end   = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    private String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return "[]";
        int start = json.indexOf("[", idx);
        if (start == -1) return "[]";
        int depth = 0, end = start;
        for (; end < json.length(); end++) {
            if (json.charAt(end) == '[') depth++;
            else if (json.charAt(end) == ']') { if (--depth == 0) break; }
        }
        return json.substring(start, end + 1);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
