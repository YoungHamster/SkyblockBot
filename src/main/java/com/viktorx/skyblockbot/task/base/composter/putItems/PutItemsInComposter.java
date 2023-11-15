package com.viktorx.skyblockbot.task.base.composter.putItems;

import com.viktorx.skyblockbot.task.base.BaseTask;

import java.util.List;

public class PutItemsInComposter extends BaseTask<PutItemsInComposterExecutor> {
    private final List<Integer> slotsToClick;

    public PutItemsInComposter(List<Integer> slotsToClick, Runnable whenCompleted, Runnable whenAborted) {
        super(PutItemsInComposterExecutor.INSTANCE, whenCompleted, whenAborted);
        this.slotsToClick = slotsToClick;
    }

    public List<Integer> getSlotsToClick() {
        return slotsToClick;
    }

    public String getComposterMenuName() {
        return "Composter";
    }
}
