package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Currently this is the only base task depending on some other task
 * When visitor is too far to click on them, visitor executor starts playing replay that goes around visitors spawn area,
 * in an attempt to get close enough to click on visitor
 */
public abstract class AbstractVisitorExecutor extends AbstractMenuClickingExecutor {
    protected AtomicBoolean keepTracking = new AtomicBoolean(false);
    private final AtomicBoolean replayCompleted = new AtomicBoolean(false);
    private final AtomicBoolean replayAborted = new AtomicBoolean(false);
    private Replay goAroundVisitors;
    private final String goAroundVisitorsReplay = "go_around_visitors.bin";

    protected abstract ExecutorState getStateWhenVisitorOpened();

    public static class StartTrackingVisitor implements ExecutorState {
        private AtomicBoolean keepTracking = new AtomicBoolean(false);
        private final List<Vec3d> npcPosHistory = new ArrayList<>();
        private final AbstractVisitorExecutor parent;

        public StartTrackingVisitor(AbstractVisitorExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            AbstractVisitorTask<?> currentTask = (AbstractVisitorTask<?>) parent.task;
            Entity npc = Utils.getClosestEntity(currentTask.getVisitorName());
            if (npc != null) {
                /*
                 * If npc is moving we wait
                 * but if npc hasn't moved at all in past 10 ticks we look at it and talk to it
                 */
                npcPosHistory.add(npc.getPos());
                if (npcPosHistory.size() < 12 || npcPosHistory.get(npcPosHistory.size() - 12).subtract(npc.getPos()).length() != 0) {
                    return this;
                }

                keepTracking.set(false);
                keepTracking = new AtomicBoolean(true);

                LookHelper.trackEntityAsync(npc, keepTracking);

                return new TrackingVisitor(parent, keepTracking);
            } else {
                SkyblockBot.LOGGER.info("TalkToVisitor task aborted, no visitors left");
                parent.task.aborted();
                return new Idle();
            }
        }
    }

    public static class TrackingVisitor implements ExecutorState {
        private final AtomicBoolean keepTracking;
        private int npcTooFarTickCounter = 0;
        private final AbstractVisitorExecutor parent;

        public TrackingVisitor(AbstractVisitorExecutor parent, AtomicBoolean keepTracking) {
            this.keepTracking = keepTracking;
            this.parent = parent;
        }

        /*
         * TODO maybe refactor it later, maybe decide that this code is fine
         *  I wrote it while i was in a fuzzy state of mind
         */
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            AbstractVisitorTask<?> currentTask = (AbstractVisitorTask<?>) parent.task;
            Entity npc = Utils.getClosestEntity(currentTask.getVisitorName());
            /*
             * If npc is moving we stop tracking and wait till they stop
             */
            if (npc.getLerpedPos(10).subtract(npc.getPos()).length() != 0) {
                keepTracking.set(false);
                return new StartTrackingVisitor(parent);
            }

            Vec2f dPdY = LookHelper.getAngleDeltaForEntity(npc); // delta pitch, delta yaw

            if (Math.abs(dPdY.x) < 1.0f && Math.abs(dPdY.y) < 1.0f) {
                assert client.player != null;
                if (Utils.distanceBetween(npc.getPos(), client.player.getPos()) >= 2.8d) {
                    npcTooFarTickCounter++;
                    if (npcTooFarTickCounter > VisitorExecutorSettings.npcTooFarTickThreshold) {
                        SkyblockBot.LOGGER.warn("Visitor npc is too far, can't reach it, trying to get closer.");
                        keepTracking.set(false);

                        parent.goAroundVisitors = new Replay(parent.goAroundVisitorsReplay,
                                ()->parent.replayCompleted.set(true),
                                ()->parent.replayAborted.set(true));
                        parent.goAroundVisitors.execute();
                        return new GoingAroundVisitors(parent);
                    }

                    return this;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                keepTracking.set(false);
                SkyblockBot.LOGGER.info("Clicked on visitor. Waiting.");
                return new WaitingForNamedMenu(parent, currentTask.getVisitorName())
                        .setNextState(parent.getStateWhenVisitorOpened());
            }
            return this;
        }
    }

    private static class GoingAroundVisitors implements ExecutorState {
        private final AbstractVisitorExecutor parent;

        public GoingAroundVisitors(AbstractVisitorExecutor parent) {
            this.parent = parent;
        }
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            AbstractVisitorTask<?> currentTask = (AbstractVisitorTask<?>) parent.task;
            Entity npc = Utils.getClosestEntity(currentTask.getVisitorName());

            assert client.player != null;
            if (Utils.distanceBetween(npc.getPos(), client.player.getPos()) < 2.8d) {
                parent.goAroundVisitors.abort();
                return new StartTrackingVisitor(parent);
            }
            if(parent.replayCompleted.get() || parent.replayAborted.get()) {
                parent.task.aborted();
                return new Idle();
            }

            return this;
        }
    }
}
