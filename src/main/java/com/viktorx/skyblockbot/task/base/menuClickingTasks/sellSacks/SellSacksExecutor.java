package com.viktorx.skyblockbot.task.base.menuClickingTasks.sellSacks;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.TimeoutException;

public class SellSacksExecutor extends AbstractMenuClickingExecutor {

    public static SellSacksExecutor INSTANCE = new SellSacksExecutor();

    private SellSacks task;
    private SellSacksState state = SellSacksState.IDLE;
    private SellSacksState nextState;
    private SellSacksState stateBeforePause;
    private int waitTickCounter = 0;

    SellSacksExecutor() {
        possibleErrors.add("You may only use this command after");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickSellSacks);
    }

    @Override
    protected void restart() {
        SkyblockBot.LOGGER.info("Restarting sellSacks");
        blockingCloseCurrentInventory();
        state = SellSacksState.IDLE;
        execute(task);
    }

    @Override
    protected void whenMenuOpened() {
        state = nextState;
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.task = (SellSacks) task;
        waitTickCounter = 0;
        state = SellSacksState.SENDING_COMMAND;
        SkyblockBot.LOGGER.info("Executing sell sacks");
    }

    private void onTickSellSacks(MinecraftClient client) {

        switch (state) {
            case SENDING_COMMAND -> {
                assert client.player != null;
                Utils.sendChatMessage(task.getCommand());
                state = SellSacksState.WAITING_FOR_MENU;
                nextState = SellSacksState.SELLING;
            }

            case WAITING_FOR_MENU -> waitForMenuOrRestart();

            case SELLING -> {
                if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                    return;
                }
                waitTickCounter = 0;

                clickOnSlotOrAbort(task.getSellStacksSlotName(), SellSacksState.WAITING_FOR_MENU, SellSacksState.CONFIRMING);
            }

            case CONFIRMING -> {
                if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                    return;
                }
                waitTickCounter = 0;

                clickOnSlotOrAbort(task.getConfirmSlotName(), SellSacksState.WAITING_BEFORE_CLOSING_MENU, null);
            }

            case WAITING_BEFORE_CLOSING_MENU -> {
                if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                    return;
                }
                waitTickCounter = 0;

                asyncCloseCurrentInventory();

                SkyblockBot.LOGGER.info("Sold sacks!");
                state = SellSacksState.IDLE;
                task.completed();
            }
        }
    }

    private void clickOnSlotOrAbort(String slotName, SellSacksState stateVal, SellSacksState nextStateVal) {
        try {
            SBUtils.leftClickOnSlot(slotName);
            state = stateVal;
            nextState = nextStateVal;
        } catch (TimeoutException e) {
            state = SellSacksState.IDLE;
            task.aborted();
        }
    }

}
