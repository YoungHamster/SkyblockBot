package com.viktorx.skyblockbot.task.buyBZItem;

import com.viktorx.skyblockbot.task.Task;

public class BuyBZItem extends Task {

    private String itemName;

    public BuyBZItem(String itemName) {
        this.itemName = itemName;
    }

    public void execute() {

    }

    public void pause() {

    }

    public void resume() {

    }

    public void abort() {

    }

    public boolean isExecuting() {
        return BuyBZItemExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return BuyBZItemExecutor.INSTANCE.isPaused();
    }
}
