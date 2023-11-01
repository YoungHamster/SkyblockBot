package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.replay.Replay;

public class SetupMinionsForSkillsTask extends Task {
    private static final String buildPlaceForMinionsReplay = "build_place_for_minions";
    private Task currentTask;
    private final Task buildPlaceForMinions;
    /*private final Task getSkills;
    private final Task buyBZItems;
    private final Task craftItem;*/

    public SetupMinionsForSkillsTask() {
        buildPlaceForMinions = new Replay(buildPlaceForMinionsReplay);

    }

    public void execute() {
        if(isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute SetupMinionsForSkillsTask, already in execution");
            return;
        }
        // TODO
    }
    public void pause() {
        if(currentTask != null) {
            currentTask.pause();
        }
    }
    public void resume() {
        if(currentTask != null) {
            currentTask.resume();
        }
    }
    public void abort() {
        if(currentTask != null) {
            currentTask.abort();
        }
        currentTask = null;
    }
    public boolean isExecuting() {
        if(currentTask != null) {
            return currentTask.isExecuting();
        }
        return false;
    }
    public boolean isPaused() {
        if(currentTask != null) {
            return currentTask.isPaused();
        }
        return false;
    }
}
