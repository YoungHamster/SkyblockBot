package com.viktorx.skyblockbot.movement;

import baritone.api.utils.Rotation;
import com.viktorx.skyblockbot.SkyblockBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class LookHelper {
    public static boolean isYawRoughlyClose(float yaw1, float yaw2) {
        return Math.abs(yaw1 - yaw2) < 5.0F || Math.abs(Math.abs(yaw1 - yaw2) - 360) < 5.0F;
    }

    public static float getYaw() {
        assert MinecraftClient.getInstance().player != null;
        return Rotation.normalizeYaw(MinecraftClient.getInstance().player.getYaw());
    }

    public static void turnHeadSmooth(float targetYaw, float targetPitch) {
        float degreesPerMs = 360.0F / 1000.0F;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float yawDirection = (targetYaw - LookHelper.getYaw()) / Math.abs(targetYaw - getYaw());
        float pitchDirection;
        assert player != null;
        if (targetPitch > player.getPitch()) pitchDirection = 1;
        else pitchDirection = -1;

        long time = System.currentTimeMillis();
        boolean yawDone = false;
        boolean pitchDone = false;
        while(!yawDone || !pitchDone) {
            long delta = System.currentTimeMillis() - time;
            time += delta;
            if(!LookHelper.isYawRoughlyClose(LookHelper.getYaw(), targetYaw))
                player.setYaw(LookHelper.getYaw() + delta * degreesPerMs * yawDirection);
            else {
                player.setYaw(targetYaw);
                yawDone = true;
            }

            if(Math.abs(player.getPitch() - targetPitch) > 5.0F)
                player.setPitch(player.getPitch() + delta * degreesPerMs * pitchDirection);
            else {
                player.setPitch(targetPitch);
                pitchDone = true;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("Exception in runBotThread, don't care");
            }
        }
    }
}
