package com.viktorx.skyblockbot.task.base.menuClickingTasks.useItem;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class UseItem extends BaseTask<UseItemExecutor> {
    private String itemName;

    public UseItem(String itemName, Runnable whenCompleted, Runnable whenAborted) {
        super(UseItemExecutor.INSTANCE, whenCompleted, whenAborted);
        this.itemName = itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }
}
