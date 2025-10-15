// MCPServerHTTP.java
package com.example.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class MCPServerHTTP {
    private final int port;
    private final Map<String, UseCase> useCases;
    private final Gson gson;
    private HttpServer server;

    public MCPServerHTTP(int port) {
        this.port = port;
        this.useCases = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initializeUseCases();
    }

    private void initializeUseCases() {
        useCases.put("uc1", new UseCase("uc1", "Benutzer registrieren",
            "Registriert einen neuen Benutzer im System"));
        useCases.put("uc2", new UseCase("uc2", "Bestellung aufgeben",
            "Erstellt eine neue Bestellung"));
        useCases.put("uc3", new UseCase("uc3", "Rechnung erstellen",
            "Generiert eine Rechnung für eine Bestellung"));
        useCases.put("uc4", new UseCase("uc4", "Daten exportieren",
            "Exportiert Daten in verschiedene Formate"));
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/mcp", new MCPHandler());
        server.createContext("/health", new HealthHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("✓ MCP HTTP Server läuft auf Port " + port);
        System.out.println("  Endpoint: http://localhost:" + port + "/mcp");
        System.out.println("  Health Check: http://localhost:" + port + "/health");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Server beendet.");
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"ok\",\"useCases\":" + useCases.size() + "}";
            sendResponse(exchange, 200, response);
        }
    }

    private class MCPHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS Headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405,
                    gson.toJson(Map.of("error", "Nur POST Methode erlaubt")));
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);
                System.out.println("← Request: " + requestBody);

                MCPRequest request = gson.fromJson(requestBody, MCPRequest.class);
                MCPResponse response = processRequest(request);

                String responseJson = gson.toJson(response);
                System.out.println("→ Response: " + responseJson);

                sendResponse(exchange, 200, responseJson);
            } catch (Exception e) {
                System.err.println("Fehler: " + e.getMessage());
                e.printStackTrace();
                MCPResponse errorResponse = new MCPResponse("error",
                    "Interner Server-Fehler: " + e.getMessage());
                sendResponse(exchange, 500, gson.toJson(errorResponse));
            }
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response)
        throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private MCPResponse processRequest(MCPRequest request) {
        String method = request.getMethod();
        String requestId = request.getId();

        return switch (method) {
            case "tools/list" -> handleListTools(requestId);
            case "tools/call" -> handleToolCall(requestId, request.getParams());
            case "initialize" -> handleInitialize(requestId);
            default -> new MCPResponse(requestId, "Unbekannte Methode: " + method);
        };
    }

    private MCPResponse handleInitialize(String requestId) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", Map.of(
            "name", "UseCase MCP Server HTTP",
            "version", "1.0.0"
        ));
        result.put("capabilities", Map.of(
            "tools", Map.of()
        ));
        return new MCPResponse(requestId, result);
    }

    private MCPResponse handleListTools(String requestId) {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(Map.of(
            "name", "list_use_cases",
            "description", "Listet alle verfügbaren Anwendungsfälle auf",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
            )
        ));

        tools.add(Map.of(
            "name", "start_use_case",
            "description", "Startet einen bestimmten Anwendungsfall",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "useCaseId", Map.of(
                        "type", "string",
                        "description", "Die ID des zu startenden Anwendungsfalls"
                    )
                ),
                "required", List.of("useCaseId")
            )
        ));

        return new MCPResponse(requestId, Map.of("tools", tools));
    }

    private MCPResponse handleToolCall(String requestId, Object params) {
        JsonObject paramsObj = gson.toJsonTree(params).getAsJsonObject();
        String toolName = paramsObj.get("name").getAsString();
        JsonObject arguments = paramsObj.has("arguments") ?
            paramsObj.getAsJsonObject("arguments") : new JsonObject();

        return switch (toolName) {
            case "list_use_cases" -> listUseCases(requestId);
            case "start_use_case" -> startUseCase(requestId, arguments);
            default -> new MCPResponse(requestId, "Unbekanntes Tool: " + toolName);
        };
    }

    private MCPResponse listUseCases(String requestId) {
        List<Map<String, String>> useCaseList = new ArrayList<>();

        for (UseCase uc : useCases.values()) {
            useCaseList.add(Map.of(
                "id", uc.getId(),
                "name", uc.getName(),
                "description", uc.getDescription(),
                "status", uc.getStatus()
            ));
        }

        String content = String.format("Verfügbare Anwendungsfälle (%d):\n\n",
            useCaseList.size());
        for (Map<String, String> uc : useCaseList) {
            content += String.format("- [%s] %s\n  Status: %s\n  %s\n\n",
                uc.get("id"), uc.get("name"), uc.get("status"), uc.get("description"));
        }

        return new MCPResponse(requestId, Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", content
            ))
        ));
    }

    private MCPResponse startUseCase(String requestId, JsonObject arguments) {
        String useCaseId = arguments.get("useCaseId").getAsString();
        UseCase useCase = useCases.get(useCaseId);

        if (useCase == null) {
            return new MCPResponse(requestId,
                "Use Case mit ID '" + useCaseId + "' nicht gefunden");
        }

        useCase.setStatus("LÄUFT");
        String result = String.format("✓ Use Case '%s' wurde gestartet!\n\n" +
                "Details:\n" +
                "- ID: %s\n" +
                "- Name: %s\n" +
                "- Beschreibung: %s\n" +
                "- Status: %s\n\n" +
                "Der Anwendungsfall wird nun ausgeführt...",
            useCase.getName(), useCase.getId(), useCase.getName(),
            useCase.getDescription(), useCase.getStatus());

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                useCase.setStatus("ABGESCHLOSSEN");
                System.out.println("✓ Use Case " + useCaseId + " abgeschlossen.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return new MCPResponse(requestId, Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result
            ))
        ));
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 3000;
        MCPServerHTTP server = new MCPServerHTTP(port);

        try {
            server.start();

            // Graceful Shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

            // Server läuft weiter
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Server-Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}