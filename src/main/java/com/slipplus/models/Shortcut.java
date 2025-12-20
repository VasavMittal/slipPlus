package com.slipplus.models;

public class Shortcut {
    private String alphabet;
    private String description;
    private String operation; // "+" or "-"
    private boolean showInPurchaseBook;

    public Shortcut() {}

    public Shortcut(String alphabet, String description, String operation) {
        this.alphabet = alphabet;
        this.description = description;
        this.operation = operation;
        this.showInPurchaseBook = false; // Default to false
    }

    public Shortcut(String alphabet, String description, String operation, boolean showInPurchaseBook) {
        this.alphabet = alphabet;
        this.description = description;
        this.operation = operation;
        this.showInPurchaseBook = showInPurchaseBook;
    }

    // Getters and setters
    public String getAlphabet() { return alphabet; }
    public void setAlphabet(String alphabet) { this.alphabet = alphabet; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public boolean isShowInPurchaseBook() { return showInPurchaseBook; }
    public void setShowInPurchaseBook(boolean showInPurchaseBook) { this.showInPurchaseBook = showInPurchaseBook; }

    @Override
    public String toString() {
        return alphabet + " - " + description + " (" + operation + ")";
    }
}

