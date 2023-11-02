package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseTask;

public class BuyItem extends BaseTask<BuyItemExecutor> {
    private String itemName = null;
    private String[] itemLoreKeyWords;
    private long priceLimit = 10000000;

    public BuyItem() {
        super(BuyItemExecutor.INSTANCE);
    }

    public void setItemInfo(String itemName, String[] itemLoreKeyWords) {
        if (!BuyItemExecutor.INSTANCE.isExecuting(this)) {
            this.itemName = itemName;
            this.itemLoreKeyWords = itemLoreKeyWords;
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
