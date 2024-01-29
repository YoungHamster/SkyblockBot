package com.viktorx.skyblockbot.task.base.replay.tickState;

import com.viktorx.skyblockbot.movement.LookHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
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

        LookHelper.setYaw(getYaw());
        client.player.setPitch(getPitch());
    }

    public void setRotationRelative(MinecraftClient client, TickState firstTick, Vec2f startRot) {
        assert client.player != null;
        Vec2f rot = getRotationRelative(firstTick, startRot);
        client.player.setPitch(rot.x);
        client.player.setYaw(rot.y);
    }

    public void setPositionForClient(MinecraftClient client) {
        assert client.player != null;
        client.player.setPosition(getPosition());
    }

    public void setPositionRelative(MinecraftClient client, TickState firstTick, Vec2f startRot, Vec3d startPos) {
        assert client.player != null;
        client.player.setPosition(getPositionRelative(firstTick, startRot, startPos));
    }

    public Vec2f getRotationRelative(TickState firstTick, Vec2f startRot) {
        float deltaPitch = startRot.x - firstTick.getPitch();
        float deltaYaw = startRot.y - firstTick.getYaw();

        return new Vec2f(MathHelper.clamp(getPitch() + deltaPitch, -90, 90), getYaw() + deltaYaw);
    }

    public Vec3d getPositionRelative(TickState firstTick, Vec2f startRot, Vec3d startPos) {
        float deltaYaw = firstTick.getYaw() - startRot.y;
        double deltaYawRads = Math.toRadians(deltaYaw);
        Vec3d deltaPos = getPosition().subtract(firstTick.getPosition());
        // -- rotate delta pos the angle of deltaYaw
        double rotX = deltaPos.x * Math.cos(deltaYawRads) - deltaPos.z * Math.sin(deltaYawRads);
        double rotZ = deltaPos.z * Math.cos(deltaYawRads) + deltaPos.x * Math.sin(deltaYawRads);

        return new Vec3d(startPos.x + rotX, startPos.y + (startPos.y - firstTick.getPosition().y), startPos.z + rotZ);
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
