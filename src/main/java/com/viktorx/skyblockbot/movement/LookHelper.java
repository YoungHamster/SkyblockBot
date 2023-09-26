package com.viktorx.skyblockbot.movement;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.CompletableFuture;

public class LookHelper {

    public static boolean isYawRoughlyClose(float yaw1, float yaw2) {
        return Math.abs(yaw1 - yaw2) < 5.0F || Math.abs(Math.abs(yaw1 - yaw2) - 360) < 5.0F;
    }

    public static float getYaw() {
        assert MinecraftClient.getInstance().player != null;

        return Utils.normalize(MinecraftClient.getInstance().player.getYaw(), 0, 360);
    }

    public static CompletableFuture<Void> changeYawSmoothAsync(float targetYaw, float degreesPerSecond) {
        return CompletableFuture.runAsync(() -> changeYawSmooth(targetYaw, degreesPerSecond));
    }

    public static CompletableFuture<Void> changePitchSmoothAsync(float targetPitch, float degreesPerSecond) {
        return CompletableFuture.runAsync(() -> changePitchSmooth(targetPitch, degreesPerSecond));
    }

    public static void changeYawSmooth(float targetYaw, float degreesPerSecond) {
        targetYaw = Utils.normalize(targetYaw, 0, 360);
        float degreesPerMs = degreesPerSecond / 1000.0F;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        float yawDirection = (targetYaw - LookHelper.getYaw()) / Math.abs(targetYaw - getYaw());
        long time = System.currentTimeMillis();

        while (!LookHelper.isYawRoughlyClose(LookHelper.getYaw(), targetYaw)) {
            long delta = System.currentTimeMillis() - time;
            time += delta;
            assert player != null;

            player.setYaw(LookHelper.getYaw() + delta * degreesPerMs * yawDirection);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("Exception in runBotThread, don't care");
            }
        }

        assert player != null;
        player.setYaw(targetYaw);
    }

    public static void changePitchSmooth(float targetPitch, float degreesPerSecond) {
        float degreesPerMs = degreesPerSecond / 1000.0F;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        float pitchDirection;
        assert player != null;

        if (targetPitch > player.getPitch()) {
            pitchDirection = 1;
        } else pitchDirection = -1;

        long time = System.currentTimeMillis();
        while (!(Math.abs(player.getPitch() - targetPitch) > degreesPerMs * 50)) {
            long delta = System.currentTimeMillis() - time;

            time += delta;
            player.setPitch(player.getPitch() + delta * degreesPerMs * pitchDirection);

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("Exception in runBotThread, don't care");
            }
        }

        player.setPitch(targetPitch);
    }
}
