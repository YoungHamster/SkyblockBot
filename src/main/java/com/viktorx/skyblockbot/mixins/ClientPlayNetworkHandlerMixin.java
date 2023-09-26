package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.replay.ReplayBot;
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

    /* i want to count how many times minecraft sends this packet when I normally move and when bot moves
     * if there is no difference then hypixel most likely won't auto-detect the bot
     */
    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    public void detectServerChangingPosRot(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (ReplayBot.isPlaying() || ReplayBot.isRecording()) {
            ReplayBot.serverChangedPositionRotation = true;
        }
    }

    @Inject(method = "onLookAt", at = @At("HEAD"))
    public void detectServerChangingMyRotation(LookAtS2CPacket packet, CallbackInfo ci) {
        if (ReplayBot.isPlaying()) {
            ReplayBot.serverChangedPositionRotation = true;
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    public void detectServerChangingSlot(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (ReplayBot.isPlaying()) {
            ReplayBot.serverChangedSlot = true;
        }
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"))
    public void detectServerChangingItem(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
        if (ReplayBot.isPlaying()) {
            ReplayBot.serverChangedItem = true;
        }
    }
}
