package com.slipplus.screens.mainSlip;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.models.MainSlip;
import com.slipplus.models.Shortcut;
import com.slipplus.models.SubSlip;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainSlipScreen {
    
    private Stage stage;
    private LocalDate selectedDate;
    private String selectedParty;
    private List<SubSlip> subSlips;
    private List<Shortcut> shortcuts;
    private DecimalFormat moneyFmt = new DecimalFormat("#,##0");
    
    private VBox contentArea;
    private VBox operationArea;
    private Label totalBeforeLabel;
    private Label totalAfterLabel;
    private TextField currentOperationField;
    private List<MainSlip.Operation> operations = new ArrayList<>();
    private double totalBeforeOperations;
    private double totalAfterOperations;
    private double fontSize;
    private boolean popupOpen = false; // Add this flag
    
    public MainSlipScreen(LocalDate date, String party) {
        this.selectedDate = date;
        this.selectedParty = party;
        this.subSlips = StorageManager.getSubSlipsForDateAndParty(date, party);
        this.shortcuts = StorageManager.loadShortcuts();
        
        // Check if main slip already exists
        MainSlip existingMainSlip = StorageManager.getMainSlip(date, party);
        if (existingMainSlip != null) {
            // Load existing operations
            this.operations = new ArrayList<>(existingMainSlip.getOperations());
            this.totalBeforeOperations = existingMainSlip.getTotalBeforeOperations();
            this.totalAfterOperations = existingMainSlip.getTotalAfterOperations();
        } else {
            // New main slip
            this.operations = new ArrayList<>();
            calculateInitialTotal();
        }
    }
    
    public void start(Stage stage) {
        this.stage = stage;
        
        // Calculate dynamic font size based on screen
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        this.fontSize = (screenWidth / 1920.0) * 24; // Increased base font size from 18 to 24px
        if (fontSize < 18) fontSize = 18; // Increased minimum font size from 14 to 18
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        
        // Create main content area
        contentArea = new VBox(20);
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setPadding(new Insets(50));
        
        buildPartyHeader();
        buildSubSlipData();
        buildOperationArea();
        buildBottomButtons();
        
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        root.setCenter(scrollPane);
        
        Scene scene = new Scene(root, 1600, 900);
        scene.setOnKeyPressed(e -> {
            // Don't handle keys if popup is open
            if (popupOpen) {
                return;
            }
            
            if (e.getCode() == KeyCode.ESCAPE) {
                AppNavigator.startApp(stage);
            } else if (e.getCode() == KeyCode.F2) {
                // Change date - go back to date selection
                changeDate();
            } else if (e.getCode() == KeyCode.F3) {
                // Change party - go back to party selection for current date
                changeParty();
            } else if (e.getCode() == KeyCode.F5) {
                // Reset main slip
                resetMainSlip();
            }
        });
        
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        
        Platform.runLater(() -> {
            if (currentOperationField != null) {
                currentOperationField.requestFocus();
            }
        });
    }
    
    private void buildPartyHeader() {
        Label partyLabel = new Label(selectedParty);
        partyLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: black;", fontSize * 1.5));
        
        // Instructions label
        Label instructionsLabel = new Label("ESC = Main Menu   F2 = Change Date   F3 = Change Party   F5 = Reset");
        instructionsLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: #666666; -fx-font-style: italic;", fontSize * 0.8));
        instructionsLabel.setAlignment(Pos.CENTER);
        
        // Empty line space
        Label spacer = new Label(" ");
        
        contentArea.getChildren().addAll(partyLabel, instructionsLabel, spacer);
    }
    
    private void buildSubSlipData() {
        VBox subSlipArea = new VBox(8);
        subSlipArea.setAlignment(Pos.CENTER);
        subSlipArea.setMaxWidth(600); // Reduce column width
        
        for (SubSlip slip : subSlips) {
            Label dataLine = new Label(String.format("%.0f kg  →  ₹%s", 
                slip.getMainWeight(), moneyFmt.format(slip.getFinalAmount())));
            dataLine.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: black;", fontSize));
            subSlipArea.getChildren().add(dataLine);
        }
        
        // Smaller horizontal line
        Separator separator = new Separator();
        separator.setMaxWidth(400); // Make border smaller
        separator.setStyle("-fx-background-color: black; -fx-border-color: black; -fx-border-width: 1px;");
        
        // Total before operations
        totalBeforeLabel = new Label("Total: ₹" + moneyFmt.format(totalBeforeOperations));
        totalBeforeLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: black;", fontSize * 1.2));
        
        subSlipArea.getChildren().addAll(separator, totalBeforeLabel);
        contentArea.getChildren().add(subSlipArea);
    }
    
    private void buildOperationArea() {
        operationArea = new VBox(10);
        operationArea.setAlignment(Pos.CENTER);
        operationArea.setMaxWidth(600); // Reduce column width
        
        // If existing operations, display them first
        if (!operations.isEmpty()) {
            for (MainSlip.Operation op : operations) {
                String operationText = String.format("₹%s %s (%s)", 
                    moneyFmt.format(op.getAmount()), op.getDescription(), op.getOperationType());
                Label operationLabel = new Label(operationText);
                operationLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: black;", fontSize));
                operationArea.getChildren().add(operationLabel);
            }
            
            // Show final total if operations exist
            totalAfterLabel = new Label("Final Total: ₹" + moneyFmt.format(totalAfterOperations));
            totalAfterLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: green;", fontSize * 1.3));
            operationArea.getChildren().add(totalAfterLabel);
        } else {
            // No existing operations, create input field
            createNewOperationField();
        }
        
        contentArea.getChildren().add(operationArea);
    }
    
    private void createNewOperationField() {
        currentOperationField = new TextField();
        currentOperationField.setPromptText("Enter amount and shortcut (e.g., 2000 R) or 0.0 to finish");
        currentOperationField.setStyle(String.format("-fx-font-size: %.0fpx; -fx-pref-width: 400px;", fontSize));
        currentOperationField.setAlignment(Pos.CENTER);
        
        currentOperationField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                processOperation();
            }
        });
        
        operationArea.getChildren().add(currentOperationField);
    }
    
    private void processOperation() {
        String input = currentOperationField.getText().trim();
        
        // Check for termination
        if ("0.0".equals(input) || "0.00".equals(input)) {
            finishOperations();
            return;
        }
        
        // Parse input (e.g., "2000 R")
        String[] parts = input.split("\\s+");
        if (parts.length != 2) {
            showError("Invalid format. Use: amount shortcut (e.g., 2000 R)");
            return;
        }
        
        try {
            double amount = Double.parseDouble(parts[0]);
            String shortcutKey = parts[1];
            
            // Find shortcut
            Shortcut shortcut = shortcuts.stream()
                    .filter(s -> s.getAlphabet().equals(shortcutKey))
                    .findFirst()
                    .orElse(null);
            
            if (shortcut == null) {
                showError("Shortcut '" + shortcutKey + "' not found!");
                return;
            }
            
            // Create operation
            MainSlip.Operation operation = new MainSlip.Operation(
                amount, shortcut.getAlphabet(), shortcut.getDescription(), shortcut.getOperation());
            operations.add(operation);
            
            // Display operation
            String operationText = String.format("₹%s %s (%s)", 
                moneyFmt.format(amount), shortcut.getDescription(), shortcut.getOperation());
            Label operationLabel = new Label(operationText);
            operationLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: black;", fontSize));
            
            // Replace current field with label and create new field
            operationArea.getChildren().remove(currentOperationField);
            operationArea.getChildren().add(operationLabel);
            createNewOperationField();
            
            Platform.runLater(() -> currentOperationField.requestFocus());
            
        } catch (NumberFormatException e) {
            showError("Invalid amount format!");
        }
    }
    
    private void finishOperations() {
        // Remove current operation field
        operationArea.getChildren().remove(currentOperationField);
        
        // Calculate final total
        calculateFinalTotal();
        
        // Show final total
        totalAfterLabel = new Label("Final Total: ₹" + moneyFmt.format(totalAfterOperations));
        totalAfterLabel.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: green;", fontSize * 1.3));
        
        operationArea.getChildren().add(totalAfterLabel);
    }
    
    private void calculateInitialTotal() {
        totalBeforeOperations = subSlips.stream()
                .mapToDouble(SubSlip::getFinalAmount)
                .sum();
        totalAfterOperations = totalBeforeOperations;
    }
    
    private void calculateFinalTotal() {
        totalAfterOperations = totalBeforeOperations;
        
        for (MainSlip.Operation op : operations) {
            if ("+".equals(op.getOperationType())) {
                totalAfterOperations += op.getAmount();
            } else if ("-".equals(op.getOperationType())) {
                totalAfterOperations -= op.getAmount();
            }
        }
    }
    
    private void buildBottomButtons() {
        Button saveButton = new Button("Save & Print");
        saveButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-pref-height: 40px;");
        saveButton.setOnAction(e -> saveMainSlip());
        
        Button resetButton = new Button("Reset");
        resetButton.setStyle("-fx-font-size: 16px; -fx-pref-width: 150px; -fx-pref-height: 40px;");
        resetButton.setOnAction(e -> resetMainSlip());
        
        HBox buttonArea = new HBox(20, saveButton, resetButton);
        buttonArea.setAlignment(Pos.CENTER);
        buttonArea.setPadding(new Insets(30, 0, 0, 0));
        
        contentArea.getChildren().add(buttonArea);
    }
    
    private void saveMainSlip() {
        MainSlip mainSlip = new MainSlip(selectedDate, selectedParty, 
            totalBeforeOperations, new ArrayList<>(operations), totalAfterOperations);
        
        StorageManager.saveMainSlip(mainSlip);
        
        // Open print preview instead of just showing success message
        MainSlipPrintPreview printPreview = new MainSlipPrintPreview(this, selectedDate, selectedParty, mainSlip);
        printPreview.start(stage);
    }
    
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        
        Platform.runLater(() -> currentOperationField.requestFocus());
    }

    private void changeDate() {
        // Set popup flag when going to date selection
        popupOpen = true;
        
        // Go back to MainSlipViewerScreen which will show date selection
        // Pass reference to this screen so it can return properly
        com.slipplus.screens.mainSlip.MainSlipViewerScreen viewerScreen = 
            new com.slipplus.screens.mainSlip.MainSlipViewerScreen(this);
        viewerScreen.start(stage);
    }

    // Add method to reset popup flag
    public void resetPopupFlag() {
        popupOpen = false;
    }

    private void changeParty() {
        // Check if there are multiple parties for current date
        List<String> partyIds = StorageManager.getPartyIdsForDate(selectedDate);
        
        if (partyIds.size() <= 1) {
            // Only one party available, show message and stay on current screen
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Other Parties");
            alert.setHeaderText(null);
            alert.setContentText("No other parties available for date: " + selectedDate.toString());
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }
        
        // Set popup flag
        popupOpen = true;
        
        // Multiple parties available, show party selection
        com.slipplus.screens.subSlipViewer.PartySelectionPopup partyPopup = 
            new com.slipplus.screens.subSlipViewer.PartySelectionPopup(stage, selectedDate,
                (date, partyName) -> {
                    // Reset popup flag
                    popupOpen = false;
                    // Create new MainSlipScreen with selected party
                    MainSlipScreen newMainSlip = new MainSlipScreen(date, partyName);
                    newMainSlip.start(stage);
                },
                () -> {
                    // Reset popup flag when back is pressed
                    popupOpen = false;
                    // If back is pressed, stay on current screen (do nothing)
                    // Focus will return to the operation field
                    Platform.runLater(() -> {
                        if (currentOperationField != null) {
                            currentOperationField.requestFocus();
                        }
                    });
                });
        partyPopup.show();
    }

    private void resetMainSlip() {
        // Clear all operations
        operations.clear();
        
        // Clear operation area and recreate input field
        operationArea.getChildren().clear();
        createNewOperationField();
        
        // Remove final total label if it exists
        if (totalAfterLabel != null) {
            totalAfterLabel = null;
        }
        
        // Reset totals to initial values
        calculateInitialTotal();
        
        // Focus on operation field
        Platform.runLater(() -> currentOperationField.requestFocus());
    }
}

