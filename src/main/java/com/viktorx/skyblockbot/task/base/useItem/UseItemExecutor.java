package com.viktorx.skyblockbot.task.base.useItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public class UseItemExecutor extends BaseExecutor {

    private static final int defaultHotbarSlot = 1;

    public static UseItemExecutor INSTANCE = new UseItemExecutor();

    private UseItem task;
    private int itemSlot;
    private int startingSlot;
    private boolean wasUsedHotbarSlotEmpty = true;
    private int waitBeforeActionIterator = 0;

    private UseItemExecutor() {
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.task = (UseItem) task;
        waitBeforeActionIterator = 0;
        wasUsedHotbarSlotEmpty = true;
        state = states.get("CHECKING_INVENTORY");
    }

    private void onTick(MinecraftClient client) {

        switch (state) {
            case states.get("CHECKING_INVENTORY") -> {
                itemSlot = getItemSlot(client);
                if (itemSlot == -1) {
                    SkyblockBot.LOGGER.warn("Item " + task.getItemName() + " can't be found in inventory. Aborting UseItem.");
                    state = UseItemState.IDLE;
                    task.aborted();
                    return;
                }

                if (itemSlot < 9) {
                    state = UseItemState.GOING_TO_HOTBAR_SLOT;
                } else {
                    state = UseItemState.OPENING_INVENTORY;
                }
            }

            case GOING_TO_HOTBAR_SLOT -> {
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

                state = UseItemState.USING_ITEM;
            }

            case USING_ITEM -> {
                if (waitBeforeAction()) {
                    return;
                }

                client.options.useKey.setPressed(true);
                state = UseItemState.ITEM_IN_USE;
            }

            case ITEM_IN_USE -> {
                if (waitBeforeAction()) {
                    return;
                }

                client.options.useKey.setPressed(false);
                state = UseItemState.GOING_BACK_TO_HOTBAR_SLOT;
            }

            case GOING_BACK_TO_HOTBAR_SLOT -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[startingSlot]);

                if (!wasUsedHotbarSlotEmpty) {
                    state = UseItemState.OPENING_INVENTORY_TO_MOVE_ITEM_BACK;
                    return;
                }

                state = UseItemState.IDLE;
                task.completed();
            }

            case OPENING_INVENTORY -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = UseItemState.MOVING_ITEM_TO_CORRECT_SLOT;
            }

            case MOVING_ITEM_TO_CORRECT_SLOT -> {
                if (waitBeforeAction()) {
                    return;
                }

                assert client.player != null;
                wasUsedHotbarSlotEmpty = client.player.getInventory()
                        .getStack(defaultHotbarSlot).getName().getString().equals("Air");

                SBUtils.quickSwapSlotWithHotbar(itemSlot, defaultHotbarSlot);

                state = UseItemState.CLOSING_INVENTORY;
            }

            case CLOSING_INVENTORY -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = UseItemState.GOING_TO_HOTBAR_SLOT;
            }

            case OPENING_INVENTORY_TO_MOVE_ITEM_BACK -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = UseItemState.MOVING_ITEM_BACK;
            }

            case MOVING_ITEM_BACK -> {
                if (waitBeforeAction()) {
                    return;
                }

                SBUtils.quickSwapSlotWithHotbar(itemSlot, defaultHotbarSlot);
                state = UseItemState.CLOSING_INVENTORY_FINAL;
            }

            case CLOSING_INVENTORY_FINAL -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                state = UseItemState.IDLE;
                task.completed();
            }
        }
    }

    private boolean waitBeforeAction() {
        if (waitBeforeActionIterator++ < GlobalExecutorInfo.waitTicksBeforeAction / 2) {
            return true;
        }
        waitBeforeActionIterator = 0;
        return false;
    }

    private int getItemSlot(MinecraftClient client) {
        assert client.player != null;
        PlayerInventory inventory = client.player.getInventory();

        for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            if (inventory.getStack(i).getName().getString().equals(task.getItemName())) {
                return i;
            }
        }
        return -1;
    }
}
