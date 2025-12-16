package com.slipplus.models;

import java.time.LocalDate;
import java.util.List;

public class MainSlip {
    private LocalDate date;
    private String partyName;
    private double totalBeforeOperations;
    private List<Operation> operations;
    private double totalAfterOperations;

    public MainSlip() {}

    public MainSlip(LocalDate date, String partyName, double totalBeforeOperations, 
                   List<Operation> operations, double totalAfterOperations) {
        this.date = date;
        this.partyName = partyName;
        this.totalBeforeOperations = totalBeforeOperations;
        this.operations = operations;
        this.totalAfterOperations = totalAfterOperations;
    }

    // Getters and setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public double getTotalBeforeOperations() { return totalBeforeOperations; }
    public void setTotalBeforeOperations(double totalBeforeOperations) { this.totalBeforeOperations = totalBeforeOperations; }

    public List<Operation> getOperations() { return operations; }
    public void setOperations(List<Operation> operations) { this.operations = operations; }

    public double getTotalAfterOperations() { return totalAfterOperations; }
    public void setTotalAfterOperations(double totalAfterOperations) { this.totalAfterOperations = totalAfterOperations; }

    public static class Operation {
        private double amount;
        private String shortcutId;
        private String description;
        private String operationType; // "+" or "-"

        public Operation() {}

        public Operation(double amount, String shortcutId, String description, String operationType) {
            this.amount = amount;
            this.shortcutId = shortcutId;
            this.description = description;
            this.operationType = operationType;
        }

        // Getters and setters
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getShortcutId() { return shortcutId; }
        public void setShortcutId(String shortcutId) { this.shortcutId = shortcutId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
    }
}