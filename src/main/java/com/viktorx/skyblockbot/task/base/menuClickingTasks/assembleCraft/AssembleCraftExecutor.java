package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.replay.ExecutorState;
import javafx.util.Pair;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.TimeoutException;

public class AssembleCraftExecutor extends AbstractMenuClickingExecutor {

    public static final AssembleCraftExecutor INSTANCE = new AssembleCraftExecutor();
    private AssembleCraft task;
    private ExecutorState nextState;
    private ItemPutter itemPutter;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected synchronized ExecutorState restart() {
        return new OpeningSBMenu();
    }

    @Override
    protected ExecutorState whenMenuOpened() {
        return nextState;
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.itemPutter = new ItemPutter();
        this.task = (AssembleCraft) task;
        return new CheckingIngredients();
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    protected static class CheckingIngredients implements ExecutorState {
        private final AssembleCraftExecutor parent = AssembleCraftExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            for (Pair<String, Integer> ingredient : parent.task.getRecipe().getIngredients()) {
                if (!SBUtils.isAmountInInventory(ingredient.getKey(), ingredient.getValue())) {
                    parent.abort();
                    return new Idle();
                }
            }
            assert client.player != null;
            if (client.player.getInventory().selectedSlot != GlobalExecutorInfo.sbMenuHotbarSlot) {
                Keybinds.asyncPressKeyAfterTick(
                        client.options.hotbarKeys[GlobalExecutorInfo.sbMenuHotbarSlot]);
            }
            return new OpeningSBMenu();
        }
    }

    protected static class OpeningSBMenu extends WaitingExecutorState {
        private final AssembleCraftExecutor parent = AssembleCraftExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!waitBeforeAction()) {
                Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                parent.nextState = new OpeningCraftingTable();
                return new WaitingForMenu(parent);
            }
            return this;
        }
    }

    protected static class OpeningCraftingTable extends WaitingExecutorState {
        private final AssembleCraftExecutor parent = AssembleCraftExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!waitBeforeAction()) {
                try {
                    SBUtils.leftClickOnSlot(parent.task.getCraftingTableSlotName());
                    parent.nextState = new PuttingItems();
                    return new WaitingForMenu(parent);
                } catch (TimeoutException e) {
                    SkyblockBot.LOGGER.warn("Can't click on crafting table slot! Restarting AssembleCraft tsak");
                    return parent.restart();
                }
            }
            return this;
        }
    }

    protected static class PuttingItems extends WaitingExecutorState {
        private final AssembleCraftExecutor parent = AssembleCraftExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!waitBeforeAction()) {
                if (!parent.itemPutter.putNextCraftItem()) {
                    return new CollectingCraft();
                }
            }
            return this;
        }
    }


    protected static class CollectingCraft extends WaitingExecutorState {
        private final AssembleCraftExecutor parent = AssembleCraftExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!waitBeforeAction()) {

            }
            return this;
        }
    }

    private class ItemPutter {
        private static final int craftingTableSize = 9;
        private PutItemState putItemState = PutItemState.PICKING_ITEM;
        private int ingridientIterator = 0;

        private boolean putNextCraftItem() {
            Pair<String, Integer> ingredient;
            do {
                ingredient = task.getRecipe().getIngredient(ingridientIterator);
            } while (ingredient == null);

            switch (putItemState) {
                case PICKING_ITEM -> {
                    putItemState = PutItemState.PUTTING_ITEM;
                    // TODO
                }

                case PUTTING_ITEM -> {
                    putItemState = PutItemState.PICKING_ITEM;
                }
            }

            ingridientIterator++;
            return ingridientIterator < craftingTableSize;
        }

        private enum PutItemState {
            PICKING_ITEM,
            PUTTING_ITEM;

            PutItemState() {
            }
        }
    }
}
