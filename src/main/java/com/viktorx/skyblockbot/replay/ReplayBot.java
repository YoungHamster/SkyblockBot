package com.viktorx.skyblockbot.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import com.viktorx.skyblockbot.movement.LookHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReplayBot {

    private static final List<TickState> tickStates = new ArrayList<>();
    private static int tickIterator;

    private static boolean recording = false;
    private static boolean playing = false;

    private static CompletableFuture<Void> yawTask = null;
    private static CompletableFuture<Void> pitchTask = null;

    private static boolean antiDetectTrigger = false;
    private static String itemWhenStarted;
    public static boolean serverChangedPositionRotation = false;
    public static boolean serverChangedItem = false;
    public static boolean serverChangedSlot = false;
    private static boolean isStuck = false;
    private static int stucknessCounter = 0;
    private static List<TickState> lastNTicks = new ArrayList<>();
    public static int debugRecordingPacketCounter = 0;
    public static int debugPlayingPacketCounter = 0;
    public static int debugOnGroundOnlyCounter = 0;
    public static int debugLookAndOnGroundCounter = 0;
    public static int debugPositionAndOnGroundCounter = 0;
    public static int debugFullCounter = 0;

    protected final static String ABSOLUTE_PATH_BIN = "C:/Users/Nobody/AppData/Roaming/.minecraft/recording.bin";

    public static void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(ReplayBot::onTickRec);
        ClientTickEvents.START_CLIENT_TICK.register(ReplayBot::onTickPlay);
    }

    private static void onTickRec(MinecraftClient client) {
        if (!recording) {
            return;
        }

        assert client.player != null;
        detectAndCorrectLagBack(client);

        tickStates.add(
                new TickState(
                        client.player.getPos(),
                        new Vec2f(Utils.normalize(client.player.getYaw()), Utils.normalize(client.player.getPitch())),
                        client.options.attackKey.isPressed(),
                        client.options.forwardKey.isPressed(),
                        client.options.backKey.isPressed(),
                        client.options.rightKey.isPressed(),
                        client.options.leftKey.isPressed(),
                        client.options.sneakKey.isPressed(),
                        client.options.sprintKey.isPressed(),
                        client.options.jumpKey.isPressed()
                )
        );
    }

    public static void startRecording() {
        if (playing) {
            SkyblockBot.LOGGER.info("Can't start recording when playing");
            return;
        }

        if (recording) {
            SkyblockBot.LOGGER.info("Already recording");
            return;
        }

        SkyblockBot.LOGGER.info("started recording");
        tickStates.clear();
        debugRecordingPacketCounter = 0;

        debugOnGroundOnlyCounter = 0;
        debugLookAndOnGroundCounter = 0;
        debugPositionAndOnGroundCounter = 0;
        debugFullCounter = 0;

        recording = true;
    }

    public static void stopRecording() {
        SkyblockBot.LOGGER.info("stopped recording");
        recording = false;

        SkyblockBot.LOGGER.info("recording packet counter = " + debugRecordingPacketCounter);
        SkyblockBot.LOGGER.info("on ground only counter = " + debugOnGroundOnlyCounter);
        SkyblockBot.LOGGER.info("look and on ground counter = " + debugLookAndOnGroundCounter);
        SkyblockBot.LOGGER.info("position and on ground counter = " + debugPositionAndOnGroundCounter);
        SkyblockBot.LOGGER.info("full counter = " + debugFullCounter);

        saveRecordingAsync();
    }

    private static void onTickPlay(MinecraftClient client) {
        if (!ReplayBot.playing) {
            return;
        }

        if (yawTask != null && pitchTask != null) {
            if (!yawTask.isDone() || !pitchTask.isDone()) {
                return;
            } else {
                yawTask = null;
                pitchTask = null;
            }
        }

        if (!antiDetectTrigger) {
            if (antiDetect(client)) {
                antiDetectTrigger = true;
                playing = false;
                unpressButtons();
                asyncPlayAlarmSound();
                return;
            }
        }

        TickState state = tickStates.get(tickIterator);

        assert client.player != null;

        state.setRotationForClient(client);
        state.setPositionForClient(client);
        state.setButtonsForClient(client);

        tickIterator++;
        if (tickIterator == tickStates.size()) {
            // if last point is very close to the first we don't have to stop playing, instead can just loop
            if (Utils.distanceBetween(state.getPosition(), tickStates.get(0).getPosition())
                    <= ReplayBotSettings.maxDistanceToFirstPoint) {
                SkyblockBot.LOGGER.info("looped");
                tickIterator = 0;
                adjustHeadWhenDoneLoop();
            } else {
                SkyblockBot.LOGGER.info("stopped playing because can't do a loop");
                playing = false;
                SkyblockBot.LOGGER.info("playing packet counter = " + debugPlayingPacketCounter);
                SkyblockBot.LOGGER.info("on ground only counter = " + debugOnGroundOnlyCounter);
                SkyblockBot.LOGGER.info("look and on ground counter = " + debugLookAndOnGroundCounter);
                SkyblockBot.LOGGER.info("position and on ground counter = " + debugPositionAndOnGroundCounter);
                SkyblockBot.LOGGER.info("full counter = " + debugFullCounter);
            }
        }
    }


    public static void startPlaying() {
        if (tickStates.size() == 0) {
            SkyblockBot.LOGGER.warn("can't start playing, nothing to play");
            loadRecordingAsync();
            return;
        }

        if (playing) {
            SkyblockBot.LOGGER.warn("Already playing");
            return;
        }

        if (recording) {
            SkyblockBot.LOGGER.warn("Can't play while recording");
            return;
        }

        assert MinecraftClient.getInstance().player != null;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double distanceToStartPoint = player.getPos().add(tickStates.get(0).getPosition().multiply(-1.0d)).length();

        if (distanceToStartPoint > ReplayBotSettings.maxDistanceToFirstPoint) {
            SkyblockBot.LOGGER.warn("Can't start so far from first point");
            return;
        }

        SkyblockBot.LOGGER.info("Starting playing");
        itemWhenStarted = player.getActiveItem().getName().getString();
        tickIterator = 0;
        debugPlayingPacketCounter = 0;

        debugOnGroundOnlyCounter = 0;
        debugLookAndOnGroundCounter = 0;
        debugPositionAndOnGroundCounter = 0;
        debugFullCounter = 0;

        antiDetectTrigger = false;
        adjustHeadBeforeStarting();

        playing = true;
    }

    public static void stopPlaying() {
        SkyblockBot.LOGGER.info("stopped playing");
        SkyblockBot.LOGGER.info("playing packet counter = " + debugPlayingPacketCounter);
        SkyblockBot.LOGGER.info("on ground only counter = " + debugOnGroundOnlyCounter);
        SkyblockBot.LOGGER.info("look and on ground counter = " + debugLookAndOnGroundCounter);
        SkyblockBot.LOGGER.info("position and on ground counter = " + debugPositionAndOnGroundCounter);
        SkyblockBot.LOGGER.info("full counter = " + debugFullCounter);
        unpressButtons();

        playing = false;
    }

    /*
     * IMPORTANT: call this method before doing anything to player like changing rotation or position
     * anti-detection stuff
     * check for correct rotation
     * check what item is in hand(anti-captcha)
     */
    private static boolean antiDetect(@NotNull MinecraftClient client) {
        if (!isNextStatePossible(client)) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: next position is impossible, must be wall check, stopping the bot");
            return true;
        }

        if (serverChangedItem) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: server changed held item, stopping the bot");
            serverChangedItem = false;
            return true;
        }

        if (serverChangedPositionRotation) {
            serverChangedPositionRotation = false;

            if (tickIterator > 0) {
                if (!checkPosAdjustLag(client)) {
                    SkyblockBot.LOGGER.warn("Anti-detection alg: server changed position, can't adjust for it, stopping the bot");
                    return true;
                }

                if (!checkRot(client)) {
                    SkyblockBot.LOGGER.warn("Anti-detection alg: server changed rotation, can't adjust for it");
                    return true;
                }
            }
        }

        if (serverChangedSlot) {
            if (!client.player.getActiveItem().getName().getString().equals(itemWhenStarted)) {
                SkyblockBot.LOGGER.warn("Anti-detection alg: server changed hotbar slot");
                serverChangedSlot = false;
                return true;
            }
        }

        return false;
    }

    private static boolean isNextStatePossible(@NotNull MinecraftClient client) {
        return false;
    }

    /*
     * Corrects position
     * return - true if it is correct or was corrected (correct by changing tickIterator to index of state closest to current state)
     *          false if the position can't be corrected(which means player was teleported to check for macros or lagged back too far)
     */
    private static boolean checkPosAdjustLag(@NotNull MinecraftClient client) {
        TickState state = tickStates.get(tickIterator - 1);
        double deltaExpectedPos = Utils.distanceBetween(client.player.getPos(), state.getPosition());
        if (deltaExpectedPos > ReplayBotSettings.reactToLagbackThreshold) {
            // if delta is too big something is wrong, check if we were simply lagged back and if not-stop
            double minDelta = ReplayBotSettings.reactToLagbackThreshold;
            int bestTickIndex = -1;

            for (int i = 1; i <= ReplayBotSettings.maxLagbackTicks && tickIterator >= i; i++) {
                double delta = Utils.distanceBetween(client.player.getPos(), tickStates.get(tickIterator - i).getPosition());

                /*
                 * If delta is < max then we're roughly at the correct tick
                 * Press correct buttons, set correct rotation
                 * If delta is < min then we don't have to correct position, otherwise correct it
                 */
                if (delta < minDelta) {
                    minDelta = delta;
                    bestTickIndex = tickIterator - i;
                }
            }
            if (bestTickIndex != -1) {
                if (isStuck(client)) {
                    // TODO do something
                    SkyblockBot.LOGGER.info("Detected being stuck. Shutting down for now, will do something else later");
                    return false;
                }
                tickIterator = bestTickIndex;
                SkyblockBot.LOGGER.info("Adjusted for lagback. Min delta = " + minDelta);
                return true;
            }
            /* If previous states aren't close to current position it must not be lagback, but teleport or something else */
            return false;
        }
        return true;
    }

    /*
     * This thing at the moment is only called when server lags back the player(not every tick)
     * But my guess right now is that it's still gonna do what I intend it to do,
     *  because "stuckness coefficient" will be higher when that method is called rarely, not lower
     */
    private static boolean isStuck(@NotNull MinecraftClient client) {
        lastNTicks.add(new TickState(
                        client.player.getPos(),
                        new Vec2f(
                                Utils.normalize(client.player.getYaw()),
                                Utils.normalize(client.player.getPitch())
                        ),
                        client.options.attackKey.isPressed(),
                        client.options.forwardKey.isPressed(),
                        client.options.backKey.isPressed(),
                        client.options.rightKey.isPressed(),
                        client.options.leftKey.isPressed(),
                        client.options.sneakKey.isPressed(),
                        client.options.sprintKey.isPressed(),
                        client.options.jumpKey.isPressed()
                )
        );
        if (lastNTicks.size() > ReplayBotSettings.antiStucknessTickCount) {
            lastNTicks.remove(0);
        } else {
            // don't actually check for stuckness when we're just starting to move
            return false;
        }

        double movedLastNTicks = Utils.distanceBetween(
                lastNTicks.get(0).getPosition(),
                lastNTicks.get(lastNTicks.size() - 1).getPosition()
        );

        double expectedToMove = Utils.distanceBetween(
                client.player.getPos(),
                tickStates.get(tickIterator - lastNTicks.size()
                ).getPosition()
        );

        if (movedLastNTicks / expectedToMove <= ReplayBotSettings.detectStucknessCoefficient) {
            SkyblockBot.LOGGER.warn("Bot is stuck! moved in last ticks: " + movedLastNTicks + ", expected to move: " + expectedToMove);
            return true;
        }
        return false;
    }

    private static boolean checkRot(@NotNull MinecraftClient client) {
        float dPitch = Math.abs(tickStates.get(tickIterator - 1).getPitch() - client.player.getPitch());
        float expectedYaw = Utils.normalize(tickStates.get(tickIterator - 1).getYaw());
        float currentYaw = Utils.normalize(client.player.getYaw());

        float dYaw = Math.abs(expectedYaw - currentYaw);
        float dYaw2 = Math.abs(Math.abs(expectedYaw - currentYaw) - 360);

        float threshold = ReplayBotSettings.antiDetectDeltaAngleThreshold;

        if (!(dPitch < threshold && (dYaw < threshold || dYaw2 < threshold))) {
            SkyblockBot.LOGGER.info("Can't fix rotation. dYaw = " + dYaw + ", dPitch = " + dPitch + ", current yaw = " + currentYaw + ", expected yaw = " + expectedYaw);
            return false;
        } else {
            return true;
        }
    }

    /* Used for recording only */
    private static void detectAndCorrectLagBack(@NotNull MinecraftClient client) {
        if (serverChangedPositionRotation) {
            int bestFit = -1;
            double bestFitDistance = ReplayBotSettings.reactToLagbackThreshold;

            for (int i = 0; i < ReplayBotSettings.maxLagbackTicksWhenRecording && tickStates.size() > i; i++) {
                int index = tickStates.size() - i - 1;

                double distance = Utils.distanceBetween(
                        client.player.getPos(),
                        tickStates.get(index).getPosition()
                );

                if (distance < bestFitDistance) {
                    bestFit = index;
                    bestFitDistance = distance;
                }
            }

            if (bestFit != -1) {
                tickStates.subList(bestFit + 1, tickStates.size()).clear();
                SkyblockBot.LOGGER.info("Lagged back when recording, corrected! Best fit distance = " + bestFitDistance);
            } else {
                SkyblockBot.LOGGER.warn("Lagged back when recording, can't correct, stopping the recording");
                recording = false;
            }

            serverChangedPositionRotation = false;
        }
    }

    private static void unpressButtons() {
        new TickState(
                new Vec3d(0, 0, 0),
                new Vec2f(0, 0),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ).setButtonsForClient(MinecraftClient.getInstance());
    }

    private static void adjustHeadBeforeStarting() {
        pitchTask = LookHelper.changePitchSmoothAsync(tickStates.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(tickStates.get(0).getYaw(), 90.0f);
    }

    private static void adjustHeadWhenDoneLoop() {
        pitchTask = LookHelper.changePitchSmoothAsync(tickStates.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(tickStates.get(0).getYaw(), 90.0f);
    }

    private static void asyncPlayAlarmSound() {
        CompletableFuture.runAsync(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.world.playSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.PLAYERS,
                    100.0f,
                    10.0f,
                    false
            );
        });
    }

    private static void loadRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            ByteBuffer file;

            try {
                InputStream is = new FileInputStream(ABSOLUTE_PATH_BIN);
                file = ByteBuffer.wrap(is.readAllBytes());
                is.close();
                SkyblockBot.LOGGER.info("Loaded the recording");
            } catch (IOException e) {
                SkyblockBot.LOGGER.info("Exception when trying to load from file");
                return;
            }

            while (file.hasRemaining()) {
                Vec3d pos = new Vec3d(file.getDouble(), file.getDouble(), file.getDouble());
                Vec2f rot = new Vec2f(file.getFloat(), file.getFloat());

                boolean isAttacking = file.get() == 1;
                boolean forward = file.get() == 1;
                boolean backward = file.get() == 1;
                boolean right = file.get() == 1;
                boolean left = file.get() == 1;
                boolean sneak = file.get() == 1;
                boolean sprint = file.get() == 1;
                boolean jump = file.get() == 1;
                tickStates.add(new TickState(pos, rot, isAttacking, forward,
                        backward, right, left, sneak, sprint, jump)
                );
            }
        });
    }

    private static void saveRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            ByteBuffer bb = ByteBuffer.allocate(tickStates.size() * (TickState.getTickStateSize()));

            for (TickState state : tickStates) {
                bb.putDouble(state.getPosition().getX());
                bb.putDouble(state.getPosition().getY());
                bb.putDouble(state.getPosition().getZ());
                bb.putFloat(state.getYaw());
                bb.putFloat(state.getPitch());

                bb.put((byte) (state.isAttack() ? 1 : 0));
                bb.put((byte) (state.isForward() ? 1 : 0));
                bb.put((byte) (state.isBackward() ? 1 : 0));
                bb.put((byte) (state.isRight() ? 1 : 0));
                bb.put((byte) (state.isLeft() ? 1 : 0));
                bb.put((byte) (state.isSneak() ? 1 : 0));
                bb.put((byte) (state.isSprint() ? 1 : 0));
                bb.put((byte) (state.isJump() ? 1 : 0));
            }
            try {
                OutputStream os = new FileOutputStream(ABSOLUTE_PATH_BIN, false);

                os.write(bb.array());
                os.close();

                assert MinecraftClient.getInstance().player != null;
                SkyblockBot.LOGGER.info("Saved the recording");
            } catch (IOException e) {
                SkyblockBot.LOGGER.info("Exception when trying to save movement recording to a file");
            }
        });
    }

    public static boolean isRecording() {
        return recording;
    }

    public static boolean isPlaying() {
        return playing;
    }
}
