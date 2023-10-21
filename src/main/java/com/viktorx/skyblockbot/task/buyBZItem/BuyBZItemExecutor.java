package com.viktorx.skyblockbot.task.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class BuyBZItemExecutor {

    public static BuyBZItemExecutor INSTANCE = new BuyBZItemExecutor();

    private BuyBZItemState state = BuyBZItemState.IDLE;
    private BuyBZItemState stateBeforePause;
    private BuyBZItem task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public void execute(BuyBZItem task) {
        this.task = task;
        state = BuyBZItemState.SENDING_COMMAND;
    }

    public void pause() {
        stateBeforePause = state;
        state = BuyBZItemState.PAUSED;
    }

    public void resume() {
        if(state.equals(BuyBZItemState.PAUSED)) {
            state = stateBeforePause;
        } else {
            SkyblockBot.LOGGER.info("Can't resume when not paused! State = " + state.name());
        }
    }

    public void abort() {
        state = BuyBZItemState.IDLE;
    }

    public boolean isExecuting(BuyBZItem task) {
        return !state.equals(BuyBZItemState.IDLE) && this.task.equals(task);
    }

    public boolean isPaused() {
        return state.equals(BuyBZItemState.PAUSED);
    }

    public void onTickBuy(MinecraftClient client) {
        switch (state) {

        }
    }
}
