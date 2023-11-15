package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.replay.Replay;

public class ComposterTask extends CompoundTask {
    private static final String goToComposterRecName = "go_to_composter.bin";
    private Task goToComposter;

    public ComposterTask(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);

        this.goToComposter = new Replay(goToComposterRecName, this::whenGoToComposterCompleted, this::whenGoToComposterAborted);
    }

    @Override
    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        currentTask = goToComposter;
    }
}
