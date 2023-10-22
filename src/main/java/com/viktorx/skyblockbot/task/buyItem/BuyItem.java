package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.task.Task;

public class BuyItem extends Task {
    private String itemName;
    private String[] itemLoreKeyWords;
    private boolean paused = false;
    private long priceLimit = 100000;

    public BuyItem() {
    }

    public BuyItem setItemInfo(String itemName, String[] itemLoreKeyWords) {
        this.itemName = itemName;
        this.itemLoreKeyWords = itemLoreKeyWords;
        return this;
    }

    public BuyItem setPriceLimit(long priceLimit) {
        this.priceLimit = priceLimit;
        return this;
    }

    public long getPriceLimit() {
        return priceLimit;
    }

    public void execute() {
        paused = false;
        BuyItemExecutor.INSTANCE.execute(this);
    }

    public void pause() {
        BuyItemExecutor.INSTANCE.pause();
        paused = true;
    }

    public void resume() {
        BuyItemExecutor.INSTANCE.resume();
        paused = false;
    }

    public void abort() {
        BuyItemExecutor.INSTANCE.abort();
    }

    public boolean isExecuting() {
        return BuyItemExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return paused;
    }

    public String getItemName() {
        return itemName;
    }

    public String[] getItemLoreKeyWords() {
        return itemLoreKeyWords;
    }

    public String getBuySlotName() {
        return "Buy Item";
    }

    public String getConfirmSlotName() {
        return "Confirm";
    }

    public String getViewBidsSlotName() {
        return "View Bids";
    }

    public String getCollectAuctionSlotName() {
        return "Collect Auction";
    }
}
