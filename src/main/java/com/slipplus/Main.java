package com.slipplus;

import com.slipplus.core.AppNavigator;
import com.slipplus.licensing.LicenseManager;
import com.slipplus.licensing.LicenseValidationResult;
import com.slipplus.licensing.LicenseActivationScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // Add application icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/logos/logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Could not load application icon: " + e.getMessage());
        }
        
        long start = System.currentTimeMillis();
        
        // Force JavaFX to initialize early
        System.out.println("Initializing JavaFX: " + (System.currentTimeMillis() - start) + "ms");
        
        LicenseManager licenseManager = LicenseManager.getInstance();
        System.out.println("License manager created: " + (System.currentTimeMillis() - start) + "ms");
        
        LicenseValidationResult result = licenseManager.validateLicense();
        System.out.println("License validated: " + (System.currentTimeMillis() - start) + "ms");
        
        if (result.isValid()) {
            System.out.println("License is valid, starting app: " + (System.currentTimeMillis() - start) + "ms");
            startApplication(stage);
            System.out.println("App started: " + (System.currentTimeMillis() - start) + "ms");
        } else {
            System.out.println("License invalid, creating activation screen: " + (System.currentTimeMillis() - start) + "ms");
            
            // Use the primary stage for license activation
            showLicenseActivationOnPrimaryStage(stage);
            
            System.out.println("Activation screen created: " + (System.currentTimeMillis() - start) + "ms");
        }
    }
    
    private void startApplication(Stage stage) {
        stage.setMaximized(true);
        AppNavigator.startApp(stage);
    }

    private void showLicenseActivationOnPrimaryStage(Stage primaryStage) {
        // Create license UI directly on primary stage
        LicenseActivationScreen activationScreen = new LicenseActivationScreen(() -> {
            startApplication(primaryStage);
        });
        activationScreen.showOnStage(primaryStage); // New method
    }

    public static void main(String[] args) {
        launch();
    }
}
