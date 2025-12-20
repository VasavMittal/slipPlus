package com.slipplus.screens.purchaseBook;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class PurchaseBookRow {
    private String partyName;
    private double mainWeight;
    private double subWeight; // Single sub-weight for this row
    private double calculatedPrice; // Single calculated price for this row
    private Map<String, Double> shortcutAmounts;
    private double totalBeforeGst;
    private double gst;
    private double finalAmount;
    private boolean isFirstRowOfParty;
    private boolean isFirstRowOfSlip;
    private int totalSubWeights;
    
    private DecimalFormat moneyFormat = new DecimalFormat("#,##0");
    
    public PurchaseBookRow(String partyName, double mainWeight, double subWeight, 
                          double calculatedPrice, Map<String, Double> shortcutAmounts,
                          double totalBeforeGst, double gst, double finalAmount,
                          boolean isFirstRowOfParty, boolean isFirstRowOfSlip, int totalSubWeights) {
        this.partyName = partyName;
        this.mainWeight = mainWeight;
        this.subWeight = subWeight;
        this.calculatedPrice = calculatedPrice;
        this.shortcutAmounts = shortcutAmounts;
        this.totalBeforeGst = totalBeforeGst;
        this.gst = gst;
        this.finalAmount = finalAmount;
        this.isFirstRowOfParty = isFirstRowOfParty;
        this.isFirstRowOfSlip = isFirstRowOfSlip;
        this.totalSubWeights = totalSubWeights;
    }
    
    public String getSubWeightDisplay() {
        return moneyFormat.format(subWeight);
    }
    
    public String getCalculatedPriceDisplay() {
        return moneyFormat.format(calculatedPrice);
    }
    
    // Getters
    public String getPartyName() { return partyName; }
    public double getMainWeight() { return mainWeight; }
    public double getSubWeight() { return subWeight; }
    public double getCalculatedPrice() { return calculatedPrice; }
    public Map<String, Double> getShortcutAmounts() { return shortcutAmounts; }
    public double getTotalBeforeGst() { return totalBeforeGst; }
    public double getGst() { return gst; }
    public double getFinalAmount() { return finalAmount; }
    public boolean isFirstRowOfParty() { return isFirstRowOfParty; }
    public boolean isFirstRowOfSlip() { return isFirstRowOfSlip; }
    public int getTotalSubWeights() { return totalSubWeights; }
}
