package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractVisitorExecutor extends AbstractMenuClickingExecutor {
    protected AtomicBoolean keepTracking = new AtomicBoolean(false);
    protected AbstractVisitorTask<?> task;

    public static class StartTrackingVisitor implements ExecutorState {
        private AtomicBoolean keepTracking = new AtomicBoolean(false);
        private final AbstractVisitorExecutor parent;

        public StartTrackingVisitor(AbstractVisitorExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            Entity npc = Utils.getClosestEntity(parent.task.getVisitorName());
            if (npc != null) {
                /*
                 * If npc is moving we wait
                 */
                if(npc.getLerpedPos(10).subtract(npc.getPos()).length() != 0) {
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

        public TrackingVisitor(AbstractVisitorExecutor parent ,AtomicBoolean keepTracking) {
            this.keepTracking = keepTracking;
            this.parent = parent;
        }

        /*
         * TODO maybe refactor it later, maybe decide that this code is fine
         *  I wrote it while i was in a fuzzy state of mind
         */
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            Entity npc = Utils.getClosestEntity(parent.task.getVisitorName());
            /*
             * If npc is moving we stop tracking and wait till they stop
             */
            if(npc.getLerpedPos(10).subtract(npc.getPos()).length() != 0) {
                keepTracking.set(false);
                return new StartTrackingVisitor(parent);
            }

            Vec2f dPdY = LookHelper.getAngleDeltaForEntity(npc); // delta pitch, delta yaw

            if(Math.abs(dPdY.x) < 1.0f && Math.abs(dPdY.y) < 1.0f) {
                assert client.player != null;
                if(Utils.distanceBetween(npc.getPos(), client.player.getPos()) > 4.0d) {
                    npcTooFarTickCounter++;
                    if(npcTooFarTickCounter > VisitorExecutorSettings.npcTooFarTickThreshold) {
                        SkyblockBot.LOGGER.warn("Visitor npc is too far, can't reach it, aborting visitor task.");
                        parent.abort();
                        return new Idle();
                    }

                    return this;
                }

                Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                keepTracking.set(false);
                SkyblockBot.LOGGER.info("Clicked on visitor. Waiting.");
                return new WaitingForMenu(parent);
            }
            return this;
        }
    }
}
