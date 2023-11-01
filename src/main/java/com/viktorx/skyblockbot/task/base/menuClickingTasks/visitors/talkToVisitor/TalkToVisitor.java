package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.task.Task;
import javafx.util.Pair;

import java.util.List;

public class TalkToVisitor extends Task {
    private List<Pair<String, Integer>> items;
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

    public Pair<String, Integer> getNextItem() {
        Pair<String, Integer> item = items.get(0);
        items.remove(0);
        return item;
    }

    public boolean isItemRemaining() {
        return !items.isEmpty();
    }

    public String getVisitorName() {
        return visitorName;
    }

    protected void addItem(Pair<String, Integer> newItem) {
        items.add(newItem);
    }

    protected void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }
}
