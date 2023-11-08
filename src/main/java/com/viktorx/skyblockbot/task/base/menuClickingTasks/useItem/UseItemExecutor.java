package com.viktorx.skyblockbot.task.base.menuClickingTasks.useItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.replay.ExecutorState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public class UseItemExecutor extends AbstractMenuClickingExecutor {

    private static final int defaultHotbarSlot = 1;

    public static UseItemExecutor INSTANCE = new UseItemExecutor();

    private UseItem task;
    private int startingSlot;
    private boolean wasUsedHotbarSlotEmpty = true;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = (UseItem) task;
        waitForMenuCounter = 0;
        wasUsedHotbarSlotEmpty = true;
        return new CheckingInventory(this);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.info("Restarting UseItem task");
        return execute(task);
    }

    // This class isn't supposed to use this method, so it returns null
    @Override
    protected ExecutorState whenMenuOpened() {
        return null;
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
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

    protected static class CheckingInventory implements ExecutorState {
        private final UseItemExecutor parent;

        public CheckingInventory(UseItemExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            int itemSlot = parent.getItemSlot(client);
            if (itemSlot == -1) {
                SkyblockBot.LOGGER.warn("Item " + parent.task.getItemName() + " can't be found in inventory. Aborting UseItem.");
                parent.task.aborted();
                return new Idle();
            }

            if (itemSlot < 9) {
                return new GoingBackToHotBarSlot(parent);
            } else {
                return new OpeningInventory();
            }
        }
    }

    protected static class GoingToHotBarSlot extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public GoingToHotBarSlot() {
            parent = UseItemExecutor.INSTANCE;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            assert client.player != null;
            parent.startingSlot = client.player.getInventory().selectedSlot;

            if (parent.getItemSlot(client) < 9) {
                Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[parent.getItemSlot(client)]);
            } else {
                Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[defaultHotbarSlot]);
            }

            return new UsingItem(parent);
        }
    }

    protected static class UsingItem extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public UsingItem(UseItemExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            client.options.useKey.setPressed(true);
            return new ItemInUse(parent);
        }
    }

    protected static class ItemInUse extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public ItemInUse(UseItemExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            client.options.useKey.setPressed(false);
            return new GoingBackToHotBarSlot(parent);
        }
    }

    protected static class GoingBackToHotBarSlot extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public GoingBackToHotBarSlot(UseItemExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[parent.startingSlot]);

            if (!parent.wasUsedHotbarSlotEmpty) {
                return new OpeningInventoryToMoveItemBack();
            }

            parent.task.completed();
            return new Idle();
        }
    }

    protected static class OpeningInventory extends WaitingExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!waitBeforeAction()) {
                Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
                return new MovingItemToCorrectSlot();
            }
            return this;
        }
    }

    protected static class MovingItemToCorrectSlot extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public MovingItemToCorrectSlot() {
            parent = UseItemExecutor.INSTANCE;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            assert client.player != null;
            parent.wasUsedHotbarSlotEmpty = client.player.getInventory()
                    .getStack(defaultHotbarSlot).getName().getString().equals("Air");

            SBUtils.quickSwapSlotWithHotbar(parent.getItemSlot(client), defaultHotbarSlot);

            return new ClosingInventory();
        }
    }

    protected static class ClosingInventory extends WaitingExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
            return new GoingToHotBarSlot();
        }
    }

    protected static class OpeningInventoryToMoveItemBack extends WaitingExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
            return new MovingItemBack();
        }
    }

    protected static class MovingItemBack extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public MovingItemBack() {
            parent = UseItemExecutor.INSTANCE;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            SBUtils.quickSwapSlotWithHotbar(parent.getItemSlot(client), defaultHotbarSlot);
            return new ClosingInventoryFinal();
        }
    }

    protected static class ClosingInventoryFinal extends WaitingExecutorState {
        private final UseItemExecutor parent;

        public ClosingInventoryFinal() {
            parent = UseItemExecutor.INSTANCE;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            Keybinds.asyncPressKeyAfterTick(client.options.inventoryKey);
            parent.task.completed();
            return new Idle();
        }
    }
}
