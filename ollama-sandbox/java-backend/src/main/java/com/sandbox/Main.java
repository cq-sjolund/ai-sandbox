package com.sandbox;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Minimal Java HTTP server — no external dependencies.
 * Starts on port 8080 and registers two endpoints:
 *   POST /api/chat   → streams a reply from Ollama
 *   GET  /api/health → liveness probe
 */
public class Main {

    public static void main(String[] args) throws IOException {

        String ollamaUrl  = System.getenv().getOrDefault("OLLAMA_URL",  "http://localhost:11434");
        String modelName  = System.getenv().getOrDefault("MODEL_NAME",  "llama3.2:3b");
        int    port       = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register handlers
        server.createContext("/api/chat",   new ChatHandler(ollamaUrl, modelName));
        server.createContext("/api/health", new HealthHandler());

        // Virtual-thread executor (Java 21)
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.printf("✅  Java backend listening on :%d%n", port);
        System.out.printf("    Ollama URL : %s%n", ollamaUrl);
        System.out.printf("    Model      : %s%n", modelName);
    }
}
