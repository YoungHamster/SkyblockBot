package com.viktorx.skyblockbot.skyblock.flipping;

import com.google.gson.Gson;
import com.viktorx.skyblockbot.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionBrowser {
    private static final String auctionPageAddress = "https://api.hypixel.net/skyblock/auctions?page=";

    public static AuctionBrowser INSTANCE = new AuctionBrowser();

    private List<Auction> auctions;
    private AtomicBoolean loaded = new AtomicBoolean(false);

    public void loadAH() {
        loaded.set(false);

        Gson gson = new Gson();
        String json = Utils.getSBApiPage(auctionPageAddress + '0');
        AuctionPage page = gson.fromJson(json, AuctionPage.class);

        auctions = new ArrayList<>(Arrays.stream(page.auctions).toList());

        // TODO make this parallel
        for(int i = 1; i < page.totalPages; i++) {
            json = Utils.getSBApiPage(auctionPageAddress + '0');
            auctions.addAll(Arrays.stream(gson.fromJson(json, AuctionPage.class).auctions).toList());
        }

        loaded.set(true);
    }

    public boolean isAHLoaded() {
        return loaded.get();
    }

    /*
     * returns /viewauction command with uuid of that auction
     * or null if no bin auction with that item were found
     */
    public String getAuctionWithBestPrice(String itemName, String[] itemLoreKeyWords) {
        if(!loaded.get()) {
            return null;
        }

        int lowestPrice = -1;
        String lowestPriceUUID = null;
        for (Auction auction : auctions) {
            if(auction.bin) {
                if(isItemRight(itemName, itemLoreKeyWords, auction)) {
                    if(lowestPrice == -1 || auction.starting_bid < lowestPrice) {
                        lowestPrice = auction.starting_bid;
                        lowestPriceUUID = auction.uuid;
                    }
                }
            }
        }

        return "/viewauction " + lowestPriceUUID;
    }

    public boolean isItemRight(String itemName, String[] itemLoreKeywords, Auction item) {
        if(!item.item_name.contains(itemName)) {
            return false;
        }

        for(int i = 0; i < itemLoreKeywords.length; i++) {
            if(!item.item_lore.contains(itemLoreKeywords[i])) {
                return false;
            }
        }

        return true;
    }
}
