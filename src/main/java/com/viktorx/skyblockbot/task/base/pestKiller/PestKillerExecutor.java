package com.viktorx.skyblockbot.task.base.pestKiller;

import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import com.viktorx.skyblockbot.utils.MyKeyboard;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PestKillerExecutor extends BaseExecutor {
    public static PestKillerExecutor INSTANCE = new PestKillerExecutor();

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        return new StartFlying();
    }

    /**
     * First bot need to start flying and get to height at which there are no blocks in current plot
     */
    private static class StartFlying implements ExecutorState {
        private int spacePressedCounter = 0;
        private static final int spacePressesToFly = 2;
        private static final int ticksToPress = 2;
        private int tickCounter = 0;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (tickCounter++ < ticksToPress) {
                return this;
            }
            if (client.options.jumpKey.isPressed()) {
                spacePressedCounter++;
                client.options.jumpKey.setPressed(false);
            } else {
                client.options.jumpKey.setPressed(true);
            }
            if (spacePressedCounter == spacePressesToFly) {
                return new FlyToFreeHeight();
            }
            return this;
        }
    }

    private static class FlyToFreeHeight implements ExecutorState {
        private final double freeHeight;

        public FlyToFreeHeight() {
            freeHeight = findFreeHeight();
        }

        private double findFreeHeight() {
            // TODO
            return 6;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            if (client.player.getPos().y < freeHeight) {
                if (!client.options.jumpKey.isPressed()) {
                    client.options.jumpKey.setPressed(true);
                }
                return this;
            }
            client.options.jumpKey.setPressed(false);

            return new FindPest();
        }
    }

    private static class FindPest implements ExecutorState {
        private final PestKillerExecutor parent = PestKillerExecutor.INSTANCE;
        private static final String pestFindingReplayName = "find_pest.bin";
        private static final Replay pestFindingReplay = new Replay(true, pestFindingReplayName, null, null);

        public FindPest() {
            pestFindingReplay.execute();
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            PestKiller pestTask = (PestKiller) parent.task;

            Entity pest = Utils.getClosestEntity(pestTask.getPestName());
            if (pest != null) {
                pestFindingReplay.abort();
                return new GetCloseToPest(pest);
            }

            return this;
        }
    }

    private static class GetCloseToPest extends KeyPressingState {
        private final Entity pest;
        private final AtomicBoolean keepTracking = new AtomicBoolean(true);

        public GetCloseToPest(Entity pest) {
            this.pest = pest;
            LookHelper.trackEntity(pest, keepTracking);
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            Vec3d deltaPos = client.player.getPos().subtract(pest.getPos());

            if (deltaPos.length() > 2.5) {
                Vec2f angleDelta = LookHelper.getAngleDeltaForEntity(pest);

                // forward-backward
                if (angleDelta.y > 90 || angleDelta.y < -90) {
                    press(client.options.forwardKey);

                    // up-down
                    if (deltaPos.y > 1) {
                        press(client.options.jumpKey);
                    } else if (deltaPos.y < -1) {
                        press(client.options.sneakKey);
                    }
                } else {
                    press(client.options.backKey);
                }

                // left-right
                if (angleDelta.x > 15) {
                    press(client.options.rightKey);
                } else if (angleDelta.x < -15) {
                    press(client.options.leftKey);
                }

                return this;
            } else {
                unpressAll();
                return new TrackAndKill(pest, keepTracking);
            }
        }
    }

    private static class TrackAndKill extends KeyPressingState {
        private final PestKillerExecutor parent = PestKillerExecutor.INSTANCE;
        private final AtomicBoolean keepTracking;
        private final Entity pest;

        public TrackAndKill(Entity pest, AtomicBoolean keepTracking) {
            this.keepTracking = keepTracking;
            this.pest = pest;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            Vec3d deltaPos = client.player.getPos().subtract(pest.getPos());

            if (deltaPos.length() > 2.5) {
                keepTracking.set(false);
                unpressAll();
                return new FindPest();
            }

            if (!pest.isAlive()) {
                keepTracking.set(false);
                parent.task.completed();
                unpressAll();
                return new Idle();
            }

            Vec2f deltaAngle = LookHelper.getAngleDeltaForEntity(pest);

            if (deltaAngle.x < 3 && deltaAngle.y < 3) {
                press(client.options.attackKey);
            }
            return this;
        }
    }

    private abstract static class KeyPressingState implements ExecutorState {
        private final List<KeyBinding> pressedKeys = new ArrayList<>();

        protected void press(KeyBinding key) {
            MyKeyboard.INSTANCE.press(key);
            if (!pressedKeys.contains(key)) {
                pressedKeys.add(key);
            }
        }

        protected void unpressAll() {
            pressedKeys.forEach(MyKeyboard.INSTANCE::press);
            pressedKeys.clear();
        }
    }
}
