package com.viktorx.skyblockbot.task.base.menuClickingTasks.getSkills;

import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;

public class GetSkillsExecutor extends BaseExecutor {

    public static final GetSkillsExecutor INSTANCE = new GetSkillsExecutor();


    @Override
    public <T extends BaseTask<?>> void execute(T task) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void abort() {

    }

    @Override
    public <T extends BaseTask<?>> boolean isExecuting(T task) {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }
}
