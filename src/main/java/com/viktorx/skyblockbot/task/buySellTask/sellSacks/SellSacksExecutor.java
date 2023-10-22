package com.viktorx.skyblockbot.task.buySellTask.sellSacks;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.TimeoutException;

public class SellSacksExecutor {

    public static SellSacksExecutor INSTANCE = new SellSacksExecutor();

    private SellSacks task;
    private SellSacksState state = SellSacksState.IDLE;
    private SellSacksState nextState;
    private SellSacksState stateBeforePause;
    private int waitTickCounter = 0;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickSellSacks);
    }

    public void execute(SellSacks task) {
        if (!state.equals(SellSacksState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute SellSacks when already running");
            return;
        }

        this.task = task;
        waitTickCounter = 0;
        state = SellSacksState.SENDING_COMMAND;
        SkyblockBot.LOGGER.info("Executing sell sacks");
    }

    public void pause() {
        if (state.equals(SellSacksState.IDLE) || state.equals(SellSacksState.PAUSED)) {
            SkyblockBot.LOGGER.info("Can't pause sell sacks executor when not executing or already paused");
            return;
        }

        stateBeforePause = state;
        state = SellSacksState.PAUSED;
    }

    public void resume() {
        if (state != SellSacksState.PAUSED) {
            SkyblockBot.LOGGER.info("SellSacksExecutor not paused!");
            return;
        }

        state = stateBeforePause;
    }

    public void abort() {
        state = SellSacksState.IDLE;
    }

    public boolean isExecuting(SellSacks task) {
        return this.task.equals(task) && state != SellSacksState.IDLE;
    }

    public boolean isPaused() {
        return state.equals(SellSacksState.PAUSED);
    }

    private void onTickSellSacks(MinecraftClient client) {

        switch (state) {
            case SENDING_COMMAND -> {
                assert client.player != null;
                client.player.sendChatMessage(task.getCommand());
                state = SellSacksState.WAITING_FOR_MENU;
                nextState = SellSacksState.SELLING;
            }

            case WAITING_FOR_MENU -> {
                if (CurrentInventory.syncIDChanged()) {
                    state = nextState;
                }
            }

            case SELLING -> {
                if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                    return;
                }
                waitTickCounter = 0;

                clickOnSlotOrAbort(task.getSellStacksSlotName());

                state = SellSacksState.WAITING_FOR_MENU;
                nextState = SellSacksState.CONFIRMING;
            }

            case CONFIRMING -> {
                if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                    return;
                }
                waitTickCounter = 0;

                clickOnSlotOrAbort(task.getConfirmSlotName());

                state = SellSacksState.WAITING_BEFORE_CLOSING_MENU;
            }

            case WAITING_BEFORE_CLOSING_MENU -> {
                if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                    return;
                }
                waitTickCounter = 0;

                clickOnSlotOrAbort(task.getClosingSlotName());

                SkyblockBot.LOGGER.info("Sold sacks!");
                state = SellSacksState.IDLE;
                task.completed();
            }

            default -> {
            }
        }
    }

    private void clickOnSlotOrAbort(String slotName) {
        try {
            SBUtils.leftClickOnSlot(slotName);
        } catch (TimeoutException e) {
            state = SellSacksState.IDLE;
            task.aborted();
        }
    }

}
