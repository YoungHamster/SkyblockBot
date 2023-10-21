package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.task.Task;

import java.util.concurrent.CompletableFuture;

public class BuyItem extends Task {
    private String itemName;
    private String[] itemLoreKeyWords;
    private boolean paused = false;

    public BuyItem() {
    }

    public void setItemInfo(String itemName, String[] itemLoreKeyWords) {
        this.itemName = itemName;
        this.itemLoreKeyWords = itemLoreKeyWords;
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
    
    public void saveToFile(String filename) {}
    
    public void completed() {
        if(whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
    }
    
    public void aborted() {
        if(whenAborted != null)
            CompletableFuture.runAsync(whenAborted);
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
        return "Buy Item Right Now";
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
