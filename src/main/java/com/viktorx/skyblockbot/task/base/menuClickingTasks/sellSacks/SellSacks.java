package com.viktorx.skyblockbot.task.base.menuClickingTasks.sellSacks;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;

import java.util.concurrent.CompletableFuture;

public class SellSacks extends BaseTask<SellSacksExecutor> {

    public SellSacks(Runnable whenCompleted, Runnable whenAborted) {
        super(SellSacksExecutor.INSTANCE, whenCompleted, whenAborted);
    }

    @Override
    public void completed() {
        GlobalExecutorInfo.totalSackCount.set(0);
        if (whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
        else
            SkyblockBot.LOGGER.warn("whenCompleted == null!!!!!!!");
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
}
