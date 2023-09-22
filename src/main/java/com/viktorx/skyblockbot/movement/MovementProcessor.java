package com.viktorx.skyblockbot.movement;

import baritone.api.utils.BetterBlockPos;
import com.viktorx.skyblockbot.NoiseGenerator;
import com.viktorx.skyblockbot.NotBotCore;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class MovementProcessor {

    NoiseGenerator perlin = new NoiseGenerator();

    // theoretically the most realistic looking movement should be turning your head at the same time as running
    // but that's kinda difficult to implement
    // so for now I'll probably do turning, then running
    public void turnHead(BetterBlockPos xz) {

    }

    // it's not just hitting W, it's moving your head like if you were a real player
    public void goForwardUntilXZ(BetterBlockPos xz) {
        MinecraftClient instance = MinecraftClient.getInstance();
        Keybinds.keepKeyPressed(instance.options.forwardKey);
        long time = System.currentTimeMillis();
        while((instance.player.getBlockX() != xz.getX() || instance.player.getBlockZ() != xz.getZ()) && NotBotCore.runBotThread) {
            // should probably use perlin noise or some other noise for slight random "mouse" movements
            // adjust direction based on what cursor is aiming at and not based on player position
            float val = (float)perlin.noise(LookHelper.getYaw()) % 0.5F;
            if(System.currentTimeMillis() - time > 100) {
                time = System.currentTimeMillis();
                LookHelper.turnHeadSmoothAsync(
                        LookHelper.getYaw() + ((float)perlin.noise(LookHelper.getYaw()) % 0.5F),
                        instance.player.getPitch(),
                        90.0F);
            }
            Vec2f fromPLayerToGoal = new Vec2f(xz.x + 0.5F, xz.y + 0.5F)
                    .add(new Vec2f((float)instance.player.getX(), (float)instance.player.getY()).multiply(-1.0F));
            Vec2f yawVec = new Vec2f((float)Math.cos(LookHelper.getYaw()), (float)Math.sin(LookHelper.getYaw())); // TODO
            float angleBetweenVecs = Utils.angleBetweenVecs(fromPLayerToGoal, yawVec); // TODO
            if(angleBetweenVecs < -3.0F) {
                LookHelper.turnHeadSmoothAsync(instance.player.getYaw() + 1.0F, instance.player.getPitch(), 90.0F);
            } else if (angleBetweenVecs > 3.0F) {
                LookHelper.turnHeadSmoothAsync(instance.player.getYaw() - 1.0F, instance.player.getPitch(), 90.0F);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.warn("Thread interruption during walking, wtf?");
                Keybinds.clearPressedKeys();
                return;
            }
        }
        Keybinds.clearPressedKeys();
    }
}
