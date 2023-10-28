package com.viktorx.skyblockbot.task.replay.tickState;

import net.minecraft.client.MinecraftClient;

import java.nio.ByteBuffer;

public class KeyboardKeyRecord extends AnyKeyRecord {
    private final int scancode;

    public int getSize() {
        return 17;
    }

    public static byte getType() {
        return 1;
    }

    public KeyboardKeyRecord(int key, int scancode, int action, int modifiers) {
        super(key, action, modifiers);
        this.scancode = scancode;
    }

    public void press() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        MinecraftClient.getInstance().keyboard.onKey(window, key, scancode, action, modifiers);
    }

    public byte[] getData() {
        ByteBuffer bb = ByteBuffer.allocate(getSize());
        bb.put(getType());
        bb.putInt(key);
        bb.putInt(scancode);
        bb.putInt(action);
        bb.putInt(modifiers);
        return bb.array();
    }
}
