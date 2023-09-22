package com.viktorx.skyblockbot;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.movement.LookHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class NotBotCore {
    private static class Bot implements Runnable {
        public ClientPlayerEntity client;

        @Override
        public void run() {
            IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(client);
            BaritoneAPI.getSettings().randomLooking.value = 0.0D;
            BaritoneAPI.getSettings().randomLooking113.value = 0.0D;
            BaritoneAPI.getSettings().antiCheatCompatibility.value = true;

            // assume player starts the process looking roughly in the direction of first carrot row
            // and standing at the starting position

            // all this stuff just aligns the camera
            float oldYaw = LookHelper.getYaw();
            float newYaw = oldYaw + 180.0F;
            if (newYaw % 90.0F > 45.0F) newYaw = oldYaw + (90.0F - newYaw % 90.0F);
            else newYaw = oldYaw - newYaw % 90.0F;

            // Player sets the pitch, bot obliges
            assert MinecraftClient.getInstance().player != null;
            float targetPitch = MinecraftClient.getInstance().player.getPitch();

            LookHelper.turnHeadSmooth(newYaw, targetPitch);

            // this is where the magic happens
            List<BetterBlockPos> checkpoints = createPathAroundField(baritone, "Carrots");
            int turnHeadIterations = 0;

            int nextPos = 1;
            boolean loop = false;
            while (runBotThread) {
                if (!baritone.getCustomGoalProcess().isActive()) { // hit checkpoint-turn and go to the next one
                    if (nextPos == checkpoints.size()) { // if we did the loop-go through it again
                        nextPos = 0;
                        Keybinds.clearPressedKeys(); // and don't break anything while going to the starting point
                        LookHelper.turnHeadSmooth(LookHelper.getYaw() + 90.0F, targetPitch);
                        loop = true;
                    } else if (nextPos == 1) {
                        if(loop) {
                            LookHelper.turnHeadSmooth(LookHelper.getYaw() + 90.0F, targetPitch);
                            loop = false;
                            turnHeadIterations = 0;
                        }
                        Keybinds.keepKeyPressed(MinecraftClient.getInstance().options.attackKey); // if we're going to start the loop-start breaking
                    }

                    if(nextPos != 1 && !loop) {
                        switch (turnHeadIterations++) {
                            case 0 -> LookHelper.turnHeadSmooth(LookHelper.getYaw() + 90.0F, 60.0F);
                            case 1 -> LookHelper.turnHeadSmooth(LookHelper.getYaw() + 90.0F, targetPitch);
                            case 2 -> LookHelper.turnHeadSmooth(LookHelper.getYaw() - 90.0F, 60.0F);
                            case 3 -> {
                                LookHelper.turnHeadSmooth(LookHelper.getYaw() - 90.0F, targetPitch);
                                turnHeadIterations = 0;
                            }
                        }
                    }
                    // go go go
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(checkpoints.get(nextPos++)));
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    SkyblockBot.LOGGER.info("Exception in runBotThread, don't care");
                }
            }
            Keybinds.clearPressedKeys();
            baritone.getPathingBehavior().cancelEverything();
        }

        // List of positions bot should go through to loop around whole farm and come back to the starting point
        private List<BetterBlockPos> createPathAroundField(IBaritone baritone, String cropBlockName) { // field as in "corn field"
            List<BetterBlockPos> nodes = new ArrayList<>();

            assert MinecraftClient.getInstance().player != null;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            BlockPos pos = baritone.getPlayerContext().playerFeet();
            World world = baritone.getPlayerContext().world();
            List<Vec3i> directions = new ArrayList<>();
            directions.add(player.getMovementDirection().getVector()); // go forward
            directions.add(player.getMovementDirection().rotateYClockwise().getVector()); // go to the side
            directions.add(player.getMovementDirection().rotateYClockwise().rotateYClockwise().getVector()); // go backward

            // if not the whole farm is loaded this algorithm may fuck up. Best case scenario-it just stops the loop where loaded part ends
            // TODO: right now it won't work correctly if there is an uneven number of rows of crops, should be an easy fix for later
            int direction = 0; // for example - first we go north, then move one block, then go south. This 0 is for going north
            pos = pos.add(directions.get(direction));
            do {
                if(direction == 0) {
                    nodes.add(new BetterBlockPos(pos.add(directions.get(2)))); // it's hard to explain, but easy in principle, figure it out
                } else {
                    nodes.add(new BetterBlockPos(pos));
                }
                while (world.getBlockState(pos.add(directions.get(direction))).getBlock().getName().getString().equals(cropBlockName)) {
                    pos = pos.add(directions.get(direction));
                }
                if(direction == 2) {
                    // move 1 more block when going back for more square-like pathing(should also look more human)
                    nodes.add(new BetterBlockPos(pos.add(directions.get(direction))));
                } else {
                    nodes.add(new BetterBlockPos(pos));
                }
                pos = pos.add(directions.get(1)); // this 1 is for going east in the example
                // for example - we went north for the whole row, then we move 1 block east, if there is no crops
                // it could not be the end of the farm, but a water row, so we have to move 1 more block east
                if(!world.getBlockState(pos).getBlock().getName().getString().equals(cropBlockName)) {
                    pos = pos.add(directions.get(1));
                }
                if (direction == 0) direction = 2; // this 2 is for going south in the example
                else direction = 0;
            } while (world.getBlockState(pos).getBlock().getName().getString().equals(cropBlockName));
            return nodes;
        }
    }

    private static final Bot botThread = new Bot();
    private static boolean runBotThread = true;

    public static void run(ClientPlayerEntity client) {
        botThread.client = client;
        runBotThread = true;
        Thread t = new Thread(botThread);
        t.start();
    }

    public static void stop() {
        runBotThread = false;
    }
}
