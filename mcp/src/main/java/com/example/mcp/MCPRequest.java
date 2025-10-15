// MCPRequest.java
package com.example.mcp;

public class MCPRequest {
    private String jsonrpc;
    private String method;
    private Object params;
    private String id;

    // Getters und Setters
    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Object getParams() { return params; }
    public void setParams(Object params) { this.params = params; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
