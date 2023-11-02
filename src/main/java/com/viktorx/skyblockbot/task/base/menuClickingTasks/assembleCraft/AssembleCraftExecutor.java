package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import javafx.util.Pair;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.TimeoutException;

public class AssembleCraftExecutor extends AbstractMenuClickingExecutor {

    public static final AssembleCraftExecutor INSTANCE = new AssembleCraftExecutor();

    private AssembleCraft task;
    private int nextState;
    private ItemPutter itemPutter;

    private AssembleCraftExecutor() {
        addState("CHECKING_INGREDIENTS");
        addState("OPENING_SB_MENU");
        addState("OPENING_CRAFTING_TABLE");
        addState("PUTTING_ITEMS");
        addState("COLLECTING_CRAFT");
        addState("CLOSING_INVENTORY");
        addState("WAITING_FOR_MENU");
        addState("RESTARTING");
        addState("ABORTED");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void restart() {
        state = getState("OPENING_SB_MENU");
    }

    @Override
    protected void whenMenuOpened() {
        state = nextState;
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.itemPutter = new ItemPutter();
        this.task = (AssembleCraft) task;
        state = getState("CHECKING_INGREDIENTS");
    }

    private void onTick(MinecraftClient client) {
        switch (getState(state)) {
            case "CHECKING_INGREDIENTS" -> {
                task.getRecipe().getIngredients().forEach(ingredient -> {
                    if (!SBUtils.isAmountInInventory(ingredient.getKey(), ingredient.getValue())) {
                        state = getState("ABORTED");
                    }
                });

                if (state == getState("ABORTED")) {
                    abort();
                    return;
                }

                state = getState("OPENING_SB_MENU");
                assert client.player != null;
                if (client.player.getInventory().selectedSlot != GlobalExecutorInfo.sbMenuHotbarSlot) {
                    Keybinds.asyncPressKeyAfterTick(
                            client.options.hotbarKeys[GlobalExecutorInfo.sbMenuHotbarSlot]);
                }
            }

            case "OPENING_SB_MENU" -> {
                if (!waitBeforeAction()) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    nextState = getState("OPENING_CRAFTING_TABLE");
                    state = getState("WAITING_FOR_MENU");
                }
            }

            case "OPENING_CRAFTING_TABLE" -> {
                if (!waitBeforeAction()) {
                    try {
                        SBUtils.leftClickOnSlot(task.getCraftingTableSlotName());
                        nextState = getState("PUTTING_ITEMS");
                        state = getState("WAITING_FOR_MENU");
                    } catch (TimeoutException e) {
                        SkyblockBot.LOGGER.warn("Can't click on crafting table slot! Restarting AssembleCraft tsak");
                        state = getState("RESTARTING");
                    }
                }
            }

            case "PUTTING_ITEMS" -> {
                if (!waitBeforeAction()) {
                    if (!itemPutter.putNextCraftItem()) {
                        state = getState("COLLECTING_CRAFT");
                    }
                }
            }

            case "COLLECTING_CRAFT" -> {
                if (!waitBeforeAction()) {

                }
            }

            case "WAITING_FOR_MENU" -> waitForMenuOrRestart();
        }
    }

    private class ItemPutter {
        private enum PutItemState {
            PICKING_ITEM,
            PUTTING_ITEM;

            PutItemState() {
            }
        }

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
    }
}
