package com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.putItems;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class PutItemsInComposterExecutor extends AbstractMenuClickingExecutor {
    public static final PutItemsInComposterExecutor INSTANCE = new PutItemsInComposterExecutor();

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        return new Clicking();
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    @Override
    protected ExecutorState restart() {
        CompletableFuture.runAsync(() -> {
            blockingCloseCurrentInventory();
            state = new Clicking();
        });
        return new Idle();
    }

    private static class Clicking implements ExecutorState {
        private final PutItemsInComposterExecutor parent = PutItemsInComposterExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            Keybinds.asyncPressKeyAfterTick(client.options.useKey);
            return new WaitingForNamedMenu(parent, ((PutItemsInComposter) parent.task).getComposterMenuName())
                    .setNextState(new PuttingItemsInComposter());
        }
    }

    private static class PuttingItemsInComposter extends WaitingExecutorState {
        private final PutItemsInComposterExecutor parent = PutItemsInComposterExecutor.INSTANCE;
        private int clickCounter = 0;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            PutItemsInComposter putItems = (PutItemsInComposter) parent.task;

            String itemName;
            if (SBUtils.isItemInInventory(putItems.getOrganicMatterName()) && clickCounter < GlobalExecutorInfo.inventorySlotCount / 2) {
                itemName = putItems.getOrganicMatterName();
            } else if (SBUtils.isItemInInventory(putItems.getFuelName())) {
                itemName = putItems.getFuelName();
            } else {
                parent.asyncCloseCurrentInventory();
                return new WaitForMenuToClose(new Completed(parent));
            }

            try {
                SBUtils.leftClickOnSlot(itemName);
            } catch (TimeoutException e) {
                SkyblockBot.LOGGER.error("Timeout when clicking on slot during PutItemsInComposter task!");
                return parent.restart();
            }
            clickCounter++;

            if (clickCounter > GlobalExecutorInfo.inventorySlotCount) {
                SkyblockBot.LOGGER.error("PutItemsInComposterExecutor has been clicking for too long!! Aborting");
                parent.asyncCloseCurrentInventory();
                return new WaitForMenuToClose(new Aborted(parent));
            }

            return this;
        }
    }
}
