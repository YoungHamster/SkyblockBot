package com.viktorx.skyblockbot.task.base.menuClickingTasks.getSkills;

import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.replay.ExecutorState;

public class GetSkillsExecutor extends BaseExecutor {

    public static final GetSkillsExecutor INSTANCE = new GetSkillsExecutor();


    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        return new Idle();
    }
}
