package com.slipplus.screens.menu;

import com.slipplus.constants.Colors;
import com.slipplus.core.StorageManager;
import com.slipplus.models.Party;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public class PartyOverlay {

    private ObservableList<Party> parties;
    private TableView<Party> table;

    public void open(Stage parentStage) {

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        List<Party> loaded = StorageManager.loadParties();
        parties = FXCollections.observableArrayList(loaded);

        table = new TableView<>(parties);

        TableColumn<Party, Number> colId = new TableColumn<>("Sr");
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colId.setPrefWidth(60);

        TableColumn<Party, String> colName = new TableColumn<>("Party Name");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colName.setPrefWidth(600);

        table.getColumns().addAll(colId, colName);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(Colors.POPUP_BG) + ";");
        root.setCenter(table);

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

        popup.setScene(scene);
        popup.centerOnScreen();
        popup.show();

        table.requestFocus();
    }

    private void handleKeys(KeyCode code) {
        switch (code) {
            case F1, INSERT -> addParty();
            case ENTER -> editParty();
            case DELETE -> deleteParty();
            case ESCAPE -> ((Stage) table.getScene().getWindow()).close();
            default -> {}
        }
    }

    private void addParty() {
        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.initOwner(table.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Add Party");
            dialog.setHeaderText(null);
            dialog.setContentText("Enter party name:");
            
            var result = dialog.showAndWait();  // Wait for user input

            // If user pressed Cancel or ESC → exit add loop
            if (result.isEmpty()) {
                break;
            }

            String trimmed = result.get().trim();
            if (trimmed.isEmpty()) {
                continue; // Skip blank entries without closing popup
            }

            // Add new party
            int id = parties.isEmpty() ? 1 : parties.get(parties.size() - 1).getId() + 1;
            parties.add(new Party(id, trimmed));
            StorageManager.saveParties(parties);

            table.refresh();
            table.getSelectionModel().selectLast();
            table.requestFocus();
            // Loop repeats → popup appears again for next input
        }
    }


    private void editParty() {
        Party selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.initOwner(table.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Edit Party");
        dialog.setHeaderText(null);
        dialog.setContentText("Update party name:");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty()) return;
            selected.setName(trimmed);
            StorageManager.saveParties(parties);
            table.refresh();
            table.requestFocus();
        });
    }


    private void deleteParty() {
        Party selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        // Check if party has sub-slip records
        if (hasSubSlipRecords(selected.getId())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(table.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setTitle("Cannot Delete Party");
            alert.setHeaderText(null);
            alert.setContentText("Cannot delete party '" + selected.getName() + "'.\n\n" +
                    "This party has sub-slip records. Please delete all sub-slip records for this party first.");
            alert.showAndWait();
            return;
        }
        
        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(table.getScene().getWindow());
        confirm.initModality(Modality.WINDOW_MODAL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete party '" + selected.getName() + "'?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                parties.remove(selected);
                StorageManager.saveParties(parties);
                table.refresh();
                table.requestFocus();
            }
        });
    }

    private boolean hasSubSlipRecords(int partyId) {
        try {
            // Check if this party ID exists in any sub-slip records
            return StorageManager.hasSubSlipRecordsForParty(String.valueOf(partyId));
        } catch (Exception e) {
            e.printStackTrace();
            return true; // Err on the side of caution
        }
    }

    private String toHex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}
