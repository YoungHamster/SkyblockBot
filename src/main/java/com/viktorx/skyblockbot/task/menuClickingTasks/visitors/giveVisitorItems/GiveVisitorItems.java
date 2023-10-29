package com.viktorx.skyblockbot.task.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.task.Task;

public class GiveVisitorItems extends Task {

    public void execute() {
        GiveVisitorItemsExecutor.INSTANCE.execute(this);
    }
    public void pause() {
        GiveVisitorItemsExecutor.INSTANCE.pause();
    }
    public void resume() {
        GiveVisitorItemsExecutor.INSTANCE.resume();
    }
    public void abort() {
        GiveVisitorItemsExecutor.INSTANCE.abort();
    }
    public boolean isExecuting() {
        return GiveVisitorItemsExecutor.INSTANCE.isExecuting(this);
    }
    public boolean isPaused() {
        return GiveVisitorItemsExecutor.INSTANCE.isPaused();
    }

    public String getAcceptOfferStr() {
        return "Accept Offer";
    }

    public String getConfirmationChatMsg() {
        return "OFFER ACCEPTED";
    }
}
