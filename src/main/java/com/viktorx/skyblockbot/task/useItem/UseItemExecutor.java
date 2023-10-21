package com.viktorx.skyblockbot.task.useItem;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class UseItemExecutor {

    public static UseItemExecutor INSTANCE = new UseItemExecutor();

    private UseItem task;
    private UseItemState state = UseItemState.IDLE;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    public boolean isExecuting(UseItem task) {
        return !state.equals(UseItemState.IDLE) && this.task.equals(task);
    }

    public boolean isPaused() {
        return state.equals(UseItemState.PAUSED);
    }

    private void onTick(MinecraftClient client) {

    }
}
