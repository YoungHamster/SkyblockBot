package com.viktorx.skyblockbot.task.useItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

public class UseItemExecutor {

    public static UseItemExecutor INSTANCE = new UseItemExecutor();

    private UseItem task;
    private UseItemState state = UseItemState.IDLE;
    private UseItemState stateBeforePause;
    private UseItemState nextState;
    private int itemSlot;
    private int startingSlot;
    private Queue<Integer> slotsToClick = new ArrayBlockingQueue<>(3);
    private int waitBeforeActionIterator = 0;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    public void execute(UseItem task) {
        if (!state.equals(UseItemState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute UseItem when already executing");
            return;
        }

        this.task = task;
        waitBeforeActionIterator = 0;
        slotsToClick.clear();
        state = UseItemState.CHECKING_INVENTORY;
    }

    public void pause() {
        if (state.equals(UseItemState.IDLE) || state.equals(UseItemState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't pause UseItem when idle or already paused");
            return;
        }

        stateBeforePause = state;
        state = UseItemState.PAUSED;
    }

    public void resume() {
        if (!state.equals(UseItemState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't resume UseItem when not paused");
        }

        state = stateBeforePause;
    }

    public void abort() {
        state = UseItemState.IDLE;
    }

    public boolean isExecuting(UseItem task) {
        return !state.equals(UseItemState.IDLE) && this.task.equals(task);
    }

    public boolean isPaused() {
        return state.equals(UseItemState.PAUSED);
    }

    private void onTick(MinecraftClient client) {
        switch (state) {
            case CHECKING_INVENTORY -> {
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
                    slotsToClick.add(itemSlot);
                    slotsToClick.add(1);
                    slotsToClick.add(itemSlot);
                }
            }

            case GOING_TO_HOTBAR_SLOT -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[itemSlot]);
                state = UseItemState.USING_ITEM;
            }

            case USING_ITEM -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                state = UseItemState.GOING_BACK_TO_HOTBAR_SLOT;
            }

            case GOING_BACK_TO_HOTBAR_SLOT -> {
                if (waitBeforeAction()) {
                    return;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[itemSlot]);
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

                Integer nextSlot = slotsToClick.poll();
                if (nextSlot != null) {
                    try {
                        SBUtils.leftClickOnSlot(nextSlot);
                    } catch (TimeoutException ignored) {}
                } else {

                }
            }
        }
    }

    private boolean waitBeforeAction() {
        if (waitBeforeActionIterator++ < GlobalExecutorInfo.waitTicksBeforeAction) {
            return true;
        }
        waitBeforeActionIterator = 0;
        return false;
    }

    private int getItemSlot(MinecraftClient client) {
        for (int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            if (MinecraftClient.getInstance().player.getInventory().getStack(i).getName().getString().equals(task.getItemName())) {

            }
        }
    }
}
