package com.slipplus.licensing;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class LicenseActivationScreen {
    
    private Stage stage;
    private Runnable onSuccess;
    
    public LicenseActivationScreen(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }
    
    public void show() {
        System.out.println("LicenseActivationScreen.show() called");
        Platform.runLater(() -> {
            System.out.println("Platform.runLater() executing...");
            createAndShowWindow();
            System.out.println("Window created and shown");
        });
    }

    public void showOnStage(Stage primaryStage) {
        this.stage = primaryStage;
        Platform.runLater(() -> {
            setupStageAndShow();
        });
    }

    private void createAndShowWindow() {
        System.out.println("Creating stage...");
        stage = new Stage();
        System.out.println("Stage created, setting properties...");

        stage.initStyle(StageStyle.DECORATED); // Change from UTILITY
        stage.setTitle("SlipPlus - License Activation");
        stage.setResizable(false);
        stage.setAlwaysOnTop(true); // Force on top

        setupStageAndShow();
    }

    private void setupStageAndShow() {
        System.out.println("Creating UI components...");
        // Simplified layout - remove heavy styling
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        // Add logo at the top
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/logos/logo.png"));
            ImageView logoView = new ImageView(logoImage);
            logoView.setFitWidth(60);  // Reduced size
            logoView.setFitHeight(60);
            logoView.setPreserveRatio(true);
            root.getChildren().add(logoView);
        } catch (Exception e) {
            System.out.println("Could not load logo: " + e.getMessage());
        }

        // Title
        Label titleLabel = new Label("SlipPlus License Activation");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // System ID display
        LicenseManager licenseManager = LicenseManager.getInstance();
        String systemId = licenseManager.generateSystemId();

        Label systemIdLabel = new Label("System ID: " + systemId);
        systemIdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Button copySystemIdBtn = new Button("Copy System ID");
        copySystemIdBtn.setStyle("-fx-font-size: 12px;");
        copySystemIdBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(systemId);
            clipboard.setContent(content);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Copied");
            alert.setHeaderText(null);
            alert.setContentText("System ID copied to clipboard!");
            alert.showAndWait();
        });

        // Company name input
        Label companyLabel = new Label("Company Name:");
        TextField companyField = new TextField();
        companyField.setPromptText("Enter company name");
        companyField.setPrefWidth(300);

        // Pre-fill company name if stored
        String storedCompany = licenseManager.getStoredCompanyName();
        if (storedCompany != null) {
            companyField.setText(storedCompany);
        }

        // License key input
        Label licenseLabel = new Label("License Key:");
        TextField licenseField = new TextField();
        licenseField.setPromptText("Enter your license key");
        licenseField.setPrefWidth(300);

        // Activate button
        Button activateBtn = new Button("Activate License");
        activateBtn.setPrefWidth(150);
        activateBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        activateBtn.setOnAction(e -> activateLicense(companyField.getText(), licenseField.getText()));

        // Status label
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: red;");

        root.getChildren().addAll(
            titleLabel,
            new Separator(),
            systemIdLabel,
            copySystemIdBtn,
            new Separator(),
            companyLabel,
            companyField,
            licenseLabel,
            licenseField,
            activateBtn,
            statusLabel
        );

        System.out.println("Creating scene...");
        Scene scene = new Scene(root, 450, 500); // Increased height

        // Set stage properties
        stage.setTitle("SlipPlus - License Activation");
        stage.setResizable(false);
        stage.setScene(scene);

        System.out.println("Showing stage...");
        stage.show();
        stage.toFront(); // Bring to front
        stage.requestFocus(); // Request focus
        System.out.println("Stage shown successfully");

        // Focus on appropriate field
        Platform.runLater(() -> {
            if (storedCompany != null) {
                licenseField.requestFocus();
            } else {
                companyField.requestFocus();
            }
        });
    }
    
    private void activateLicense(String companyName, String licenseKey) {
        if (companyName.trim().isEmpty()) {
            showError("Please enter company name");
            return;
        }
        
        if (licenseKey.trim().isEmpty()) {
            showError("Please enter license key");
            return;
        }
        
        LicenseManager licenseManager = LicenseManager.getInstance();
        LicenseValidationResult result = licenseManager.activateLicense(companyName, licenseKey);
        
        if (result.isValid()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(result.getMessage());
            alert.showAndWait();
            
            stage.close();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            showError(result.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("License Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}




