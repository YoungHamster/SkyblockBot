package com.viktorx.skyblockbot;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class RayTraceStuff {
    private static Vec3d getStartVec(PlayerEntity player) {
        double startX = player.getPos().x;
        double startY = player.getPos().y + player.getEyeHeight(EntityPose.STANDING);
        double startZ = player.getPos().z;
        return new Vec3d(startX, startY, startZ);
    }

    private static Vec3d getEndVec(PlayerEntity player, Vec3d startVec, double range) {
        double endX = startVec.x + player.getRotationVec(1).x * range;
        double endY = startVec.y + player.getRotationVec(1).y * range;
        double endZ = startVec.z + player.getRotationVec(1).z * range;
        return new Vec3d(endX, endY, endZ);
    }

    public static Entity rayTraceEntityFromPlayer(PlayerEntity player, ClientWorld world, double range) {
        Vec3d startVec = getStartVec(player);
        Vec3d endVec = getEndVec(player, startVec, range);

        for (Entity entity : world.getEntities()) {
            Optional<Vec3d> collision = entity.getBoundingBox().raycast(startVec, endVec);
            if (collision.isPresent())
                return entity;
        }
        return null;
    }
}
