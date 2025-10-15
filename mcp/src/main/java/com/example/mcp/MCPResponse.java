// MCPResponse.java
package com.example.mcp;

public class MCPResponse {
    private String jsonrpc = "2.0";
    private Object result;
    private Object error;
    private String id;

    public MCPResponse(String id, Object result) {
        this.id = id;
        this.result = result;
    }

    public MCPResponse(String id, String errorMessage) {
        this.id = id;
        this.error = new ErrorObject(-32603, errorMessage);
    }

    // Getters
    public String getJsonrpc() { return jsonrpc; }
    public Object getResult() { return result; }
    public Object getError() { return error; }
    public String getId() { return id; }

    static class ErrorObject {
        private int code;
        private String message;

        public ErrorObject(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public String getMessage() { return message; }
    }
}