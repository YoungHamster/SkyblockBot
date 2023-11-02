package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.RayTraceStuff;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class GiveVisitorItemsExecutor extends AbstractMenuClickingExecutor {

    public static final GiveVisitorItemsExecutor INSTANCE = new GiveVisitorItemsExecutor();

    private GiveVisitorItemsState state = GiveVisitorItemsState.IDLE;
    private GiveVisitorItemsState stateBeforePause;
    private GiveVisitorItems task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void restart() {
        SkyblockBot.LOGGER.info("Restarting GiveVisitorItems");
        state = GiveVisitorItemsState.IDLE;
        execute(task);
    }

    /**
     * This executor doesn't use this method, so I leave it empty
     */
    @Override
    protected void whenMenuOpened() {
    }

    @Override
    public <T extends BaseTask<?>> void execute(T task) {
        if (!state.equals(GiveVisitorItemsState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute when already running");
            return;
        }
        currentClickRunning = false;
        waitTickCounter = 0;
        this.task = (GiveVisitorItems) task;
        state = GiveVisitorItemsState.CLICKING_ON_VISITOR;
    }

    @Override
    public void pause() {
        if (state.equals(GiveVisitorItemsState.PAUSED) || state.equals(GiveVisitorItemsState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't pause when paused or idle");
            return;
        }

        stateBeforePause = state;
        state = GiveVisitorItemsState.PAUSED;
    }

    @Override
    public void resume() {
        if (!state.equals(GiveVisitorItemsState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't resume when not paused!!!");
            return;
        }

        state = stateBeforePause;
    }

    @Override
    public void abort() {
        state = GiveVisitorItemsState.IDLE;
    }

    @Override
    public <T extends BaseTask<?>> boolean isExecuting(T task) {
        return !state.equals(GiveVisitorItemsState.IDLE) && this.task == task;
    }

    @Override
    public boolean isPaused() {
        return state.equals(GiveVisitorItemsState.PAUSED);
    }

    private void onTick(MinecraftClient client) {
        switch (state) {
            case CLICKING_ON_VISITOR -> {
                assert client.world != null;
                if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4.0d) != null) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    SkyblockBot.LOGGER.info("GiveVisitorItems task executor clicked on visitor");
                    state = GiveVisitorItemsState.WAITING_FOR_MENU;
                }
            }

            case WAITING_FOR_MENU -> {
                if (CurrentInventory.syncIDChanged()) {
                    state = GiveVisitorItemsState.ACCEPTING_OFFER;
                }
            }

            case ACCEPTING_OFFER -> {
                if (!asyncClickOrRestart(task.getAcceptOfferStr())) {
                    return;
                }
                state = GiveVisitorItemsState.WAITING_TILL_NPC_LEAVES;
            }

            case WAITING_TILL_NPC_LEAVES -> {
                /*
                 * Basically we ray trace every tick and when we aren't hitting the npc anymore it means he left
                 */
                assert client.world != null;
                if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4) != null) {
                    return;
                }

                state = GiveVisitorItemsState.IDLE;
                task.completed();
            }
        }
    }
}
