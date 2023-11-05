package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.AbstractVisitorTask;

public class GiveVisitorItems extends AbstractVisitorTask<GiveVisitorItemsExecutor> {

    public GiveVisitorItems(Runnable whenCompleted, Runnable whenAborted) {
        super(GiveVisitorItemsExecutor.INSTANCE, whenCompleted, whenAborted);
    }

    public String getAcceptOfferStr() {
        return "Accept Offer";
    }
}
