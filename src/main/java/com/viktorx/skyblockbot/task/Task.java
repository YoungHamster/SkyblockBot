package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;

import java.util.concurrent.CompletableFuture;

public abstract class Task {


    protected Runnable whenCompleted = null;
    protected Runnable whenAborted = null;

    public abstract void execute();
    public abstract void pause();
    public abstract void resume();
    public abstract void abort();
    public void saveToFile(String filename) {}
    public void loadFromFile(String filename) {}
    public void completed() {
        if(whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
        else
            SkyblockBot.LOGGER.warn("whenCompleted == null!!!!!!!");
    }
    public void aborted() {
        if(whenAborted != null)
            CompletableFuture.runAsync(whenAborted);
        else
            SkyblockBot.LOGGER.warn("whenAborted == null!!!!!!!");
    }
    public void whenCompleted(Runnable whenCompleted) {
        this.whenCompleted = whenCompleted;
    }
    public void whenAborted(Runnable whenAborted) {
        this.whenAborted = whenAborted;
    }
    public abstract boolean isExecuting();
    public abstract boolean isPaused();
}
