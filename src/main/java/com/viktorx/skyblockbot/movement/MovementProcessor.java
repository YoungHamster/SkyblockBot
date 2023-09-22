package com.viktorx.skyblockbot.movement;

import com.viktorx.skyblockbot.NotBotCore;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class MovementProcessor {

    Random random = new Random();

    // theoretically the most realistic looking movement should be turning your head at the same time as running
    // but that's kinda difficult to implement
    // so for now I'll probably do turning, then running
    public void doALoop(List<Vec2f> loop) {
        MinecraftClient instance = MinecraftClient.getInstance();

        int loopIterator = 1;

        Keybinds.unpressKey(instance.options.sprintKey);
        Keybinds.keepKeyPressed(instance.options.forwardKey); // go
        while (NotBotCore.runBotThread && loopIterator < loop.size()){
            // when getting close to the next goal - stop sprinting and start turning
            float distance = getDistanceFromPlayerToGoal(loop.get(loopIterator));
            if (getDistanceFromPlayerToGoal(loop.get(loopIterator)) < 8.0f) {
                if (Keybinds.isKeyPressed(instance.options.sprintKey)) {
                    Keybinds.unpressKey(instance.options.sprintKey);
                    doTurn(loop, loopIterator); // TODO consider turning around water
                    loopIterator += 2;
                }
            } else if (!Keybinds.isKeyPressed(instance.options.sprintKey)) {
                Keybinds.keepKeyPressed(instance.options.sprintKey);
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.warn("Thread interruption during head turning, wtf?");
                return;
            }
        }
        Keybinds.unpressKey(instance.options.forwardKey);
        Keybinds.unpressKey(instance.options.sprintKey);
    }

    private void doTurn(List<Vec2f> loop, int loopIterator) {
        float s1 = MinecraftClient.getInstance().player.getMovementSpeed();
        float s2 = MinecraftClient.getInstance().player.speed;
        float s3 = MinecraftClient.getInstance().player.forwardSpeed;
        float s4 = MinecraftClient.getInstance().player.horizontalSpeed;
        float s5 = MinecraftClient.getInstance()
        long millisecondsToTurn = (long) (500.0f * 2.8f / MinecraftClient.getInstance().player.speed);
        Vec2f v1 = loop.get(loopIterator - 1).add(loop.get(loopIterator).multiply(-1.0f)).normalize();
        Vec2f v2 = loop.get(loopIterator).add(loop.get(loopIterator + 1).multiply(-1.0f)).normalize();
        float angleBetween = (float)Math.acos(v1.dot(v2)); // in radians
        CompletableFuture<Void> turn =
                CompletableFuture.runAsync(() -> doTurnMovementAsync(millisecondsToTurn, angleBetween / Math.abs(angleBetween)));
    }

    //TODO replace magic number with actual integral
    private void doTurnMovementAsync(long millisecondsToTurn, float directionMultiplier) {
        long startTime = System.currentTimeMillis();
        long timeYaw = System.currentTimeMillis();
        long timePitch = System.currentTimeMillis();
        int iYaw = 0; // iterator
        int iPitch = 0; // iterator
        int steps = 26; // DO NOT CHANGE, or do it carefully and adjust magic number below
        List<Long> yawTimestamps = getTimestampsForYaw(steps, millisecondsToTurn, startTime);
        List<Long> pitchTimestamps = getTimestampsForPitch(steps, millisecondsToTurn, startTime);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float startYaw = LookHelper.getYaw();
        float startPitch = player.getPitch();
        while (System.currentTimeMillis() - startTime <= millisecondsToTurn) {

            if (System.currentTimeMillis() - timeYaw >= yawTimestamps.get(iYaw)) {
                timeYaw = System.currentTimeMillis();
                player.setYaw(startYaw + 180.0f / steps * iYaw++);
            }

            if (System.currentTimeMillis() - timePitch >= pitchTimestamps.get(iPitch)) {
                timePitch = System.currentTimeMillis();
                if(iPitch < steps / 2) {
                    player.setPitch(startPitch - (40.0f / steps) * iPitch++);
                } else {
                    player.setPitch(player.getPitch() + (40.0f / steps));
                    iPitch++;
                }
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.warn("Thread interruption during head turning, wtf?");
                return;
            }
        }
        player.setYaw(startYaw + 180.0f * directionMultiplier);
        player.setPitch(startPitch);
    }

    private List<Long> getTimestampsForYaw(int steps, long millisecondsToTurn, long startTime) {
        List<Long> result = new ArrayList<>();
        long timestamp = startTime;
        double magicNumber = 14646.666D;
        for (int i = 0; i < steps; i++) {
            // this thing here equals to (x^2-x*steps) / ( integral from 0 to number of steps of (x^2-x*steps) ) * msToTurn
            long dtYaw = (long) (
                    ((double) i * (double) i - (double) i * (double) steps) / magicNumber * millisecondsToTurn); // this magic brings integral of this equation to 1 for steps=26
            result.add(timestamp + dtYaw);
            timestamp = timestamp + dtYaw;
        }
        return result;
    }

    private List<Long> getTimestampsForPitch(int steps, long millisecondsToTurn, long startTime) {
        List<Long> result = new ArrayList<>();
        long timestamp = startTime;
        double magicNumber = 2929.333D;
        for (int i = 0; i < steps; i++) {
            // this thing here equals to (-(x^2)+x*steps) / ( integral from 0 to number of steps of (-(x^2)+x*steps) ) * msToTurn
            long dtPitch = (long) (
                    (-((double) i * (double) i) + (double) i * steps) / magicNumber * millisecondsToTurn); // this magic brings integral of this equation to 1 for steps=26
            result.add(timestamp + dtPitch);
            timestamp = timestamp + dtPitch;
        }
        return result;
    }

    public float getDistanceFromPlayerToGoal(Vec2f goal) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Vec2f playerPos = new Vec2f((float) player.getX(), (float) player.getZ());
        return playerPos.add(goal.multiply(-1.0F)).length();
    }
}
