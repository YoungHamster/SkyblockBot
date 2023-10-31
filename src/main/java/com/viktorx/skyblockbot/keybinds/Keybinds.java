package com.viktorx.skyblockbot.keybinds;

import com.viktorx.skyblockbot.GlobalSettingsManager;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.InputRelated.IMouseMixin;
import com.viktorx.skyblockbot.mixins.InputRelated.KeyBindingMixin;
import com.viktorx.skyblockbot.task.compound.FarmingTask;
import com.viktorx.skyblockbot.task.base.replay.ReplayExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class Keybinds {
    private static final Queue<Integer> tickKeyPressQueue = new LinkedBlockingQueue<>();
    private static KeyBinding startStopBot;
    //private static KeyBinding printTestInfo;
    private static KeyBinding startStopRecording;
    private static KeyBinding loadRecording;
    private static KeyBinding pauseTask;

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
                if (!FarmingTask.INSTANCE.isExecuting()) {
                    FarmingTask.INSTANCE.execute();
                } else {
                    FarmingTask.INSTANCE.abort();
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
                try {
                    GlobalSettingsManager.getInstance().loadSettings();
                } catch (IOException e) {
                    SkyblockBot.LOGGER.warn("Coudln't load settings");
                }
                FarmingTask.INSTANCE.loadRecordingAsync();
            }

            if (pauseTask.wasPressed()) {
                if (!FarmingTask.INSTANCE.isPaused()) {
                    FarmingTask.INSTANCE.pause();
                } else if (FarmingTask.INSTANCE.isPaused()) {
                    FarmingTask.INSTANCE.resume();
                }
            }

            /*if (printTestInfo.wasPressed()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(CraftPriceCalculator.getInstance()::debugPrintRecipesPrices);
            }*/
        });

    }

    // puts keybinds in a queue where no more than one key gets pressed every tick
    public static void asyncPressKeyAfterTick(KeyBinding key) {
        Keybinds.tickKeyPressQueue.add(((KeyBindingMixin) key).getBoundKey().getCode());
    }

    private static void asyncPressKeyAfterTick() {
        Integer key;
        key = Keybinds.tickKeyPressQueue.poll();

        if (key != null) {
            blockingPressCustomKey(key);
        }
    }

    public static void blockingPressKey(KeyBinding key) {
        int keyCode = ((KeyBindingMixin) key).getBoundKey().getCode();
        blockingPressCustomKey(keyCode);
    }

    public static void blockingPressCustomKey(int keyCode) {
        MinecraftClient client = MinecraftClient.getInstance();

        /*
         * I guess this is shitty code, but i think it will work
         * Instead of figuring out if it is a mouse button or keyboard key i just try to press it on both mouse and keyboard
         */
        if (keyCode < 32) {
            ((IMouseMixin) client.mouse).callOnMouseButton(
                    client.getWindow().getHandle(),
                    keyCode,
                    1,
                    0);
        } else {
            client.keyboard.onKey(
                    client.getWindow().getHandle(),
                    keyCode,
                    keyCode,
                    1,
                    0);
        }

        try {
            Thread.sleep(KeybindsSettings.buttonPressDelay); // press button for around 1 tick, maybe make it random later
        } catch (InterruptedException e) {
            SkyblockBot.LOGGER.info("InterruptedException. Don't care");
        }

        if (keyCode < 32) {
            ((IMouseMixin) client.mouse).callOnMouseButton(
                    client.getWindow().getHandle(),
                    keyCode,
                    1,
                    0);
        } else {
            client.keyboard.onKey(
                    client.getWindow().getHandle(),
                    keyCode,
                    keyCode,
                    0,
                    0);
        }
    }

    public static int getStartStopRecrodingKeyCode() {
        return ((KeyBindingMixin) startStopRecording).getBoundKey().getCode();
    }
}
