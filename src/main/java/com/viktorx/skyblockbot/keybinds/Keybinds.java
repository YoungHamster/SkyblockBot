package com.viktorx.skyblockbot.keybinds;

import com.viktorx.skyblockbot.ScreenshotDaemon;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.KeyBindingMixin;
import com.viktorx.skyblockbot.task.ComplexFarmingTask;
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
    private static KeyBinding pauseTask;
    private static boolean screenshotDaemonStarted = false;

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

        pauseTask = KeyBindingHelper.registerKeyBinding(new KeyBinding(
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
            CompletableFuture.runAsync(Keybinds::asyncPressKeyAfterTick);

            if (startStopBot.wasPressed()) {
                if(!screenshotDaemonStarted) {
                    screenshotDaemonStarted = true;
                    ScreenshotDaemon.INSTANCE.start();
                }
                if (!ComplexFarmingTask.INSTANCE.isExecuting()) {
                    ComplexFarmingTask.INSTANCE.execute();
                } else {
                    ComplexFarmingTask.INSTANCE.abort();
                }
            }

            if (startStopRecording.wasPressed()) {
                if (!ReplayExecutor.INSTANCE.isRecording()) {
                    ReplayExecutor.INSTANCE.startRecording();
                } else {
                    ReplayExecutor.INSTANCE.stopRecording();
                }
            }

            if (loadRecording.wasPressed()) {
                if (!ComplexFarmingTask.INSTANCE.isExecuting()) {
                    ComplexFarmingTask.INSTANCE.loadRecordingAsync();
                }
            }

            if(pauseTask.wasPressed()) {
                if(!ComplexFarmingTask.INSTANCE.isPaused()) {
                    ComplexFarmingTask.INSTANCE.pause();
                } else if(ComplexFarmingTask.INSTANCE.isPaused()) {
                    ComplexFarmingTask.INSTANCE.resume();
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
