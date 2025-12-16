package com.slipplus.models;

import java.util.List;

public class SubSlip {
    private String partyName;
    private String truckNumber;
    private double mainWeight;
    private List<Double> subWeights;
    private double price1;
    private double price2;
    private List<Double> qualityValues;
    private List<Double> calculatedPrices;
    private String dustDiscount;
    private double totalBeforeGst;
    private double gst;
    private double finalAmount;

    public SubSlip() {}

    public SubSlip(String partyName,
                   String truckNumber,
                   double mainWeight,
                   List<Double> subWeights,
                   double price1,
                   double price2,
                   List<Double> qualityValues,
                   List<Double> calculatedPrices,
                   String dustDiscount,
                   double totalBeforeGst,
                   double gst,
                   double finalAmount) {
        this.partyName = partyName;
        this.truckNumber = truckNumber;
        this.mainWeight = mainWeight;
        this.subWeights = subWeights;
        this.price1 = price1;
        this.price2 = price2;
        this.qualityValues = qualityValues;
        this.calculatedPrices = calculatedPrices;
        this.dustDiscount = dustDiscount;
        this.totalBeforeGst = totalBeforeGst;
        this.gst = gst;
        this.finalAmount = finalAmount;
    }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public String getTruckNumber() { return truckNumber; }
    public void setTruckNumber(String truckNumber) { this.truckNumber = truckNumber; }

    public double getMainWeight() { return mainWeight; }
    public void setMainWeight(double mainWeight) { this.mainWeight = mainWeight; }

    public List<Double> getSubWeights() { return subWeights; }
    public void setSubWeights(List<Double> subWeights) { this.subWeights = subWeights; }

    public double getPrice1() { return price1; }
    public void setPrice1(double price1) { this.price1 = price1; }

    public double getPrice2() { return price2; }
    public void setPrice2(double price2) { this.price2 = price2; }

    public List<Double> getQualityValues() { return qualityValues; }
    public void setQualityValues(List<Double> qualityValues) { this.qualityValues = qualityValues; }

    public List<Double> getCalculatedPrices() { return calculatedPrices; }
    public void setCalculatedPrices(List<Double> calculatedPrices) { this.calculatedPrices = calculatedPrices; }

    public String getDustDiscount() { return dustDiscount; }
    public void setDustDiscount(String dustDiscount) { this.dustDiscount = dustDiscount; }

    public double getTotalBeforeGst() { return totalBeforeGst; }
    public void setTotalBeforeGst(double totalBeforeGst) { this.totalBeforeGst = totalBeforeGst; }

    public double getGst() { return gst; }
    public void setGst(double gst) { this.gst = gst; }

    public double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(double finalAmount) { this.finalAmount = finalAmount; }
}
