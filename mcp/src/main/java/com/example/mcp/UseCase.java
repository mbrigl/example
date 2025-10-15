// UseCase.java
package com.example.mcp;

public class UseCase {
    private String id;
    private String name;
    private String description;
    private String status;

    public UseCase(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = "BEREIT";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("UseCase[id=%s, name=%s, status=%s]", id, name, status);
    }
}
