package com.viktorx.skyblockbot.task.changeIsland;

import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ChangeIslandExecutor {
    public static ChangeIslandExecutor INSTANCE = new ChangeIslandExecutor();

    private ChangeIsland changeIsland;
    private boolean executing;
    private boolean sentCommand;
    private int waitChunksTickCounter;
    private int waitBeforeAttemptTickCounter;
    private int attemptCounter;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickChangeIsland);
    }

    public void execute(ChangeIsland changeIsland) {
        this.changeIsland = changeIsland;
        waitChunksTickCounter = 0;
        waitBeforeAttemptTickCounter = 0;
        attemptCounter = 0;
        sentCommand = false;
        executing = true;
    }

    public void onTickChangeIsland(MinecraftClient client) {
        if(!executing) {
            return;
        }

        if(!sentCommand) {
            client.player.sendChatMessage(changeIsland.getCommand());
            sentCommand = true;
        }

        if(GlobalExecutorInfo.worldChangeDetected) {
            if(waitChunksTickCounter++ >= ChangeIslandSettings.ticksToWaitForChunks) {
                executing = false;
                GlobalExecutorInfo.worldChangeDetected = false;
                changeIsland.completed();
            }
        } else {
            if(waitBeforeAttemptTickCounter++ >= ChangeIslandSettings.ticksToWaitBeforeAttempt) {

                if(attemptCounter++ >= ChangeIslandSettings.maxAttempts) {
                    executing = false;
                    changeIsland.aborted();
                } else {
                    sentCommand = false;
                    waitBeforeAttemptTickCounter = 0;
                }

            }
        }
    }
}
