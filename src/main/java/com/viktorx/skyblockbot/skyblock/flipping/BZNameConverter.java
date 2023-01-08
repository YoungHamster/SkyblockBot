package com.viktorx.skyblockbot.skyblock.flipping;

import java.util.HashMap;
import java.util.Map;

public class BZNameConverter {
    private static boolean wasTableLoaded = false;
    private static Map<String, String> normalToBZ = new HashMap<>();

    public static String getBZName(String itemName) {
        if(!wasTableLoaded) {
            loadTable();
        }
        return normalToBZ.get(itemName);
    }


    private static void loadTable() {

    }
}
