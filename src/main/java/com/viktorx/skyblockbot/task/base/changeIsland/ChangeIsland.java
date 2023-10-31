package com.viktorx.skyblockbot.task.base.changeIsland;

import com.viktorx.skyblockbot.task.Task;

import java.util.concurrent.CompletableFuture;

public class ChangeIsland extends Task {

    private String command;
    private boolean paused = false;

    public ChangeIsland(String command) {
        this.command = command;
    }

    public void execute() {
        paused = false;
        ChangeIslandExecutor.INSTANCE.execute(this);
    }

    public void pause() {
        ChangeIslandExecutor.INSTANCE.pause();
        paused = true;
    }

    public void resume() {
        ChangeIslandExecutor.INSTANCE.resume();
        paused = false;
    }

    public void abort() {
        ChangeIslandExecutor.INSTANCE.abort();
    }

    public void saveToFile(String filename) {

    }

    public void completed() {
        if(super.whenCompleted != null)
            CompletableFuture.runAsync(whenCompleted);
    }

    public void aborted() {
        if(whenAborted != null)
            CompletableFuture.runAsync(whenAborted);
    }

    public boolean isExecuting() {
        return ChangeIslandExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return paused;
    }

    public String getCommand() {
        return command;
    }
}
