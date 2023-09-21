package com.viktorx.skyblockbot;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
            float oldYaw = Utils.getYaw();
            float newYaw = oldYaw + 180.0F;
            if (newYaw % 90.0F > 45.0F) newYaw = oldYaw + (90.0F - newYaw % 90.0F);
            else newYaw = oldYaw - newYaw % 90.0F;

            // Player sets the pitch, bot obliges
            float targetPitch = MinecraftClient.getInstance().player.getPitch();

            turnHeadSmooth(newYaw, targetPitch);

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
                        turnHeadSmooth(Utils.getYaw() + 90.0F, targetPitch);
                        loop = true;
                    } else if (nextPos == 1) {
                        if(loop) {
                            turnHeadSmooth(Utils.getYaw() + 90.0F, targetPitch);
                            loop = false;
                            turnHeadIterations = 0;
                        }
                        Keybinds.keepKeyPressed(MinecraftClient.getInstance().options.attackKey); // if we're going to start the loop-start breaking
                    }

                    if(nextPos != 1 && !loop) {
                        switch (turnHeadIterations++) {
                            case 0 -> turnHeadSmooth(Utils.getYaw() + 90.0F, 60.0F);
                            case 1 -> turnHeadSmooth(Utils.getYaw() + 90.0F, targetPitch);
                            case 2 -> turnHeadSmooth(Utils.getYaw() - 90.0F, 60.0F);
                            case 3 -> {
                                turnHeadSmooth(Utils.getYaw() - 90.0F, targetPitch);
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

        private void turnHeadSmooth(float targetYaw, float targetPitch) {
            float degreesPerMs = 360.0F / 1000.0F;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            float yawDirection = (targetYaw - Utils.getYaw()) / Math.abs(targetYaw - Utils.getYaw());
            float pitchDirection;
            if (targetPitch > player.getPitch()) pitchDirection = 1;
            else pitchDirection = -1;

            long time = System.currentTimeMillis();
            boolean yawDone = false;
            boolean pitchDone = false;
            while(!yawDone || !pitchDone) {
                long delta = System.currentTimeMillis() - time;
                time += delta;
                if(!Utils.isYawRoughlyClose(Utils.getYaw(), targetYaw))
                    player.setYaw(Utils.getYaw() + delta * degreesPerMs * yawDirection);
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

        // List of positions bot should go through to loop around whole farm and come back to the starting point
        private List<BetterBlockPos> createPathAroundField(IBaritone baritone, String cropBlockName) { // field as in "corn field"
            List<BetterBlockPos> nodes = new ArrayList<>();

            float clientYaw = MinecraftClient.getInstance().player.getYaw();
            BlockPos pos = baritone.getPlayerContext().playerFeet();
            World world = baritone.getPlayerContext().world();
            List<Vec3i> directions = new ArrayList<>();
            if (Utils.isYawRoughlyClose(clientYaw, 0.0F)) {
                directions.add(new Vec3i(0, 0, 1)); // south
                directions.add(new Vec3i(-1, 0, 0)); // west
                directions.add(new Vec3i(0, 0, -1)); // north
            } else if (Utils.isYawRoughlyClose(clientYaw, 90.0F)) {
                directions.add(new Vec3i(-1, 0, 0)); // west
                directions.add(new Vec3i(0, 0, -1)); // north
                directions.add(new Vec3i(1, 0, 0)); // east
            } else if (Utils.isYawRoughlyClose(clientYaw, 180.0F)) {
                directions.add(new Vec3i(0, 0, -1)); // north
                directions.add(new Vec3i(1, 0, 0)); // east
                directions.add(new Vec3i(0, 0, 1)); // south
            } else {
                directions.add(new Vec3i(1, 0, 0)); // east
                directions.add(new Vec3i(0, 0, 1)); // south
                directions.add(new Vec3i(-1, 0, 0)); // west
            }
            int direction = 0; // for example - first we go north, then move one block, then go south. This 0 is for going north
            pos = pos.add(directions.get(direction));
            do {
                if(direction == 0) {
                    nodes.add(new BetterBlockPos(pos.add(directions.get(2)))); // it's hard to explain, but easy in principle, figure it out
                } else {
                    nodes.add(new BetterBlockPos(pos));
                }
                BlockPos pos2 = pos.add(directions.get(direction));
                String str = world.getBlockState(pos.add(directions.get(direction))).getBlock().getName().getString();
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

    public static void stop(ClientPlayerEntity client) {
        runBotThread = false;
    }
}
