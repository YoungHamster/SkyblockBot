package com.viktorx.skyblockbot.task.base;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.Task;

public abstract class BaseTask<T extends BaseExecutor> extends Task {
    private final T executor;

    public BaseTask (T executor, Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);
        this.executor = executor;
    }

    public void execute() {
        executor.execute(this);
    }

    public void pause() {
        executor.pause();
    }

    public void resume() {
        executor.resume();
    }

    public void abort() {
        if(GlobalExecutorInfo.debugMode.get()) {
            SkyblockBot.LOGGER.debug("abort() called on task " + this.getTaskName());
        }
        executor.abort();
        if(GlobalExecutorInfo.debugMode.get()) {
            SkyblockBot.LOGGER.debug("abort() finished on task " + this.getTaskName());
        }
    }

    public boolean isExecuting() {
        return executor.isExecuting(this);
    }

    public boolean isPaused() {
        return executor.isPaused();
    }

}
