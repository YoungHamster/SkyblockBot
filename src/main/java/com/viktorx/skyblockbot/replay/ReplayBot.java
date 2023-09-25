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
    private static MinecraftClient client;

    private static final List<FullTickState> fullTickData = new ArrayList<>();
    private static int stateIterator;
    private static boolean positionCorrectedThisTick = false;

    private static boolean recording = false;
    private static boolean playing = false;

    private static boolean tickStarted = false;

    private static CompletableFuture<Void> yawTask = null;
    private static CompletableFuture<Void> pitchTask = null;

    private static String itemWhenStarted;
    private static boolean antiDetectTrigger = false;
    private static int antiDetectCounter = 0;
    public static boolean serverChangedPositionRotation = false;
    public static boolean serverChangedItem = false;
    public static boolean serverChangedSlot = false;

    public static int debugRecordingPacketCounter = 0;
    public static int debugPlayingPacketCounter = 0;

    public static void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(ReplayBot::onTick);
        new Thread(ReplayBot::updateLoop).start();
    }

    private static void updateLoop() {
        while(true) {
            if(!tickStarted || !(recording && playing)) {
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                continue;
            }
            // tick started
            long time = System.nanoTime();
            if(recording) {
                updateWhenRecord();
            } else if (playing) {
                updateWhenPlay();
            }

            while(tickStarted && (recording || playing)) {
                if(System.nanoTime() - time >= 10000000) {
                    if(recording) {
                        updateWhenRecord();
                    } else if (playing) {
                        updateWhenPlay();
                    }
                    break;
                } else {
                    try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    private static void updateWhenRecord() {
        if (recording) {
            tickStarted = true;
            assert client.player != null;

            detectAndCorrectLagBack(client);

            fullTickData.add(
                    new FullTickState(
                            client.player.getPos(),
                            new Vec2f(client.player.getYaw(), client.player.getPitch()),
                            client.options.attackKey.isPressed(),
                            client.options.forwardKey.isPressed(),
                            client.options.backKey.isPressed(),
                            client.options.rightKey.isPressed(),
                            client.options.leftKey.isPressed(),
                            client.options.sneakKey.isPressed(),
                            client.options.sprintKey.isPressed(),
                            client.options.jumpKey.isPressed()));
        }
    }

    public static void startRecording() {
        if(playing) {
            SkyblockBot.LOGGER.info("Can't start recording when playing");
            return;
        }
        if(recording) {
            SkyblockBot.LOGGER.info("Already recording");
            return;
        }

        SkyblockBot.LOGGER.info("started recording");
        fullTickData.clear();

        recording = true;
    }

    public static void stopRecording() {
        SkyblockBot.LOGGER.info("stopped recording");
        recording = false;
        SkyblockBot.LOGGER.info("recording packet counter = " + debugRecordingPacketCounter);

        // Rotation shift so that bots look where they go and don't teleport around like idiots
        for(int i = 0; i < fullTickData.size() - 1; i++) {
            fullTickData.get(i).setRotation(fullTickData.get(i + 1).getRotation());
        }

        saveRecordingAsync();
    }

    private static void onTick(MinecraftClient client) {
        if(playing || recording) {
            ReplayBot.client = client;
            tickStarted = true;
        }
    }

    /* I just measured and this whole thing takes 0 to 1 millisecond on my pc
    * Meaning I can move it to another thread, detect tick updates and poll states at 40-60-120HZ no problem */
    private static void updateWhenPlay() {
        if (!playing) {
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
        if (stateIterator >= fullTickData.size()) {
            stateIterator = 0;
            playing = false;
            SkyblockBot.LOGGER.warn("Entered advanced play tick with playIterator above tick data size, this SHOULDNT HAPPEN");
            return;
        }

        FullTickState state = fullTickData.get(stateIterator);

        /*
         * call anti detect first
         * if it was already triggered - keep mashing buttons and turning for some ticks, like we're a real player with slow reaction
         * then stop
         */
        if(!antiDetectTrigger) {
            positionCorrectedThisTick = false;
            if (antiDetect(client)) {
                antiDetectTrigger = true;
                SkyblockBot.LOGGER.warn("Bot stopped. Anti-detection triggered");
                return;
            }
        } else if(antiDetectCounter >= ReplayBotSettings.antiDetectTriggeredWaitTicks) {
            playing = false;
            antiDetectTrigger = false;
            antiDetectCounter = 0;
            // unpress all buttons
            FullTickState unpressed = new FullTickState(new Vec3d(0, 0, 0), new Vec2f(0, 0),
                    false, false, false, false, false, false, false, false);
            unpressed.setButtonsForClient(client);
            return;
        } else {
            asyncPlayAlarmSound();

            if(stateIterator <= fullTickData.size()) {
                Vec2f deltaRotation = fullTickData.get(stateIterator - 1).getRotation().add(state.getRotation().multiply(-1.0f));
                client.player.setYaw(deltaRotation.x + client.player.getYaw());
                client.player.setPitch(deltaRotation.y + client.player.getPitch());
            }

            state.setButtonsForClient(client);
            antiDetectCounter++;
            stateIterator++;
            return;
        }
        // move rotate etc.
        state.setRotationForClient(client);
        state.setButtonsForClient(client);

        // I guess this will have to combine anti-detection and movement
        if(!positionCorrectedThisTick) {
            if (!correctPosition(client)) {
                antiDetectTrigger = true;
            }
        }

        stateIterator++;
        // loop if it's a closed loop and stop if not
        if (stateIterator == fullTickData.size()) {
            // if last point is very close to the first we don't have to stop playing, instead can just loop
            if (Utils.distanceBetween(state.getPosition(), fullTickData.get(0).getPosition())
                    <= ReplayBotSettings.maxDistanceToFirstPoint) {
                SkyblockBot.LOGGER.info("looped");
                stateIterator = 0;
                adjustHeadWhenDoneLoop();
            } else {
                SkyblockBot.LOGGER.info("stopped playing because can't do a loop");
                playing = false;
                SkyblockBot.LOGGER.info("playing packet counter = " + debugPlayingPacketCounter);
            }
        }
    }


    public static void play() {
        if (fullTickData.size() == 0) {
            SkyblockBot.LOGGER.warn("can't start playing, nothing to play");
            loadRecordingAsync();
            return;
        }
        if(playing) {
            SkyblockBot.LOGGER.warn("Already playing");
            return;
        }
        if(recording) {
            SkyblockBot.LOGGER.warn("Can't play while recording");
            return;
        }
        assert MinecraftClient.getInstance().player != null;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        double distanceToStartPoint = player.getPos().add(fullTickData.get(0).getPosition().multiply(-1.0d)).length();
        if(distanceToStartPoint > ReplayBotSettings.maxDistanceToFirstPoint) {
            SkyblockBot.LOGGER.warn("Can't start so far from first point");
            return;
        }

        SkyblockBot.LOGGER.info("Starting playing");
        itemWhenStarted = player.getStackInHand(player.getActiveHand()).getName().getString();
        stateIterator = 0;
        antiDetectTrigger = false;

        adjustHeadBeforeStarting();
        playing = true;
    }

    public static void stopPlaying() {
        SkyblockBot.LOGGER.info("stopped playing");
        playing = false;
    }

    /*
     * IMPORTANT: call this method before doing anything to player like changing rotation or position
     * anti-detection stuff
     * check for correct rotation
     * check what item is in hand(anti-captcha)
     */
    private static boolean antiDetect(@NotNull MinecraftClient client) {
        if(serverChangedItem) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: server changed held item");
            serverChangedItem = false;
            return true;
        }
        if(stateIterator > 0) {
            float dPitch = Math.abs(fullTickData.get(stateIterator - 1).getPitch() - client.player.getPitch());
            float dYaw = Math.abs(fullTickData.get(stateIterator - 1).getYaw() - client.player.getYaw());
            if (dPitch > ReplayBotSettings.antiDetectDeltaAngleThreshold
                    || dYaw > ReplayBotSettings.antiDetectDeltaAngleThreshold) {
                SkyblockBot.LOGGER.warn("Anti-detection alg: rotation changed, but no packet was detect, wtf?");
                return true;
            }
        }
        if(serverChangedPositionRotation) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: server changed position or rotation");
            if(!correctPosition(client)) {
                serverChangedPositionRotation = false;
                return true;
            } else {
                SkyblockBot.LOGGER.info("Anti-detection alg: change in rotation wasn't critical");
            }
        }
        if(serverChangedSlot) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: server changed hotbar slot");
            serverChangedSlot = false;
            return true;
        }
        if(!client.player.getStackInHand(client.player.getActiveHand()).getName().getString().equals(itemWhenStarted)) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: held item changed but no packet was detected, wtf?");
            return true;
        }
        return false;
    }

    /* Used for recording only */
    private static void detectAndCorrectLagBack(@NotNull MinecraftClient client) {
        if(serverChangedPositionRotation) {
            int bestFit = -1;
            double bestFitDistance = ReplayBotSettings.maxDeltaToAdjust;
            for(int i = 0; i < ReplayBotSettings.maxLagbackTicksWhenRecording && fullTickData.size() > i; i++) {
                int index = fullTickData.size() - i - 1;
                double distance = Utils.distanceBetween(client.player.getPos(), fullTickData.get(index).getPosition());
                if(distance < bestFitDistance) {
                    bestFit = index;
                    bestFitDistance = distance;
                }
            }
            if(bestFit != -1) {
                fullTickData.subList(bestFit + 1, fullTickData.size()).clear();
                SkyblockBot.LOGGER.info("Lagged back when recording, corrected! Best fit distance = " + bestFitDistance);
            } else {
                SkyblockBot.LOGGER.warn("Lagged back when recording, can't correct, stopping the recording");
                recording = false;
            }
            serverChangedPositionRotation = false;
        }
    }

    /*
     * Corrects position
     * return - true if it is correct or was corrected
     *          (either by teleporting a small distance or rolling back the state to the position player was lagged back to)
     *          false if the position can't be corrected(which means player was teleported to check for macros or lagged back too far)
     */
    private static boolean correctPosition(@NotNull MinecraftClient client) {
        positionCorrectedThisTick = true;

        FullTickState state = fullTickData.get(stateIterator);
        double deltaExpectedPos = Utils.distanceBetween(client.player.getPos(), state.getPosition());
        if (deltaExpectedPos > ReplayBotSettings.minDeltaToAdjust && deltaExpectedPos <= ReplayBotSettings.maxDeltaToAdjust) {
            state.setPositionForClient(client);
            SkyblockBot.LOGGER.info("Bot corrected, delta = " + deltaExpectedPos);
            return true;
        } else if (deltaExpectedPos > ReplayBotSettings.maxDeltaToAdjust) {
            // if delta is too big something is wrong, check if we were simply lagged back and if not-stop
            for (int i = 1; i <= ReplayBotSettings.maxLagbackTicks && stateIterator >= i; i++) {
                double delta = Utils.distanceBetween(client.player.getPos(), fullTickData.get(stateIterator - i).getPosition());

                /*
                 * If delta is < max then we're roughly at the correct tick
                 * Press correct buttons, set correct rotation
                 * If delta is < min then we don't have to correct position, otherwise correct it
                 */
                if (delta < ReplayBotSettings.maxDeltaToAdjust) {
                    stateIterator = stateIterator - i;
                    FullTickState newState = fullTickData.get(stateIterator);
                    newState.setButtonsForClient(client);
                    newState.setRotationForClient(client);
                    if (delta > ReplayBotSettings.minDeltaToAdjust) {
                        newState.setPositionForClient(client);
                        SkyblockBot.LOGGER.info("Bot lagged back, correcting position");
                    }
                    SkyblockBot.LOGGER.info("Bot lagged back, delta = " + deltaExpectedPos + ", play iterator changed by " + i);
                    return true;
                }
            }
            /* If previous states aren't close to current position it must not be lagback, but teleport or something else */
            return false;
        }
        return true;
    }

    private static void adjustHeadBeforeStarting() {
        pitchTask = LookHelper.changePitchSmoothAsync(fullTickData.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(fullTickData.get(0).getYaw(), 90.0f);
    }

    private static void adjustHeadWhenDoneLoop() {
        pitchTask = LookHelper.changePitchSmoothAsync(fullTickData.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(fullTickData.get(0).getYaw(), 90.0f);
    }

    private static void asyncPlayAlarmSound() {
        CompletableFuture.runAsync(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS,
                    100.0f, 10.0f,false);
        });
    }

    private static void loadRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            ByteBuffer file;
            try {
                InputStream is = new FileInputStream(
                        "C:/Users/Nobody/AppData/Roaming/.minecraft/recording.bin");
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
                fullTickData.add(new FullTickState(pos, rot, isAttacking,
                                                                 forward, backward, right, left,
                                                                 sneak, sprint, jump));
            }
        });
    }

    private static void saveRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            ByteBuffer bb = ByteBuffer.allocate(fullTickData.size() * (FullTickState.getTickStateSize()));
            for (FullTickState state : fullTickData) {
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
                OutputStream os = new FileOutputStream(
                        "C:/Users/Nobody/AppData/Roaming/.minecraft/recording.bin",
                        false);
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
