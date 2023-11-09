package com.viktorx.skyblockbot.task.base.changeIsland;

import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ChangeIslandExecutor extends BaseExecutor {
    public static ChangeIslandExecutor INSTANCE = new ChangeIslandExecutor();

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickChangeIsland);
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = task;
        return new SendingCommand();
    }

    public synchronized void onTickChangeIsland(MinecraftClient client) {
        state = state.onTick(client);
    }

    protected static class SendingCommand implements ExecutorState {
        private final ChangeIslandExecutor parent = ChangeIslandExecutor.INSTANCE;
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            GlobalExecutorInfo.worldLoaded.set(false);

            ChangeIsland islandTask = (ChangeIsland) parent.task;
            Utils.sendChatMessage(islandTask.getCommand());
            return new WaitingAfterCommand();
        }
    }

    protected static class WaitingAfterCommand implements ExecutorState {
        private final ChangeIslandExecutor parent = ChangeIslandExecutor.INSTANCE;
        private int waitBeforeAttemptTickCounter = 0;
        private int attemptCounter = 0;
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!GlobalExecutorInfo.worldLoading.get()) {
                if (waitBeforeAttemptTickCounter++ == ChangeIslandSettings.ticksToWaitBeforeAttempt) {
                    if (attemptCounter++ == ChangeIslandSettings.maxAttempts) {
                        parent.task.aborted();
                        return new Idle();
                    } else {
                        assert client.player != null;
                        waitBeforeAttemptTickCounter = 0;

                        ChangeIsland islandTask = (ChangeIsland) parent.task;
                        Utils.sendChatMessage(islandTask.getCommand());
                    }
                }
            } else {
                return new WaitingForWorldLoad();
            }
            return this;
        }
    }

    protected static class WaitingForWorldLoad implements ExecutorState {
        private final ChangeIslandExecutor parent = ChangeIslandExecutor.INSTANCE;
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (GlobalExecutorInfo.worldLoaded.get()) {
                parent.task.completed();
                return new Idle();
            }
            return this;
        }
    }
}
