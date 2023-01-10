package com.viktorx.skyblockbot.skyblock.flipping;

import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class BZNameConverter {
    private static boolean wasTableLoaded = false;
    private static final Map<String, String> normalToBZ = new HashMap<>();
    private static final Map<String, String> bzToNormal = new HashMap<>();

    public static String getBZName(String itemName) {
        if (!wasTableLoaded) {
            loadTable();
            wasTableLoaded = true;
        }
        return normalToBZ.get(itemName);
    }

    public static String getNormalName(String productId) {
        if (!wasTableLoaded) {
            loadTable();
            wasTableLoaded = true;
        }
        return bzToNormal.get(productId);
    }

    private static void loadTable() {
        String result = Utils.getSBApiPage("https://api.hypixel.net/resources/skyblock/items");

        // Fine, i'll do simple parsing myself
        // It's easier than doing it properly with some json tool
        int index = result.indexOf("\"name\"");
        while(index != -1) {
            int nameStart = index + "\"name\"".length() + 2; // +2 for :" cuz it goes like "name":"actualname"
            int nameEnd = nameStart;
            while(result.charAt(nameEnd) != '"') {
                nameEnd++;
            }

            int idStart = result.indexOf("\"id\"") + "\"id\"".length() + 2;
            int idEnd = idStart;
            while(result.charAt(idEnd) != '"') {
                idEnd++;
            }

            normalToBZ.put(result.substring(nameStart, nameEnd), result.substring(idStart, idEnd));
            bzToNormal.put(result.substring(idStart, idEnd), result.substring(nameStart, nameEnd));
            result = result.substring(idEnd);
            index = result.indexOf("\"name\"");
        }

    }

    public static void debugPrintTable() {
        if (!wasTableLoaded) {
            loadTable();
            wasTableLoaded = true;
        }
        normalToBZ.forEach((item, id) -> SkyblockBot.LOGGER.info(item + ":" + id));
    }
}
