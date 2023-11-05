package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.movement.LookHelper;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractVisitorExecutor extends AbstractMenuClickingExecutor {
    protected AtomicBoolean keepTracking = new AtomicBoolean(false);
    protected AbstractVisitorTask<?> task;

    protected void whenStartTrackingVisitor() {
        Entity npc = Utils.getClosestEntity(task.getVisitorName());
        if (npc != null) {
            /*
             * If npc is moving we wait
             */
            if(npc.getLerpedPos(10).subtract(npc.getPos()).length() != 0) {
                return;
            }

            keepTracking.set(false);
            keepTracking = new AtomicBoolean(true);

            LookHelper.trackEntityAsync(npc, keepTracking);

            state = getState("TRACKING_VISITOR");
        } else {
            SkyblockBot.LOGGER.info("TalkToVisitor task aborted, no visitors left");
            state = getState("IDLE");
            task.aborted();
        }
    }

    /*
     * TODO maybe refactor it later, maybe decide that this code is fine
     *  I wrote it while i was in a fuzzy state of mind
     */
    protected void whenTrackingVisitor(MinecraftClient client) {
        Entity npc = Utils.getClosestEntity(task.getVisitorName());
        /*
         * If npc is moving we stop tracking and wait till they stop
         */
        if(npc.getLerpedPos(10).subtract(npc.getPos()).length() != 0) {
            keepTracking.set(false);
            state = getState("START_TRACKING_VISITOR");
            return;
        }

        Vec2f dPdY = LookHelper.getAngleDeltaForEntity(npc); // delta pitch, delta yaw

        if(Math.abs(dPdY.x) < 1.0f && Math.abs(dPdY.y) < 1.0f) {
            Keybinds.asyncPressKeyAfterTick(client.options.useKey);
            keepTracking.set(false);
            state = getState("WAITING_FOR_MENU");

            SkyblockBot.LOGGER.info("Clicked on visitor. Waiting.");
        }
    }
}
