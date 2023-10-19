package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.ScreenshotDaemon;
import com.viktorx.skyblockbot.task.replay.ReplayExecutor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onInventory", at = @At("HEAD"))
    public void interceptInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        CurrentInventory.setItemStacks(packet.getContents());
        CurrentInventory.setSyncID(packet.getSyncId());
    }

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    public void detectServerChangingPosRot(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying() || ReplayExecutor.INSTANCE.isRecording()) {
            ReplayExecutor.INSTANCE.serverChangedPositionRotation = true;
        }
    }

    @Inject(method = "onLookAt", at = @At("HEAD"))
    public void detectServerChangingMyRotation(LookAtS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying()) {
            ReplayExecutor.INSTANCE.serverChangedPositionRotation = true;
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    public void detectServerChangingSlot(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying()) {
            ReplayExecutor.INSTANCE.serverChangedSlot = true;
        }
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"))
    public void detectServerChangingItem(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying()) {
            ReplayExecutor.INSTANCE.serverChangedItem = true;
        }
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    public void interceptMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String message = packet.getMessage().getString();
        if(message.contains("Sacks") && message.contains("Last 30s.")) {
            int delta = 0;
            String[] split = message.split(" ");

            String increase = split[1];
            increase = increase.substring(1);
            delta += Integer.parseInt(increase);

            String decrease = split[3];
            decrease = decrease.substring(1);
            delta -= Integer.parseInt(decrease);

            ScreenshotDaemon.INSTANCE.updateSackCount(delta);
        }
    }
}
