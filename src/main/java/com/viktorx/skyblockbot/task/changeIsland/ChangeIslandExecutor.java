package com.viktorx.skyblockbot.task.changeIsland;

import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ChangeIslandExecutor {
    public static ChangeIslandExecutor INSTANCE = new ChangeIslandExecutor();

    private ChangeIsland changeIsland;
    ChangeIslandState state = ChangeIslandState.IDLE;
    ChangeIslandState stateBeforePause;
    private int waitBeforeAttemptTickCounter;
    private int attemptCounter;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickChangeIsland);
    }

    public void execute(ChangeIsland changeIsland) {
        this.changeIsland = changeIsland;
        waitBeforeAttemptTickCounter = 0;
        attemptCounter = 0;
        state = ChangeIslandState.SENDING_COMMAND;
    }

    public void pause() {
        stateBeforePause = state;
        state = ChangeIslandState.PAUSED;
    }

    public void resume() {
        state = stateBeforePause;
    }

    public void abort() {
        state = ChangeIslandState.IDLE;
    }

    public boolean isExecuting(ChangeIsland task) {
        return state != ChangeIslandState.IDLE && changeIsland == task;
    }

    public void onTickChangeIsland(MinecraftClient client) {

        switch(state) {
            case SENDING_COMMAND -> {
                GlobalExecutorInfo.worldLoaded = false;
                assert client.player != null;
                client.player.sendChatMessage(changeIsland.getCommand());
                state = ChangeIslandState.WAITING_AFTER_COMMAND;
            }

            case WAITING_AFTER_COMMAND -> {
                if (!GlobalExecutorInfo.worldLoading) {
                    if (waitBeforeAttemptTickCounter++ == ChangeIslandSettings.ticksToWaitBeforeAttempt) {
                        if (attemptCounter++ == ChangeIslandSettings.maxAttempts) {
                            changeIsland.aborted();
                            state = ChangeIslandState.IDLE;
                        } else {
                            assert client.player != null;
                            waitBeforeAttemptTickCounter = 0;
                            client.player.sendChatMessage(changeIsland.getCommand());
                        }
                    }
                } else {
                    state = ChangeIslandState.WAITING_FOR_WORLD_LOAD;
                }
            }

            case WAITING_FOR_WORLD_LOAD -> {
                if (GlobalExecutorInfo.worldLoaded) {
                    state = ChangeIslandState.IDLE;
                    changeIsland.completed();
                }
            }
        }
    }
}
