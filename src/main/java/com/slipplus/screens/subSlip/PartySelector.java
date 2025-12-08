package com.slipplus.screens.subSlip;

import com.slipplus.core.StorageManager;
import com.slipplus.models.Party;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

class PartySelector {

    private final SlipContext ctx;

    PartySelector(SlipContext ctx) {
        this.ctx = ctx;
        setupAutoComplete();
    }

    private void setupAutoComplete() {
        // Show party selection dialog when user starts typing in the field
        ctx.partyField.textProperty().addListener((obs, oldText, newText) -> {
            // Only show dialog if user is typing and field has focus
            if (ctx.partyField.isFocused() && newText != null && !newText.isEmpty() &&
                (oldText == null || oldText.isEmpty())) {
                Platform.runLater(this::showPartySelectionDialog);
            }
        });
    }

    private void showPartySelectionDialog() {
        List<Party> parties = StorageManager.loadParties();
        
        // Create dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Party");
        dialog.setHeaderText("Choose a party or type a new one:");
        dialog.initOwner(ctx.scene.getWindow());
        
        // Create search field at top
        TextField searchField = new TextField();
        searchField.setPromptText("Search or type new party name...");
        searchField.setPrefWidth(400);
        
        // Create ListView for parties
        ListView<String> partyList = new ListView<>();
        partyList.setPrefHeight(250);
        partyList.setPrefWidth(400);
        
        List<String> partyNames = parties.stream()
                .map(Party::getName)
                .collect(Collectors.toList());
        
        ObservableList<String> items = FXCollections.observableArrayList(partyNames);
        partyList.setItems(items);
        
        // Pre-select first item
        if (!partyNames.isEmpty()) {
            partyList.getSelectionModel().selectFirst();
        }
        
        // Layout with search field at top
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Search Party:"),
            searchField,
            new Label("Existing Parties:"),
            partyList
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType selectButton = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButton, cancelButton);
        
        // Filter parties as user types
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.trim().isEmpty()) {
                partyList.setItems(FXCollections.observableArrayList(partyNames));
            } else {
                List<String> filtered = partyNames.stream()
                        .filter(name -> name.toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                partyList.setItems(FXCollections.observableArrayList(filtered));
            }
            // Auto-select first item after filtering
            if (!partyList.getItems().isEmpty()) {
                partyList.getSelectionModel().selectFirst();
            }
        });
        
        // Handle key events in search field
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String searchText = searchField.getText().trim();
                if (!searchText.isEmpty()) {
                    dialog.setResult(searchText);
                    dialog.close();
                }
            } else if (e.getCode() == KeyCode.DOWN) {
                // Move to list
                partyList.requestFocus();
                if (!partyList.getItems().isEmpty()) {
                    partyList.getSelectionModel().selectFirst();
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                // Just close popup, stay on party field
                dialog.setResult("__CANCELLED__");
                dialog.close();
            }
        });

        // Handle key events in party list
        partyList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String selected = partyList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    dialog.setResult(selected);
                    dialog.close();
                }
            } else if (e.getCode() == KeyCode.UP && partyList.getSelectionModel().getSelectedIndex() == 0) {
                // Move back to search field
                searchField.requestFocus();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                // Just close popup, stay on party field
                dialog.setResult("__CANCELLED__");
                dialog.close();
            }
        });

        // Handle mouse clicks on list items - single click to select
        partyList.setOnMouseClicked(e -> {
            String selected = partyList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                dialog.setResult(selected);
                dialog.close();
            }
        });
        
        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButton) {
                String searchText = searchField.getText().trim();
                if (!searchText.isEmpty()) {
                    return searchText;
                } else {
                    return partyList.getSelectionModel().getSelectedItem();
                }
            }
            return null;
        });
        
        // Show dialog and handle result
        Platform.runLater(() -> searchField.requestFocus());

        dialog.showAndWait().ifPresent(selectedParty -> {
            if ("__CANCELLED__".equals(selectedParty)) {
                // ESC pressed - just stay on party field, clear any partial text
                ctx.partyField.clear();
                Platform.runLater(() -> ctx.partyField.requestFocus());
            } else if (selectedParty != null && !selectedParty.trim().isEmpty()) {
                handlePartySelection(selectedParty.trim());
            }
        });

        // If dialog result is null (closed via X button), stay on party field
        if (dialog.getResult() == null) {
            ctx.partyField.clear();
            Platform.runLater(() -> ctx.partyField.requestFocus());
        }
    }

    private void handlePartySelection(String partyName) {
        ctx.partyField.setText(partyName);
        
        List<Party> parties = StorageManager.loadParties();
        Party match = parties.stream()
                .filter(p -> p.getName().equalsIgnoreCase(partyName))
                .findFirst()
                .orElse(null);
        
        if (match != null) {
            ctx.selectedParty = match;
        } else {
            // Auto-add new party
            int newId = parties.isEmpty() ? 1 : parties.get(parties.size() - 1).getId() + 1;
            Party newParty = new Party(newId, partyName);
            parties.add(newParty);
            StorageManager.saveParties(parties);
            ctx.selectedParty = newParty;
        }
        
        Platform.runLater(() -> ctx.truckField.requestFocus());
    }

    void handlePartyEnter() {
        // If party field is empty, show dialog
        if (ctx.partyField.getText().trim().isEmpty()) {
            showPartySelectionDialog();
        } else {
            // Move to next field
            ctx.truckField.requestFocus();
        }
    }
}
