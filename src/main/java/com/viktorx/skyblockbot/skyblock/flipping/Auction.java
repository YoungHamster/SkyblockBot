package com.viktorx.skyblockbot.skyblock.flipping;

public class Auction {
    public String uuid = null;
    public String auctioneer = null;
    public String profile_id = null;
    public transient String[] coop = null;
    public long start;
    public long end;
    public String item_name = null;
    public String item_lore = null;
    public String extra = null;
    public String category = null;
    public String tier = null;
    public int starting_bid;
    public String itemBytes = null;
    public boolean claimed;
    public String[] claimed_bidders = null;
    public int highest_bid_amount;
    public long last_updated;
    public boolean bin;
    public String[] bids = null;
    public String item_uuid = null;
}
