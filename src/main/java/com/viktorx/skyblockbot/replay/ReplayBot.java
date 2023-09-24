package com.viktorx.skyblockbot.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import com.viktorx.skyblockbot.movement.LookHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
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
    
    private static String itemWhenStarted;
    private static boolean antiDetectTrigger = false;
    private static int antiDetectCounter = 0;
    public static boolean serverChangedRotation = false;
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
        if (ReplayBot.recording) {
            assert client.player != null;
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
        assert MinecraftClient.getInstance().player != null;
        SkyblockBot.LOGGER.info("started recording");
        advancedTickData.clear();
        recording = true;
    }

    public static void stopRecording() {
        assert MinecraftClient.getInstance().player != null;
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
        if (!ReplayBot.playing) {
            return;
        }
        if (yawTask != null && pitchTask != null) {
            if (!yawTask.isDone() || !pitchTask.isDone()) {
                return;
            }
        }
        if (playIterator >= advancedTickData.size()) {
            playIterator = 0;
            ReplayBot.playing = false;
            SkyblockBot.LOGGER.warn("Entered advanced play tick with playIterator above tick data size, this SHOULDNT HAPPEN");
            return;
        }

        // call anti detect first
        if(!antiDetectTrigger) {
            if (antiDetect(client)) {
                asyncPlayAlarmSound();
                ReplayBot.antiDetectTrigger = true;
                antiDetectCounter = 0;
                SkyblockBot.LOGGER.warn("Bot stopped. Anti-detection triggered");
                return;
            }
        } else if(antiDetectCounter >= ReplayBotSettings.antiDetectTriggeredWaitTicks) {
            ReplayBot.playing = false;
            antiDetectTrigger = false;

            // unpress all buttons
            PlayerTickStateAdvanced unpressed = new PlayerTickStateAdvanced(new Vec3d(0, 0, 0), new Vec2f(0, 0),
                    false, false, false, false, false, false, false, false);
            unpressed.setButtonsForClient(client);

            return;
        } else {
            // TODO it should also change rotation but only relatively
            advancedTickData.get(playIterator++).setButtonsForClient(client);
            antiDetectCounter++;
            return;
        }

        PlayerTickStateAdvanced state = advancedTickData.get(playIterator++);
        // move rotate etc.
        assert client.player != null;
        state.setRotationForClient(client);
        state.setButtonsForClient(client);

        // I guess this will have to combine anti-detection and movement
        double deltaExpectedPos = Utils.distanceBetween(client.player.getPos(), state.getPosition());
        if (deltaExpectedPos > ReplayBotSettings.minDeltaToAdjust && deltaExpectedPos <= ReplayBotSettings.maxDeltaToAdjust) {
            state.setPositionForClient(client);
            SkyblockBot.LOGGER.info("Bot corrected, delta = " + deltaExpectedPos);
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
                    return;
                }
            }
            /* If previous states aren't close to current position it must not be lagback, but teleport or something else */
            antiDetectTrigger = true;
        }

        // loop if it's a closed loop and stop if not
        if (playIterator == advancedTickData.size()) {
            // if last point is very close to the first we don't have to stop playing, instead can just loop
            if (Utils.distanceBetween(
                    advancedTickData.get(advancedTickData.size() - 1).getPosition(), advancedTickData.get(0).getPosition())
                    <= 0.2) {
                assert MinecraftClient.getInstance().player != null;
                SkyblockBot.LOGGER.info("looped");
                playIterator = 0;
                adjustHeadWhenDoneLoop();
            } else {
                assert MinecraftClient.getInstance().player != null;
                SkyblockBot.LOGGER.info("stopped playing 2");
                ReplayBot.playing = false;
                SkyblockBot.LOGGER.info("playing packet counter = " + debugPlayingPacketCounter);
            }
        }
    }

    public static void playRecording() {
        if (advancedTickData.size() == 0) {
            assert MinecraftClient.getInstance().player != null;
            SkyblockBot.LOGGER.info("can't start playing, nothing to play");
            loadRecordingAsync();
            return;
        }
        assert MinecraftClient.getInstance().player != null;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        itemWhenStarted = player.getStackInHand(player.getActiveHand()).getName().getString();
        SkyblockBot.LOGGER.info("started playing");
        playIterator = 0;
        adjustHeadBeforeStarting();
        playing = true;
    }

    public static void stopPlaying() {
        assert MinecraftClient.getInstance().player != null;
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
            return true;
        }
        if(serverChangedRotation) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: server changed rotation");
            return true;
        }
        if(serverChangedSlot) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: server changed hotbar slot");
            return true;
        }
        if(playIterator != 0) {
            float dPitch = Math.abs(advancedTickData.get(playIterator - 1).getPitch() - client.player.getPitch());
            float dYaw = Math.abs(advancedTickData.get(playIterator - 1).getYaw() - client.player.getYaw());
            if (dPitch > ReplayBotSettings.antiDetectDeltaAngleThreshold
                    || dYaw > ReplayBotSettings.antiDetectDeltaAngleThreshold) {
                SkyblockBot.LOGGER.warn("Anti-detection alg: rotation changed, but no packet was detect, wtf?");
                return true;
            }
        }
        if(!client.player.getStackInHand(client.player.getActiveHand()).getName().getString().equals(itemWhenStarted)) {
            SkyblockBot.LOGGER.warn("Anti-detection alg: held item changed but no packet was detected, wtf?");
            return true;
        }
        return false;
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
            MinecraftClient.getInstance().options.setSoundVolume(SoundCategory.PLAYERS, 100.0f);
            SoundManager sm = MinecraftClient.getInstance().getSoundManager();
            sm.play(new ElytraSoundInstance(MinecraftClient.getInstance().player));
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
                assert MinecraftClient.getInstance().player != null;
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
