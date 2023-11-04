package com.viktorx.skyblockbot.mixins.Network;

import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(YggdrasilMinecraftSessionService.class)
public class YggdrasilMinecraftSessionServiceMixin {

    @Shadow(remap = false)
    @Final
    private static Logger LOGGER;
    @Shadow(remap = false)
    @Final
    private ServicesKeySet servicesKeySet;

    /**
     * @author viktorX
     * @reason This is needed to suppress mojangs dumb errors the like to spam when i play on hypixel
     */
    @Overwrite(remap = false)
    public String getSecurePropertyValue(Property property) {
        if (!property.hasSignature()) {
            throw new InsecurePublicKeyException.MissingException();
        }
        if (this.servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY).stream().noneMatch(key -> key.validateProperty(property))) {
            LOGGER.error("Property {} has been tampered with (signature invalid)", property.name());
            throw new InsecurePublicKeyException.InvalidException("Property has been tampered with (signature invalid)");
        }

        return property.value();
    }

}
