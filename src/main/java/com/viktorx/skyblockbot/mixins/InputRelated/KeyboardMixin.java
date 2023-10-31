package com.viktorx.skyblockbot.mixins.InputRelated;

import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.task.base.replay.ReplayExecutor;
import com.viktorx.skyblockbot.task.base.replay.tickState.KeyboardKeyRecord;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("TAIL"))
    public void interceptOnKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key != Keybinds.getStartStopRecrodingKeyCode()) {
            ReplayExecutor.INSTANCE.onKeyPress(new KeyboardKeyRecord(key, scancode, action, modifiers));
        }
    }
}
