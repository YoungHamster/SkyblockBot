package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.task.base.replay.ReplayExecutor;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "sendImmediately", at = @At("HEAD"))
    public void countPackets(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (packet.getClass().getName().equals(PlayerMoveC2SPacket.OnGroundOnly.class.getName())) {
            ReplayExecutor.INSTANCE.debugOnGroundOnlyCounter++;
        } else if (packet.getClass().getName().equals(PlayerMoveC2SPacket.LookAndOnGround.class.getName())) {
            ReplayExecutor.INSTANCE.debugLookAndOnGroundCounter++;
        } else if (packet.getClass().getName().equals(PlayerMoveC2SPacket.PositionAndOnGround.class.getName())) {
            ReplayExecutor.INSTANCE.debugPositionAndOnGroundCounter++;
        } else if (packet.getClass().getName().equals(PlayerMoveC2SPacket.Full.class.getName())) {
            ReplayExecutor.INSTANCE.debugFullCounter++;
        }

        if (ReplayExecutor.INSTANCE.isPlaying() || ReplayExecutor.INSTANCE.isRecording()) {
            ReplayExecutor.INSTANCE.debugPacketCounter++;
        }
    }

}
