package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.utils.CurrentInventory;
import com.viktorx.skyblockbot.utils.RayTraceStuff;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class GiveVisitorItemsExecutor extends AbstractMenuClickingExecutor {

    public static final GiveVisitorItemsExecutor INSTANCE = new GiveVisitorItemsExecutor();

    private GiveVisitorItems task;

    private GiveVisitorItemsExecutor() {
        addState("CLICKING_ON_VISITOR");
        addState("WAITING_FOR_MENU");
        addState("ACCEPTING_OFFER");
        addState("WAITING_TILL_NPC_LEAVES");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void restart() {
        SkyblockBot.LOGGER.info("Restarting GiveVisitorItems");
        state = getState("IDLE");
        execute(task);
    }

    /**
     * This executor doesn't use this method, so I leave it empty
     */
    @Override
    protected void whenMenuOpened() {
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        currentClickRunning = false;
        waitTickCounter = 0;
        this.task = (GiveVisitorItems) task;
        state = getState("CLICKING_ON_VISITOR");
    }

    private void onTick(MinecraftClient client) {
        switch (getState(state)) {
            case "CLICKING_ON_VISITOR" -> {
                assert client.world != null;
                if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4.0d) != null) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    SkyblockBot.LOGGER.info("GiveVisitorItems task executor clicked on visitor");
                    state = getState("WAITING_FOR_MENU");
                }
            }

            case "WAITING_FOR_MENU" -> {
                if (CurrentInventory.syncIDChanged()) {
                    state = getState("ACCEPTING_OFFER");
                }
            }

            case "ACCEPTING_OFFER" -> {
                if (!asyncClickOrRestart(task.getAcceptOfferStr())) {
                    return;
                }
                state = getState("WAITING_TILL_NPC_LEAVES");
            }

            case "WAITING_TILL_NPC_LEAVES" -> {
                /*
                 * Basically we ray trace every tick and when we aren't hitting the npc anymore it means he left
                 */
                assert client.world != null;
                if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4) != null) {
                    return;
                }

                state = getState("IDLE");
                task.completed();
            }
        }
    }
}
