package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.AbstractVisitorTask;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class TalkToVisitor extends AbstractVisitorTask<TalkToVisitorExecutor> {
    private final List<Pair<String, Integer>> items = new ArrayList<>();

    public TalkToVisitor(Runnable whenCompleted, Runnable whenAborted) {
        super(TalkToVisitorExecutor.INSTANCE, whenCompleted, whenAborted);
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

    protected void addItem(Pair<String, Integer> newItem) {
        items.add(newItem);
    }

}
