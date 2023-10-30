package com.viktorx.skyblockbot.movement;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.CompletableFuture;

public class LookHelper {

    public static boolean isYawRoughlyClose(float yaw1, float yaw2) {
        return Math.abs(yaw1 - yaw2) < 2.0F || Math.abs(Math.abs(yaw1 - yaw2) - 360) < 2.0F;
    }

    public static float getYaw() {
        assert MinecraftClient.getInstance().player != null;

        return Utils.normalize(MinecraftClient.getInstance().player.getYaw(), -180, 180);
    }

    public static CompletableFuture<Void> changeYawSmoothAsync(float targetYaw, float degreesPerSecond) {
        return CompletableFuture.runAsync(() -> changeYawSmooth(targetYaw, degreesPerSecond));
    }

    public static CompletableFuture<Void> changePitchSmoothAsync(float targetPitch, float degreesPerSecond) {
        return CompletableFuture.runAsync(() -> changePitchSmooth(targetPitch, degreesPerSecond));
    }

    public static void changeYawSmooth(float targetYaw, float degreesPerSecond) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        float degreesPerStep = degreesPerSecond / 16.0F;
        float deltaAngle = MathHelper.subtractAngles(getYaw(), targetYaw);

        int steps = (int) Math.abs(deltaAngle / degreesPerStep);
        for(int i = 0; i < steps; i++) {
            player.setYaw(player.getYaw() + deltaAngle / steps);

            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {}
        }
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
