package com.viktorx.skyblockbot.task.useItem;

import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.buyBZItem.BuyBZItemExecutor;

public class UseItem extends Task {
    private String itemName;

    public UseItem(String itemName) {
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
        return UseItemExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return UseItemExecutor.INSTANCE.isPaused();
    }
}
