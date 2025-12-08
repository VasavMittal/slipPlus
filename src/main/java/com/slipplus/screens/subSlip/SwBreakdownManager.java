package com.slipplus.screens.subSlip;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

class SwBreakdownManager {

    private final SlipContext ctx;
    private final PriceCalculationEngine priceEngine;

    SwBreakdownManager(SlipContext ctx, PriceCalculationEngine priceEngine) {
        this.ctx = ctx;
        this.priceEngine = priceEngine;
    }

    void addSwLiveField() {
        TextField sw = makeField("", 360);
        HBox row = new HBox(12, sw);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        ctx.swArea.getChildren().add(row);
        ctx.swLive.add(sw);
        Platform.runLater(sw::requestFocus);
    }

    void handleEnterOnSw(TextField focused) {
        if (ctx.subweightsFinished) return;

        for (int i = 0; i < ctx.swLive.size(); i++) {
            TextField sw = ctx.swLive.get(i);
            if (sw == focused) {
                String t = sw.getText().trim();
                if (t.equals("0") || t.equals("0.0") || t.equals("0.00")) {
                    finishSubweightsFromLive(i);
                    return;
                }
                if (i == ctx.swLive.size() - 1) addSwLiveField();
                else ctx.swLive.get(i + 1).requestFocus();
                return;
            }
        }
    }

    // ---- original-style finishing logic, simplified but same behavior ----
    private void finishSubweightsFromLive(int sentinelIndex) {
        List<String> raw = new ArrayList<>();
        for (int i = 0; i < sentinelIndex; i++) {
            raw.add(ctx.swLive.get(i).getText().trim());
        }
        finishSubweightsFromRaw(raw);
    }

