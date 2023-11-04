package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.getSkills.GetSkills;
import com.viktorx.skyblockbot.task.base.replay.Replay;

public class SetupMinionsForSkills extends CompoundTask {
    private static final String buildPlaceForMinionsReplay = "build_place_for_minions";
    private final Task buildPlaceForMinions;
    private final Task getSkills;
    private final Task craft;

    public SetupMinionsForSkills(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);

        buildPlaceForMinions = new Replay(buildPlaceForMinionsReplay,
                this::whenBuildPlaceForMinionsCompleted,
                this::whenBuildPlaceForMinionsAborted);

        getSkills = new GetSkills(this::whenGetSkillCompleted, this::whenGetSkillsAborted);

        craft = new CraftTask(this::whenCraftCompleted, this::whenCraftAborted);

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
}
