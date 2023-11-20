package com.viktorx.skyblockbot.utils;

import com.viktorx.skyblockbot.mixins.InputRelated.KeyBindingMixin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyKeyboard {
    public static final MyKeyboard INSTANCE = new MyKeyboard();

    private MyKeyboard() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private static class KeyPressedFirstLast {
        public final long firstPressed;
        public long lastPressed;

        public KeyPressedFirstLast(long firstPressed) {
            this.firstPressed = firstPressed;
            this.lastPressed = firstPressed;
        }
    }

    private final Map<Integer, KeyPressedFirstLast> pressedKeys = new HashMap<>();

    public void press(KeyBinding key) {
        int keyCode = ((KeyBindingMixin) key).getBoundKey().getCode();

        synchronized (pressedKeys) {
            if (pressedKeys.containsKey(keyCode)) {
                if (System.currentTimeMillis() - pressedKeys.get(keyCode).firstPressed >= 400) {
                    press(keyCode, 2);
                    pressedKeys.get(keyCode).lastPressed = System.currentTimeMillis();
                }
            } else {
                press(keyCode, 1);
                pressedKeys.put(keyCode, new KeyPressedFirstLast(System.currentTimeMillis()));
            }
        }
    }

    public void unpress(KeyBinding key) {
        int keyCode = ((KeyBindingMixin) key).getBoundKey().getCode();

        synchronized (pressedKeys) {
            if (pressedKeys.containsKey(keyCode)) {
                press(keyCode, 0);
                pressedKeys.remove(keyCode);
            }
        }
    }

    private void onTick(MinecraftClient client) {
        List<Integer> unpressedKeys = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        synchronized (pressedKeys) {
            pressedKeys.forEach((key, value) -> {
                if (currentTime - value.lastPressed > 30) {
                    press(key, 0);
                    unpressedKeys.add(key);
                }
            });

            unpressedKeys.forEach(pressedKeys::remove);
        }
    }

    private void press(int keyCode, int action) {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        MinecraftClient.getInstance().keyboard.onKey(window, keyCode, 0, action, 0);
    }
}
