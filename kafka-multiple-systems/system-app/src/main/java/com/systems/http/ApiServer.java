package com.systems.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.systems.config.Config;
import com.systems.kafka.MessageProducer;
import com.systems.model.Message;
import com.systems.store.MessageStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class ApiServer {
    private final MessageStore store;
    private final MessageProducer producer;
    private final Gson gson = new Gson();
    private HttpServer server;

    public ApiServer(MessageStore store, MessageProducer producer) {
        this.store = store;
        this.producer = producer;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(Config.HTTP_PORT), 0);
        server.createContext("/config", this::handleConfig);
        server.createContext("/messages", this::handleMessages);
        server.createContext("/send", this::handleSend);
        server.createContext("/", this::handleRoot);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("System " + Config.SYSTEM_NAME + " HTTP server on port " + Config.HTTP_PORT);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        try (InputStream is = getClass().getResourceAsStream("/static/index.html")) {
            if (is == null) {
                sendText(exchange, 404, "index.html not found");
                return;
            }
            byte[] bytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        JsonObject config = new JsonObject();
        config.addProperty("systemName", Config.SYSTEM_NAME);
        config.add("allSystems", gson.toJsonTree(Config.ALL_SYSTEMS));
        sendJson(exchange, 200, config.toString());
    }

    private void handleMessages(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        List<Message> messages = store.getAll();
        sendJson(exchange, 200, gson.toJson(messages));
    }

    private void handleSend(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String to = json.get("to").getAsString().trim();
            String message = json.get("message").getAsString().trim();

            if (to.isEmpty() || message.isEmpty()) {
                sendText(exchange, 400, "Missing 'to' or 'message'");
                return;
            }

            producer.send(to, message);
            sendJson(exchange, 200, "{\"status\":\"sent\"}");
        } catch (Exception e) {
            sendText(exchange, 400, "Bad request: " + e.getMessage());
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}
