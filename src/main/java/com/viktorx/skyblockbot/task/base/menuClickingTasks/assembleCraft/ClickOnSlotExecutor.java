package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.InputRelated.KeyBindingMixin;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.utils.CurrentInventory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;

public class ClickOnSlotExecutor extends AbstractMenuClickingExecutor {
    public static final ClickOnSlotExecutor INSTANCE = new ClickOnSlotExecutor();

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        return null;
    }

    @Override
    protected ExecutorState restart() {
        return null;
    }

    protected static class MovingMouse extends WaitingExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(waitBeforeAction()) {
                return this;
            }
            return new Clicking();
        }
    }

    protected static class Clicking implements ExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            ClickOnSlot clickTask = (ClickOnSlot) ClickOnSlotExecutor.INSTANCE.task;

            if (client.currentScreen == null) {
                SkyblockBot.LOGGER.error("Trying to click on slot " + clickTask.getSlot() + ", but current screen is null!!!");
                throw new RuntimeException();
            }

            /*
             * Hardcoding left mouse button because it will never change and it'll work fine
             * Actually the previous method i used here didn't work when you change attack key in settings,
             * because inventory doesn't care about settings, it only cares about default buttons
             */
            int button = ((KeyBindingMixin) clickTask.getKey()).getBoundKey().getCode();
            assert client.interactionManager != null;
            client.interactionManager.clickSlot(
                    CurrentInventory.getSyncId(),
                    clickTask.getSlot(),
                    button,
                    SlotActionType.PICKUP,
                    client.player);
        }
    }
}
