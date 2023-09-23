package com.viktorx.skyblockbot.replay;

import com.viktorx.skyblockbot.movement.LookHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;

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
            if(ReplayBot.isRecording) {
                assert client.player != null;
                everyTickData.add(
                        new PlayerTickState(
                                client.player.getPos(),
                                new Vec2f(client.player.getYaw(), client.player.getPitch()),
                                client.options.attackKey.isPressed()));
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if(!ReplayBot.playing) {
                return;
            }
            if(yawTask != null && pitchTask != null) {
                if(!yawTask.isDone() || !pitchTask.isDone()) {
                    return;
                }
            }

            PlayerTickState state = everyTickData.get(playIterator++);
            assert client.player != null;
            client.player.setYaw(state.getYaw());
            client.player.setPitch(state.getPitch());
            client.player.setPosition(state.getPosition());
            client.options.attackKey.setPressed(state.getAttacking());

            if(playIterator == everyTickData.size()) {
                // if last point is very close to the first we don't have to stop playing, instead can just loop
                if(everyTickData.get(everyTickData.size() - 1).getPosition().add(everyTickData.get(0).getPosition().multiply(-1.0d)).length() <= 0.2) {
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
    }

    public static void playRecording() {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendChatMessage("started playing");
        playIterator = 0;
        adjustHeadBeforeStarting();
        playing = true;
    }

    private static void adjustHeadBeforeStarting() {
        LookHelper.changePitchSmooth(everyTickData.get(0).getPitch(), 270.0f);
        LookHelper.changeYawSmooth(everyTickData.get(0).getYaw(), 270.0f);
    }

    private static void adjustHeadWhenDoneLoop() {
        pitchTask = LookHelper.changePitchSmoothAsync(everyTickData.get(0).getPitch(), 270.0f);
        yawTask = LookHelper.changeYawSmoothAsync(everyTickData.get(0).getYaw(), 270.0f);
    }

    public static void stopPlaying() {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendChatMessage("stopped playing");
        playing = false;
    }

    public static boolean isRecording() {
        return isRecording;
    }

    public static boolean isPlaying() {
        return playing;
    }
}
