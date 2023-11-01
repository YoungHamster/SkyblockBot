package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft.AssembleCraft;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem.BuyItem;

public class CraftTask extends Task {
    private Task currentTask;
    private final Task buyBZItem;
    private final Task buyItem;
    private final Task assembleCraft;

    public CraftTask() {
        this.buyBZItem = new BuyBZItem();
        this.buyItem = new BuyItem();
        this.assembleCraft = new AssembleCraft();
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        // TODO
    }

    public void pause() {
        if (currentTask != null) {
            currentTask.pause();
        }
    }

    public void resume() {
        if (currentTask != null) {
            currentTask.resume();
        }
    }

    public void abort() {
        if (currentTask != null) {
            currentTask.abort();
        }
        currentTask = null;
    }

    public boolean isExecuting() {
        if (currentTask != null) {
            return currentTask.isExecuting();
        }
        return false;
    }

    public boolean isPaused() {
        if (currentTask != null) {
            return currentTask.isPaused();
        }
        return false;
    }
}
