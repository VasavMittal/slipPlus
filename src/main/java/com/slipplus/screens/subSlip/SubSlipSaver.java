package com.slipplus.screens.subSlip;

import com.slipplus.core.StorageManager;
import com.slipplus.models.SubSlip;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class SubSlipSaver {

    private final SlipContext ctx;
    private final PriceCalculationEngine priceEngine;

    SubSlipSaver(SlipContext ctx, PriceCalculationEngine priceEngine) {
        this.ctx = ctx;
        this.priceEngine = priceEngine;
    }

    void saveSlip() {
        try {
            String partyName = ctx.partyField.getText().trim();
            String truck = ctx.truckField.getText().trim();
            double mainWeight = parse(ctx.mainWeightField.getText());
            double price1 = parse(ctx.price1Field.getText());
            double price2 = parse(ctx.price2Field.getText());
            double totalBeforeGst = priceEngine.totalsValue();
            double gst = parse(ctx.gstField != null ? ctx.gstField.getText() : "0");
            double finalAmount = totalBeforeGst + gst;

            if (partyName.isEmpty() || mainWeight <= 0 || totalBeforeGst <= 0) {
                return;
            }

            List<Double> subWeights = new ArrayList<>();
            for (var sw : ctx.swFields) {
                subWeights.add(parse(sw.getText()));
            }

            List<Double> qualityValues = new ArrayList<>();
            for (var q : ctx.qualityFields) {
                if (q != null && q.isVisible()) qualityValues.add(parse(q.getText()));
                else qualityValues.add(0.0);
            }

            String dustDiscount = (ctx.dustDiscountBox != null && ctx.dustDiscountBox.getValue() != null)
                    ? ctx.dustDiscountBox.getValue()
                    : "";

            SubSlip slip = new SubSlip(
                    partyName,
                    truck,
                    mainWeight,
                    subWeights,
                    price1,
                    price2,
                    qualityValues,
                    dustDiscount,
                    totalBeforeGst,
                    gst,
                    finalAmount
            );

            String partyKey = (ctx.selectedParty != null)
                    ? String.valueOf(ctx.selectedParty.getId())
                    : partyName;

            StorageManager.saveSubSlip(LocalDate.now(), partyKey, slip);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double parse(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s.replace(",", "")); }
        catch (Exception e) { return 0; }
    }
}
