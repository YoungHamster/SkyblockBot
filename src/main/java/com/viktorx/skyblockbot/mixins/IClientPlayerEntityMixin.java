package com.viktorx.skyblockbot.mixins;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(ClientPlayerEntity.class)
public interface IClientPlayerEntityMixin {
    @Invoker
    boolean callWouldCollideAt(BlockPos blockPos);
}
