package com.viktorx.skyblockbot.skyblock.flipping;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceDatabase {
    private static PriceDatabase instance;

    // using normalized names instead of "id"'s here
    private final Map<String, Pair<Double, Integer>> pricesAnd24hVols = new HashMap<>(); // item:price,24h trade vol
    private Connection conn;

    private PriceDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/skyblock?" +
                    "user=root&password=8585623");
        } catch (Exception ex) {
            SkyblockBot.LOGGER.info("Couldn't connect to database with ended auctions. Error when creating sql driver or smth.");
            ex.printStackTrace();
        }
        loadBazaarPrices();
        SkyblockBot.LOGGER.info("Loaded bazaar prices");
    }

    public static synchronized PriceDatabase getInstance() {
        if (instance == null) {
            instance = new PriceDatabase();
        }
        return instance;
    }

    public Pair<Double, Integer> fetchPriceTradeVol(String itemName) {
        Pair<Double, Integer> pv = pricesAnd24hVols.get(itemName);
        if (pv == null) {
            pv = fetchAHPriceVol(itemName);
        }
        return pv;
    }

    public Double fetchItemPrice(String itemName) {
        Pair<Double, Integer> priceVol = pricesAnd24hVols.get(itemName);
        if (priceVol != null) {
            return priceVol.getLeft();
        } else {
            Pair<Double, Integer> pair = fetchAHPriceVol(itemName);
            if (pair != null) {
                return pair.getLeft();
            }
        }
        return null;
    }

    // median of 20 recently ended auctions, but only ones which ended in the last 48 hours
    private Pair<Double, Integer> fetchAHPriceVol(String itemName) {
        String productId = BZNameConverter.getInstance().getBZName(itemName);
        List<Integer> prices = new ArrayList<>();
        try {
            // without index it takes like 300ms
            // with index on productId column in db this request takes like 0-2ms on my pc
            // even though there is a mil of rows in db
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT price FROM compact_ah_no_enchants WHERE productId = '%s' AND timestamp > %s ORDER BY timestamp"
                            .formatted(productId, System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // 24 hours
            while (rs.next()) {
                prices.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            SkyblockBot.LOGGER.info("Error " + e.getErrorCode() + " when trying to query " + itemName +
                    " from database. Sql state: " + e.getSQLState());
            return null;
        }
        if (prices.size() == 0) {
            return null;
        }
        prices.sort(Integer::compare);
        Pair<Double, Integer> priceAndVol = new ImmutablePair<>(Double.valueOf(prices.get(prices.size() / 2)), prices.size());
        pricesAnd24hVols.put(itemName, priceAndVol);
        return priceAndVol;
    }

    private void loadBazaarPrices() {
        String bazaar = Utils.getSBApiPage("https://api.hypixel.net/skyblock/bazaar");
        bazaar = bazaar.substring(bazaar.indexOf("\"quick_status\":{") + "\"quick_status\":{".length());
        while (bazaar.contains("\"productId\"")) {
            Pair<String, String> pair = Utils.getJSONTokenAndCutItOut("\"productId\"", bazaar);
            bazaar = pair.getLeft();

            String itemName = pair.getRight();
            itemName = itemName.substring(1, itemName.length() - 1);
            itemName = BZNameConverter.getInstance().getNormalName(itemName);

            pair = Utils.getJSONTokenAndCutItOut("\"sellPrice\"", bazaar);
            bazaar = pair.getLeft();
            double sellPrice = Float.parseFloat(pair.getRight());

            pair = Utils.getJSONTokenAndCutItOut("\"sellMovingWeek\"", bazaar);
            bazaar = pair.getLeft();
            int sold7d = Integer.parseInt(pair.getRight());

            pair = Utils.getJSONTokenAndCutItOut("\"buyPrice\"", bazaar);
            bazaar = pair.getLeft();
            double buyPrice = Float.parseFloat(pair.getRight());

            pair = Utils.getJSONTokenAndCutItOut("\"buyMovingWeek\"", bazaar);
            bazaar = pair.getLeft();
            int bought7d = Integer.parseInt(pair.getRight());

            pricesAnd24hVols.put(itemName, new ImmutablePair<>((sellPrice + buyPrice) / 2, (sold7d + bought7d) / 7));

            bazaar = bazaar.substring(bazaar.indexOf("\"quick_status\"") + "\"quick_status\":{".length());
        }
    }
}
