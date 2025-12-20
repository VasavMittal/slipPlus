package com.slipplus.licensing;

public class LicenseValidationResult {
    private boolean valid;
    private String message;
    
    public LicenseValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
    
    public boolean isValid() { return valid; }
    public String getMessage() { return message; }
}