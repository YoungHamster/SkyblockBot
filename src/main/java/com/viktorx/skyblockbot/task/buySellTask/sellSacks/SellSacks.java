package com.viktorx.skyblockbot.task.buySellTask.sellSacks;

import com.viktorx.skyblockbot.task.Task;

public class SellSacks extends Task {

    public void execute() {
        SellSacksExecutor.INSTANCE.execute(this);
    }


    public void pause() {
        SellSacksExecutor.INSTANCE.pause();
    }

    public void resume() {
        SellSacksExecutor.INSTANCE.resume();
    }

    public void abort() {
        SellSacksExecutor.INSTANCE.abort();
    }

    public void saveToFile(String filename) {}

    public boolean isExecuting() {
        return SellSacksExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return SellSacksExecutor.INSTANCE.isPaused();
    }

    public String getCommand() {
        return "/bz";
    }

    public String getSellStacksSlotName() {
        return "Sell Sacks Now";
    }

    public String getConfirmSlotName() {
        return "Selling whole inventory";
    }

    public String getClosingSlotName() {
        return "Close";
    }
}
