package com.slipplus.licensing;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.prefs.Preferences;

public class LicenseManager {
    
    private static final String LICENSE_FILE = "system.lic";
    private static final String COMPANY_KEY = "company_name";
    private static final String LICENSE_KEY = "license_data";
    private static final String SYSTEM_ID_KEY = "system_id";
    
    // Master encryption key (in production, this should be obfuscated)
    private static final String MASTER_KEY = "SlipPlus2024SecureKey!@#$%";
    
    private static LicenseManager instance;
    private LicenseInfo currentLicense;
    
    private LicenseManager() {}
    
    public static LicenseManager getInstance() {
        if (instance == null) {
            instance = new LicenseManager();
        }
        return instance;
    }
    
    public LicenseValidationResult validateLicense() {
        try {
            // Check if license file exists
            Path licensePath = Paths.get(LICENSE_FILE);
            if (!Files.exists(licensePath)) {
                return new LicenseValidationResult(false, "No license found. Please enter your license key.");
            }
            
            // Load and decrypt license
            String encryptedData = Files.readString(licensePath);
            String decryptedData = decrypt(encryptedData);
            
            currentLicense = LicenseInfo.fromString(decryptedData);
            
            // Validate system ID
            String currentSystemId = generateSystemId();
            if (!currentSystemId.equals(currentLicense.getSystemId())) {
                return new LicenseValidationResult(false, "License is not valid for this system. Please contact support for a new license.");
            }
            
            // Check if trial expired
            if (currentLicense.isTrialLicense() && currentLicense.isExpired()) {
                return new LicenseValidationResult(false, "Trial period has expired. Please enter a lifetime license key.");
            }
            
            return new LicenseValidationResult(true, "License valid");
            
        } catch (Exception e) {
            return new LicenseValidationResult(false, "License validation failed: " + e.getMessage());
        }
    }
    
