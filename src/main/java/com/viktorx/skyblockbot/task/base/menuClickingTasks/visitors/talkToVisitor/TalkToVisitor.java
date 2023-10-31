package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.task.Task;

public class TalkToVisitor extends Task {
    private String itemName;
    private int itemCount;
    private String visitorName;

    public void execute() {
        TalkToVisitorExecutor.INSTANCE.execute(this);
    }
    public void pause() {
        TalkToVisitorExecutor.INSTANCE.pause();
    }
    public void resume() {
        TalkToVisitorExecutor.INSTANCE.resume();
    }
    public void abort() {
        TalkToVisitorExecutor.INSTANCE.abort();
    }
    public boolean isExecuting() {
        return TalkToVisitorExecutor.INSTANCE.isExecuting(this);
    }
    public boolean isPaused() {
        return TalkToVisitorExecutor.INSTANCE.isPaused();
    }

    public String getAcceptOfferStr() {
        return "Accept Offer";
    }

    public String getItemName() {
        return itemName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public String getVisitorName() {
        return visitorName;
    }

    protected void setItemName(String itemName) {
        this.itemName = itemName;
    }

    protected void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    protected void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }
}
