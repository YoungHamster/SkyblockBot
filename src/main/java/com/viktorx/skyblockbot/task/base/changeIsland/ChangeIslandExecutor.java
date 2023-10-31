package com.viktorx.skyblockbot.task.base.changeIsland;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ChangeIslandExecutor {
    public static ChangeIslandExecutor INSTANCE = new ChangeIslandExecutor();
    ChangeIslandState state = ChangeIslandState.IDLE;
    ChangeIslandState stateBeforePause;
    private ChangeIsland changeIsland;
    private int waitBeforeAttemptTickCounter;
    private int attemptCounter;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickChangeIsland);
    }

    public void execute(ChangeIsland changeIsland) {
        if (!state.equals(ChangeIslandState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute ChangeIsland when already running");
            return;
        }

        this.changeIsland = changeIsland;
        waitBeforeAttemptTickCounter = 0;
        attemptCounter = 0;
        state = ChangeIslandState.SENDING_COMMAND;
    }

    public void pause() {
        if (state.equals(ChangeIslandState.IDLE) || state.equals(ChangeIslandState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't pause ChangeIsland when already paused or not running");
            return;
        }

        stateBeforePause = state;
        state = ChangeIslandState.PAUSED;
    }

    public void resume() {
        if (!state.equals(ChangeIslandState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't resume ChangeIsland when not paused");
            return;
        }

        state = stateBeforePause;
    }

    public void abort() {
        state = ChangeIslandState.IDLE;
    }

    public boolean isExecuting(ChangeIsland task) {
        return state != ChangeIslandState.IDLE && changeIsland == task;
    }

    public void onTickChangeIsland(MinecraftClient client) {

        switch (state) {
            case SENDING_COMMAND -> {
                GlobalExecutorInfo.worldLoaded.set(false);
                assert client.player != null;
                Utils.sendChatMessage(changeIsland.getCommand());
                state = ChangeIslandState.WAITING_AFTER_COMMAND;
            }

            case WAITING_AFTER_COMMAND -> {
                if (!GlobalExecutorInfo.worldLoading.get()) {
                    if (waitBeforeAttemptTickCounter++ == ChangeIslandSettings.ticksToWaitBeforeAttempt) {
                        if (attemptCounter++ == ChangeIslandSettings.maxAttempts) {
                            changeIsland.aborted();
                            state = ChangeIslandState.IDLE;
                        } else {
                            assert client.player != null;
                            waitBeforeAttemptTickCounter = 0;
                            Utils.sendChatMessage(changeIsland.getCommand());
                        }
                    }
                } else {
                    state = ChangeIslandState.WAITING_FOR_WORLD_LOAD;
                }
            }

            case WAITING_FOR_WORLD_LOAD -> {
                if (GlobalExecutorInfo.worldLoaded.get()) {
                    state = ChangeIslandState.IDLE;
                    changeIsland.completed();
                }
            }
        }
    }
}
