package com.viktorx.skyblockbot;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
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
            Rotation playerRotation = baritone.getPlayerContext().playerRotations();
            float newYaw = playerRotation.normalizeAndClamp().getYaw() + 180.0F;
            if (newYaw % 90.0F > 45.0F) newYaw = playerRotation.getYaw() + (90.0F - newYaw % 90.0F);
            else newYaw = playerRotation.getYaw() - newYaw % 90.0F;
            baritone.getLookBehavior().updateTarget(new Rotation(newYaw, playerRotation.getPitch()).normalize(), true);

            // Player sets the pitch, bot obliges
            float targetPitch = playerRotation.getPitch();

            // this is where the magic happens
            List<BetterBlockPos> loopAroundFarm = getPosListForCrop(baritone, "Carrots");

            int nextPos = 1;
            while (runBotThread) {
                if (!baritone.getCustomGoalProcess().isActive()) {
                    if (nextPos == loopAroundFarm.size()) { // if we did the loop-go through it again
                        nextPos = 0;
                        baritone.getInputOverrideHandler().clearAllKeys(); // and don't break anything while going to the starting point
                    } else if (nextPos == 1) {
                        Keybinds.keepKeyPressed(MinecraftClient.getInstance().options.attackKey);
                    }
                    turnHeadCorrectly(baritone, loopAroundFarm.get(nextPos));
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(loopAroundFarm.get(nextPos++)));
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

        private void turnHeadCorrectly(IBaritone baritone, BetterBlockPos nextPos, float targetPitch) {
            float targetYaw;
            for(int i = 0; i < 20; i++) {

            }
        }

        // List of positions bot should go through to loop around whole farm and come back to the starting point
        private List<BetterBlockPos> getPosListForCrop(IBaritone baritone, String cropBlockName) {
            List<BetterBlockPos> nodes = new ArrayList<>();

            Rotation playerRot = baritone.getPlayerContext().playerRotations();
            BlockPos pos = baritone.getPlayerContext().playerFeet();
            World world = baritone.getPlayerContext().world();
            List<Vec3i> directions = new ArrayList<>();
            if (playerRot.isReallyCloseTo(new Rotation(0.0f, playerRot.getPitch()))) {
                directions.add(new Vec3i(0, 0, 1)); // south
                directions.add(new Vec3i(-1, 0, 0)); // west
                directions.add(new Vec3i(0, 0, -1)); // north
            } else if (playerRot.isReallyCloseTo(new Rotation(90.0f, playerRot.getPitch()))) {
                directions.add(new Vec3i(-1, 0, 0)); // west
                directions.add(new Vec3i(0, 0, -1)); // north
                directions.add(new Vec3i(1, 0, 0)); // east
            } else if (playerRot.isReallyCloseTo(new Rotation(180.0f, playerRot.getPitch()))) {
                directions.add(new Vec3i(0, 0, -1)); // north
                directions.add(new Vec3i(1, 0, 0)); // east
                directions.add(new Vec3i(0, 0, 1)); // south
            } else {
                directions.add(new Vec3i(1, 0, 0)); // east
                directions.add(new Vec3i(0, 0, 1)); // south
                directions.add(new Vec3i(-1, 0, 0)); // west
            }
            int direction = 0; // for example - first we go north, then move one block, then go south. This 0 is for going north
            do {
                nodes.add(new BetterBlockPos(pos));
                BlockPos pos2 = pos.add(directions.get(direction));
                String str = world.getBlockState(pos.add(directions.get(direction))).getBlock().getName().getString();
                while (world.getBlockState(pos.add(directions.get(direction))).getBlock().getName().getString().equals(cropBlockName)) {
                    pos = pos.add(directions.get(direction));
                }
                nodes.add(new BetterBlockPos(pos));
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

    public static void stop(ClientPlayerEntity client) {
        runBotThread = false;
    }
}
