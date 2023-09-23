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

        instance.options.sprintKey.setPressed(true);
        instance.options.forwardKey.setPressed(true); // go
        CompletableFuture<Void> turnExecutor = null;
        while (NotBotCore.runBotThread && loopIterator < loop.size()) {
            // when getting close to the next goal - stop sprinting and start turning
            float distance = getDistanceFromPlayerToGoal(loop.get(loopIterator));
            if (getDistanceFromPlayerToGoal(loop.get(loopIterator)) < 6.0f) {
                if (instance.options.sprintKey.isPressed()) {
                    instance.options.sprintKey.setPressed(false);
                    turnExecutor = doTurn(loop, loopIterator); // TODO consider turning around water
                    loopIterator += 2;
                }
            }
            if(turnExecutor != null) {
                if(turnExecutor.isDone()) {
                    instance.options.sprintKey.setPressed(true);
                    turnExecutor = null;
                }
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.warn("Thread interruption during head turning, wtf?");
                return;
            }
        }
        instance.options.forwardKey.setPressed(false);
        instance.options.sprintKey.setPressed(false);
    }

    private CompletableFuture<Void> doTurn(List<Vec2f> loop, int loopIterator) {
        long millisecondsToTurn = (long) (1000.0f * 2.8f / 5.0f);// TODO (float) Someclass.getSpeed());
        Vec2f v1 = loop.get(loopIterator - 1).add(loop.get(loopIterator).multiply(-1.0f)).normalize();
        Vec2f v2 = loop.get(loopIterator).add(loop.get(loopIterator + 1).multiply(-1.0f)).normalize();
        float angleBetween = (float) Math.acos(v1.dot(v2)); // in radians
        CompletableFuture<Void> turn =
                CompletableFuture.runAsync(() -> doTurnMovementAsync(millisecondsToTurn, angleBetween / Math.abs(angleBetween)));
        return turn;
    }

    //TODO replace magic number with actual integral
    private void doTurnMovementAsync(long millisecondsToTurn, float directionMultiplier) {
        long startTime = System.currentTimeMillis();
        long time = System.currentTimeMillis();
        int iter = 0; // iterator
        int steps = 26; // DO NOT CHANGE, or do it carefully and adjust magic numbers in timestamp functions
        List<Float> dYaw = getDeltasYaw(steps);
        List<Float> dPitch = getDeltasPitch(steps);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float startYaw = LookHelper.getYaw();
        float startPitch = player.getPitch();
        while (System.currentTimeMillis() - startTime <= millisecondsToTurn) {

            if(System.currentTimeMillis() - time >= millisecondsToTurn / steps) {
                time = System.currentTimeMillis();
                player.setYaw(player.getYaw() + dYaw.get(iter) * 180.0f * directionMultiplier);
                player.setPitch(player.getPitch() + dPitch.get(iter) * 40.0f);
                iter++;
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

    private List<Float> getDeltasPitch(int steps) {
        List<Float> result = new ArrayList<>();
        double magicNumber = 1464.666D;
        double dsteps = steps;
        for (int i = 0; i < steps; i++) {
            // this thing here equals to (x^2-x*steps+(steps/2)^2) / ( integral from 0 to number of steps of (x^2-x*steps+(steps/2)^2) )
            double di = i;
            double stepAdder = (dsteps / 2) * (dsteps / 2);
            double negative = 1.0d;
            if(i < steps / 2) { negative = -1.0d; }
            float dPitch = (float) (
                    (di * di - di * dsteps + stepAdder) / magicNumber * negative); // this magic brings integral of this equation to 1 for steps=26
            result.add(dPitch);
        }
        return result;
    }

    private List<Float> getDeltasYaw(int steps) {
        List<Float> result = new ArrayList<>();
        double magicNumber = 2929.333D;
        for (int i = 0; i < steps; i++) {
            // this thing here equals to (-(x^2)+x*steps) / ( integral from 0 to number of steps of (-(x^2)+x*steps) )
            float dYaw = (float) (
                    (-((double) i * (double) i) + (double) i * steps) / magicNumber); // this magic brings integral of this equation to 1 for steps=26
            result.add(dYaw);
        }
        return result;
    }

    public float getDistanceFromPlayerToGoal(Vec2f goal) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Vec2f playerPos = new Vec2f((float) player.getX(), (float) player.getZ());
        return playerPos.add(goal.multiply(-1.0F)).length();
    }
}
