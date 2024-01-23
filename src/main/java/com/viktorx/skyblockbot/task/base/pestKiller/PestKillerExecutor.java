package com.viktorx.skyblockbot.task.base.pestKiller;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import com.viktorx.skyblockbot.utils.MyKeyboard;
import com.viktorx.skyblockbot.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PestKillerExecutor extends BaseExecutor {
    public static PestKillerExecutor INSTANCE = new PestKillerExecutor();

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        return new StartFlying();
    }

    private void onTick(MinecraftClient client) {
        this.state = this.state.onTick(client);
    }

    /**
     * First bot need to start flying and get to height at which there are no blocks in current plot
     */
    private static class StartFlying implements ExecutorState {
        private static final int spacePressesToFly = 2;
        private static final int ticksToPress = 2;
        private int spacePressedCounter = 0;
        private int tickCounter = 0;
        private boolean shouldBePressedCurrently = true;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (tickCounter++ < ticksToPress) {
                if (shouldBePressedCurrently) {
                    MyKeyboard.INSTANCE.press(client.options.jumpKey);
                } else {
                    MyKeyboard.INSTANCE.unpress(client.options.jumpKey);
                }
                return this;
            }
            tickCounter = 0;

            if (!shouldBePressedCurrently) { // if last action was unpressing spacebar then we increase press counter
                spacePressedCounter++;
            }
            shouldBePressedCurrently = !shouldBePressedCurrently;
            if (spacePressedCounter == spacePressesToFly) {
                SkyblockBot.LOGGER.info("Started flying, going up to free height");
                return new FlyToFreeHeight();
            }
            return this;
        }
    }

    private static class FlyToFreeHeight implements ExecutorState {
        private static final int plotsize = 94;
        private static final int maxY = 77;
        private static final int minY = 67;
        private final double freeHeight;

        public FlyToFreeHeight() {
            freeHeight = findFreeHeight(
                    ((PestKiller) PestKillerExecutor.INSTANCE.task).getPlotNumber());
            SkyblockBot.LOGGER.info("Found free height: " + freeHeight);
        }

        private double findFreeHeight(int plotNumber) {
            int minx = -1;
            int minz = -1;

            // TODO - get rid of this loop
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    if (PestKiller.gardenPlotMap[i][j] == plotNumber) {
                        minz = (int) ((i - 2.5) * plotsize);
                        minx = (int) ((j - 2.5) * plotsize);

                        SkyblockBot.LOGGER.info("Found plots min x and z. i = " + i + ", j = " + j);
                    }
                }
            }

            SkyblockBot.LOGGER.info("minX = " + minx + ", minZ = " + minz);

            boolean goToNextY = false;
            for (int y = minY; y < maxY; y++) {
                for (int x = minx; (x < minx + plotsize) && !goToNextY; x++) {
                    for (int z = minz; (z < minz + plotsize) && !goToNextY; z++) {
                        if (Utils.isBlockSolid(new BlockPos(x, y, z))) {
                            goToNextY = true;
                        }
                    }
                }
                if (!goToNextY) {
                    return y;
                }
                goToNextY = false;
            }

            return -1;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            if (client.player.getPos().y < freeHeight) {
                MyKeyboard.INSTANCE.press(client.options.jumpKey);
                SkyblockBot.LOGGER.info("Pressing jump key to fly to free height");
                return this;
            }
            MyKeyboard.INSTANCE.unpress(client.options.jumpKey);
            SkyblockBot.LOGGER.info("Got to free height, current y = " + client.player.getPos().y + ", trying to find pest");

            return new FindPest();
        }
    }

    private static class FindPest implements ExecutorState {
        private static final String pestFindingReplayName = "find_pest.bin";
        private static final Replay pestFindingReplay = new Replay(true, pestFindingReplayName, null, null);
        private final PestKillerExecutor parent = PestKillerExecutor.INSTANCE;

        public FindPest() {
            SkyblockBot.LOGGER.info("Playing pest finding replay");
            pestFindingReplay.execute();
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            PestKiller pestTask = (PestKiller) parent.task;

            Entity pest = Utils.getClosestEntity(pestTask.getPestName());
            if (pest != null) {
                pestFindingReplay.abort();
                SkyblockBot.LOGGER.info("Found pest, getting close to it");
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
            pressedKeys.forEach(MyKeyboard.INSTANCE::unpress);
            pressedKeys.clear();
        }
    }
}
