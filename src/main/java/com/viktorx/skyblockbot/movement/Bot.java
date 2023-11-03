package com.viktorx.skyblockbot.movement;

import com.viktorx.skyblockbot.NotBotCore;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.movement.MovementProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class Bot implements Runnable {

    private final MovementProcessor movementProcessor = new MovementProcessor();

    @Override
    public void run() {

        // assume player starts the process looking roughly in the direction of first carrot row
        // and standing at the starting position

        // Player sets the pitch, bot obliges
        assert MinecraftClient.getInstance().player != null;

        LookHelper.changeYawSmooth(MinecraftClient.getInstance().player.getMovementDirection().asRotation());

        // this is where the magic happens
        List<Vec2f> checkpoints = createPathAroundField("Carrots");
        int turnHeadIterations = 0;

        int nextPos = 1;
        boolean loop = false;

        while (NotBotCore.runBotThread) {
            movementProcessor.doALoop(checkpoints);
        }
    }

    // List of positions bot should go through to loop around whole farm and come back to the starting point
    private List<Vec2f> createPathAroundField(String cropBlockName) { // field as in "corn field"
        List<Vec2f> nodes = new ArrayList<>();

        assert MinecraftClient.getInstance().player != null;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        BlockPos pos = MinecraftClient.getInstance().player.getBlockPos();
        World world = MinecraftClient.getInstance().world;
        List<Vec3i> directions = new ArrayList<>();

        directions.add(player.getMovementDirection().getVector()); // go forward
        directions.add(player.getMovementDirection().rotateYClockwise().getVector()); // go to the side
        directions.add(player.getMovementDirection().rotateYClockwise().rotateYClockwise().getVector()); // go backward

        // if not the whole farm is loaded this algorithm may fuck up. Best case scenario-it just stops the loop where loaded part ends
        // TODO: right now it won't work correctly if there is an uneven number of rows of crops, should be an easy fix for later
        int direction = 0; // for example - first we go north, then move one block, then go south. This 0 is for going north
        pos = pos.add(directions.get(direction));

        do {
            if (direction == 0) {
                BlockPos newNode = pos.add(directions.get(2));// it's hard to explain, but easy in principle, figure it out
                nodes.add(new Vec2f(newNode.getX(), newNode.getZ()));
            } else {
                nodes.add(new Vec2f(pos.getX(), pos.getZ()));
            }

            while (world.getBlockState(pos.add(directions.get(direction))).getBlock().getName().getString().equals(cropBlockName)) {
                pos = pos.add(directions.get(direction));
            }

            if (direction == 2) {
                // move 1 more block when going back for more square-like pathing(should also look more human)
                BlockPos newNode = pos.add(directions.get(direction));
                nodes.add(new Vec2f(newNode.getX(), newNode.getZ()));
            } else {
                nodes.add(new Vec2f(pos.getX(), pos.getZ()));
            }

            pos = pos.add(directions.get(1)); // this 1 is for going east in the example
            // for example - we went north for the whole row, then we move 1 block east, if there is no crops
            // it could not be the end of the farm, but a water row, so we have to move 1 more block east
            if (!world.getBlockState(pos).getBlock().getName().getString().equals(cropBlockName)) {
                pos = pos.add(directions.get(1));
            }

            if (direction == 0) direction = 2; // this 2 is for going south in the example
            else direction = 0;
        } while (world.getBlockState(pos).getBlock().getName().getString().equals(cropBlockName));

        for (Vec2f node : nodes) {
            node.add(new Vec2f(0.5f, 0.5f));
        }

        return nodes;
    }
}
