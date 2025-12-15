package com.slipplus.screens.subSlipViewer;

import com.slipplus.constants.Colors;
import com.slipplus.core.StorageManager;
import com.slipplus.models.Party;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class PartySelectionPopup implements BasePopup {
    
    private Stage popup;
    private ListView<PartyItem> partyList;
    private List<PartyItem> availableParties;
    private LocalDate selectedDate;
    private BiConsumer<LocalDate, String> onPartySelected;
    private Runnable onBackToDate;
    
    // Inner class to hold party ID and name together
    private static class PartyItem {
        private final String id;
        private final String name;
        
        public PartyItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        
        @Override
        public String toString() {
            return name; // ListView will display the name
        }
    }
    
    public PartySelectionPopup(Stage parentStage, LocalDate selectedDate, 
                              BiConsumer<LocalDate, String> onPartySelected, 
                              Runnable onBackToDate) {
        this.selectedDate = selectedDate;
        this.onPartySelected = onPartySelected;
        this.onBackToDate = onBackToDate;
        createPopup(parentStage);
    }
    
    private void createPopup(Stage parentStage) {
        // Load available party IDs for selected date
        List<String> partyIds = StorageManager.getPartyIdsForDate(selectedDate);
        
        // Convert to PartyItem objects with names
        availableParties = new ArrayList<>();
        List<Party> allParties = StorageManager.loadParties();
        
        for (String partyId : partyIds) {
            String partyName = allParties.stream()
                    .filter(p -> String.valueOf(p.getId()).equals(partyId))
                    .map(Party::getName)
                    .findFirst()
                    .orElse("Unknown Party");
            availableParties.add(new PartyItem(partyId, partyName));
        }
        
        // Check if no parties (all were deleted)
        if (availableParties.isEmpty()) {
            showNoDataMessage(parentStage);
            return;
        }
        
        // Check if only one party - auto select
        if (availableParties.size() == 1) {
            Platform.runLater(() -> {
                onPartySelected.accept(selectedDate, availableParties.get(0).getName());
            });
            return;
        }
        
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        
        popup = new Stage();
        popup.initOwner(parentStage);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setTitle("Select Party");
        popup.setResizable(false);
        
        // Create party list
        partyList = new ListView<>();
        partyList.setPrefHeight(300);
        partyList.setPrefWidth(400);
        
        partyList.setItems(FXCollections.observableArrayList(availableParties));
        partyList.getSelectionModel().selectFirst();
        
        // Layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(Colors.POPUP_BG) + ";");
        root.setCenter(partyList);
        
        // Header with selected date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Label header = new Label("Select Party for Date: " + selectedDate.format(formatter));
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-font-weight: bold;");
        header.setAlignment(Pos.CENTER);
        root.setTop(header);
        
        Label footer = new Label("ENTER = Select   ESC = Back to Date   F4 = Main Screen");
        footer.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
        footer.setAlignment(Pos.CENTER);
        root.setBottom(footer);
        
        Scene scene = new Scene(root, screenWidth * 0.35, screenHeight * 0.45);
        
        // Use event filter to intercept keys before ListView processes them
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    selectParty();
                    e.consume();
                }
                case ESCAPE -> {
                    close();
                    onBackToDate.run();
                    e.consume();
                }
                case F4 -> {
                    close();
                    com.slipplus.core.AppNavigator.startApp((Stage) popup.getOwner());
                    e.consume();
                }
            }
        });
        
        popup.setScene(scene);
        popup.centerOnScreen();
        
        Platform.runLater(() -> partyList.requestFocus());
    }
    
    private void selectParty() {
        PartyItem selectedItem = partyList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            close();
            onPartySelected.accept(selectedDate, selectedItem.getName());
        }
    }
    
    private void showNoDataMessage(Stage parentStage) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Data");
        alert.setHeaderText(null);
        alert.setContentText("No parties found for date: " + selectedDate.format(formatter));
        alert.initOwner(parentStage);
        alert.showAndWait();
        // Go back to date selection since no parties are left
        onBackToDate.run();
    }
    
    @Override
    public void show() {
        if (popup != null) {
            popup.show();
        }
    }
    
    @Override
    public void close() {
        if (popup != null) {
            popup.close();
        }
    }
    
    private String toHex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}


