package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.CurrentInventory;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
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
}
