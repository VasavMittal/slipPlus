package com.slipplus.screens.subSlip;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

class PriceCalculationEngine {

    private final SlipContext ctx;

    PriceCalculationEngine(SlipContext ctx) {
        this.ctx = ctx;
    }

    void computeRowPrice(int index) {
        if (index < 0 || index >= ctx.priceFields.size()) return;

        TextField sw = ctx.swFields.get(index);
        TextField priceOut = ctx.priceFields.get(index);
        TextField q = ctx.qualityFields.get(index);

        if (sw == null || priceOut == null || q == null) return;

        double swKg = safeParse(sw.getText());
        if (swKg <= 0) return;

        double p1 = safeParse(ctx.price1Field.getText());
        double p2 = safeParse(ctx.price2Field.getText());
        double qv = safeParse(q.getText());

        double rowDisplayRate = p1 + p2 + qv;
        priceOut.setText(ctx.moneyFmt.format(rowDisplayRate));

        double actualPrice = rowDisplayRate * (swKg / 1000.0);
        priceOut.setUserData(actualPrice);

        updateTotalsIfVisible();
    }

    void recomputeAllRows() {
        for (int i = 0; i < ctx.priceFields.size(); i++) {
            TextField pf = ctx.priceFields.get(i);
            TextField q = (i < ctx.qualityFields.size()) ? ctx.qualityFields.get(i) : null;
            if (pf != null && q != null && q.isVisible()) {
                computeRowPrice(i);
            }
        }
    }

    void showTotals() {
        updateTotalsIfVisible();

        ctx.totalsArea.getChildren().clear();

        // ---- TOP LINE ----
        Region topLine = new Region();
        topLine.setPrefHeight(6);
        topLine.setMaxWidth(Double.MAX_VALUE);
        topLine.setStyle("-fx-background-color: black;");

        // ---- TOTAL VALUE ----
        Label totalVal = new Label(totalsFormatted());
        totalVal.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get()), ctx.fontSize));
        totalVal.setAlignment(javafx.geometry.Pos.CENTER);

        // ---- GST FIELD ----
        ctx.gstField = new TextField();
        ctx.gstField.setPromptText("GST Amount (flat)");
        ctx.gstField.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get() * 0.9), ctx.fontSize));
        ctx.gstField.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: white;");
        ctx.gstField.setAlignment(javafx.geometry.Pos.CENTER);
        ctx.gstField.setMaxWidth(260);

        // ---- BOTTOM LINE ----
        Region bottomLine = new Region();
        bottomLine.setPrefHeight(6);
        bottomLine.setMaxWidth(Double.MAX_VALUE);
        bottomLine.setStyle("-fx-background-color: black;");

        // ---- FINAL VALUE ----
        Label finalVal = new Label(finalFormatted());
        finalVal.setId("finalValue");
        finalVal.fontProperty().bind(Bindings.createObjectBinding(
                () -> Font.font(ctx.fontSize.get()), ctx.fontSize));
        finalVal.setAlignment(javafx.geometry.Pos.CENTER);

        // Add all (EXACT ORDER same as demo)
        ctx.totalsArea.getChildren().addAll(
                topLine, totalVal, ctx.gstField, bottomLine, finalVal
        );

        ctx.totalsArea.setVisible(true);
        ctx.totalsArea.setManaged(true);

        ctx.gstField.setOnKeyReleased(e -> updateTotalsIfVisible());
        ctx.gstField.requestFocus();
    }


    void updateTotalsIfVisible() {
        if (!ctx.totalsArea.isVisible()) return;
        if (!ctx.totalsArea.getChildren().isEmpty()) {
            Label totalVal = (Label) ctx.totalsArea.getChildren().get(0);
            totalVal.setText(totalsFormatted());
        }
        Label finalVal = (Label) ctx.totalsArea.lookup("#finalValue");
        if (finalVal != null) finalVal.setText(finalFormatted());
    }

    double totalsValue() {
        double total = 0;
        for (TextField pf : ctx.priceFields) {
            if (pf == null || !pf.isVisible()) continue;
            Object ud = pf.getUserData();
            if (ud instanceof Double d) total += d;
        }

        if (ctx.dustDiscountBox != null) {
            String v = ctx.dustDiscountBox.getValue();
            double pct = 0;
            if ("1.5".equals(v)) pct = 1.5;
            else if ("1".equals(v)) pct = 1;
            double discount = total * (pct / 100.0);
            total -= discount;
        }
        return total;
    }

    double finalValue() {
        double gst = safeParse(ctx.gstField != null ? ctx.gstField.getText() : "0");
        return totalsValue() + gst;
    }

    String totalsFormatted() {
        return ctx.moneyFmt.format(totalsValue());
    }

    String finalFormatted() {
        return ctx.moneyFmt.format(finalValue());
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
