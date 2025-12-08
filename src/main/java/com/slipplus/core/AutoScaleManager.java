package com.slipplus.core;

import javafx.stage.Screen;

public class AutoScaleManager {

    private static double screenWidth = Screen.getPrimary().getBounds().getWidth();
    private static double screenHeight = Screen.getPrimary().getBounds().getHeight();

    public static double scaleWidth(double value) {
        return (screenWidth / 1920.0) * value;
    }

    public static double scaleHeight(double value) {
        return (screenHeight / 1080.0) * value;
    }
}
