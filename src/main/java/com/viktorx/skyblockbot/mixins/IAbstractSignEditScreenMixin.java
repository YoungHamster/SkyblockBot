package com.viktorx.skyblockbot.mixins;

import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSignEditScreen.class)
public interface IAbstractSignEditScreenMixin {
    @Accessor
    String[] getMessages();
}
