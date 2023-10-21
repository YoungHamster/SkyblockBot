package com.viktorx.skyblockbot.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.replay.ReplayExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
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
        /*
         * Idk why, but this method gets called twice, once on render thread, and once on netty thread
         * So i have to skip one of those calls to get correct results
         */
        if(RenderSystem.isOnRenderThread()) {
            return;
        }

        /*
         * Counting new items in inventory
         */
        int slot = packet.getSlot();
        if(slot >= 0) {
            String newName = packet.getItemStack().getName().getString();
            int newCount = packet.getItemStack().getCount();

            String currentName = MinecraftClient.getInstance().player.getInventory().getStack(slot).getName().getString();
            int currentCount;
            if(currentName.equals(ItemNames.CARROT.getName()) ||
                    currentName.equals(ItemNames.RED_MUSHROOM.getName()) ||
                    currentName.equals(ItemNames.BROWN_MUSHROOM.getName())) {
                currentCount = MinecraftClient.getInstance().player.getInventory().getStack(slot).getCount();
            } else {
                currentCount = 0;
            }

            int delta = newCount - currentCount;

            SkyblockBot.LOGGER.info(
                            "New item name: " + newName +
                            ", new count: " + newCount +
                            ", current item name: " + currentName +
                            ", current count: " + currentCount);
            if (delta > 0) {
                if (newName.equals(ItemNames.CARROT.getName())) {
                    GlobalExecutorInfo.carrotCount.addAndGet(delta);
                } else if (newName.equals(ItemNames.RED_MUSHROOM.getName())) {
                    GlobalExecutorInfo.redMushroomCount.addAndGet(delta);
                } else if (newName.equals(ItemNames.BROWN_MUSHROOM.getName())) {
                    GlobalExecutorInfo.brownMushroomCount.addAndGet(delta);
                }
            }
        }

        /*
         * Anti-detect
         */
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
        /*
         * Counting cropies
         */
        String message = packet.getMessage().getString();
        if (message.contains("Cropie (Armor Set Bonus)")) {
            GlobalExecutorInfo.cropieCount.addAndGet(1);
        }
    }
}
