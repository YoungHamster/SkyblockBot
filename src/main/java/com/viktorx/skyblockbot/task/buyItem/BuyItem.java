package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.task.Task;

import java.util.concurrent.CompletableFuture;

public class BuyItem implements Task {
    private String itemName;
    private String[] itemLoreKeyWords;
    private Runnable whenCompleted;
    private Runnable whenAborted;
    private boolean paused = false;

    @Override
    public void execute() {
        paused = false;
        BuyItemExecutor.INSTANCE.execute(this);
    }

    @Override
    public void pause() {
        BuyItemExecutor.INSTANCE.pause();
        paused = true;
    }

    @Override
    public void resume() {
        BuyItemExecutor.INSTANCE.resume();
        paused = false;
    }

    @Override
    public void abort() {
        BuyItemExecutor.INSTANCE.abort();
    }

    @Override
    public void saveToFile(String filename) {}

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
        return BuyItemExecutor.INSTANCE.isExecuting(this);
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    public String getItemName() {
        return itemName;
    }

    public String[] getItemLoreKeyWords() {
        return itemLoreKeyWords;
    }
}
