package com.viktorx.skyblockbot.skyblock.flipping;

import com.google.gson.Gson;
import com.viktorx.skyblockbot.Utils;

public class AuctionBrowser {
    private static final String auctionPageAddress = "";

    public static AuctionBrowser INSTANCE = new AuctionBrowser();

    /*
     * returns /viewauction command with uuid of that auction
     * or null if no bin auction with that item were found
     */
    public String getAuctionWithBestPrice(String itemName) {
        Gson gson = new Gson();
        // TODO get all pages
        String json = Utils.getSBApiPage(auctionPageAddress);
        AuctionPage page = gson.fromJson(json, AuctionPage.class);

        int lowestPrice = -1;
        String lowestPriceUUID = null;
        for (Auction auction : page.auctions) {
            if(auction.item_name.contains(itemName)) {
                if(auction.bin) {
                    if(lowestPrice == -1 || auction.starting_bid < lowestPrice) {
                        lowestPrice = auction.starting_bid;
                        lowestPriceUUID = auction.uuid;
                    }
                }
            }
        }

        return "/viewauction " + lowestPriceUUID;
    }
}
