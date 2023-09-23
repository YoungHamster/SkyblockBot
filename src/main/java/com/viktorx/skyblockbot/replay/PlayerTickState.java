package com.viktorx.skyblockbot.replay;

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PlayerTickState {
    private final Vec3d position;
    private final Vec2f rotation;
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

    public boolean getAttacking() {
        return isAttacking;
    }
}
