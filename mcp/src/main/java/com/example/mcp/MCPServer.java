// MCPServer.java
package com.example.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MCPServer {
    private final Map<String, UseCase> useCases;
    private final Gson gson;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public MCPServer() {
        this.useCases = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);

        // Beispiel Use Cases hinzufügen
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

    public void start() {
        System.err.println("MCP Server gestartet...");

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                handleRequest(line);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Eingabe: " + e.getMessage());
        }
    }

    private void handleRequest(String requestJson) {
        try {
            MCPRequest request = gson.fromJson(requestJson, MCPRequest.class);
            MCPResponse response = processRequest(request);
            writer.println(gson.toJson(response));
        } catch (Exception e) {
            System.err.println("Fehler bei der Verarbeitung: " + e.getMessage());
            MCPResponse errorResponse = new MCPResponse("error",
                "Interner Server-Fehler: " + e.getMessage());
            writer.println(gson.toJson(errorResponse));
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
            "name", "UseCase MCP Server",
            "version", "1.0.0"
        ));
        result.put("capabilities", Map.of(
            "tools", Map.of()
        ));
        return new MCPResponse(requestId, result);
    }

    private MCPResponse handleListTools(String requestId) {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool 1: Liste aller Use Cases
        tools.add(Map.of(
            "name", "list_use_cases",
            "description", "Listet alle verfügbaren Anwendungsfälle auf",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
            )
        ));

        // Tool 2: Use Case starten
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

        String content = String.format("Verfügbare Anwendungsfälle (%d):\n\n", useCaseList.size());
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

        // Use Case "ausführen" (Simulation)
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

        // Nach einer simulierten Ausführung Status ändern
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                useCase.setStatus("ABGESCHLOSSEN");
                System.err.println("Use Case " + useCaseId + " abgeschlossen.");
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
        MCPServer server = new MCPServer();
        server.start();
    }
}