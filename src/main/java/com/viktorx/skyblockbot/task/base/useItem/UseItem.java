package com.viktorx.skyblockbot.task.base.useItem;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class UseItem extends BaseTask<UseItemExecutor> {
    private String itemName;

    public UseItem() {
        super(UseItemExecutor.INSTANCE);
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }
}
