package com.viktorx.skyblockbot.task.replay.tickState;

import java.nio.ByteBuffer;

public abstract class AnyKeyRecord {
    protected final int key;
    protected final int action;
    protected final int modifiers;

    public abstract int getSize();
    public abstract void press();
    public abstract byte[] getData();

    public AnyKeyRecord(int key, int action, int modifiers) {
        this.key = key;
        this.action = action;
        this.modifiers = modifiers;
    }
}
