package com.viktorx.skyblockbot.task.base.menuClickingTasks.getSkills;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class GetSkills extends BaseTask<GetSkillsExecutor> {


    public GetSkills(Runnable whenCompleted, Runnable whenAborted) {
        super(GetSkillsExecutor.INSTANCE, whenCompleted, whenAborted);
    }
}
