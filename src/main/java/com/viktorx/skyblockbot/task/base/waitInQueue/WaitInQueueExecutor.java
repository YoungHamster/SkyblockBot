package com.viktorx.skyblockbot.task.base.waitInQueue;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;

public class WaitInQueueExecutor extends BaseExecutor {
    public static WaitInQueueExecutor INSTANCE = new WaitInQueueExecutor();

    private WaitInQueue task;

    private WaitInQueueExecutor() {
        addState("WAITING");
        addState("WORLD_LOADING");
    }

    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.task = (WaitInQueue) task;
    }

    private void onTick(MinecraftClient client) {
        switch (getState(state)) {
            case "WAITING" -> {
                if (GlobalExecutorInfo.worldLoading.get()) {
                    state = getState("WORLD_LOADING");
                }
            }

            case "WORLD_LOADING" -> {
                if (GlobalExecutorInfo.worldLoaded.get()) {
                    state = getState("IDLE");
                    task.completed();
                }
            }
        }
    }

}
