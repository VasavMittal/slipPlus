package com.slipplus.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slipplus.models.Party;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.slipplus.models.SubSlip;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import com.slipplus.models.Shortcut;
import com.slipplus.models.MainSlip;

public class StorageManager {

    private static final String PARTY_PATH = "src/main/resources/data/parties.json";
    private static final String SUB_SLIP_PATH = "src/main/resources/data/sub_slips.json";
    private static final String MAIN_SLIP_PATH = "src/main/resources/data/main_slips.json";
    private static final ObjectMapper mapper = createObjectMapper();
    private static final String DATA_DIR = "src/main/resources/data";

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static List<Party> loadParties() {
        try {
            File file = new File(PARTY_PATH);
            if (!file.exists()) return new ArrayList<>();
            return mapper.readValue(file, new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveParties(List<Party> list) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PARTY_PATH), list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Map<String, List<SubSlip>>> loadSubSlips() {
        try {
            File file = new File(SUB_SLIP_PATH);
            if (!file.exists()) return new HashMap<>();
            return mapper.readValue(file, new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void saveSubSlip(LocalDate date, String partyKey, SubSlip slip) {
        try {
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            String dateKey = date.toString();
            data.putIfAbsent(dateKey, new HashMap<>());
            Map<String, List<SubSlip>> byParty = data.get(dateKey);

            byParty.putIfAbsent(partyKey, new ArrayList<>());
            byParty.get(partyKey).add(slip);

            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(SUB_SLIP_PATH), data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<LocalDate> getAvailableDates() {
        try {
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            return data.keySet().stream()
                    .map(LocalDate::parse)
                    .distinct()
                    .sorted((d1, d2) -> d2.compareTo(d1)) // Latest first
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static List<String> getPartyIdsForDate(LocalDate date) {
        try {
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            String dateKey = date.toString();
            Map<String, List<SubSlip>> byParty = data.get(dateKey);
            
            if (byParty == null) return new ArrayList<>();
            
            return byParty.keySet().stream()
                    .sorted()
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static List<SubSlip> getSubSlipsForDateAndParty(LocalDate date, String partyName) {
        try {
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            String dateKey = date.toString();
            Map<String, List<SubSlip>> byParty = data.get(dateKey);
            
            if (byParty == null) return new ArrayList<>();
            
            // Debug output
            System.out.println("Looking for party: " + partyName + " on date: " + dateKey);
            System.out.println("Available party IDs: " + byParty.keySet());
            
            // Convert party name to ID for lookup
            String partyId = getPartyIdByName(partyName);
            System.out.println("Converted party name '" + partyName + "' to ID: " + partyId);
            
            List<SubSlip> slips = byParty.get(partyId);
            System.out.println("Found slips: " + (slips != null ? slips.size() : "null"));
            
            return slips != null ? new ArrayList<>(slips) : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void deleteSubSlips(LocalDate date, String partyKey, List<SubSlip> slipsToDelete) {
        try {
            System.out.println("Deleting slips for date: " + date + ", party: " + partyKey);
            System.out.println("Slips to delete count: " + slipsToDelete.size());
            
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            String dateKey = date.toString();
            System.out.println("Looking for dateKey: " + dateKey);
            
            Map<String, List<SubSlip>> byParty = data.get(dateKey);
            
            if (byParty != null) {
                System.out.println("Found date entry, parties: " + byParty.keySet());
                List<SubSlip> slips = byParty.get(partyKey);
                if (slips != null) {
                    System.out.println("Found party entry, slips count before: " + slips.size());
                    
                    // Remove the specific slips by comparing truck numbers and amounts
                    slips.removeIf(slip -> slipsToDelete.stream().anyMatch(toDelete -> 
                        slip.getTruckNumber().equals(toDelete.getTruckNumber()) && 
                        Math.abs(slip.getFinalAmount() - toDelete.getFinalAmount()) < 0.01));
                    
                    System.out.println("Slips count after removal: " + slips.size());
                    
                    // Remove empty party entry
                    if (slips.isEmpty()) {
                        byParty.remove(partyKey);
                        System.out.println("Removed empty party entry");
                    }
                    
                    // Remove empty date entry
                    if (byParty.isEmpty()) {
                        data.remove(dateKey);
                        System.out.println("Removed empty date entry");
                    }
                    
                    // Save the updated data back to JSON
                    File file = new File(SUB_SLIP_PATH);
                    System.out.println("Saving to file: " + file.getAbsolutePath());
                    mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
                    System.out.println("File saved successfully");
                } else {
                    System.out.println("Party not found: " + partyKey);
                }
            } else {
                System.out.println("Date not found: " + dateKey);
            }
        } catch (Exception e) {
            System.out.println("Error deleting slips: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void deleteAllSubSlipsForParty(LocalDate date, String partyKey) {
        try {
            System.out.println("Deleting ALL slips for date: " + date + ", party: " + partyKey);
            
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            String dateKey = date.toString();
            Map<String, List<SubSlip>> byParty = data.get(dateKey);
            
            if (byParty != null) {
                System.out.println("Found date entry, removing party: " + partyKey);
                byParty.remove(partyKey);
                
                // Remove empty date entry
                if (byParty.isEmpty()) {
                    data.remove(dateKey);
                    System.out.println("Removed empty date entry");
                }
                
                // Save the updated data back to JSON
                File file = new File(SUB_SLIP_PATH);
                System.out.println("Saving to file: " + file.getAbsolutePath());
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
                System.out.println("File saved successfully");
            } else {
                System.out.println("Date not found: " + dateKey);
            }
        } catch (Exception e) {
            System.out.println("Error deleting all slips: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getPartyNameById(String partyKey) {
        try {
            // First try to parse as ID
            int partyId = Integer.parseInt(partyKey);
            List<Party> parties = loadParties();
            return parties.stream()
                    .filter(p -> p.getId() == partyId)
                    .map(Party::getName)
                    .findFirst()
                    .orElse(partyKey); // Return key if not found
        } catch (NumberFormatException e) {
            // If not a number, return as is (it's already a name)
            return partyKey;
        }
    }

    public static String getPartyIdByName(String partyName) {
        try {
            List<Party> parties = loadParties();
            System.out.println("Looking for party name: '" + partyName + "'");
            System.out.println("Available parties:");
            for (Party p : parties) {
                System.out.println("  ID: " + p.getId() + ", Name: '" + p.getName() + "'");
            }
            
            String result = parties.stream()
                    .filter(p -> p.getName().equals(partyName))
                    .map(p -> String.valueOf(p.getId()))
                    .findFirst()
                    .orElse(partyName);
            
            System.out.println("Result: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return partyName;
        }
    }

    public static List<String> getPartiesForDate(LocalDate date) {
        try {
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            String dateKey = date.toString();
            Map<String, List<SubSlip>> byParty = data.get(dateKey);
            
            if (byParty == null) return new ArrayList<>();
            
            // Convert party IDs to party names
            List<Party> allParties = loadParties();
            return byParty.keySet().stream()
                    .map(partyId -> {
                        try {
                            int id = Integer.parseInt(partyId);
                            return allParties.stream()
                                    .filter(p -> p.getId() == id)
                                    .map(Party::getName)
                                    .findFirst()
                                    .orElse("Unknown Party");
                        } catch (NumberFormatException e) {
                            return partyId; // Already a name
                        }
                    })
                    .sorted()
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static boolean hasSubSlipRecordsForParty(String partyId) {
        try {
            Map<String, Map<String, List<SubSlip>>> data = loadSubSlips();
            
            // Check all dates for this party ID
            for (Map<String, List<SubSlip>> byParty : data.values()) {
                if (byParty.containsKey(partyId)) {
                    List<SubSlip> slips = byParty.get(partyId);
                    if (slips != null && !slips.isEmpty()) {
                        return true; // Found records for this party
                    }
                }
            }
            return false; // No records found
        } catch (Exception e) {
            e.printStackTrace();
            return true; // Err on the side of caution
        }
    }

    public static List<Shortcut> loadShortcuts() {
        try {
            File file = new File(DATA_DIR, "shortcuts.json");
            if (!file.exists()) {
                // Create default shortcuts
                List<Shortcut> defaults = new ArrayList<>();
                defaults.add(new Shortcut("r", "RTGS", "+"));
                defaults.add(new Shortcut("c", "Cash Discount", "-"));
                defaults.add(new Shortcut("t", "Transport", "-"));
                saveShortcuts(defaults);
                return defaults;
            }

            return mapper.readValue(file, new TypeReference<List<Shortcut>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveShortcuts(List<Shortcut> shortcuts) {
        try {
            ensureDataDirExists();
            File file = new File(DATA_DIR, "shortcuts.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, shortcuts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureDataDirExists() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                throw new RuntimeException("Failed to create data directory: " + DATA_DIR);
            }
        }
    }

    public static void saveMainSlip(MainSlip mainSlip) {
        try {
            Map<String, Map<String, MainSlip>> data = loadMainSlips();
            String dateKey = mainSlip.getDate().toString();
            data.putIfAbsent(dateKey, new HashMap<>());
            data.get(dateKey).put(mainSlip.getPartyName(), mainSlip);
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(MAIN_SLIP_PATH), data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Map<String, MainSlip>> loadMainSlips() {
        try {
            File file = new File(MAIN_SLIP_PATH);
            if (!file.exists()) return new HashMap<>();
            return mapper.readValue(file, new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static MainSlip getMainSlip(LocalDate date, String partyName) {
        try {
            Map<String, Map<String, MainSlip>> data = loadMainSlips();
            String dateKey = date.toString();
            Map<String, MainSlip> byParty = data.get(dateKey);
            
            if (byParty != null) {
                return byParty.get(partyName);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
