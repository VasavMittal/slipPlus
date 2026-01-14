package com.slipplus.screens.menu;

import com.slipplus.constants.Colors;
import com.slipplus.core.AppNavigator;
import com.slipplus.core.AutoScaleManager;
import com.slipplus.core.StorageManager;
import com.slipplus.constants.FontSizes;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class MenuScreen {

    private int selectedIndex = 0;
    private Button[] buttons;
    private double fontSize; // <-- Class variable so NOT a lambda problem

    public void start(Stage stage) {
        // Add logo to window icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/logos/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Could not load logo: " + e.getMessage());
        }

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + toHex(Colors.BACKGROUND) + ";");

        String[] labels = {"New", "Sub Slip", "Main Slip", "Purchase Book", "General Data"};
        buttons = new Button[labels.length];

        HBox container = new HBox(AutoScaleManager.scaleWidth(40));
        container.setAlignment(Pos.CENTER);

        // SET FONT SIZE ONLY ONCE
        fontSize = FontSizes.scale(32, screenWidth);
        if (fontSize < 24) fontSize = 24;

        for (int i = 0; i < labels.length; i++) {
            Button btn = new Button(labels[i]);
            btn.setPrefWidth(AutoScaleManager.scaleWidth(300)); // Increased from 260 to 300
            btn.setPrefHeight(AutoScaleManager.scaleHeight(100));
            btn.setFocusTraversable(false);
            btn.setStyle(buttonStyleNormal());
            buttons[i] = btn;
            container.getChildren().add(btn);
        }

        highlight();

        // Add keyboard shortcuts info at top
        Label keyboardHelp = new Label("LEFT/RIGHT = Navigate   ENTER = Select   ESC = Exit   F1 = Party Management   F3 = Shortcuts");
        keyboardHelp.setStyle("-fx-font-size: " + (fontSize * 0.6) + "px; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 10px; -fx-background-radius: 5px;");
        keyboardHelp.setAlignment(Pos.CENTER);
        
        Scene scene = new Scene(root, screenWidth, screenHeight);
        root.getChildren().addAll(keyboardHelp, container);
        
        // Position elements
        StackPane.setAlignment(keyboardHelp, Pos.TOP_CENTER);
        StackPane.setAlignment(container, Pos.CENTER);
        StackPane.setMargin(keyboardHelp, new Insets(30, 0, 0, 0));

        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case RIGHT -> moveSelection(1);
                case LEFT -> moveSelection(-1);
                case ENTER -> openSelected(stage);
                case ESCAPE -> confirmExit(stage);
                case F1 -> new PartyOverlay().open(stage);
                case F3 -> new ShortcutOverlay().open(stage);
                default -> {}
            }
        });

        stage.setScene(scene);
        stage.setMaximized(true); // Use maximized instead of fullscreen
        stage.show();
    }

    private void confirmExit(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit SlipPlus");
        alert.setHeaderText(null);
        alert.setContentText("What would you like to do?");
        alert.initOwner(stage);

        // Create custom buttons
        ButtonType exitButton = new ButtonType("Exit");
        ButtonType deleteAllButton = new ButtonType("Delete All Data & Exit");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(exitButton, deleteAllButton, cancelButton);

        alert.showAndWait().ifPresent(result -> {
            if (result == exitButton) {
                // Normal exit
                stage.close();
                System.exit(0);
            } else if (result == deleteAllButton) {
                // Delete all data and exit
                confirmDeleteAllData(stage);
            }
            // Cancel - do nothing
        });
    }

    private void confirmDeleteAllData(Stage stage) {
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Delete All Data");
        confirmAlert.setHeaderText("Are you sure?");
        confirmAlert.setContentText("This will permanently delete:\n" +
                "• All sub-slip records\n" +
                "• All main slip records\n" +
                "• Purchase book data\n\n" +
                "Party data, shortcuts, and license will be preserved.\n\n" +
                "This action cannot be undone!");
        confirmAlert.initOwner(stage);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteAllDataAndExit(stage);
            }
        });
    }

    private void deleteAllDataAndExit(Stage stage) {
        try {
            // Delete data files (keep parties.json, shortcuts.json, and license)
            StorageManager.deleteAllData();
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Data Deleted");
            successAlert.setHeaderText(null);
            successAlert.setContentText("All data has been successfully deleted.");
            successAlert.initOwner(stage);
            successAlert.showAndWait();
            
            // Exit application
            stage.close();
            System.exit(0);
            
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Failed to delete data: " + e.getMessage());
            errorAlert.initOwner(stage);
            errorAlert.showAndWait();
        }
    }

    private void moveSelection(int step) {
        selectedIndex = Math.floorMod(selectedIndex + step, buttons.length);
        highlight();
    }

    private void highlight() {
        for (int i = 0; i < buttons.length; i++) {
            if (i == selectedIndex) {
                buttons[i].setStyle(buttonStyleSelected());
            } else {
                buttons[i].setStyle(buttonStyleNormal());
            }
        }
    }

    private String buttonStyleNormal() {
        return "-fx-background-color: " + toHex(Colors.PRIMARY_BLUE) +
                "; -fx-text-fill: white; -fx-font-size: " + fontSize +
                "px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;";
    }

    private String buttonStyleSelected() {
        return "-fx-background-color: " + toHex(Colors.HIGHLIGHT_GREEN) +
                "; -fx-text-fill: white; -fx-font-size: " + fontSize +
                "px; -fx-font-weight: bold; -fx-border-color: black; -fx-border-width: 2;" +
                " -fx-background-radius: 8; -fx-border-radius: 8;";
    }

    private String toHex(javafx.scene.paint.Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private void openSelected(Stage stage) {
        switch (selectedIndex) {
            case 0 -> AppNavigator.openNewSubSlip(stage);
            case 1 -> AppNavigator.openSubSlipViewer(stage);
            case 2 -> AppNavigator.openMainSlip(stage);
            case 3 -> AppNavigator.openPurchaseBook(stage);
            case 4 -> AppNavigator.openGeneralData(stage);

        }
    }
}
