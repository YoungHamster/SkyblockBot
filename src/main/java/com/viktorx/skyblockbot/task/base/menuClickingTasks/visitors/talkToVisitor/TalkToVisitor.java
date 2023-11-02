package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.task.base.BaseTask;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class TalkToVisitor extends BaseTask<TalkToVisitorExecutor> {
    private final List<Pair<String, Integer>> items = new ArrayList<>();
    private String visitorName;

    public TalkToVisitor() {
        super(TalkToVisitorExecutor.INSTANCE);
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
