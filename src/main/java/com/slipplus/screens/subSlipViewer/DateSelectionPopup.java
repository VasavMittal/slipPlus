package com.slipplus.screens.subSlipViewer;

import com.slipplus.constants.Colors;
import com.slipplus.core.StorageManager;
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
import java.util.List;
import java.util.function.Consumer;

public class DateSelectionPopup implements BasePopup {
    
    private Stage popup;
    private ListView<String> dateList;
    private List<LocalDate> availableDates;
    private Consumer<LocalDate> onDateSelected;
    private Runnable onExit;
    
    public DateSelectionPopup(Stage parentStage, Consumer<LocalDate> onDateSelected, Runnable onExit) {
        this.onDateSelected = onDateSelected;
        this.onExit = onExit;
        createPopup(parentStage);
    }
    
    private void createPopup(Stage parentStage) {
        // Load available dates
        availableDates = StorageManager.getAvailableDates();
        
        // Check if only one date - auto select
        if (availableDates.size() == 1) {
            Platform.runLater(() -> {
                onDateSelected.accept(availableDates.get(0));
            });
            return;
        }
        
        // Check if no dates
        if (availableDates.isEmpty()) {
            showNoDataMessage(parentStage);
            return;
        }
        
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();
        
        popup = new Stage();
        popup.initOwner(parentStage);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setTitle("Select Date");
        popup.setResizable(false);
        
        // Create date list
        dateList = new ListView<>();
        dateList.setPrefHeight(300);
        dateList.setPrefWidth(400);
        dateList.setFocusTraversable(true);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        List<String> dateStrings = availableDates.stream()
                .map(date -> date.format(formatter))
                .toList();
        
        dateList.setItems(FXCollections.observableArrayList(dateStrings));
        dateList.getSelectionModel().selectFirst();
        
        // Layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(Colors.POPUP_BG) + ";");
        root.setCenter(dateList);
        
        Label footer = new Label("ENTER = Select   ESC = Exit   F4 = Main Screen");
        footer.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
        footer.setAlignment(Pos.CENTER);
        root.setBottom(footer);
        
        Scene scene = new Scene(root, screenWidth * 0.3, screenHeight * 0.4);
        
        // Add event filter to intercept keys BEFORE ListView processes them
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            System.out.println("Key filter: " + e.getCode());
            switch (e.getCode()) {
                case ENTER -> {
                    System.out.println("Enter intercepted");
                    selectDate();
                    e.consume();
                }
                case ESCAPE -> {
                    System.out.println("Escape intercepted");
                    close();
                    onExit.run();
                    e.consume();
                }
                case F4 -> {
                    System.out.println("F4 intercepted");
                    close();
                    onExit.run();
                    e.consume();
                }
            }
        });
        
        popup.setScene(scene);
        popup.centerOnScreen();
        
        // Ensure focus and selection
        popup.setOnShown(e -> {
            Platform.runLater(() -> {
                dateList.requestFocus();
                dateList.getSelectionModel().selectFirst();
                System.out.println("Popup shown, focus set");
            });
        });
    }
    
    private void selectDate() {
        int selectedIndex = dateList.getSelectionModel().getSelectedIndex();
        System.out.println("selectDate called, selectedIndex: " + selectedIndex);
        if (selectedIndex >= 0 && selectedIndex < availableDates.size()) {
            LocalDate selectedDate = availableDates.get(selectedIndex);
            System.out.println("Selected date: " + selectedDate);
            close();
            onDateSelected.accept(selectedDate);
        }
    }
    
    private void showNoDataMessage(Stage parentStage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Data");
        alert.setHeaderText(null);
        alert.setContentText("No sub-slip data available");
        alert.initOwner(parentStage);
        alert.showAndWait();
        onExit.run();
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



