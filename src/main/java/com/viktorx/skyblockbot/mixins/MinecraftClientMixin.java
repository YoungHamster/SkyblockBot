package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow private boolean windowFocused;

    /*
     * This detects if world is changing and also if it has already loaded and we can start doing something
     */
    @Inject(method = "setScreen", at = @At("TAIL"))
    public void detectWorldLoad(@Nullable Screen screen, CallbackInfo ci) {
        if (screen == null) {
            GlobalExecutorInfo.worldLoading.set(false);
            GlobalExecutorInfo.worldLoaded.set(true);
        } else if (screen.getClass() == DownloadingTerrainScreen.class) {
                GlobalExecutorInfo.worldLoading.set(true);
                GlobalExecutorInfo.worldLoaded.set(false);
        }
    }

    /**
     * @author ViktorX
     * @reason When window isn't focused bot stops breaking blocks after any event that causes Mouse.unlockCursor() to fire
     * (cursor unlocks and never lock until manually focusing on the window and bot can't break stuff)
     */
    @Overwrite
    public void onWindowFocusChanged(boolean focused) {
        this.windowFocused = true;
    }
}
