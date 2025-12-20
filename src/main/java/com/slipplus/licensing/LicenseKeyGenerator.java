package com.slipplus.licensing;

import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Utility class for generating license keys (for your use only)
 */
public class LicenseKeyGenerator {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== SlipPlus License Key Generator ===");
        System.out.println();
        
        // Get system ID from user
        System.out.print("Enter System ID (from customer): ");
        String systemId = scanner.nextLine().trim();
        
        // Get company name
        System.out.print("Enter Company Name: ");
        String companyName = scanner.nextLine().trim();
        
        // Ask for license type
        System.out.print("License Type (1=Trial, 2=Lifetime): ");
        String choice = scanner.nextLine().trim();
        
        if ("1".equals(choice)) {
            // Generate trial key
            System.out.print("Trial days (default 4): ");
            String daysStr = scanner.nextLine().trim();
            int days = daysStr.isEmpty() ? 4 : Integer.parseInt(daysStr);
            
            LocalDateTime trialExpiry = LocalDateTime.now().plusDays(days);
            String trialKey = LicenseManager.generateLicenseKey(companyName, systemId, true, trialExpiry);
            
            System.out.println();
            System.out.println("=== TRIAL LICENSE KEY ===");
            System.out.println("Company: " + companyName);
            System.out.println("System ID: " + systemId);
            System.out.println("Trial Days: " + days);
            System.out.println("Expires: " + trialExpiry.toLocalDate());
            System.out.println();
            System.out.println("LICENSE KEY:");
            System.out.println(trialKey);
            
        } else if ("2".equals(choice)) {
            // Generate lifetime key
            String lifetimeKey = LicenseManager.generateLicenseKey(companyName, systemId, false, null);
            
            System.out.println();
            System.out.println("=== LIFETIME LICENSE KEY ===");
            System.out.println("Company: " + companyName);
            System.out.println("System ID: " + systemId);
            System.out.println("Type: Lifetime");
            System.out.println();
            System.out.println("LICENSE KEY:");
            System.out.println(lifetimeKey);
        }
        
        scanner.close();
    }
}