    private void finishSubweightsFromRaw(List<String> raw) {
        ctx.subweightsFinished = true;

        List<Double> entries = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.trim().isEmpty()) entries.add(null);
            else {
                try {
                    entries.add(Double.parseDouble(s.replace(",", "")));
                } catch (Exception ex) {
                    entries.add(null);
                }
            }
        }

        int lastNonNullIndex = -1;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i) != null) {
                lastNonNullIndex = i;
                break;
            }
        }

        double mainKg = safeParse(ctx.mainWeightField.getText());

        if (entries.isEmpty() || lastNonNullIndex == -1) {
            // everything is dust
            ctx.swArea.getChildren().clear();
            ctx.swFields.clear();
            ctx.priceFields.clear();
            ctx.qualityFields.clear();

            TextField dust = makeReadOnly(360, ctx.moneyFmt.format(mainKg));
            HBox row = new HBox(12, dust);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ctx.swArea.getChildren().add(row);

            ctx.swFields.add(dust);
            ctx.priceFields.add(null);
            ctx.qualityFields.add(null);

            ctx.price1Field.setDisable(false);
            ctx.price2Field.setDisable(false);
            ctx.price1Field.requestFocus();
            return;
        }

        double dustValue = entries.get(lastNonNullIndex);

        List<Double> preDust = new ArrayList<>();
        for (int i = 0; i < lastNonNullIndex; i++) {
            preDust.add(entries.get(i));
        }

        double sumPre = 0;
        int firstEmptyIndex = -1;
        for (int i = 0; i < preDust.size(); i++) {
            Double v = preDust.get(i);
            if (v == null) {
                if (firstEmptyIndex == -1) firstEmptyIndex = i;
            } else {
                sumPre += v;
            }
        }

        double sumWithDust = sumPre + dustValue;
        double remaining = mainKg - sumWithDust;
        if (remaining < 0) remaining = 0;

        if (firstEmptyIndex != -1) {
            preDust.set(firstEmptyIndex, remaining);
        } else {
            if (!preDust.isEmpty()) {
                Double v0 = preDust.get(0);
                if (v0 == null) v0 = 0.0;
                preDust.set(0, v0 + remaining);
            } else {
                preDust.add(remaining);
            }
        }

        List<Double> finalSWs = new ArrayList<>();
        for (Double d : preDust) finalSWs.add(d == null ? 0.0 : d);
        finalSWs.add(dustValue);

        ctx.swArea.getChildren().clear();
        ctx.swFields.clear();
        ctx.priceFields.clear();
        ctx.qualityFields.clear();

        for (int i = 0; i < finalSWs.size(); i++) {
            double val = finalSWs.get(i);
            boolean isDust = (i == finalSWs.size() - 1);

            TextField swR = makeReadOnly(360, ctx.moneyFmt.format(val));

            if (isDust) {
                Region mulEmpty = new Region();
                mulEmpty.setPrefWidth(30);
                Region priceEmpty = new Region();
                priceEmpty.setPrefWidth(220);

                ComboBox<String> discount = new ComboBox<>();
                discount.setEditable(true);
                discount.getItems().addAll("1.5", "N", "1");
                discount.setValue("1.5");
                discount.setPrefWidth(180);
                discount.setStyle("-fx-border-color: black; -fx-border-width: 3;");
                discount.getEditor().setFont(Font.font(ctx.fontSize.get() * 0.85));

                discount.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (newV) Platform.runLater(discount::show);
                });

                discount.setOnKeyPressed(ev -> {
                    switch (ev.getCode()) {
                        case DOWN, UP -> discount.show();
                        case ENTER -> {
                            String entered = discount.getEditor().getText();
                            if (!discount.getItems().contains(entered)) discount.setValue(entered);
                            priceEngine.updateTotalsIfVisible();
                            priceEngine.showTotals();
                            ev.consume();
                        }
                    }
                });

                discount.getEditor().setOnKeyPressed(ev -> {
                    if (ev.getCode() == KeyCode.ENTER) {
                        String entered = discount.getEditor().getText();
                        if (!discount.getItems().contains(entered)) discount.setValue(entered);
                        priceEngine.updateTotalsIfVisible();
                        priceEngine.showTotals();
                        ev.consume();
                    }
                });

                discount.valueProperty().addListener((o, ov, nv) -> priceEngine.updateTotalsIfVisible());
                discount.getEditor().textProperty().addListener((o, ov, nv) -> priceEngine.updateTotalsIfVisible());

                HBox row = new HBox(12, swR, mulEmpty, priceEmpty, discount);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                ctx.swArea.getChildren().add(row);

                ctx.swFields.add(swR);
                ctx.priceFields.add(null);
                ctx.qualityFields.add(discount.getEditor());
                ctx.dustDiscountBox = discount;
            } else {
                Label mul = makeMultiplyLabel();
                TextField priceRes = makePriceResult(220);
                TextField qual = makeQualityField(180);

                HBox row = new HBox(12, swR, mul, priceRes, qual);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                ctx.swArea.getChildren().add(row);

                ctx.swFields.add(swR);
                ctx.priceFields.add(priceRes);
                ctx.qualityFields.add(qual);
            }
        }

        ctx.price1Field.setDisable(false);
        ctx.price2Field.setDisable(false);
        ctx.price1Field.requestFocus();

        ctx.totalsArea.setVisible(false);
        ctx.totalsArea.setManaged(false);
    }

    // ---- helpers copied from original style ----
    private TextField makeField(String prompt, double width) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(width);
        tf.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get()), ctx.fontSize));
        tf.setStyle("-fx-border-color: black; -fx-border-width: 3; -fx-background-color: white;");
        return tf;
    }

    private TextField makeReadOnly(double width, String text) {
        TextField tf = new TextField(text);
        tf.setEditable(false);
        tf.setPrefWidth(width);
        tf.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get() * 0.85), ctx.fontSize));
        tf.setStyle("-fx-border-color: black; -fx-border-width: 3; -fx-background-color: white;");
        return tf;
    }

    private TextField makePriceResult(double width) {
        TextField tf = new TextField();
        tf.setEditable(false);
        tf.setPrefWidth(width);
        tf.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get() * 0.85), ctx.fontSize));
        tf.setStyle("-fx-border-color: black; -fx-border-width: 3; -fx-background-color: white;");
        tf.setText("");
        tf.setVisible(true);
        tf.setManaged(true);
        return tf;
    }

    private TextField makeQualityField(double width) {
        TextField tf = new TextField();
        tf.setPrefWidth(width);
        tf.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get() * 0.85), ctx.fontSize));
        tf.setStyle("-fx-border-color: black; -fx-border-width: 3; -fx-background-color: white;");
        return tf;
    }

    private Label makeMultiplyLabel() {
        Label l = new Label("×");
        l.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get() * 0.9), ctx.fontSize));
        l.setMinWidth(30);
        l.setAlignment(javafx.geometry.Pos.CENTER);
        return l;
    }

    private double safeParse(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
