package com.viktorx.skyblockbot.task.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;

public class BuyBZItem extends Task {

    private String itemName = null;

    public void setItemName(String itemName) {
        if(!BuyBZItemExecutor.INSTANCE.isExecuting(this)) {
            this.itemName = itemName;
        }
    }

    public void execute() {
        if(itemName == null) {
            SkyblockBot.LOGGER.warn("Can't execute BuyBZItem because itemName is null");
            aborted();
            return;
        }
        BuyBZItemExecutor.INSTANCE.execute(this);
    }

    public void pause() {
        BuyBZItemExecutor.INSTANCE.pause();
    }

    public void resume() {
        BuyBZItemExecutor.INSTANCE.resume();
    }

    public void abort() {
        BuyBZItemExecutor.INSTANCE.abort();
    }

    public boolean isExecuting() {
        return BuyBZItemExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return BuyBZItemExecutor.INSTANCE.isPaused();
    }
}
