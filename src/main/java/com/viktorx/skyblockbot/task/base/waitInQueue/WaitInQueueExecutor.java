package com.viktorx.skyblockbot.task.base.waitInQueue;

import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.replay.ExecutorState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class WaitInQueueExecutor extends BaseExecutor {
    public static WaitInQueueExecutor INSTANCE = new WaitInQueueExecutor();

    private WaitInQueue task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = (WaitInQueue) task;
        return new Waiting();
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    protected static class Waiting implements ExecutorState {
        public ExecutorState onTick(MinecraftClient client) {
            if (GlobalExecutorInfo.worldLoading.get()) {
                return new WorldLoading();
            }
            return this;
        }
    }

    protected static class WorldLoading implements ExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (GlobalExecutorInfo.worldLoaded.get()) {
                WaitInQueueExecutor.INSTANCE.task.completed();
                return new Idle();
            }
            return this;
        }
    }

}
