package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.base.waitInQueue.WaitInQueue;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;
import com.viktorx.skyblockbot.utils.Utils;

public class GetToSkyblock extends CompoundTask {
    private final Task waitInQueue;
    private final Task getOutOfLimbo;
    private final Task getToSkyblock;

    public GetToSkyblock() {
        waitInQueue = new WaitInQueue();
        waitInQueue.whenCompleted(this::whenWaitInQueueCompleted);
        waitInQueue.whenAborted(this::whenWaitInQueueAborted);

        getOutOfLimbo = new ChangeIsland("/lobby");
        getOutOfLimbo.whenCompleted(this::whenGetOutOfLimboCompleted);
        getOutOfLimbo.whenAborted(this::whenGetOutOfLimboAborted);

        getToSkyblock = new ChangeIsland("/play sb");
        getToSkyblock.whenCompleted(this::whenGetToSkyblockCompleted);
        getToSkyblock.whenAborted(this::whenGetToSkyblockAborted);
    }

    private void whenWaitInQueueCompleted() {
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + currentTask.getTaskName());
        currentTask = getToSkyblock;
        currentTask.execute();
    }

    private void whenWaitInQueueAborted() {
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + currentTask.getTaskName());
    }

    private void whenGetOutOfLimboCompleted() {
        whenWaitInQueueCompleted();
    }

    private void whenGetOutOfLimboAborted() {
        whenWaitInQueueAborted();
    }

    private void whenGetToSkyblockCompleted() {
        this.completed();
    }

    private void whenGetToSkyblockAborted() {
        // TODO
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        if (Utils.isStringInRecentChat("You were spawned in limbo", 3)
                || Utils.isStringInRecentChat("Вы АФК", 3)) {
            currentTask = getOutOfLimbo;
        } else if (Utils.isStringInRecentChat("You are in queue", 4)) {
            currentTask = waitInQueue;
        } else {
            currentTask = getToSkyblock;
        }
        currentTask.execute();
    }
}
