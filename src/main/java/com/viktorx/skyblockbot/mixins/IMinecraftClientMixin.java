package com.viktorx.skyblockbot.mixins;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface IMinecraftClientMixin {
    @Invoker
    void callOpenChatScreen(String message);
}
