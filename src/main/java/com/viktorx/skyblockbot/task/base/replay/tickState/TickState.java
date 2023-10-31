package com.viktorx.skyblockbot.task.base.replay.tickState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class TickState {

    private static final int tickStateSize = 8 * 3 + 4 * 2 + 4;

    private final Vec3d position;
    private final Vec2f rotation;
    private final List<AnyKeyRecord> keys;

    public TickState(List<AnyKeyRecord> keys) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;

        this.position = client.player.getPos();
        this.rotation = new Vec2f(
                client.player.getYaw(),
                client.player.getPitch()
        );
        this.keys = keys;
    }

    public TickState(Vec3d pos, Vec2f rot, List<AnyKeyRecord> keys) {
        this.position = pos;
        this.rotation = rot;
        this.keys = keys;
    }

    public static int getEmptyTickStateSize() {
        return tickStateSize;
    }

    public void setRotationForClient(MinecraftClient client) {
        assert client.player != null;

        client.player.setYaw(getYaw());
        client.player.setPitch(getPitch());
    }

    public void setPositionForClient(MinecraftClient client) {
        assert client.player != null;
        client.player.setPosition(getPosition());
    }

    public void setButtonsForClient() {
        for (AnyKeyRecord key : keys) {
            key.press();
        }
    }

    public Vec2f getRotation() {
        return rotation;
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

    public List<AnyKeyRecord> getKeys() {
        return keys;
    }
}
