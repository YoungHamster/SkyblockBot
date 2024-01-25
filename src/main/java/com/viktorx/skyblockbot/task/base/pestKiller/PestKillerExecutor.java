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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

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
                return this;
            }
            MyKeyboard.INSTANCE.unpress(client.options.jumpKey);
            SkyblockBot.LOGGER.info("Got to free height, current y = " + client.player.getPos().y + ", trying to find pest");

            PestKiller pestTask = (PestKiller) PestKillerExecutor.INSTANCE.task;

            Entity pest = Utils.getClosestEntity(pestTask.getPestName());
            if (pest != null) {
                return new GetCloseToPest(pest);
            } else {
                return new FindPest();
            }
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

                Vec3d pestPos = pest.getPos();
                SkyblockBot.LOGGER.info("Found pest at x: " + pestPos.x + " y: " + pestPos.y + " z: " + pestPos.z + ", getting close to it");
                return new GetCloseToPest(pest);
            }

            return this;
        }
    }

    private static class GetCloseToPest implements ExecutorState {
        private final Entity pest;
        private final AtomicBoolean keepTracking = new AtomicBoolean(true);

        public GetCloseToPest(Entity pest) {
            this.pest = pest;
            LookHelper.trackEntityAsync(pest, keepTracking);
        }

        private boolean isObstacleBelowPlayer(PlayerEntity player, int range) {
            Box playerBB = player.getBoundingBox();
            Vec3d[] corners = {new Vec3d(playerBB.minX, playerBB.minY, playerBB.minZ),
                    new Vec3d(playerBB.maxX, playerBB.minY, playerBB.minZ),
                    new Vec3d(playerBB.minX, playerBB.minY, playerBB.maxZ),
                    new Vec3d(playerBB.maxX, playerBB.minY, playerBB.maxZ)};

            for (Vec3d corner : corners) {
                for (int i = 0; i < range; i++) {
                    if (Utils.isBlockSolid(new BlockPos((int) corner.x, (int) corner.y - i, (int) corner.z))) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            Vec3d deltaPos = client.player.getPos().subtract(pest.getPos());

            if (deltaPos.length() > 2.5) {
                Vec2f angleDelta = LookHelper.getAngleDeltaForEntity(pest);

                // forward-backward
                if (angleDelta.y < 90 || angleDelta.y > -90) {
                    MyKeyboard.INSTANCE.press(client.options.forwardKey);
                    SkyblockBot.LOGGER.info("Going forward");

                    // up-down
                    // was my intention to only change height when moving forward?
                    // will probably change that later
                    // unless it works unchanged
                    if (deltaPos.y < -1) {
                        MyKeyboard.INSTANCE.press(client.options.jumpKey);
                        SkyblockBot.LOGGER.info("Going up");
                    } else if (deltaPos.y > 1) {
                        // only go down if there are no obstacles, otherwise risk shifting over block and breaking simplistic AI
                        if (!isObstacleBelowPlayer(client.player, 2)) {
                            MyKeyboard.INSTANCE.press(client.options.sneakKey);
                            SkyblockBot.LOGGER.info("Going down");
                        }
                    }
                } else {
                    MyKeyboard.INSTANCE.press(client.options.backKey);
                    SkyblockBot.LOGGER.info("Going backward");
                }

                // left-right
                if (angleDelta.y > 15) {
                    MyKeyboard.INSTANCE.press(client.options.rightKey);
                    SkyblockBot.LOGGER.info("Going right");
                } else if (angleDelta.y < -15) {
                    MyKeyboard.INSTANCE.press(client.options.leftKey);
                    SkyblockBot.LOGGER.info("Going left");
                }

                return this;
            } else {
                MyKeyboard.INSTANCE.unpressAll();
                SkyblockBot.LOGGER.info("Got close to pest, trying to kill it");
                return new TrackAndKill(pest, keepTracking);
            }
        }
    }

    private static class TrackAndKill extends WaitingExecutorState {
        private final PestKillerExecutor parent = PestKillerExecutor.INSTANCE;
        private final AtomicBoolean keepTracking;
        private final Entity pest;

        public TrackAndKill(Entity pest, AtomicBoolean keepTracking) {
            this.keepTracking = keepTracking;
            this.pest = pest;
            setWaitTickLimit(4);
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            assert client.player != null;
            Vec3d deltaPos = client.player.getPos().subtract(pest.getPos());

            if (deltaPos.length() > 2.5) {
                keepTracking.set(false);

                PestKiller pestTask = (PestKiller) parent.task;

                Entity pest = Utils.getClosestEntity(pestTask.getPestName());
                if (pest != null) {
                    return new GetCloseToPest(pest);
                } else {
                    return new FindPest();
                }
            }

            if (!pest.isAlive()) {
                keepTracking.set(false);
                parent.task.completed();
                return new Idle();
            }

            Vec2f deltaAngle = LookHelper.getAngleDeltaForEntity(pest);

            if (deltaAngle.x < 3 && deltaAngle.y < 3) {
                if (!waitBeforeAction()) {
                    MyKeyboard.INSTANCE.press(client.options.attackKey);
                }
            }
            return this;
        }
    }
}
