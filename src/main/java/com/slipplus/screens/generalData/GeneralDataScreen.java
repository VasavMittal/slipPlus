package com.slipplus.screens.generalData;

import com.slipplus.core.AppNavigator;
import com.slipplus.core.StorageManager;
import com.slipplus.screens.subSlipViewer.DateSelectionPopup;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class GeneralDataScreen {

    private Stage stage;

    public void start(Stage stage) {
        this.stage = stage;

        List<LocalDate> dates = StorageManager.getAvailableDates();
        if (dates.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "No Main Slip data found");
            a.showAndWait();
            AppNavigator.startApp(stage);
            return;
        }

        if (dates.size() == 1) {
            openView(dates.get(0));
        } else {
            DateSelectionPopup popup =
                new DateSelectionPopup(stage, this::openView, this::exit);
            popup.show();
        }
    }

    private void openView(LocalDate date) {
        new GeneralDataView(date).start(stage);
    }

    private void exit() {
        AppNavigator.startApp(stage);
    }
}
