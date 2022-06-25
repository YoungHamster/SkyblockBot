package com.viktorx.skyblockbot.keybinds;

import com.viktorx.skyblockbot.NotBotCore;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.skyblock.ScoreboardUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static KeyBinding startStopBot;
    private static KeyBinding printTestInfo;

    private static boolean wasPressed = false;

    public static void Init() {
        startStopBot = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockbot.spook", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_O, // The keycode of the key
                "category.skyblockbot.toggle" // The translation key of the keybinding's category.
        ));

        printTestInfo = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockbot.spook2", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_I, // The keycode of the key
                "category.skyblockbot.getInfo" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (startStopBot.wasPressed()) {
                if (!wasPressed) {
                    NotBotCore.run(client.player);
                } else {
                    NotBotCore.stop(client.player);
                }
                wasPressed = !wasPressed;
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (printTestInfo.wasPressed()) {
                SkyblockBot.LOGGER.info(SBUtils.getBankBalance());
            }
        });

    }
}
