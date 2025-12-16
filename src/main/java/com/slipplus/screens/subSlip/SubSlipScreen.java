package com.slipplus.screens.subSlip;

import com.slipplus.core.AppNavigator;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class SubSlipScreen {

    private final SlipContext ctx = new SlipContext();
    private PartySelector partySelector;
    private SwBreakdownManager swManager;
    private PriceCalculationEngine priceEngine;
    private SubSlipSaver saver;

    public void start(Stage stage) {

        System.out.println("SubSlipScreen.start()");

        ctx.root = new BorderPane();
        ctx.root.setStyle("-fx-background-color: white;");
        ctx.overlay = new StackPane(ctx.root);

        Scene scene = new Scene(ctx.overlay, 1600, 900);
        ctx.scene = scene;
        ctx.fontSize = scene.heightProperty().divide(30);

        buildTopFields();
        buildLeftFields();
        partySelector = new PartySelector(ctx);
        priceEngine   = new PriceCalculationEngine(ctx);
        swManager     = new SwBreakdownManager(ctx, priceEngine);
        saver         = new SubSlipSaver(ctx, priceEngine);
        buildRightFields();
        buildBottomButtons();
        buildLoader();

        ctx.price1Field.textProperty().addListener((o, ov, nv) -> priceEngine.recomputeAllRows());
        ctx.price2Field.textProperty().addListener((o, ov, nv) -> priceEngine.recomputeAllRows());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                resetAll();
                AppNavigator.startApp(stage);
                return;
            }
            if (e.getCode() == KeyCode.F2) {
                resetAll();
                return;
            }
            if (e.getCode() == KeyCode.ENTER) {
                handleEnter(stage);
                e.consume();
            }
        });

        stage.setScene(scene);
        stage.setMaximized(true); // Ensure maximized
        stage.show();
        
        // Force proper maximization after show
        Platform.runLater(() -> {
            stage.setMaximized(false);
            stage.setMaximized(true);
            ctx.partyField.requestFocus();
        });
    }

    // ---------- UI building ----------
    private void buildTopFields() {
        // PARTY NAME – added without disturbing layout
        ctx.partyField = makeField("", 360);
        ctx.partyField.setPromptText("Party Name");

        HBox center = new HBox(ctx.partyField);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(20, 0, 0, 0));

        // TRUCK number EXACT like demo
        ctx.truckField = makeField("", 260);
        ctx.truckField.setPromptText("Truck Number");

        HBox right = new HBox(ctx.truckField);
        right.setAlignment(Pos.TOP_RIGHT);
        right.setPadding(new Insets(20, 24, 0, 0));

        // Instructions label - positioned at top left without affecting layout
        Label instructionsLabel = new Label("ESC = Main Menu   F2 = Reset");
        instructionsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 10px; -fx-background-radius: 5px;");
        
        HBox left = new HBox(instructionsLabel);
        left.setAlignment(Pos.BOTTOM_LEFT);
        left.setPadding(new Insets(20, 0, 0, 24));

        ctx.overlay.getChildren().addAll(center, right, left);
        StackPane.setAlignment(center, Pos.TOP_CENTER);
        StackPane.setAlignment(right, Pos.TOP_RIGHT);
        StackPane.setAlignment(left, Pos.BOTTOM_LEFT);
    }


    private void buildLeftFields() {
        ctx.leftContainer = new VBox();
        ctx.leftContainer.setAlignment(Pos.TOP_LEFT);
        ctx.leftContainer.setPadding(new Insets(20));
        ctx.leftContainer.setSpacing(12);
        ctx.leftContainer.setPrefWidth(520);

        ctx.priceStack = new VBox(8);
        ctx.priceStack.setAlignment(Pos.TOP_LEFT);
        ctx.price1Field = makeField("Price 1 (per ton)", 220);
        ctx.price2Field = makeField("Price 2 (per ton)", 220);
        ctx.priceStack.getChildren().addAll(ctx.price1Field, ctx.price2Field);
        ctx.price1Field.setDisable(true);
        ctx.price2Field.setDisable(true);

        Region spacer = new Region();
        spacer.prefHeightProperty().bind(ctx.root.heightProperty().multiply(0.12)); // Reduced from 0.15

        ctx.mainWeightField = makeField("Main Weight (kg)", 440);

        ctx.leftContainer.getChildren().addAll(ctx.priceStack, spacer, ctx.mainWeightField);
        ctx.root.setLeft(ctx.leftContainer);
    }


    private TextField makeField(String prompt, double width) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(width);
        tf.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get()), ctx.fontSize));
        tf.setStyle("-fx-border-color: black; -fx-border-width: 3; -fx-background-color: white;");
        return tf;
    }

    private void buildRightFields() {
        ctx.rightContainer = new VBox(12);
        ctx.rightContainer.setPadding(new Insets(20, 60, 40, 40));
        ctx.rightContainer.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        spacer.prefHeightProperty().bind(ctx.scene.heightProperty().multiply(0.22)); // Reduced from 0.22
        ctx.rightContainer.getChildren().add(spacer);

        ctx.swArea = new VBox(12);
        ctx.swArea.setAlignment(Pos.TOP_LEFT);
        ctx.rightContainer.getChildren().add(ctx.swArea);

        ctx.totalsArea = new VBox(12);
        ctx.totalsArea.setAlignment(Pos.CENTER);
        ctx.totalsArea.setVisible(false);
        ctx.totalsArea.setManaged(false);
        ctx.rightContainer.getChildren().add(ctx.totalsArea);

        ctx.rightScrollPane = new ScrollPane(ctx.rightContainer);
        ctx.rightScrollPane.setFitToWidth(true);
        ctx.rightScrollPane.setFitToHeight(true);
        ctx.rightScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ctx.rightScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        ctx.rightScrollPane.setStyle(
            "-fx-background: white;" +
            "-fx-background-color: white;" +
            "-fx-border-width: 0;" +
            "-fx-border-color: transparent;" +
            "-fx-box-border: transparent;"
        );

        // remove border of internal viewport
        Platform.runLater(() ->
            ctx.rightScrollPane.lookup(".viewport").setStyle("-fx-background-color: white;")
        );


        ctx.root.setCenter(ctx.rightScrollPane);
    }


    private void buildBottomButtons() {
        ctx.saveButton = new Button("SAVE");
        ctx.resetButton = new Button("RESET");
        ctx.saveButton.setPrefWidth(200);
        ctx.resetButton.setPrefWidth(200);
        ctx.saveButton.setPrefHeight(50);
        ctx.resetButton.setPrefHeight(50);

        // Professional styling for buttons
        ctx.saveButton.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font("System", javafx.scene.text.FontWeight.BOLD, ctx.fontSize.get() * 0.8), ctx.fontSize));
        ctx.resetButton.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font("System", javafx.scene.text.FontWeight.BOLD, ctx.fontSize.get() * 0.8), ctx.fontSize));

        ctx.resetButton.setStyle(
                    "-fx-background-color: #E74C3C;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
                );
        // Focus effects (selected border)
        ctx.saveButton.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                ctx.saveButton.setStyle(
                    "-fx-background-color: #1ABC9C;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 3;" +
                    "-fx-border-color: #2C3E50;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 3);"
                );
            } else {
                ctx.saveButton.setStyle(
                    "-fx-background-color: #1ABC9C;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
                );
            }
        });

        ctx.resetButton.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                ctx.resetButton.setStyle(
                    "-fx-background-color: #E74C3C;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 3;" +
                    "-fx-border-color: #2C3E50;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 3);"
                );
            } else {
                ctx.resetButton.setStyle(
                    "-fx-background-color: #E74C3C;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
                );
            }
        });

        ctx.bottomButtons = new HBox(20, ctx.saveButton, ctx.resetButton);
        ctx.bottomButtons.setAlignment(Pos.BOTTOM_RIGHT);
        ctx.bottomButtons.setPadding(new Insets(0, 50, 40, 0));
        ctx.bottomButtons.setVisible(false);
        ctx.bottomButtons.setManaged(false);

        ctx.overlay.getChildren().add(ctx.bottomButtons);
        StackPane.setAlignment(ctx.bottomButtons, Pos.BOTTOM_RIGHT);

        ctx.saveButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                startSaveWithLoader();
            }
            if (e.getCode() == KeyCode.RIGHT) {
                ctx.resetButton.requestFocus();
            }
        });

        ctx.resetButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                resetAll();
            }
            if (e.getCode() == KeyCode.LEFT) {
                ctx.saveButton.requestFocus();
            }
        });
    }

    private void buildLoader() {
        ctx.loaderOverlay = new StackPane();
        ctx.loaderOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        ctx.loaderOverlay.setVisible(false);
        ctx.loaderOverlay.setManaged(false);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(120, 120);
        ctx.loaderOverlay.getChildren().add(pi);

        ctx.overlay.getChildren().add(ctx.loaderOverlay);
    }

    // ---------- ENTER FLOW ----------
    private void handleEnter(Stage stage) {
        var focused = stage.getScene().getFocusOwner();

        if (focused == ctx.partyField) { partySelector.handlePartyEnter(); return; }
        if (focused == ctx.truckField) { ctx.mainWeightField.requestFocus(); return; }
        if (focused == ctx.mainWeightField) {
            swManager.addSwLiveField();
            ctx.swLive.get(0).requestFocus();
            return;
        }

        if (!ctx.subweightsFinished) {
            for (TextField sw : ctx.swLive) {
                if (focused == sw) {
                    swManager.handleEnterOnSw(sw);
                    return;
                }
            }
        }

        if (focused == ctx.price1Field) {
            ctx.price2Field.requestFocus();
            return;
        }
        if (focused == ctx.price2Field) {
            for (TextField q : ctx.qualityFields) {
                if (q != null && q.isVisible()) {
                    q.requestFocus();
                    return;
                }
            }
            return;
        }

        for (int i = 0; i < ctx.qualityFields.size(); i++) {
            TextField q = ctx.qualityFields.get(i);
            if (q != null && focused == q) {
                priceEngine.computeRowPrice(i);
                int next = nextVisibleQuality(i);
                if (next >= 0) ctx.qualityFields.get(next).requestFocus();
                else {
                    priceEngine.showTotals();
                }
                return;
            }
        }

        if (focused == ctx.gstField) {
            showBottomButtons();
            Platform.runLater(() -> ctx.saveButton.requestFocus());
        }

        if (focused == ctx.saveButton) {
            startSaveWithLoader();
        }

        if (focused == ctx.resetButton) {
            resetAll();
        }
    }

    private int nextVisibleQuality(int from) {
        for (int j = from + 1; j < ctx.qualityFields.size(); j++) {
            TextField tf = ctx.qualityFields.get(j);
            if (tf != null && tf.isVisible()) return j;
        }
        return -1;
    }

    private void showBottomButtons() {
        ctx.bottomButtons.setVisible(true);
        ctx.bottomButtons.setManaged(true);
    }

    // ---------- SAVE + RESET ----------
    private void startSaveWithLoader() {
        ctx.loaderOverlay.setVisible(true);
        ctx.loaderOverlay.setManaged(true);

        new Thread(() -> {
            saver.saveSlip();
            Platform.runLater(() -> {
                ctx.loaderOverlay.setVisible(false);
                ctx.loaderOverlay.setManaged(false);
                resetAll();
            });
        }).start();
    }

    private void resetAll() {
        ctx.partyField.clear();
        ctx.truckField.clear();
        ctx.price1Field.clear();
        ctx.price2Field.clear();
        ctx.mainWeightField.clear();

        ctx.swLive.clear();
        ctx.swFields.clear();
        ctx.priceFields.clear();
        ctx.qualityFields.clear();
        ctx.dustDiscountBox = null;
        ctx.subweightsFinished = false;

        ctx.rightContainer.getChildren().clear();
        Region spacer = new Region();
        spacer.prefHeightProperty().bind(ctx.scene.heightProperty().multiply(0.22));
        ctx.rightContainer.getChildren().add(spacer);

        ctx.swArea = new VBox(12);
        ctx.swArea.setAlignment(Pos.TOP_LEFT);
        ctx.rightContainer.getChildren().add(ctx.swArea);

        ctx.totalsArea = new VBox(12);
        ctx.totalsArea.setAlignment(Pos.CENTER);
        ctx.totalsArea.setVisible(false);
        ctx.totalsArea.setManaged(false);
        ctx.rightContainer.getChildren().add(ctx.totalsArea);

        ctx.bottomButtons.setVisible(false);
        ctx.bottomButtons.setManaged(false);

        Platform.runLater(() -> ctx.partyField.requestFocus());
    }

    // private void confirmExit(Stage stage) {
    //     Alert a = new Alert(Alert.AlertType.CONFIRMATION);
    //     a.setTitle("Exit SlipPlus");
    //     a.setHeaderText(null);
    //     a.setContentText("Are you sure you want to exit?");
    //     a.initOwner(stage);
    //     a.showAndWait().ifPresent(res -> {
    //         if (res == ButtonType.OK) {
    //             stage.close();
    //             System.exit(0);
    //         }
    //     });
    // }
}
