package com.viktorx.skyblockbot.replay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;

public class BetweenTickState {
    private final Vec2f rotation;
    private final boolean attack;
    private final boolean forward;
    private final boolean backward;
    private final boolean right;
    private final boolean left;
    private final boolean sneak;
    private final boolean sprint;
    private final boolean jump;

    BetweenTickState(Vec2f rotation, boolean attack, boolean forward, boolean backward, boolean right, boolean left,
                     boolean sneak, boolean sprint, boolean jump) {
        this.rotation = rotation;
        this.attack = attack;
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
        client.player.setYaw(getYaw());
        client.player.setPitch(getPitch());
    }

    public void setButtonsForClient(MinecraftClient client) {
        client.options.attackKey.setPressed(isAttack());
        client.options.forwardKey.setPressed(forward);
        client.options.backKey.setPressed(backward);
        client.options.rightKey.setPressed(right);
        client.options.leftKey.setPressed(left);
        client.options.sneakKey.setPressed(sneak);
        client.options.sprintKey.setPressed(sprint);
        client.options.jumpKey.setPressed(jump);
    }

    public float getYaw() {
        return rotation.x;
    }

    public float getPitch() {
        return rotation.y;
    }

    public boolean isAttack() {
        return attack;
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
