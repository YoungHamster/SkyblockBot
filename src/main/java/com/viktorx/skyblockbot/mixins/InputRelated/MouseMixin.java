package com.viktorx.skyblockbot.mixins.InputRelated;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.replay.ReplayExecutor;
import com.viktorx.skyblockbot.task.replay.tickState.MouseKeyRecord;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("TAIL"))
    public void interceptOnMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        ReplayExecutor.INSTANCE.onKeyPress(new MouseKeyRecord(button, action, mods));
    }
}
