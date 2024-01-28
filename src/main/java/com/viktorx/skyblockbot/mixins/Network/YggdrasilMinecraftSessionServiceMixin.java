package com.viktorx.skyblockbot.mixins.Network;

import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(YggdrasilMinecraftSessionService.class)
public class YggdrasilMinecraftSessionServiceMixin {

    @Shadow(remap = false)
    @Final
    private static Logger LOGGER;
    @Shadow(remap = false)
    @Final
    private ServicesKeySet servicesKeySet;
}
