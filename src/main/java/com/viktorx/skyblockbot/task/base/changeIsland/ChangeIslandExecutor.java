package com.viktorx.skyblockbot.task.base.changeIsland;

import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ChangeIslandExecutor extends BaseExecutor {
    public static ChangeIslandExecutor INSTANCE = new ChangeIslandExecutor();
    private ChangeIsland task;
    private int waitBeforeAttemptTickCounter;
    private int attemptCounter;

    private ChangeIslandExecutor() {
        addState("SENDING_COMMAND");
        addState("WAITING_AFTER_COMMAND");
        addState("WAITING_FOR_WORLD_LOAD");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickChangeIsland);
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.task = (ChangeIsland) task;
        waitBeforeAttemptTickCounter = 0;
        attemptCounter = 0;
        state = getState("SENDING_COMMAND");
    }

    public void onTickChangeIsland(MinecraftClient client) {

        switch (getState(state)) {
            case "SENDING_COMMAND" -> {
                GlobalExecutorInfo.worldLoaded.set(false);
                assert client.player != null;
                Utils.sendChatMessage(task.getCommand());
                state = getState("WAITING_AFTER_COMMAND");
            }

            case "WAITING_AFTER_COMMAND" -> {
                if (!GlobalExecutorInfo.worldLoading.get()) {
                    if (waitBeforeAttemptTickCounter++ == ChangeIslandSettings.ticksToWaitBeforeAttempt) {
                        if (attemptCounter++ == ChangeIslandSettings.maxAttempts) {
                            task.aborted();
                            state = getState("IDLE");
                        } else {
                            assert client.player != null;
                            waitBeforeAttemptTickCounter = 0;
                            Utils.sendChatMessage(task.getCommand());
                        }
                    }
                } else {
                    state = getState("WAITING_FOR_WORLD_LOAD");
                }
            }

            case "WAITING_FOR_WORLD_LOAD" -> {
                if (GlobalExecutorInfo.worldLoaded.get()) {
                    state = getState("IDLE");
                    task.completed();
                }
            }
        }
    }
}
