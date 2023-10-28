package com.viktorx.skyblockbot.task.replay.tickState;

import com.viktorx.skyblockbot.mixins.IMouseMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

import java.nio.ByteBuffer;

public class MouseKeyRecord extends AnyKeyRecord {

    public MouseKeyRecord(int key, int action, int modifiers) {
        super(key, action, modifiers);
    }

    public int getSize() {
        return 13;
    }

    public static byte getType() {
        return 2;
    }

    public void press() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        Mouse mouse = MinecraftClient.getInstance().mouse;
        ((IMouseMixin) mouse).callOnMouseButton(window, key, action, modifiers);
    }

    public byte[] getData() {
        ByteBuffer bb = ByteBuffer.allocate(getSize());
        bb.put(getType());
        bb.putInt(key);
        bb.putInt(action);
        bb.putInt(modifiers);
        return bb.array();
    }
}
