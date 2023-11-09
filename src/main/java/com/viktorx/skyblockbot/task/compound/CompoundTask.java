package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;

public abstract class CompoundTask extends Task {

    protected Task currentTask;

    public CompoundTask(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);
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
        } else {
            SkyblockBot.LOGGER.warn("Can't abort compound task " + this.getClass().getSimpleName() + " because current task is null");
        }
        currentTask = null;
    }

    public boolean isExecuting() {
        if (currentTask != null) {
            return currentTask.isExecuting();
        } else {
            SkyblockBot.LOGGER.warn(this.getClass().getSimpleName() + ".currentTask is null!");
        }
        return false;
    }

    public boolean isPaused() {
        if (currentTask != null) {
            return currentTask.isPaused();
        }
        return false;
    }

    @Override
    public String getTaskName() {
        if(currentTask == null) {
            return "null. No task is currently executing";
        } else {
            return currentTask.getTaskName();
        }
    }
}
