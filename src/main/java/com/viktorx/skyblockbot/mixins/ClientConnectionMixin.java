package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.task.replay.ReplayExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "sendImmediately", at = @At("HEAD"))
    public void countPackets(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo ci) {
        if (packet.getClass().getName().equals(PlayerMoveC2SPacket.OnGroundOnly.class.getName())) {
            ReplayExecutor.debugOnGroundOnlyCounter++;
        } else if (packet.getClass().getName().equals(PlayerMoveC2SPacket.LookAndOnGround.class.getName())) {
            ReplayExecutor.debugLookAndOnGroundCounter++;
        } else if (packet.getClass().getName().equals(PlayerMoveC2SPacket.PositionAndOnGround.class.getName())) {
            ReplayExecutor.debugPositionAndOnGroundCounter++;
        } else if (packet.getClass().getName().equals(PlayerMoveC2SPacket.Full.class.getName())) {
            ReplayExecutor.debugFullCounter++;
        }

        if (ReplayExecutor.isPlaying() || ReplayExecutor.isRecording()) {
            ReplayExecutor.debugPacketCounter++;
        }
    }

}
