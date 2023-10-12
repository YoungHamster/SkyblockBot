package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.task.Task;

import java.util.concurrent.CompletableFuture;

public class BuyItem implements Task {
    private String itemName;
    private String[] itemLoreKeyWords;
    private Runnable whenCompleted;
    private Runnable whenAborted;

    @Override
    public void execute() {
        BuyItemExecutor.INSTANCE.execute(this);
    }

    @Override
    public void pause() {
        BuyItemExecutor.INSTANCE.pause();
    }

    @Override
    public void resume() {
        BuyItemExecutor.INSTANCE.resume();
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

    public String getItemName() {
        return itemName;
    }
}
