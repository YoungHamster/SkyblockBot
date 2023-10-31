package com.viktorx.skyblockbot.task.base.useItem;

import com.viktorx.skyblockbot.task.Task;

public class UseItem extends Task {
    private String itemName;

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public void execute() {
        UseItemExecutor.INSTANCE.execute(this);
    }

    public void pause() {
        UseItemExecutor.INSTANCE.pause();
    }

    public void resume() {
        UseItemExecutor.INSTANCE.resume();
    }

    public void abort() {
        UseItemExecutor.INSTANCE.abort();
    }

    public boolean isExecuting() {
        return UseItemExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return UseItemExecutor.INSTANCE.isPaused();
    }
}
