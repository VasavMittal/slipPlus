package com.slipplus.screens.menu;

import com.slipplus.constants.Colors;
import com.slipplus.core.StorageManager;
import com.slipplus.models.Shortcut;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public class ShortcutOverlay {

    private ObservableList<Shortcut> shortcuts;
    private TableView<Shortcut> table;

    public void open(Stage parentStage) {

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        List<Shortcut> loaded = StorageManager.loadShortcuts();
        shortcuts = FXCollections.observableArrayList(loaded);

        table = new TableView<>(shortcuts);

        TableColumn<Shortcut, String> colAlphabet = new TableColumn<>("Alphabet");
        colAlphabet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAlphabet()));
        colAlphabet.setPrefWidth(120);

        TableColumn<Shortcut, String> colDescription = new TableColumn<>("Description");
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colDescription.setPrefWidth(400);

        TableColumn<Shortcut, String> colOperation = new TableColumn<>("Operation");
        colOperation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperation()));
        colOperation.setPrefWidth(120);

        table.getColumns().addAll(colAlphabet, colDescription, colOperation);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(Colors.POPUP_BG) + ";");
        root.setCenter(table);

        Label header = new Label("Shortcut Management");
        header.setStyle("-fx-font-size: 20px; -fx-text-fill: black; -fx-font-weight: bold;");
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        Label footer = new Label("F1 = Add   ENTER = Edit   DEL = Delete   ESC = Close");
        footer.setStyle("-fx-font-size: 18px; -fx-text-fill: black;");
        footer.setAlignment(Pos.CENTER);
        root.setBottom(footer);

        Scene scene = new Scene(root, screenWidth * 0.70, screenHeight * 0.70);

        scene.setOnKeyPressed(e -> handleKeys(e.getCode()));

        Stage popup = new Stage();
        popup.initOwner(parentStage);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setResizable(false);
        popup.setTitle("Shortcut Management");

        popup.setScene(scene);
        popup.centerOnScreen();
        popup.show();

        table.requestFocus();
    }

    private void handleKeys(KeyCode code) {
        switch (code) {
            case F1, INSERT -> addShortcut();
            case ENTER -> editShortcut();
            case DELETE -> deleteShortcut();
            case ESCAPE -> ((Stage) table.getScene().getWindow()).close();
            default -> {}
        }
    }

    private void addShortcut() {
        Dialog<Shortcut> dialog = createShortcutDialog("Add Shortcut", null);
        
        dialog.showAndWait().ifPresent(newShortcut -> {
            // Check if alphabet already exists (case-sensitive)
            boolean exists = shortcuts.stream()
                    .anyMatch(s -> s.getAlphabet().equals(newShortcut.getAlphabet()));
            
            if (exists) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(table.getScene().getWindow());
                alert.setTitle("Duplicate Alphabet");
                alert.setHeaderText(null);
                alert.setContentText("Alphabet '" + newShortcut.getAlphabet() + "' already exists!");
                alert.showAndWait();
                return;
            }
            
            shortcuts.add(newShortcut);
            StorageManager.saveShortcuts(shortcuts);
            table.refresh();
            table.requestFocus();
        });
    }

    private void editShortcut() {
        Shortcut selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<Shortcut> dialog = createShortcutDialog("Edit Shortcut", selected);
        
        dialog.showAndWait().ifPresent(editedShortcut -> {
            // Check if alphabet already exists (excluding current item, case-sensitive)
            boolean exists = shortcuts.stream()
                    .filter(s -> s != selected)
                    .anyMatch(s -> s.getAlphabet().equals(editedShortcut.getAlphabet()));
            
            if (exists) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(table.getScene().getWindow());
                alert.setTitle("Duplicate Alphabet");
                alert.setHeaderText(null);
                alert.setContentText("Alphabet '" + editedShortcut.getAlphabet() + "' already exists!");
                alert.showAndWait();
                return;
            }
            
            selected.setAlphabet(editedShortcut.getAlphabet());
            selected.setDescription(editedShortcut.getDescription());
            selected.setOperation(editedShortcut.getOperation());

            StorageManager.saveShortcuts(shortcuts);
            table.refresh();
            table.requestFocus();
        });
    }

    private void deleteShortcut() {
        Shortcut selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(table.getScene().getWindow());
        confirm.initModality(Modality.WINDOW_MODAL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete shortcut '" + 
                selected.getAlphabet() + " - " + selected.getDescription() + "'?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                shortcuts.remove(selected);
                StorageManager.saveShortcuts(shortcuts);
                table.refresh();
                table.requestFocus();
            }
        });
    }

    private Dialog<Shortcut> createShortcutDialog(String title, Shortcut existing) {
        Dialog<Shortcut> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(table.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField alphabetField = new TextField();
        alphabetField.setPromptText("Single alphabet (e.g., r)");
        if (existing != null) alphabetField.setText(existing.getAlphabet());

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description (e.g., RTGS)");
        if (existing != null) descriptionField.setText(existing.getDescription());

        ComboBox<String> operationBox = new ComboBox<>();
        operationBox.getItems().addAll("+", "-");
        operationBox.setValue(existing != null ? existing.getOperation() : "+");

        // Add Enter key navigation
        alphabetField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                descriptionField.requestFocus();
                e.consume();
            }
        });

        descriptionField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                operationBox.requestFocus();
                e.consume();
            }
        });

        operationBox.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                // Trigger save button
                Button saveBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
                if (saveBtn != null) {
                    saveBtn.fire();
                }
                e.consume();
            }
        });

        grid.add(new Label("Alphabet:"), 0, 0);
        grid.add(alphabetField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Operation:"), 0, 2);
        grid.add(operationBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        // Set initial focus to alphabet field
        Platform.runLater(() -> alphabetField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                try {
                    String alphabet = alphabetField.getText().trim(); // Keep case-sensitive
                    String description = descriptionField.getText().trim();
                    String operation = operationBox.getValue();

                    if (alphabet.isEmpty() || description.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText(null);
                        alert.setContentText("Alphabet and Description cannot be empty!");
                        alert.showAndWait();
                        return null;
                    }

                    if (alphabet.length() != 1 || !Character.isLetter(alphabet.charAt(0))) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid Alphabet");
                        alert.setHeaderText(null);
                        alert.setContentText("Alphabet must be a single letter!");
                        alert.showAndWait();
                        return null;
                    }

                    return new Shortcut(alphabet, description, operation);
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Please check your input!");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private String toHex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}






