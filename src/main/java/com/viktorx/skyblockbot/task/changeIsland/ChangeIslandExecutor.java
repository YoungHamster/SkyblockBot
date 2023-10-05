package com.viktorx.skyblockbot.task.changeIsland;

import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.CallbackI;

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
            GlobalExecutorInfo.worldLoaded = false;
            client.player.sendChatMessage(changeIsland.getCommand());
            sentCommand = true;
            return;
        }

        if(GlobalExecutorInfo.worldLoaded) {
            executing = false;
            changeIsland.completed();
            return;
        }

        if(!GlobalExecutorInfo.worldLoading) {
            if(waitBeforeAttemptTickCounter++ == ChangeIslandSettings.ticksToWaitBeforeAttempt) {
                if(attemptCounter++ == ChangeIslandSettings.maxAttempts) {
                    changeIsland.aborted();
                    executing = false;
                } else {
                    client.player.sendChatMessage(changeIsland.getCommand());
                }
            }
        }
    }
}
