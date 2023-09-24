package com.viktorx.skyblockbot.replay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PlayerTickStateAdvanced extends PlayerTickState {
    private static final int tickStateSize = 8*3+4*2+1+1*7;
    private final boolean forward;
    private final boolean backward;
    private final boolean right;
    private final boolean left;
    private final boolean sneak;
    private final boolean sprint;
    private final boolean jump;

    PlayerTickStateAdvanced(Vec3d pos, Vec2f rot, boolean attack,
                            boolean forward, boolean backward, boolean right, boolean left,
                            boolean sneak, boolean sprint, boolean jump) {
        super(pos, rot, attack);
        this.forward = forward;
        this.backward = backward;
        this.right = right;
        this.left = left;
        this.sneak = sneak;
        this.sprint = sprint;
        this.jump = jump;
    }

    public void setRotationForClient(MinecraftClient client) {
        assert client.player != null;
        client.player.setYaw(super.getYaw());
        client.player.setPitch(super.getPitch());
    }

    public void setPositionForClient(MinecraftClient client) {
        assert client.player != null;
        client.player.setPosition(super.getPosition());
    }

    public void setButtonsForClient(MinecraftClient client) {
        client.options.attackKey.setPressed(super.isAttacking());
        client.options.forwardKey.setPressed(forward);
        client.options.backKey.setPressed(backward);
        client.options.rightKey.setPressed(right);
        client.options.leftKey.setPressed(left);
        client.options.sneakKey.setPressed(sneak);
        client.options.sprintKey.setPressed(sprint);
        client.options.jumpKey.setPressed(jump);
    }

    public static int getTickStateSize() {
        return tickStateSize;
    }

    public boolean isForward() {
        return forward;
    }

    public boolean isBackward() {
        return backward;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isSneak() {
        return sneak;
    }

    public boolean isSprint() {
        return sprint;
    }

    public boolean isJump() {
        return jump;
    }
}
