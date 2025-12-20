package com.slipplus.core;

import javafx.stage.Stage;
import com.slipplus.screens.menu.MenuScreen;
import com.slipplus.screens.subSlip.SubSlipScreen;

public class AppNavigator {

    public static void startApp(Stage stage) {
        new MenuScreen().start(stage);
    }

    public static void openNewSubSlip(Stage stage) {
        new SubSlipScreen().start(stage);
    }

    public static void openMainSlip(Stage stage) {
        new com.slipplus.screens.mainSlip.MainSlipViewerScreen().start(stage);
    }

    public static void openPurchaseBook(Stage stage) {
        // TODO: new PurchaseBookScreen().start(stage);
    }

    public static void openSubSlipViewer(Stage stage) {
        new com.slipplus.screens.subSlipViewer.SubSlipViewerScreen().start(stage);
    }
}
