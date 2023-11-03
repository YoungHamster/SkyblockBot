package com.viktorx.skyblockbot.movement;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.CompletableFuture;

public class LookHelper {

    public static float getYaw() {
        assert MinecraftClient.getInstance().player != null;

        return Utils.normalize(MinecraftClient.getInstance().player.getYaw(), -180, 180);
    }

    public static CompletableFuture<Void> changeYawSmoothAsync(float targetYaw) {
        return CompletableFuture.runAsync(() -> changeYawSmooth(targetYaw));
    }

    public static CompletableFuture<Void> changePitchSmoothAsync(float targetPitch, float degreesPerSecond) {
        return CompletableFuture.runAsync(() -> changePitchSmooth(targetPitch, degreesPerSecond));
    }

    public static void changeYawSmooth(float targetYaw) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;

        float firstDeltaAngle = MathHelper.subtractAngles(getYaw(), targetYaw);
        float firstYaw = player.getYaw();
        float deltaAngle = firstDeltaAngle;
        float degreesPerStep = deltaAngle / 20.0F;

        while(Math.abs(MathHelper.subtractAngles(getYaw(), targetYaw)) > 0.3f) {
            player.setYaw(player.getYaw() + degreesPerStep);

            if(Math.abs(degreesPerStep) >= 0.2f) {
                deltaAngle = MathHelper.subtractAngles(getYaw(), targetYaw);
                degreesPerStep = deltaAngle / 20.0F;
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {}
        }

        player.setYaw(firstYaw + firstDeltaAngle);
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

    public static void setYaw(float yaw) {
        float delta = MathHelper.subtractAngles(getYaw(), Utils.normalize(yaw, -180, 180));
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        player.setYaw(player.getYaw() + delta);
    }
}
