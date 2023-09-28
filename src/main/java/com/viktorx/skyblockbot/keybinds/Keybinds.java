package com.viktorx.skyblockbot.keybinds;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.KeyBindingMixin;
import com.viktorx.skyblockbot.task.replay.ReplayExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class Keybinds {
    private static KeyBinding startStopBot;
    //private static KeyBinding printTestInfo;
    private static KeyBinding startStopRecording;
    private static KeyBinding loadRecording;
    private static KeyBinding pauseReplay;

    private static final Queue<KeyBinding> tickKeyPressQueue = new LinkedBlockingQueue<>();

    public static void Init() {
        startStopBot = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Start/stop moving", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_O, // The keycode of the key
                "Replay bot" // The translation key of the keybinding's category.
        ));

        startStopRecording = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Start/stop recording movements", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R, // The keycode of the key
                "Replay bot" // The translation key of the keybinding's category.
        ));

        loadRecording = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Load recording from recording.bin", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_L, // The keycode of the key
                "Replay bot" // The translation key of the keybinding's category.
        ));

        pauseReplay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Pause/unpause bot", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_P, // The keycode of the key
                "Replay bot" // The translation key of the keybinding's category.
        ));

        /*printTestInfo = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockbot.spook3", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_I, // The keycode of the key
                "category.skyblockbot.getInfo" // The translation key of the keybinding's category.
        ));*/

        ClientTickEvents.START_CLIENT_TICK.register(client -> {

            // this does the clicking
            CompletableFuture<Void> clickTick = CompletableFuture.runAsync(Keybinds::asyncPressKeyAfterTick);

            if (startStopBot.wasPressed()) {
                if (!ReplayExecutor.isPlaying()) {
                    ReplayExecutor.startPlaying(true);
                } else {
                    ReplayExecutor.stopPlaying();
                }
            }

            if (startStopRecording.wasPressed()) {
                if (!ReplayExecutor.isRecording()) {
                    ReplayExecutor.startRecording();
                } else {
                    ReplayExecutor.stopRecording();
                }
            }

            if (loadRecording.wasPressed()) {
                if (ReplayExecutor.isIdle()) {
                    ReplayExecutor.loadRecordingAsync();
                }
            }

            if(pauseReplay.wasPressed()) {
                if(ReplayExecutor.isPlaying()) {
                    ReplayExecutor.pause();
                } else if(ReplayExecutor.isPaused()) {
                    ReplayExecutor.unpause();
                }
            }

            /*if (printTestInfo.wasPressed()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(CraftPriceCalculator.getInstance()::debugPrintRecipesPrices);
            }*/
        });

    }

    // puts keybinds in a queue where no more than one key gets pressed every tick
    public static void asyncPressKeyAfterTick(KeyBinding key) {
        Keybinds.tickKeyPressQueue.add(key);
    }

    private static void asyncPressKeyAfterTick() {
        KeyBinding key;
        key = Keybinds.tickKeyPressQueue.poll();

        if (key != null) {
            KeyBinding.onKeyPressed(((KeyBindingMixin) key).getBoundKey());
            key.setPressed(true);

            try {
                Thread.sleep(40); // press button for 2 ticks, maybe make it random later
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("InterruptedException. Don't care");
            }

            key.setPressed(false);
        }
    }
}
