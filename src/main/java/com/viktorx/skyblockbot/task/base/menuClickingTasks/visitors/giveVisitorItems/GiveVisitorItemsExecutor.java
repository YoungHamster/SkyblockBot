package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.AbstractVisitorExecutor;
import com.viktorx.skyblockbot.utils.RayTraceStuff;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class GiveVisitorItemsExecutor extends AbstractVisitorExecutor {

    public static final GiveVisitorItemsExecutor INSTANCE = new GiveVisitorItemsExecutor();

    private GiveVisitorItemsExecutor() {
        addState("START_TRACKING_VISITOR");
        addState("TRACKING_VISITOR");
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

    @Override
    protected void whenMenuOpened() {
        SkyblockBot.LOGGER.info("Menu loaded!");
        state = getState("ACCEPTING_OFFER");
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        currentClickRunning = false;
        waitTickCounter = 0;
        waitForMenuCounter = 0;
        this.task = (GiveVisitorItems) task;
        state = getState("START_TRACKING_VISITOR");
    }

    private void onTick(MinecraftClient client) {
        switch (getState(state)) {
            case "START_TRACKING_VISITOR" -> whenStartTrackingVisitor();

            case "TRACKING_VISITOR" -> whenTrackingVisitor(client);

            case "WAITING_FOR_MENU" -> waitForMenuOrRestart();

            case "ACCEPTING_OFFER" -> {
                if (!asyncClickOrRestart(((GiveVisitorItems) task).getAcceptOfferStr())) {
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
