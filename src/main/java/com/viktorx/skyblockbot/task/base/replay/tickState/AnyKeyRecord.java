package com.viktorx.skyblockbot.task.base.replay.tickState;

public abstract class AnyKeyRecord {
    protected final int key;
    protected final int action;
    protected final int modifiers;

    public abstract int getSize();
    public abstract void press();
    public abstract void firstPress();
    public abstract void unpress();
    public abstract byte[] getData();

    public int getKey() {
        return key;
    }

    public int getAction() {
        return action;
    }

    public AnyKeyRecord(int key, int action, int modifiers) {
        this.key = key;
        this.action = action;
        this.modifiers = modifiers;
    }
}
