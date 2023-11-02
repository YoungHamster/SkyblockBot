package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.getSkills.GetSkills;
import com.viktorx.skyblockbot.task.base.replay.Replay;

public class SetupMinionsForSkillsTask extends Task {
    private static final String buildPlaceForMinionsReplay = "build_place_for_minions";
    private Task currentTask;
    private final Task buildPlaceForMinions;
    private final Task getSkills;
    private final Task craft;

    public SetupMinionsForSkillsTask() {
        buildPlaceForMinions = new Replay(buildPlaceForMinionsReplay);
        buildPlaceForMinions.whenCompleted(this::whenBuildPlaceForMinionsCompleted);
        buildPlaceForMinions.whenAborted(this::whenBuildPlaceForMinionsAborted);

        getSkills = new GetSkills();
        getSkills.whenCompleted(this::whenGetSkillCompleted);
        getSkills.whenAborted(this::whenGetSkillsAborted);

        craft = new CraftTask();
        craft.whenCompleted(this::whenCraftCompleted);
        craft.whenAborted(this::whenCraftAborted);

    }

    private void whenBuildPlaceForMinionsCompleted() {

    }

    private void whenBuildPlaceForMinionsAborted() {

    }

    private void whenGetSkillCompleted() {

    }

    private void whenGetSkillsAborted() {

    }

    private void whenCraftCompleted() {

    }

    private void whenCraftAborted() {

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
