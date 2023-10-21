package com.viktorx.skyblockbot.skyblock.flipping.auction;

import java.util.List;

public class Auction {
    public String uuid = null;
    public String auctioneer = null;
    public String profile_id = null;
    public transient List<String> coop = null;
    public long start;
    public long end;
    public String item_name = null;
    public String item_lore = null;
    public String extra = null;
    public String category = null;
    public String tier = null;
    public long starting_bid;
    public String itemBytes = null;
    public boolean claimed;
    public List<String> claimed_bidders = null;
    public long highest_bid_amount;
    public long last_updated;
    public boolean bin;
    public List<Bid> bids = null;
    public String item_uuid = null;
}
