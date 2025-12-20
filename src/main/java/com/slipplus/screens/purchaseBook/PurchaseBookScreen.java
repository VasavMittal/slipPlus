package com.slipplus.screens.purchaseBook;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.models.SubSlip;
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
            showDateSelectionPopup(availableDatesList);
        }
    }
    
    private void showDateSelectionPopup(List<LocalDate> dates) {
        popupOpen = true;
        
        VBox popup = new VBox(20);
        popup.setAlignment(Pos.CENTER);
        popup.setPadding(new Insets(30));
        popup.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2;");
        
        Label title = new Label("Select Date for Purchase Book");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        VBox dateButtons = new VBox(10);
        dateButtons.setAlignment(Pos.CENTER);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        for (LocalDate date : dates.stream().sorted().toList()) {
            Button dateBtn = new Button(date.format(formatter));
            dateBtn.setStyle("-fx-font-size: 18px; -fx-pref-width: 200;");
            dateBtn.setOnAction(e -> {
                selectedDate = date;
                popupOpen = false;
                showPurchaseBookTable();
            });
            dateButtons.getChildren().add(dateBtn);
        }
        
        popup.getChildren().addAll(title, dateButtons);
        
        BorderPane root = new BorderPane();
        root.setCenter(popup);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        
        Scene scene = new Scene(root, 800, 600);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                popupOpen = false;
                AppNavigator.startApp(stage);
            }
        });
        
        stage.setScene(scene);
        stage.setTitle("Purchase Book - Date Selection");
        stage.show();
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
