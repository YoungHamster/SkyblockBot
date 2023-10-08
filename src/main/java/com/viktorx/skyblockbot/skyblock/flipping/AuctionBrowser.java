package com.viktorx.skyblockbot.skyblock.flipping;

import com.google.gson.Gson;

public class AuctionBrowser {
    public String getAuctionWithBestPrice(String itemName) {
        Gson gson = new Gson();

        gson.fromJson(json, AuctionPage);
    }
}
