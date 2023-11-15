package com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.putItems;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class PutItemsInComposter extends BaseTask<PutItemsInComposterExecutor> {
    private final String organicMatterName;
    private final String fuelName;

    public PutItemsInComposter(String organicMatterName, String fuelName, Runnable whenCompleted, Runnable whenAborted) {
        super(PutItemsInComposterExecutor.INSTANCE, whenCompleted, whenAborted);
        this.organicMatterName = organicMatterName;
        this.fuelName = fuelName;
    }

    public String getOrganicMatterName() {
        return organicMatterName;
    }

    public String getFuelName() {
        return fuelName;
    }

    public String getComposterMenuName() {
        return "Composter";
    }
}
