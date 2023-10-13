package com.viktorx.skyblockbot.task;

import java.util.concurrent.CompletableFuture;

public abstract class Task {


    private Runnable whenCompleted = null;
    private Runnable whenAborted = null;

    abstract void execute();
    abstract void pause();
    abstract void resume();
    abstract void abort();
    abstract void saveToFile(String filename);
    public void completed() {
        if(whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
    }
    public void aborted() {
        if(whenAborted != null)
            CompletableFuture.runAsync(whenAborted);
    }
    public void whenCompleted(Runnable whenCompleted) {
        this.whenCompleted = whenCompleted;
    }
    public void whenAborted(Runnable whenAborted) {
        this.whenAborted = whenAborted;
    }
    abstract boolean isExecuting();
    abstract boolean isPaused();
}
