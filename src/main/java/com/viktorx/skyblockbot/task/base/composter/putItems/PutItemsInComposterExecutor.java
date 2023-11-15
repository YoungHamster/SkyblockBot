package com.viktorx.skyblockbot.task.base.composter.putItems;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
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
        private int slotIterator = 0;
        private final List<Integer> slotsToClick = ((PutItemsInComposter) parent.task).getSlotsToClick();

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(waitBeforeAction()) {
                return this;
            }

            if(slotsToClick.size() == slotIterator) {
                parent.task.completed();
                return new Idle();
            }

            try {
                SBUtils.leftClickOnSlot(slotsToClick.get(slotIterator));
            } catch (TimeoutException e) {
                SkyblockBot.LOGGER.error("Timeout when clicking on slot during PutItemsInComposter task!");
                return parent.restart();
            }
            slotIterator++;

            return this;
        }
    }
}
