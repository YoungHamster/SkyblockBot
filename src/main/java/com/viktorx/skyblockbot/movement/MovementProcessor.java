package com.viktorx.skyblockbot.movement;

import baritone.api.utils.BetterBlockPos;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class MovementProcessor {
    // theoretically the most realistic looking movement should be turning your head at the same time as running
    // but that's kinda difficult to implement
    // so for now I'll probably do turning, then running
    public static void gotoXZ(BetterBlockPos xz) {

    }

    // it's not just hitting W, it's moving your head like if you were a real player
    private static void goForwarwardUntilXZ(BetterBlockPos xz) {
        MinecraftClient instance = MinecraftClient.getInstance();
        Keybinds.keepKeyPressed(instance.options.forwardKey);
        while(instance.player.getBlockX() != xz.getX() && instance.player.getBlockZ() != xz.getZ()) {
            // should probably use perlin noise or some other noise for slight random "mouse" movements
            // adjust direction based on what cursor is aiming at and not based on player position
        }
    }
}
