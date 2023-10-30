package com.viktorx.skyblockbot.mixins;

import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignEditScreen.class)
public interface ISignEditScreenMixin {
    @Accessor
    String[] getText();
}
