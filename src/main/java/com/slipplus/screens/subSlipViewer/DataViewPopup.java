package com.slipplus.screens.subSlipViewer;

import com.slipplus.constants.Colors;
import com.slipplus.core.StorageManager;
import com.slipplus.models.SubSlip;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DataViewPopup implements BasePopup {
    
    private Stage popup;
    private TableView<SubSlipRow> table;
    private ObservableList<SubSlipRow> tableData;
    private LocalDate selectedDate;
    private String selectedParty;
    private BiConsumer<LocalDate, String> onDataAction;
    private Consumer<LocalDate> onBackToParty;
    
    public DataViewPopup(Stage parentStage, LocalDate selectedDate, String selectedParty,
                        BiConsumer<LocalDate, String> onDataAction, Consumer<LocalDate> onBackToParty) {
        this.selectedDate = selectedDate;
        this.selectedParty = selectedParty;
        this.onDataAction = onDataAction;
        this.onBackToParty = onBackToParty;
        createPopup(parentStage);
    }
    
    private void createPopup(Stage parentStage) {
        // Load sub-slip data
        List<SubSlip> subSlips = StorageManager.getSubSlipsForDateAndParty(selectedDate, selectedParty);
        
        // Check if no data
        if (subSlips.isEmpty()) {
            showNoDataMessage(parentStage);
            return;
        }
        
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        
        popup = new Stage();
        popup.initOwner(parentStage);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setTitle("Sub-Slip Data - Delete View");
        popup.setResizable(false);
        
        // Convert to table rows
        tableData = FXCollections.observableArrayList();
        for (SubSlip slip : subSlips) {
            tableData.add(new SubSlipRow(slip));
        }
        
        // Create table
        table = new TableView<>(tableData);
        table.setPrefHeight(400);
        table.setPrefWidth(700);
        
        // Select column
        TableColumn<SubSlipRow, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(80);
        selectCol.setEditable(true);
        
        // Truck No column
        TableColumn<SubSlipRow, String> truckCol = new TableColumn<>("Truck No");
        truckCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTruckNo()));
        truckCol.setPrefWidth(150);
        
        // Main Weight column
        TableColumn<SubSlipRow, String> weightCol = new TableColumn<>("Main Weight");
        weightCol.setCellValueFactory(cellData -> new SimpleStringProperty(
            String.valueOf(cellData.getValue().getMainWeight())));
        weightCol.setPrefWidth(120);
        
        // Total Price column
        TableColumn<SubSlipRow, String> priceCol = new TableColumn<>("Total Price (incl. GST)");
        priceCol.setCellValueFactory(cellData -> new SimpleStringProperty(
            "₹" + String.format("%.2f", cellData.getValue().getTotalPrice())));
        priceCol.setPrefWidth(200);
        
        table.getColumns().addAll(selectCol, truckCol, weightCol, priceCol);
        table.setEditable(true);
        
        // Layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(Colors.POPUP_BG) + ";");
        root.setCenter(table);
        
        // Header - show party name instead of party key
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Label header = new Label("Date: " + selectedDate.format(formatter) + " | Party: " + selectedParty);
        header.setStyle("-fx-font-size: 16px; -fx-text-fill: black; -fx-font-weight: bold;");
        header.setAlignment(Pos.CENTER);
        root.setTop(header);
        
        // Footer with keyboard shortcuts
        Label footer = new Label("ENTER = Select/Unselect   DELETE = Delete Selected   F2 = Delete All   ESC = Back   F4 = Main Screen");
        footer.setStyle("-fx-font-size: 12px; -fx-text-fill: black;");
        footer.setAlignment(Pos.CENTER);
        root.setBottom(footer);
        
        Scene scene = new Scene(root, screenWidth * 0.6, screenHeight * 0.6);
        
        // Add event filter to handle keys before table processes them
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    toggleSelectedRow();
                    e.consume();
                }
                case DELETE -> {
                    deleteSelectedWithConfirmation();
                    e.consume();
                }
                case F2 -> {
                    deleteAllWithConfirmation();
                    e.consume();
                }
                case ESCAPE -> {
                    close();
                    onBackToParty.accept(selectedDate);
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
        
        Platform.runLater(() -> table.requestFocus());
    }
    
    private void toggleSelectedRow() {
        SubSlipRow selectedRow = table.getSelectionModel().getSelectedItem();
        if (selectedRow != null) {
            selectedRow.setSelected(!selectedRow.isSelected());
            table.refresh();
        }
    }

    private void deleteSelectedWithConfirmation() {
        List<SubSlipRow> selectedRows = tableData.stream()
                .filter(SubSlipRow::isSelected)
                .toList();
        
        if (selectedRows.isEmpty()) {
            showAlert("No Selection", "Please select rows to delete using ENTER key.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete Selected");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete " + selectedRows.size() + " selected record(s)?");
        confirm.initOwner(popup);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Extract SubSlip objects from selected rows
                List<SubSlip> slipsToDelete = selectedRows.stream()
                        .map(SubSlipRow::getSubSlip)
                        .toList();
                
                // Convert party name to ID for storage operations
                String partyId = StorageManager.getPartyIdByName(selectedParty);
                
                // Delete from storage
                StorageManager.deleteSubSlips(selectedDate, partyId, slipsToDelete);
                
                // Remove selected rows from table
                tableData.removeAll(selectedRows);
                
                // Check if table is now empty
                if (tableData.isEmpty()) {
                    showAlert("All Data Deleted", "All sub-slips for this party have been deleted.");
                    close();
                    // Go back to party selection to refresh the list
                    onBackToParty.accept(selectedDate);
                }
            }
        });
    }

    private void deleteAllWithConfirmation() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete All");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete ALL sub-slips for this party on this date?");
        confirm.initOwner(popup);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Convert party name to ID for storage operations
                String partyId = StorageManager.getPartyIdByName(selectedParty);
                
                // Delete all from storage
                StorageManager.deleteAllSubSlipsForParty(selectedDate, partyId);
                
                tableData.clear();
                showAlert("All Data Deleted", "All sub-slips for this party have been deleted.");
                close();
                // Go back to party selection to refresh the list
                onBackToParty.accept(selectedDate);
            }
        });
    }
    
    private void showNoDataMessage(Stage parentStage) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Data");
        alert.setHeaderText(null);
        alert.setContentText("No sub-slip data found for " + selectedParty + " on " + selectedDate.format(formatter));
        alert.initOwner(parentStage);
        alert.showAndWait();
        onBackToParty.accept(selectedDate);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(popup);
        alert.showAndWait();
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
    
    // Inner class for table rows
    public static class SubSlipRow {
        private final SubSlip subSlip;
        private final SimpleBooleanProperty selected;
        
        public SubSlipRow(SubSlip subSlip) {
            this.subSlip = subSlip;
            this.selected = new SimpleBooleanProperty(false);
        }
        
        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }
        
        public boolean isSelected() {
            return selected.get();
        }
        
        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }
        
        public String getTruckNo() {
            return subSlip.getTruckNumber();
        }
        
        public double getMainWeight() {
            return subSlip.getMainWeight();
        }
        
        public double getTotalPrice() {
            return subSlip.getFinalAmount();
        }
        
        public SubSlip getSubSlip() {
            return subSlip;
        }
    }
}







