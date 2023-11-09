package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.AbstractVisitorExecutor;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.utils.RayTraceStuff;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class GiveVisitorItemsExecutor extends AbstractVisitorExecutor {

    public static final GiveVisitorItemsExecutor INSTANCE = new GiveVisitorItemsExecutor();

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.info("Restarting GiveVisitorItems");
        return execute(task);
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = task;
        return new StartTrackingVisitor(this);
    }

    @Override
    protected ExecutorState getStateWhenVisitorOpened() {
        return new ClickOnSlotOrRestart(this, ((GiveVisitorItems) task).getAcceptOfferStr())
                .setNextState(new WaitingTillNpcLeaves(this));
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    protected static class WaitingTillNpcLeaves implements ExecutorState {
        private final GiveVisitorItemsExecutor parent;

        public WaitingTillNpcLeaves(GiveVisitorItemsExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            /*
             * Basically we ray trace every tick and when we aren't hitting the npc anymore it means he left
             */
            assert client.world != null;
            if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4) != null) {
                return this;
            }

            parent.task.completed();
            return new Idle();
        }
    }
}
