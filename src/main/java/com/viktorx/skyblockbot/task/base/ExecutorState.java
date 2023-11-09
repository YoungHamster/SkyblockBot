package com.viktorx.skyblockbot.task.base;

import net.minecraft.client.MinecraftClient;

public interface ExecutorState {
    ExecutorState onTick(MinecraftClient client);
}
