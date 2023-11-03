package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft.AssembleCraft;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem.BuyItem;

public class CraftTask extends CompoundTask {
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
}
