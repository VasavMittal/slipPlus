package com.slipplus.licensing;

import java.time.LocalDateTime;

public class LicenseKeyInfo {
    private String companyName;
    private String systemId;
    private boolean trialLicense;
    private LocalDateTime expiryDate;
    
    public LicenseKeyInfo(String companyName, String systemId, boolean trialLicense, LocalDateTime expiryDate) {
        this.companyName = companyName;
        this.systemId = systemId;
        this.trialLicense = trialLicense;
        this.expiryDate = expiryDate;
    }
    
    // Getters
    public String getCompanyName() { return companyName; }
    public String getSystemId() { return systemId; }
    public boolean isTrialLicense() { return trialLicense; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
}