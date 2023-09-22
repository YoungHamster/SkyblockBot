package com.viktorx.skyblockbot;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.movement.MovementProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class NotBotCore {

    private static Bot botThread = null;
    public static boolean runBotThread = true;
    private static Thread t;

    public static void run(ClientPlayerEntity client) {
        if(botThread == null) {
            botThread = new Bot();
        }
        runBotThread = true;
        Thread t = new Thread(botThread);
        t.start();
    }

    public static void stop() {
        runBotThread = false;
        //t.interrupt();
    }
}
