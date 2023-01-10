package com.viktorx.skyblockbot.skyblock.flipping;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceFetcher {
    // using normalized names instead of "id"'s here
    private static final Map<String, Float> prices = new HashMap<>();
    private static boolean werePricesLoaded = false;
    private static Connection conn;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/sb_ended_auctions?" +
                    "user=root&password=8585623");
        } catch (Exception ex) {
            SkyblockBot.LOGGER.info("Couldn't connect to database with ended auctions. Error when creating sql driver or smth.");
            ex.printStackTrace();
        }
    }


    public static Float fetchItemPrice(String itemName) {
        if(!werePricesLoaded) {
            SkyblockBot.LOGGER.info("Loading bazaar prices");
            loadBazaarPrices();
            SkyblockBot.LOGGER.info("Loaded bazaar prices");
            werePricesLoaded = true;
        }

        Float price = prices.get(itemName);
        if (price == null) {
            price = fetchAHPrice(itemName);
        }
        return price;
    }

    public static Float debugAH(String itemName) {
        return fetchAHPrice(itemName);
    }

    // median of 20 recently ended auctions, but only ones which ended in the last 48 hours
    private static Float fetchAHPrice(String itemName) {
        String productId = BZNameConverter.getBZName(itemName);
        List<Integer> prices = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT price FROM all_auctions WHERE productId = '%s' AND timestamp > %s ORDER BY timestamp LIMIT 20"
                            .formatted(productId, System.currentTimeMillis() - 48 * 60 * 60 * 1000)); // 48 hours

            while(rs.next()) {
                prices.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            SkyblockBot.LOGGER.info("Error " + e.getErrorCode() + " when trying to query " + itemName +
                    " from database. Sql state: " + e.getSQLState());
            return null;
        }
        if(prices.size() == 0) {
            return null;
        }
        prices.sort(Integer::compare);
        return Float.valueOf(prices.get(prices.size() / 2));
    }

    private static void loadBazaarPrices() {
        String bazaar = Utils.getSBApiPage("https://api.hypixel.net/skyblock/bazaar");
        bazaar = bazaar.substring(bazaar.indexOf("\"quick_status\":{") + "\"quick_status\":{".length());
        while (bazaar.contains("\"productId\"")) {
            Pair<String, String> pair = Utils.getJSONTokenAndCutItOut("\"productId\"", bazaar);
            bazaar = pair.getLeft();

            String itemName = pair.getRight();
            itemName = itemName.substring(1, itemName.length() - 1);
            itemName = BZNameConverter.getNormalName(itemName);

            pair = Utils.getJSONTokenAndCutItOut("\"sellPrice\"", bazaar);
            bazaar = pair.getLeft();
            float sellPrice = Float.parseFloat(pair.getRight());
            pair = Utils.getJSONTokenAndCutItOut("\"buyPrice\"", bazaar);
            bazaar = pair.getLeft();
            float buyPrice = Float.parseFloat(pair.getRight());

            prices.put(itemName, (sellPrice + buyPrice) / 2);

            bazaar = bazaar.substring(bazaar.indexOf("\"quick_status\"") + "\"quick_status\":{".length());
        }
    }
}
