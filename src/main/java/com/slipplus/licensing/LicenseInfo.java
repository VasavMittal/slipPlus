package com.slipplus.licensing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LicenseInfo {
    private String companyName;
    private String systemId;
    private boolean trialLicense;
    private LocalDateTime expiryDate;
    private LocalDateTime activationDate;
    
    public LicenseInfo(String companyName, String systemId, boolean trialLicense, LocalDateTime expiryDate) {
        this.companyName = companyName;
        this.systemId = systemId;
        this.trialLicense = trialLicense;
        this.expiryDate = expiryDate;
        this.activationDate = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        if (!trialLicense) {
            return false; // Lifetime license never expires
        }
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    public long getDaysRemaining() {
        if (!trialLicense) {
            return Long.MAX_VALUE; // Lifetime
        }
        return java.time.Duration.between(LocalDateTime.now(), expiryDate).toDays();
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return companyName + "|" + systemId + "|" + trialLicense + "|" + 
               (expiryDate != null ? expiryDate.format(formatter) : "LIFETIME") + "|" + 
               activationDate.format(formatter);
    }
    
    public static LicenseInfo fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid license data format");
        }
        
        String companyName = parts[0];
        String systemId = parts[1];
        boolean trialLicense = Boolean.parseBoolean(parts[2]);
        LocalDateTime expiryDate = null;
        
        if (!"LIFETIME".equals(parts[3])) {
            expiryDate = LocalDateTime.parse(parts[3], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        return new LicenseInfo(companyName, systemId, trialLicense, expiryDate);
    }
    
    // Getters
    public String getCompanyName() { return companyName; }
    public String getSystemId() { return systemId; }
    public boolean isTrialLicense() { return trialLicense; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public LocalDateTime getActivationDate() { return activationDate; }
}