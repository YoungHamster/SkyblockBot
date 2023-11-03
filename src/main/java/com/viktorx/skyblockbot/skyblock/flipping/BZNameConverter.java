package com.viktorx.skyblockbot.skyblock.flipping;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class BZNameConverter {
    private static BZNameConverter instance;

    private final Map<String, String> normalToBZ = new HashMap<>();
    private final Map<String, String> bzToNormal = new HashMap<>();

    private BZNameConverter() {
        loadTable();
        SkyblockBot.LOGGER.info("Loaded name converter table");
    }

    public static synchronized BZNameConverter getInstance() {
        if (instance == null) {
            instance = new BZNameConverter();
        }
        return instance;
    }

    public String getBZName(String itemName) {
        return normalToBZ.get(itemName);
    }

    public String getNormalName(String productId) {
        return bzToNormal.get(productId);
    }

    private void loadTable() {
        String result = Utils.getSBApiPage("https://api.hypixel.net/resources/skyblock/items");

        // Fine, i'll do simple parsing myself
        // It's easier than doing it properly with some json tool
        Pair<String, String> pair;
        while (result.contains("\"name\"")) {
            pair = Utils.getJSONTokenAndCutItOut("\"name\"", result);
            result = pair.getLeft();
            String itemName = pair.getRight();
            itemName = itemName.substring(1, itemName.length() - 1);

            pair = Utils.getJSONTokenAndCutItOut("\"id\"", result);
            result = pair.getLeft();
            String productId = pair.getRight();
            productId = productId.substring(1, productId.length() - 1);

            normalToBZ.put(itemName, productId);
        }
        normalToBZ.forEach((key, value) -> bzToNormal.put(value, key));
    }
}
