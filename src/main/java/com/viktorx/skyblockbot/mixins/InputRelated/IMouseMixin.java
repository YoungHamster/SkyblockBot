package com.viktorx.skyblockbot.mixins.InputRelated;

import net.minecraft.client.Mouse;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public interface IMouseMixin {
    @Invoker
    void callOnMouseButton(long window, int button, int action, int mods);

    @Invoker
    void callOnCursorPos(long window, double x, double y);
}
