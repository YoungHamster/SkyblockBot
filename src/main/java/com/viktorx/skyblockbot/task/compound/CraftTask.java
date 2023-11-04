package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft.AssembleCraft;

public class CraftTask extends CompoundTask {
    private final Task assembleCraft;

    public CraftTask(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);
        this.assembleCraft = new AssembleCraft(null, null);
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        // TODO
    }
}
