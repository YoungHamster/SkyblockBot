package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.TimeoutException;

public class AssembleCraftExecutor extends AbstractMenuClickingExecutor {

    public static final AssembleCraftExecutor INSTANCE = new AssembleCraftExecutor();

    private AssembleCraft task;
    private AssembleCraftState state = AssembleCraftState.IDLE;
    private AssembleCraftState stateBeforePause;
    private AssembleCraftState nextState;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void restart() {
        state = AssembleCraftState.OPENING_SB_MENU;
    }

    @Override
    protected void whenMenuOpened() {
        state = nextState;
    }

    public void execute(AssembleCraft task) {
        if (!state.equals(AssembleCraftState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute AssembleCraft task when it is already executing");
            return;
        }

        this.task = task;
        state = AssembleCraftState.CHECKING_INGRIDIENTS;
    }

    public void pause() {
        if (state.equals(AssembleCraftState.IDLE) || state.equals(AssembleCraftState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't pause talkToVisitor when already paused or not executing at all");
            return;
        }
        stateBeforePause = state;
        state = AssembleCraftState.PAUSED;
    }

    public void resume() {
        if (!state.equals(AssembleCraftState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't resume talkToVisitor when not paused");
            return;
        }
        state = stateBeforePause;
    }

    public void abort() {
        state = AssembleCraftState.IDLE;
    }

    public boolean isExecuting(AssembleCraft task) {
        return !state.equals(AssembleCraftState.IDLE) && this.task == task;
    }

    public boolean isPaused() {
        return state.equals(AssembleCraftState.PAUSED);
    }

    private void onTick(MinecraftClient client) {
        switch (state) {
            case CHECKING_INGRIDIENTS -> {
                task.getRecipe().getIngredients().forEach((ingridient, amount) -> {
                    if (!SBUtils.isAmountInInventory(ingridient, amount)) {
                        state = AssembleCraftState.ABORTED;
                    }
                });

                if (state.equals(AssembleCraftState.ABORTED)) {
                    abort();
                    return;
                }

                state = AssembleCraftState.OPENING_SB_MENU;
                assert client.player != null;
                if (client.player.getInventory().selectedSlot != GlobalExecutorInfo.sbMenuHotbarSlot) {
                    Keybinds.asyncPressKeyAfterTick(
                            client.options.hotbarKeys[GlobalExecutorInfo.sbMenuHotbarSlot]);
                }
            }

            case OPENING_SB_MENU -> {
                if (!waitBeforeAction()) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    nextState = AssembleCraftState.OPENING_CRAFTING_TABLE;
                    state = AssembleCraftState.WAITING_FOR_MENU;
                }
            }

            case OPENING_CRAFTING_TABLE -> {
                if (!waitBeforeAction()) {
                    try {
                        SBUtils.leftClickOnSlot(task.getCraftingTableSlotName());
                        nextState = AssembleCraftState.PUTTING_ITEMS;
                        state = AssembleCraftState.WAITING_FOR_MENU;
                    } catch (TimeoutException e) {
                        SkyblockBot.LOGGER.warn("Can't click on crafting table slot! Restarting AssembleCraft tsak");
                        state = AssembleCraftState.RESTARTING;
                    }
                }
            }

            case PUTTING_ITEMS -> {
                if (!waitBeforeAction()) {
                    if (!putNextCraftItem()) {
                        state = AssembleCraftState.COLLECTING_CRAFT;
                    }
                }
            }

            case COLLECTING_CRAFT -> {
                if (!waitBeforeAction()) {

                }
            }

            case WAITING_FOR_MENU -> waitForMenuOrRestart();
        }
    }

    private boolean putNextCraftItem() {
        return false;
    }
}
