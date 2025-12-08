package com.slipplus.constants;

public class FontSizes {
    public static double scale(double baseSize, double screenWidth) {
        return (screenWidth / 1920.0) * baseSize;
    }
}
