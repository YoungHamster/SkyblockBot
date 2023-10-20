package com.viktorx.skyblockbot.task.buyBZItem;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class BuyBZItemExecutor {

    public static BuyBZItemExecutor INSTANCE = new BuyBZItemExecutor();

    private BuyBZItemState state = BuyBZItemState.IDLE;
    private BuyBZItem task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public boolean isExecuting(BuyBZItem task) {
        return !state.equals(BuyBZItemState.IDLE) && this.task.equals(task);
    }

    public boolean isPaused() {
        return state.equals(BuyBZItemState.PAUSED);
    }

    public void onTickBuy(MinecraftClient client) {

    }
}
