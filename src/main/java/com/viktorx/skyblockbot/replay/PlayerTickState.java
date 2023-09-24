package com.viktorx.skyblockbot.replay;

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PlayerTickState {
    private static final int tickStateSize = 8 + 8 + 8 + 4 + 4 + 1; // in bytes
    private final Vec3d position;
    private Vec2f rotation;
    private final boolean isAttacking;

    public PlayerTickState(Vec3d position, Vec2f rotation, boolean isAttacking) {
        this.position = position;
        this.rotation = rotation;
        this.isAttacking = isAttacking;
    }

    public float getYaw() {
        return rotation.x;
    }

    public float getPitch() {
        return rotation.y;
    }

    public Vec3d getPosition() {
        return position;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public static int getTickStateSize() {
        return tickStateSize;
    }

    public void setRotation(Vec2f rotation) {
        this.rotation = rotation;
    }

    public Vec2f getRotation() {
        return rotation;
    }
}
