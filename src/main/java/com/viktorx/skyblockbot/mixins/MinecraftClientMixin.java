package com.viktorx.skyblockbot.mixins;

import com.viktorx.skyblockbot.ScreenshotDaemon;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    /*
     * This detects if world is changing and also if it has already loaded and we can start doing something
     */
    @Inject(method = "setScreen", at = @At("TAIL"))
    public void detectWorldLoad(@Nullable Screen screen, CallbackInfo ci) {
        if (screen == null) {
            SkyblockBot.LOGGER.info("Screen == null");
            GlobalExecutorInfo.worldLoading = false;
            GlobalExecutorInfo.worldLoaded = true;
        } else if (screen.getClass() == DownloadingTerrainScreen.class) {
            SkyblockBot.LOGGER.info("Screen == DownloadingTerrainScreen");
            GlobalExecutorInfo.worldLoading = true;
            GlobalExecutorInfo.worldLoaded = false;
        }
    }
}
