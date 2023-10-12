package com.viktorx.skyblockbot.task.changeIsland;

import com.viktorx.skyblockbot.task.Task;

import java.util.concurrent.CompletableFuture;

public class ChangeIsland implements Task {

    private String command;
    private Runnable whenCompleted;
    private Runnable whenAborted;
    private boolean paused = false;

    public ChangeIsland(String command) {
        this.command = command;
    }

    @Override
    public void execute() {
        paused = false;
        ChangeIslandExecutor.INSTANCE.execute(this);
    }

    @Override
    public void pause() {
        ChangeIslandExecutor.INSTANCE.pause();
        paused = true;
    }

    @Override
    public void resume() {
        ChangeIslandExecutor.INSTANCE.resume();
        paused = false;
    }

    @Override
    public void abort() {
        ChangeIslandExecutor.INSTANCE.abort();
    }

    @Override
    public void saveToFile(String filename) {

    }

    @Override
    public void completed() {
        if(whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
    }

    @Override
    public void aborted() {
        if(whenAborted != null)
            CompletableFuture.runAsync(whenAborted);
    }

    @Override
    public void whenCompleted(Runnable whenCompleted) {
        this.whenCompleted = whenCompleted;
    }

    @Override
    public void whenAborted(Runnable whenAborted) {
        this.whenAborted = whenAborted;
    }

    @Override
    public boolean isExecuting() {
        return ChangeIslandExecutor.INSTANCE.isExecuting(this);
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    public String getCommand() {
        return command;
    }
}
