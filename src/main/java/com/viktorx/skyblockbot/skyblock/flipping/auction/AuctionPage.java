package com.viktorx.skyblockbot.skyblock.flipping.auction;

import java.util.List;

public class AuctionPage {
    public boolean success;
    public int page;
    public int totalPages;
    public int totalAuctions;
    public long lastUpdated;
    public List<Auction> auctions;
}
