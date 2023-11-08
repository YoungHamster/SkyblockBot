package com.viktorx.skyblockbot.movement;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.InputRelated.IMouseMixin;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class LookHelper {

    public static float getYaw() {
        assert MinecraftClient.getInstance().player != null;

        return Utils.normalize(MinecraftClient.getInstance().player.getYaw(), -180, 180);
    }

    public static void setYaw(float yaw) {
        float delta = MathHelper.subtractAngles(getYaw(), Utils.normalize(yaw, -180, 180));
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        player.setYaw(player.getYaw() + delta);
    }

    public static CompletableFuture<Void> changeYawSmoothAsync(float targetYaw) {
        return CompletableFuture.runAsync(() -> changeYawSmooth(targetYaw));
    }

    public static CompletableFuture<Void> changePitchSmoothAsync(float targetPitch) {
        return CompletableFuture.runAsync(() -> changePitchSmooth(targetPitch));
    }

    public static void changeYawSmooth(float targetYaw) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;

        float deltaAngle = MathHelper.subtractAngles(getYaw(), targetYaw);
        float degreesPerStep = deltaAngle / 20.0F;

        while (Math.abs(MathHelper.subtractAngles(getYaw(), targetYaw)) > 0.3f) {
            player.setYaw(player.getYaw() + degreesPerStep);

            if (Math.abs(degreesPerStep) >= 0.2f) {
                deltaAngle = MathHelper.subtractAngles(getYaw(), targetYaw);
                degreesPerStep = deltaAngle / 20.0F;
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {
            }
        }

        setYaw(targetYaw);
    }

    public static void changePitchSmooth(float targetPitch) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;

        float deltaAngle = MathHelper.subtractAngles(player.getPitch(), targetPitch);
        float degreesPerStep = deltaAngle / 20.0F;

        while (Math.abs(MathHelper.subtractAngles(player.getPitch(), targetPitch)) > 0.3f) {
            player.setPitch(player.getPitch() + degreesPerStep);

            if (Math.abs(degreesPerStep) >= 0.2f) {
                deltaAngle = MathHelper.subtractAngles(player.getPitch(), targetPitch);
                degreesPerStep = deltaAngle / 20.0F;
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {
            }
        }

        player.setPitch(targetPitch);
    }

    public static CompletableFuture<Void> lookAtEntityAsync(Entity entity) {
        return CompletableFuture.runAsync(() -> lookAtEntity(entity));
    }

    public static void lookAtEntity(Entity entity) {
        Vec2f rotation = getLookVecForEntity(entity);

        CompletableFuture<Void> pitchTask = changePitchSmoothAsync(rotation.x);
        changeYawSmooth(rotation.y);

        while (!pitchTask.isDone()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void trackEntityAsync(Entity entity, AtomicBoolean keepTracking) {
        CompletableFuture.runAsync(() -> trackEntity(entity, keepTracking));
    }

    public static void trackEntity(Entity entity, AtomicBoolean keepTracking) {
        while (keepTracking.get()) {
            if(MinecraftClient.getInstance().currentScreen != null) {
                SkyblockBot.LOGGER.warn("Current screen isn't null!!!! Stopping entity tracking for entity: " + entity.getName().getString());
                return;
            }

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            assert player != null;

            Vec2f target = getLookVecForEntity(entity);

            float deltaPitch = MathHelper.subtractAngles(player.getPitch(), target.x);
            float pitchStep = deltaPitch * 0.05f;
            player.setPitch(player.getPitch() + pitchStep);


            float deltaYaw = MathHelper.subtractAngles(getYaw(), target.y);
            float yawStep = deltaYaw * 0.05f;
            player.setYaw(player.getYaw() + yawStep);

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("Interrupted track task!");
                return;
            }
        }
    }

    private static Vec2f getLookVecForEntity(Entity entity) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        Vec3d entityPos = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(entity);

        /*
         * Took this code from minecraft sources
         */
        assert player != null;
        Vec3d vec3d = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(player);
        double d = entityPos.x - vec3d.x;
        double e = entityPos.y - vec3d.y;
        double f = entityPos.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);
        float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875)));
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875) - 90.0F);

        return new Vec2f(pitch, yaw);
    }

    public static Vec2f getAngleDeltaForEntity(Entity entity) {
        Vec2f target = getLookVecForEntity(entity);

        assert MinecraftClient.getInstance().player != null;
        float deltaPitch = MathHelper.subtractAngles(MinecraftClient.getInstance().player.getPitch(), target.x);
        float deltaYaw = MathHelper.subtractAngles(getYaw(), target.y);

        return new Vec2f(deltaPitch, deltaYaw);
    }
}
