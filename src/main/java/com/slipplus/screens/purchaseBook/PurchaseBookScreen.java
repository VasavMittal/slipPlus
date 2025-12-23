package com.slipplus.screens.purchaseBook;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.models.SubSlip;
import com.slipplus.screens.subSlipViewer.DateSelectionPopup;
import com.slipplus.models.MainSlip;
import com.slipplus.models.Shortcut;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class PurchaseBookScreen {
    
    private Stage stage;
    private LocalDate selectedDate;
    private boolean popupOpen = false;
    
    public void start(Stage stage) {
        this.stage = stage;
        
        // Get available dates from sub-slips
        List<LocalDate> availableDatesList = StorageManager.getAvailableDates();
        
        if (availableDatesList.isEmpty()) {
            showNoDataMessage();
            return;
        }
        
        if (availableDatesList.size() == 1) {
            // Auto-select single date
            selectedDate = availableDatesList.get(0);
            showPurchaseBookTable();
        } else {
            // Show date selection popup
            showDateSelectionPopup();
        }
    }
    
     private void showDateSelectionPopup() {
        DateSelectionPopup datePopup =
                new DateSelectionPopup(
                        stage,
                        this::onDateSelected,
                        this::onExit
                );
        datePopup.show();
    }

    private void onDateSelected(LocalDate date) {
        this.selectedDate = date;
        showPurchaseBookTable();
    }

    private void onExit() {
        AppNavigator.startApp(stage);
    }
    
    private void showPurchaseBookTable() {
        PurchaseBookTableView tableView = new PurchaseBookTableView(selectedDate);
        tableView.start(stage);
    }
    
    private void showNoDataMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Data");
        alert.setHeaderText(null);
        alert.setContentText("No sub-slip data found!");
        alert.showAndWait();
        AppNavigator.startApp(stage);
    }
}
