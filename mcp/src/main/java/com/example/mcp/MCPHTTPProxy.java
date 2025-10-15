// MCPHTTPProxy.java
package com.example.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MCPHTTPProxy {
    private final String serverUrl;
    private final Gson gson;
    private final BufferedReader stdin;
    private final PrintWriter stdout;

    public MCPHTTPProxy(String serverUrl) {
        this.serverUrl = serverUrl;
        this.gson = new GsonBuilder().create();
        this.stdin = new BufferedReader(new InputStreamReader(System.in));
        this.stdout = new PrintWriter(System.out, true);
    }

    public void start() {
        System.err.println("MCP HTTP Proxy gestartet...");
        System.err.println("Verbinde zu: " + serverUrl);

        try {
            String line;
            while ((line = stdin.readLine()) != null) {
                System.err.println("← STDIN: " + line);
                String response = forwardToServer(line);
                System.err.println("→ STDOUT: " + response);
                stdout.println(response);
            }
        } catch (IOException e) {
            System.err.println("Fehler: " + e.getMessage());
        }
    }

    private String forwardToServer(String request) throws IOException {
        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Request senden
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = request.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Response lesen
            int responseCode = conn.getResponseCode();
            InputStream is = responseCode >= 400 ?
                conn.getErrorStream() : conn.getInputStream();

            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } finally {
            conn.disconnect();
        }
    }

    public static void main(String[] args) {
        String serverUrl = args.length > 0 ?
            args[0] : "http://localhost:3000/mcp";

        MCPHTTPProxy proxy = new MCPHTTPProxy(serverUrl);
        proxy.start();
    }
}