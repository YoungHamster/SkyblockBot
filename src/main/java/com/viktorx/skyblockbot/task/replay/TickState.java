package com.viktorx.skyblockbot.task.replay;

import com.viktorx.skyblockbot.keybinds.Keybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class TickState {

    private static final int tickStateSize = 8 * 3 + 4 * 2 + 8 + 4;

    private final Vec3d position;
    private final Vec2f rotation;
    private final boolean attack;
    private final boolean forward;
    private final boolean backward;
    private final boolean right;
    private final boolean left;
    private final boolean sneak;
    private final boolean sprint;
    private final boolean jump;
    private final int hotbarSlot;

    TickState() {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;

        this.position = client.player.getPos();
        this.rotation = new Vec2f(
                client.player.getYaw(),
                client.player.getPitch()
        );
        this.attack = client.options.attackKey.isPressed();
        this.forward = client.options.forwardKey.isPressed();
        this.backward = client.options.backKey.isPressed();
        this.right = client.options.rightKey.isPressed();
        this.left = client.options.leftKey.isPressed();
        this.sneak = client.options.sneakKey.isPressed();
        this.sprint = client.options.sprintKey.isPressed();
        this.jump = client.options.jumpKey.isPressed();

        for (int i = 0; i < client.options.hotbarKeys.length; i++) {
            if (client.options.hotbarKeys[i].isPressed()) {
                hotbarSlot = i;
                return;
            }
        }
        hotbarSlot = -1;
    }

    TickState(Vec3d pos, Vec2f rot, boolean attack,
              boolean forward, boolean backward, boolean right, boolean left,
              boolean sneak, boolean sprint, boolean jump, int hotbarSlot) {

        this.position = pos;
        this.rotation = rot;
        this.attack = attack;
        this.forward = forward;
        this.backward = backward;
        this.right = right;
        this.left = left;
        this.sneak = sneak;
        this.sprint = sprint;
        this.jump = jump;
        this.hotbarSlot = hotbarSlot;
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

    public void setButtonsForClient(MinecraftClient client) {
        client.options.attackKey.setPressed(attack);
        client.options.forwardKey.setPressed(forward);
        client.options.backKey.setPressed(backward);
        client.options.rightKey.setPressed(right);
        client.options.leftKey.setPressed(left);
        client.options.sneakKey.setPressed(sneak);
        client.options.sprintKey.setPressed(sprint);
        client.options.jumpKey.setPressed(jump);

        /*
         * KeyBinding.setPressed(true) doesn't work for hotbar. This works
         */
        if (hotbarSlot != -1) {
            Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[hotbarSlot]);
        }
    }

    public static int getTickStateSize() {
        return tickStateSize;
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

    public int getHotbarSlot() {
        return hotbarSlot;
    }
}
