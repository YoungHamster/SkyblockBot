package com.viktorx.skyblockbot.task.changeIsland;

import com.viktorx.skyblockbot.task.Task;

import java.util.concurrent.CompletableFuture;

public class ChangeIsland implements Task {

    private String command;
    private Runnable whenCompleted;
    private Runnable whenAborted;

    public ChangeIsland(String command) {
        this.command = command;
    }

    @Override
    public void execute() {
        ChangeIslandExecutor.INSTANCE.execute(this);
    }

    @Override
    public void pause() {
        ChangeIslandExecutor.INSTANCE.pause();
    }

    @Override
    public void resume() {
        ChangeIslandExecutor.INSTANCE.resume();
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

    public String getCommand() {
        return command;
    }
}
