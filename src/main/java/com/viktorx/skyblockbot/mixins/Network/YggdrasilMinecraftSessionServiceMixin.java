package com.viktorx.skyblockbot.mixins.Network;

import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(YggdrasilMinecraftSessionService.class)
public class YggdrasilMinecraftSessionServiceMixin {

    /**
     * This is needed to suppress mojangs dumb errors the like to spam when i play on hypixel
     */
    @Inject(method = "getSecurePropertyValue", at = @At("HEAD"), cancellable = true, remap = false)
    public void suppressDumbError(Property property, CallbackInfoReturnable<String> cir) throws InsecurePublicKeyException {
        cir.setReturnValue(property.value());
    }

}
