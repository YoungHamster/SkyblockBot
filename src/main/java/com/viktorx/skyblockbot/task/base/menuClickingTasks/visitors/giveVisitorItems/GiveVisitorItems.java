package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class GiveVisitorItems extends BaseTask<GiveVisitorItemsExecutor> {

    public GiveVisitorItems() {
        super(GiveVisitorItemsExecutor.INSTANCE);
    }

    public String getAcceptOfferStr() {
        return "Accept Offer";
    }
}
