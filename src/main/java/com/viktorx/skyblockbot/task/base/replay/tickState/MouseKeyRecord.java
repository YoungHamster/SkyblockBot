package com.viktorx.skyblockbot.task.base.replay.tickState;

import com.viktorx.skyblockbot.mixins.InputRelated.IMouseMixin;
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

    private void press(int action) {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        Mouse mouse = MinecraftClient.getInstance().mouse;
        ((IMouseMixin) mouse).callOnMouseButton(window, this.key, action, this.modifiers);
    }

    public void press() {
        press(this.action);
    }

    public void firstPress() {
        press(1);
    }

    public void unpress() {
        press(0);
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
