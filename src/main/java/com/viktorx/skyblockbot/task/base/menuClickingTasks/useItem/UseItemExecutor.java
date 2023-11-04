package com.viktorx.skyblockbot.task.base.menuClickingTasks.useItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public class UseItemExecutor extends AbstractMenuClickingExecutor {

    private static final int defaultHotbarSlot = 1;

    public static UseItemExecutor INSTANCE = new UseItemExecutor();

    private UseItem task;
    private int itemSlot;
    private int startingSlot;
    private boolean wasUsedHotbarSlotEmpty = true;

    private UseItemExecutor() {
        addState("CHECKING_INVENTORY");
        addState("GOING_TO_HOTBAR_SLOT");
        addState("USING_ITEM");
        addState("ITEM_IN_USE");
        addState("GOING_BACK_TO_HOTBAR_SLOT");
        addState("OPENING_INVENTORY");
        addState("MOVING_ITEM_TO_CORRECT_SLOT");
        addState("CLOSING_INVENTORY");
        addState("OPENING_INVENTORY_TO_MOVE_ITEM_BACK");
        addState("MOVING_ITEM_BACK");
        addState("CLOSING_INVENTORY_FINAL");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.task = (UseItem) task;
        waitTickCounter = 0;
        waitForMenuCounter = 0;
        wasUsedHotbarSlotEmpty = true;
        state = getState("CHECKING_INVENTORY");
    }

    @Override
    protected void restart() {
        state = getState("IDLE");
        execute(task);
    }

    @Override
    protected void whenMenuOpened() {
    }

    private void onTick(MinecraftClient client) {

        switch (getState(state)) {
            case "CHECKING_INVENTORY" -> {
                itemSlot = getItemSlot(client);
                if (itemSlot == -1) {
                    SkyblockBot.LOGGER.warn("Item " + task.getItemName() + " can't be found in inventory. Aborting UseItem.");
                    state = getState("IDLE");
                    task.aborted();
                    return;
                }

                if (itemSlot < 9) {
                    state = getState("GOING_TO_HOTBAR_SLOT");
                } else {
                    state = getState("OPENING_INVENTORY");
                }
            }

            case "GOING_TO_HOTBAR_SLOT" -> {
                if (waitBeforeAction()) {
                    return;
                }

                assert client.player != null;
                startingSlot = client.player.getInventory().selectedSlot;

                if (itemSlot < 9) {
                    Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[itemSlot]);
                } else {
                    Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[defaultHotbarSlot]);
                }

                state = getState("USING_ITEM");
            }

            case "USING_ITEM" -> {
                if (waitBeforeAction()) {
                    return;
                }

                client.options.useKey.setPressed(true);
                state = getState("ITEM_IN_USE");
            }

            case "ITEM_IN_USE" -> {
                if (waitBeforeAction()) {
                    return;
                }

                client.options.useKey.setPressed(false);
                state = getState("GOING_BACK_TO_HOTBAR_SLOT");
            }

            case "GOING_BACK_TO_HOTBAR_SLOT" -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[startingSlot]);

                if (!wasUsedHotbarSlotEmpty) {
                    state = getState("OPENING_INVENTORY_TO_MOVE_ITEM_BACK");
                    return;
                }

                state = getState("IDLE");
                task.completed();
            }

            case "OPENING_INVENTORY" -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = getState("MOVING_ITEM_TO_CORRECT_SLOT");
            }

            case "MOVING_ITEM_TO_CORRECT_SLOT" -> {
                if (waitBeforeAction()) {
                    return;
                }

                assert client.player != null;
                wasUsedHotbarSlotEmpty = client.player.getInventory()
                        .getStack(defaultHotbarSlot).getName().getString().equals("Air");

                SBUtils.quickSwapSlotWithHotbar(itemSlot, defaultHotbarSlot);

                state = getState("CLOSING_INVENTORY");
            }

            case "CLOSING_INVENTORY" -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = getState("GOING_TO_HOTBAR_SLOT");
            }

            case "OPENING_INVENTORY_TO_MOVE_ITEM_BACK" -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = getState("MOVING_ITEM_BACK");
            }

            case "MOVING_ITEM_BACK" -> {
                if (waitBeforeAction()) {
                    return;
                }

                SBUtils.quickSwapSlotWithHotbar(itemSlot, defaultHotbarSlot);
                state = getState("CLOSING_INVENTORY_FINAL");
            }

            case "CLOSING_INVENTORY_FINAL" -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = getState("IDLE");
                task.completed();
            }
        }
    }

    private int getItemSlot(MinecraftClient client) {
        assert client.player != null;
        PlayerInventory inventory = client.player.getInventory();

        for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            if (inventory.getStack(i).getName().getString().equals(task.getItemName())) {
                return i;
            }
        }

        /*
         * In a desperate attempt to find anything we look again for item who's name only contains our desired name
         */
        for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            if (inventory.getStack(i).getName().getString().contains(task.getItemName())) {
                return i;
            }
        }
        return -1;
    }
}
