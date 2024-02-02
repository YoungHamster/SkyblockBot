package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import net.minecraft.client.option.KeyBinding;

import java.util.concurrent.TimeoutException;

public class ClickOnSlot extends BaseTask<ClickOnSlotExecutor> {
    private final KeyBinding key;
    private final int slot;

    public ClickOnSlot(KeyBinding key, int slot, Runnable whenCompleted, Runnable whenAborted) {
        super(ClickOnSlotExecutor.INSTANCE, whenCompleted, whenAborted);

        this.key = key;
        this.slot = slot;
    }

    public ClickOnSlot(KeyBinding key, String itemName, Runnable whenCompleted, Runnable whenAborted) throws TimeoutException {
        super(ClickOnSlotExecutor.INSTANCE, whenCompleted, whenAborted);

        this.key = key;

        // TODO refactor getSlot to use "equals" instead of "contains" for item name
        this.slot = SBUtils.getSlot(itemName).id;
    }

    public KeyBinding getKey() {
        return key;
    }

    public int getSlot() {
        return slot;
    }
}
