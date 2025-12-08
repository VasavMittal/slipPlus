package com.slipplus.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slipplus.models.Party;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.slipplus.models.SubSlip;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;


public class StorageManager {

    private static final String PARTY_PATH = "src/main/resources/data/parties.json";
    private static final String SUB_SLIP_PATH = "src/main/resources/data/sub_slips.json";
    private static final ObjectMapper mapper = new ObjectMapper();

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

}
