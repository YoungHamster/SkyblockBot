package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.replay.ReplayBot;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "sendImmediately", at = @At("HEAD"))
    public void countPackets(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo ci) {
        if (ReplayBot.isPlaying()) {
            ReplayBot.debugPlayingPacketCounter++;
        } else if (ReplayBot.isRecording()) {
            ReplayBot.debugRecordingPacketCounter++;
        }
    }
}