    public LicenseValidationResult activateLicense(String companyName, String licenseKey) {
        try {
            // Validate the license key format and signature
            LicenseKeyInfo keyInfo = validateLicenseKey(licenseKey);
            if (keyInfo == null) {
                return new LicenseValidationResult(false, "Invalid license key format.");
            }
            
            // Check if company name matches - convert entered name to company code format
            String enteredCompanyCode = companyName.toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0, Math.min(companyName.length(), 8));
            if (!keyInfo.getCompanyName().equals(enteredCompanyCode)) {
                System.out.println("Company mismatch. Key: " + keyInfo.getCompanyName() + ", Entered: " + enteredCompanyCode);
                return new LicenseValidationResult(false, "License key does not match the company name.");
            }
            
            // Check if system ID matches
            String currentSystemId = generateSystemId();
            if (!keyInfo.getSystemId().equals(currentSystemId)) {
                return new LicenseValidationResult(false, "License key is not valid for this system.");
            }
            
            // Create license info - use the original company name, not the code
            currentLicense = new LicenseInfo(
                companyName.trim(),
                currentSystemId,
                keyInfo.isTrialLicense(),
                keyInfo.getExpiryDate()
            );
            
            // Save encrypted license with better error handling
            String licenseData = currentLicense.toString();
            String encryptedData = encrypt(licenseData);
            
            Path licensePath = Paths.get(LICENSE_FILE).toAbsolutePath();
            System.out.println("Saving license to: " + licensePath);
            Files.writeString(licensePath, encryptedData);
            
            // Store company name in registry for quick access
            Preferences prefs = Preferences.userNodeForPackage(LicenseManager.class);
            prefs.put(COMPANY_KEY, companyName.trim());
            
            return new LicenseValidationResult(true, "License activated successfully!");
            
        } catch (Exception e) {
            System.out.println("License activation error details: " + e.getMessage());
            e.printStackTrace();
            return new LicenseValidationResult(false, "License activation failed: " + e.getMessage());
        }
    }
    
    public String getStoredCompanyName() {
        Preferences prefs = Preferences.userNodeForPackage(LicenseManager.class);
        return prefs.get(COMPANY_KEY, null);
    }
    
    private static String cachedSystemId = null;

    public String generateSystemId() {
        if (cachedSystemId != null) {
            return cachedSystemId;
        }
        
        try {
            // Combine multiple system identifiers for stronger binding
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String userName = System.getProperty("user.name");
            String userHome = System.getProperty("user.home");
            String javaVersion = System.getProperty("java.version");
            
            // Get processor info if available
            String processorInfo = "";
            try {
                processorInfo = System.getenv("PROCESSOR_IDENTIFIER");
                if (processorInfo == null) {
                    processorInfo = System.getenv("PROCESSOR_ARCHITECTURE");
                }
                if (processorInfo == null) {
                    processorInfo = "unknown";
                }
            } catch (Exception e) {
                processorInfo = "unknown";
            }
            
            String combined = osName + osVersion + userName + userHome + javaVersion + processorInfo;
            
            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            
            // Convert to hex string and take first 16 characters
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            cachedSystemId = hexString.toString().substring(0, 16).toUpperCase();
            return cachedSystemId;
            
        } catch (Exception e) {
            // Fallback system ID
            return "FALLBACK_SYSTEM_ID";
        }
    }
    
    private LicenseKeyInfo validateLicenseKey(String licenseKey) {
        try {
            System.out.println("Validating license key: " + licenseKey);
            
            // Expected format: COMPANY_SYSTEMID_TYPE_EXPIRY_SIGNATURE
            String[] parts = licenseKey.split("_");
            System.out.println("Key parts count: " + parts.length);
            
            if (parts.length < 5) { // Changed from 4 to 5
                System.out.println("Invalid parts count. Expected 5, got: " + parts.length);
                return null;
            }
            
            String companyCode = parts[0];
            String systemId = parts[1];
            String licenseType = parts[2];
            String expiryOrLifetime = parts[3];
            String providedSignature = parts[4]; // Now correctly gets the signature
            
            // Validate signature
            String expectedSignature = generateSignature(companyCode, systemId, licenseType, expiryOrLifetime);
            
            if (!expectedSignature.equals(providedSignature)) {
                System.out.println("Signature mismatch. Expected: " + expectedSignature + ", Got: " + providedSignature);
                return null;
            }
            
            boolean isTrialLicense = "TRIAL".equals(licenseType);
            LocalDateTime expiryDate = null;
            
            if (isTrialLicense) {
                // Parse expiry date for trial - fix the date format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate expiryLocalDate = LocalDate.parse(expiryOrLifetime, formatter);
                expiryDate = expiryLocalDate.atTime(23, 59, 59);
            }
            
            return new LicenseKeyInfo(companyCode, systemId, isTrialLicense, expiryDate);
            
        } catch (Exception e) {
            System.out.println("License validation error: " + e.getMessage());
            return null;
        }
    }
    
    private String generateSignature(String companyCode, String systemId, String licenseType, String expiryOrLifetime) {
        try {
            String data = companyCode + systemId + licenseType + expiryOrLifetime + MASTER_KEY;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 8);
        } catch (Exception e) {
            return "INVALID";
        }
    }
    
    private String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(MASTER_KEY.getBytes(), 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    private String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(MASTER_KEY.getBytes(), 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decrypted);
    }
    
    public LicenseInfo getCurrentLicense() {
        return currentLicense;
    }
    
    public boolean isLicenseValid() {
        return validateLicense().isValid();
    }
    
    // Method to generate license keys (for your use)
    public static String generateLicenseKey(String companyName, String systemId, boolean isTrial, LocalDateTime expiryDate) {
        try {
            String companyCode = companyName.toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0, Math.min(companyName.length(), 8));
            String licenseType = isTrial ? "TRIAL" : "LIFETIME";
            String expiryOrLifetime = isTrial ? expiryDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "LIFETIME";
            
            LicenseManager manager = new LicenseManager();
            String signature = manager.generateSignature(companyCode, systemId, licenseType, expiryOrLifetime);
            
            return companyCode + "_" + systemId + "_" + licenseType + "_" + expiryOrLifetime + "_" + signature;
            
        } catch (Exception e) {
            return null;
        }
    }
}





