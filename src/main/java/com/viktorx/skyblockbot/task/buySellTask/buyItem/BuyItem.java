package com.viktorx.skyblockbot.task.buySellTask.buyItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;

public class BuyItem extends Task {
    private String itemName = null;
    private String[] itemLoreKeyWords;
    private boolean paused = false;
    private long priceLimit = 100000;

    public BuyItem() {
    }

    public void setItemInfo(String itemName, String[] itemLoreKeyWords) {
        if(!BuyItemExecutor.INSTANCE.isExecuting(this)) {
            this.itemName = itemName;
            this.itemLoreKeyWords = itemLoreKeyWords;
        }
    }

    public void setPriceLimit(long priceLimit) {
        if(!BuyItemExecutor.INSTANCE.isExecuting(this)) {
            this.priceLimit = priceLimit;
        }
    }

    public long getPriceLimit() {
        return priceLimit;
    }

    public void execute() {
        if(itemName == null) {
            SkyblockBot.LOGGER.info("Can't execute BuyItem when itemName is null");
            return;
        }

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
