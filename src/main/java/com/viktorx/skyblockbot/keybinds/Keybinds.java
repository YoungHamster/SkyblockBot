package com.viktorx.skyblockbot.keybinds;

import com.viktorx.skyblockbot.NotBotCore;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.KeyBindingMixin;
import com.viktorx.skyblockbot.skyblock.SBPlayer;
import com.viktorx.skyblockbot.skyblock.flipping.BZNameConverter;
import com.viktorx.skyblockbot.skyblock.flipping.CraftPriceCalculator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class Keybinds {
    private static KeyBinding startStopBot;
    private static KeyBinding printTestInfo;

    private static final Queue<KeyBinding> tickKeyPressQueue = new LinkedBlockingQueue<>();

    private static boolean wasPressed = false;

    private static SBPlayer sbplayer = null;

    public static void Init() {
        startStopBot = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockbot.spook", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_O, // The keycode of the key
                "category.skyblockbot.toggle" // The translation key of the keybinding's category.
        ));

        printTestInfo = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockbot.spook3", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_I, // The keycode of the key
                "category.skyblockbot.getInfo" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            CompletableFuture<Void> clickTick =
                    CompletableFuture.runAsync(Keybinds::asyncPressKeyAfterTick);

            if (startStopBot.wasPressed()) {
                if (!wasPressed) {
                    NotBotCore.run(client.player);
                } else {
                    NotBotCore.stop(client.player);
                }
                wasPressed = !wasPressed;
            }

            if (printTestInfo.wasPressed()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(CraftPriceCalculator.getInstance()::debugPrintRecipesPrices);
            }
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
