package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseTask;

public class BuyItem extends BaseTask<BuyItemExecutor> {
    private String itemName = null;
    private String[] loreKeyWords;
    private long priceLimit = 10000000;

    public BuyItem(String itemName, String[] loreKeyWords, Runnable whenCompleted, Runnable whenAborted) {
        super(BuyItemExecutor.INSTANCE, whenCompleted, whenAborted);
        this.itemName = itemName;
        this.loreKeyWords = loreKeyWords;
    }

    public void setItemInfo(String itemName, String[] itemLoreKeyWords) {
        if (!BuyItemExecutor.INSTANCE.isExecuting(this)) {
            this.itemName = itemName;
            this.loreKeyWords = itemLoreKeyWords;
        }
    }

    public void setPriceLimit(long priceLimit) {
        if (!BuyItemExecutor.INSTANCE.isExecuting(this)) {
            this.priceLimit = priceLimit;
        }
    }

    public long getPriceLimit() {
        return priceLimit;
    }

    @Override
    public void execute() {
        if (itemName == null) {
            SkyblockBot.LOGGER.info("Can't execute BuyItem when itemName is null");
            return;
        }
        BuyItemExecutor.INSTANCE.execute(this);
    }

    public String getItemName() {
        return itemName;
    }

    public String[] getLoreKeyWords() {
        return loreKeyWords;
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
