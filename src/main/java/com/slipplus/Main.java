package com.slipplus;

import com.slipplus.core.AppNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        stage.setMaximized(true); // Use maximized instead of fullscreen
        AppNavigator.startApp(stage);
    }

    public static void main(String[] args) {
        launch();
    }
}
