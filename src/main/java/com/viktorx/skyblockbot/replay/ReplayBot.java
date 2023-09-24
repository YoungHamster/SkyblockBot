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
    private static final List<PlayerTickStateAdvanced> advancedTickData = new ArrayList<>();
    private static boolean recording = false;
    private static boolean playing = false;
    private static int playIterator;
    private static CompletableFuture<Void> yawTask = null;
    private static CompletableFuture<Void> pitchTask = null;

    private static boolean positionCorrectedThisTick = false;
    private static String itemWhenStarted;
    private static boolean antiDetectTrigger = false;
    private static int antiDetectCounter = 0;
    public static boolean serverChangedPositionRotation = false;
    public static boolean serverChangedItem = false;
    public static boolean serverChangedSlot = false;

    public static int debugRecordingPacketCounter = 0;
    public static int debugPlayingPacketCounter = 0;

    public static void Init() {
        ClientTickEvents.END_CLIENT_TICK.register(ReplayBot::advancedRecordTick);
        ClientTickEvents.START_CLIENT_TICK.register(ReplayBot::advancedPlayTick);
    }

    // TODO correct for lagbacks when recording
    private static void advancedRecordTick(MinecraftClient client) {
        if (recording) {
            assert client.player != null;

            detectAndCorrectLagBack(client);

            advancedTickData.add(
                    new PlayerTickStateAdvanced(
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
        advancedTickData.clear();

        recording = true;
    }

    public static void stopRecording() {
        SkyblockBot.LOGGER.info("stopped recording");
        recording = false;
        SkyblockBot.LOGGER.info("recording packet counter = " + debugRecordingPacketCounter);

        // Rotation shift so that bots look where they go and don't teleport around like idiots
        for(int i = 0; i < advancedTickData.size() - 1; i++) {
            advancedTickData.get(i).setRotation(advancedTickData.get(i + 1).getRotation());
        }

        saveRecordingAsync();
    }

    private static void advancedPlayTick(MinecraftClient client) {
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
        if (playIterator >= advancedTickData.size()) {
            playIterator = 0;
            playing = false;
            SkyblockBot.LOGGER.warn("Entered advanced play tick with playIterator above tick data size, this SHOULDNT HAPPEN");
            return;
        }

        /* Get state for further use and increment iterator */
        PlayerTickStateAdvanced state = advancedTickData.get(playIterator++);

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
            PlayerTickStateAdvanced unpressed = new PlayerTickStateAdvanced(new Vec3d(0, 0, 0), new Vec2f(0, 0),
                    false, false, false, false, false, false, false, false);
            unpressed.setButtonsForClient(client);
            return;
        } else {
            asyncPlayAlarmSound();

            if(playIterator < advancedTickData.size()) {
                // TODO make these 3 lines better
                Vec2f deltaRotation = advancedTickData.get(playIterator).getRotation().add(state.getRotation().multiply(-1.0f));
                client.player.setYaw(deltaRotation.x + client.player.getYaw());
                client.player.setPitch(deltaRotation.y + client.player.getPitch());
            }

            state.setButtonsForClient(client);
            antiDetectCounter++;
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


        // loop if it's a closed loop and stop if not
        if (playIterator == advancedTickData.size()) {
            // if last point is very close to the first we don't have to stop playing, instead can just loop
            if (Utils.distanceBetween(state.getPosition(), advancedTickData.get(0).getPosition())
                    <= ReplayBotSettings.maxDistanceToFirstPoint) {
                SkyblockBot.LOGGER.info("looped");
                playIterator = 0;
                adjustHeadWhenDoneLoop();
            } else {
                SkyblockBot.LOGGER.info("stopped playing because can't do a loop");
                playing = false;
                SkyblockBot.LOGGER.info("playing packet counter = " + debugPlayingPacketCounter);
            }
        }
    }

    public static void play() {
        if (advancedTickData.size() == 0) {
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

        double distanceToStartPoint = player.getPos().add(advancedTickData.get(0).getPosition().multiply(-1.0d)).length();
        if(distanceToStartPoint > ReplayBotSettings.maxDistanceToFirstPoint) {
            SkyblockBot.LOGGER.warn("Can't start so far from first point");
            return;
        }

        SkyblockBot.LOGGER.info("Starting playing");
        itemWhenStarted = player.getStackInHand(player.getActiveHand()).getName().getString();
        playIterator = 0;
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
        if(playIterator > 1) {
            float dPitch = Math.abs(advancedTickData.get(playIterator - 2).getPitch() - client.player.getPitch());
            float dYaw = Math.abs(advancedTickData.get(playIterator - 2).getYaw() - client.player.getYaw());
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
            for(int i = 0; i < ReplayBotSettings.maxLagbackTicksWhenRecording && advancedTickData.size() > i; i++) {
                int index = advancedTickData.size() - i - 1;
                double distance = Utils.distanceBetween(client.player.getPos(), advancedTickData.get(index).getPosition());
                if(distance < bestFitDistance) {
                    bestFit = index;
                    bestFitDistance = distance;
                }
            }
            if(bestFit != -1) {
                advancedTickData.subList(bestFit + 1, advancedTickData.size()).clear();
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

        PlayerTickStateAdvanced state = advancedTickData.get(playIterator);
        double deltaExpectedPos = Utils.distanceBetween(client.player.getPos(), state.getPosition());
        if (deltaExpectedPos > ReplayBotSettings.minDeltaToAdjust && deltaExpectedPos <= ReplayBotSettings.maxDeltaToAdjust) {
            state.setPositionForClient(client);
            SkyblockBot.LOGGER.info("Bot corrected, delta = " + deltaExpectedPos);
            return true;
        } else if (deltaExpectedPos > ReplayBotSettings.maxDeltaToAdjust) {
            // if delta is too big something is wrong, check if we were simply lagged back and if not-stop
            for (int i = 1; i <= ReplayBotSettings.maxLagbackTicks && playIterator >= i; i++) {
                double delta = Utils.distanceBetween(client.player.getPos(), advancedTickData.get(playIterator - i).getPosition());

                /*
                 * If delta is < max then we're roughly at the correct tick
                 * Press correct buttons, set correct rotation
                 * If delta is < min then we don't have to correct position, otherwise correct it
                 */
                if (delta < ReplayBotSettings.maxDeltaToAdjust) {
                    playIterator = playIterator - i;
                    PlayerTickStateAdvanced newState = advancedTickData.get(playIterator);
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
        pitchTask = LookHelper.changePitchSmoothAsync(advancedTickData.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(advancedTickData.get(0).getYaw(), 90.0f);
    }

    private static void adjustHeadWhenDoneLoop() {
        pitchTask = LookHelper.changePitchSmoothAsync(advancedTickData.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(advancedTickData.get(0).getYaw(), 90.0f);
    }

    private static void asyncPlayAlarmSound() {
        CompletableFuture.runAsync(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS,
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
                advancedTickData.add(new PlayerTickStateAdvanced(pos, rot, isAttacking,
                                                                 forward, backward, right, left,
                                                                 sneak, sprint, jump));
            }
        });
    }

    private static void saveRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            ByteBuffer bb = ByteBuffer.allocate(advancedTickData.size() * (PlayerTickStateAdvanced.getTickStateSize()));
            for (PlayerTickStateAdvanced state : advancedTickData) {
                bb.putDouble(state.getPosition().getX());
                bb.putDouble(state.getPosition().getY());
                bb.putDouble(state.getPosition().getZ());
                bb.putFloat(state.getYaw());
                bb.putFloat(state.getPitch());
                bb.put((byte) (state.isAttacking() ? 1 : 0));
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
