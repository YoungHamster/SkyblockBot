package com.viktorx.skyblockbot.task.base.replay;

import net.minecraft.client.MinecraftClient;

public interface ExecutorState {
    ExecutorState onTick(MinecraftClient client);
}
