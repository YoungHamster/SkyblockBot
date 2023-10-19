package com.viktorx.skyblockbot.task.sellSacks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class SellSacksExecutor {

    public static SellSacksExecutor INSTANCE = new SellSacksExecutor();

    private SellSacks task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickSellSacks);
    }

    private void onTickSellSacks(MinecraftClient client) {

    }

}
