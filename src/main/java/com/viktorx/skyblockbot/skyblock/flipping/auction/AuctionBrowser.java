package com.viktorx.skyblockbot.skyblock.flipping.auction;

import com.google.gson.Gson;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AuctionBrowser {
    private static final String auctionPageAddress = "https://api.hypixel.net/skyblock/auctions?page=";

    public static AuctionBrowser INSTANCE = new AuctionBrowser();
    private final Gson gson = new Gson();
    private final List<Auction> auctions = new ArrayList<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final AtomicLong loadedTimestamp = new AtomicLong(0);

    public void loadAH() {
        if (System.currentTimeMillis() - loadedTimestamp.get() < 1000 * 60 * 2) {
            SkyblockBot.LOGGER.info("Auctions are up to date, don't need to reload them");
            return;
        }

        loaded.set(false);

        SkyblockBot.LOGGER.info("Loading auctions!");

        String json = Utils.getSBApiPage(auctionPageAddress + '0');
        AuctionPage page = gson.fromJson(json, AuctionPage.class);

        auctions.clear();
        auctions.addAll(page.auctions);

        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i < page.totalPages; i++) {
            int finalI = i;
            threads.add(new Thread(() -> loadPageAsync(finalI)));
            threads.get(threads.size() - 1).start();
        }

        for (Thread thr : threads) {
            try {
                thr.join();
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("Auction Browser interrupted exception. Wtf??? I was just loading auctions in parallel");
            }
        }

        loaded.set(true);
        loadedTimestamp.set(System.currentTimeMillis());

        SkyblockBot.LOGGER.info("Loaded auctions!");
    }

    private void loadPageAsync(int pageNumber) {
        String json = Utils.getSBApiPage(auctionPageAddress + pageNumber);
        synchronized (auctions) {
            auctions.addAll(gson.fromJson(json, AuctionPage.class).auctions);
        }
    }

    public boolean isAHLoaded() {
        return loaded.get();
    }

    /*
     * returns /viewauction command with uuid of that auction
     * or null if no bin auction with that item were found
     */
    public String getAuctionWithBestPrice(String itemName, String[] itemLoreKeyWords, long priceLimit) {
        if (!loaded.get()) {
            return null;
        }

        long start = System.currentTimeMillis();

        long lowestPrice = -1;
        String lowestPriceUUID = null;
        Auction lowestPriceAuction = null;
        for (Auction auction : auctions) {
            if (auction.bin) {
                if (isItemRight(itemName, itemLoreKeyWords, auction)) {
                    if (lowestPrice == -1 || auction.starting_bid < lowestPrice) {
                        lowestPrice = auction.starting_bid;
                        lowestPriceUUID = auction.uuid;
                        lowestPriceAuction = auction;
                    }
                }
            }
        }

        if (lowestPriceUUID == null) {
            SkyblockBot.LOGGER.info("Didn't find any auctions with item: " + itemName);
            return null;
        }

        if(lowestPrice > priceLimit) {
            SkyblockBot.LOGGER.info("Didn't find any auctions below price limit. Limit: " + priceLimit + ", lowest price: " + lowestPrice);
            return null;
        }

        /*
         * I remove lowest price auction, because on the next call of this function it will be invalid one way or another
         * Either I will buy, or someone else already bought it
         */
        auctions.remove(lowestPriceAuction);

        long end = System.currentTimeMillis();
        SkyblockBot.LOGGER.info("Found best auction! It took " + (end - start) + "ms. Price is " + lowestPrice);

        return "/viewauction " + lowestPriceUUID;
    }

    public boolean isItemRight(String itemName, String[] itemLoreKeywords, Auction item) {
        if (!item.item_name.contains(itemName)) {
            return false;
        }

        for (String itemLoreKeyword : itemLoreKeywords) {
            if (!item.item_lore.contains(itemLoreKeyword)) {
                return false;
            }
        }

        return true;
    }
}
