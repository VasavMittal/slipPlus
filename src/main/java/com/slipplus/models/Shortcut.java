package com.slipplus.models;

public class Shortcut {
    private String alphabet;
    private String description;
    private String operation; // "+" or "-"

    public Shortcut() {}

    public Shortcut(String alphabet, String description, String operation) {
        this.alphabet = alphabet;
        this.description = description;
        this.operation = operation;
    }

    // Getters and setters
    public String getAlphabet() { return alphabet; }
    public void setAlphabet(String alphabet) { this.alphabet = alphabet; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    @Override
    public String toString() {
        return alphabet + " - " + description + " (" + operation + ")";
    }
}
