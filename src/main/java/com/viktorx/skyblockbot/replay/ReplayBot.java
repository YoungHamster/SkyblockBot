package com.viktorx.skyblockbot.replay;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.movement.LookHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReplayBot {
    private static final List<PlayerTickState> everyTickData = new ArrayList<>();
    private static boolean isRecording = false;
    private static boolean playing = false;
    private static int playIterator;
    private static CompletableFuture<Void> yawTask = null;
    private static CompletableFuture<Void> pitchTask = null;

    public static int debugRecordingPacketCounter = 0;
    public static int debugPlayingPacketCounter = 0;

    public static void Init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ReplayBot.isRecording) {
                assert client.player != null;
                everyTickData.add(
                        new PlayerTickState(
                                client.player.getPos(),
                                new Vec2f(client.player.getYaw(), client.player.getPitch()),
                                client.options.attackKey.isPressed()));
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!ReplayBot.playing) {
                return;
            }
            if (yawTask != null && pitchTask != null) {
                if (!yawTask.isDone() || !pitchTask.isDone()) {
                    return;
                }
            }

            // anti-detection stuff
            // check for correct movement
            // check what item is in hand(anti-captcha)

            // move rotate etc.
            PlayerTickState state = everyTickData.get(playIterator++);
            assert client.player != null;
            client.player.setYaw(state.getYaw());
            client.player.setPitch(state.getPitch());
            client.player.setPosition(state.getPosition());
            client.options.attackKey.setPressed(state.getAttacking());

            // loop if it's a closed loop and stop if not
            if (playIterator == everyTickData.size()) {
                // if last point is very close to the first we don't have to stop playing, instead can just loop
                if (everyTickData.get(everyTickData.size() - 1).getPosition().add(everyTickData.get(0).getPosition().multiply(-1.0d)).length() <= 0.2) {
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.sendChatMessage("looped");
                    playIterator = 0;
                    adjustHeadWhenDoneLoop();
                } else {
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.sendChatMessage("stopped playing 2");
                    ReplayBot.playing = false;
                    MinecraftClient.getInstance().player.sendChatMessage("playing packet counter = " + debugPlayingPacketCounter);
                }
            }
        });
    }

    public static void startRecording() {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendChatMessage("started recording");
        everyTickData.clear();
        isRecording = true;
    }

    public static void stopRecording() {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendChatMessage("stopped recording");
        isRecording = false;
        MinecraftClient.getInstance().player.sendChatMessage("recording packet counter = " + debugRecordingPacketCounter);
        saveRecordingAsync();
    }

    public static void playRecording() {
        if (everyTickData.size() == 0) {
            MinecraftClient.getInstance().player.sendChatMessage("can't start playing, nothing to play");
            loadRecordingAsync();
            return;
        }
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendChatMessage("started playing");
        playIterator = 0;
        adjustHeadBeforeStarting();
        playing = true;
    }

    public static void stopPlaying() {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendChatMessage("stopped playing");
        playing = false;
    }

    private static void adjustHeadBeforeStarting() {
        pitchTask = LookHelper.changePitchSmoothAsync(everyTickData.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(everyTickData.get(0).getYaw(), 90.0f);
    }

    private static void adjustHeadWhenDoneLoop() {
        pitchTask = LookHelper.changePitchSmoothAsync(everyTickData.get(0).getPitch(), 90.0f);
        yawTask = LookHelper.changeYawSmoothAsync(everyTickData.get(0).getYaw(), 90.0f);
    }

    private static void loadRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            ByteBuffer file;
            try {
                InputStream is = new FileInputStream(
                        "C:/Users/Nobody/AppData/Roaming/.minecraft/recording.bin");
                file = ByteBuffer.wrap(is.readAllBytes());
                is.close();
                MinecraftClient.getInstance().player.sendChatMessage("Loaded the recording");
            } catch (IOException e) {
                SkyblockBot.LOGGER.info("Exception when trying to load from file");
                return;
            }
            while (file.hasRemaining()) {
                Vec3d pos = new Vec3d(file.getDouble(), file.getDouble(), file.getDouble());
                Vec2f rot = new Vec2f(file.getFloat(), file.getFloat());
                boolean isAttacking = file.get() == 1;
                everyTickData.add(new PlayerTickState(pos, rot, isAttacking));
            }
        });
    }

    private static void saveRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            // 8 + 8 + 8 + 4 + 4 + 1 is the size of every state in bytes considering it has 3 doubles, 2floats and 1 bool inside
            ByteBuffer bb = ByteBuffer.allocate(everyTickData.size() * (PlayerTickState.getTickStateSize()));
            for (PlayerTickState state : everyTickData) {
                bb.putDouble(state.getPosition().getX());
                bb.putDouble(state.getPosition().getY());
                bb.putDouble(state.getPosition().getZ());
                bb.putFloat(state.getYaw());
                bb.putFloat(state.getPitch());
                bb.put((byte) (state.getAttacking() ? 1 : 0));
            }
            try {
                OutputStream os = new FileOutputStream(
                        "C:/Users/Nobody/AppData/Roaming/.minecraft/recording.bin",
                        false);
                os.write(bb.array());
                os.close();
                MinecraftClient.getInstance().player.sendChatMessage("Saved the recording");
            } catch (IOException e) {
                SkyblockBot.LOGGER.info("Exception when trying to save movement recording to a file");
            }
        });
    }

    public static boolean isRecording() {
        return isRecording;
    }

    public static boolean isPlaying() {
        return playing;
    }
}
