package com.viktorx.skyblockbot.task.base.changeIsland;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class ChangeIsland extends BaseTask<ChangeIslandExecutor> {

    private final String command;

    public ChangeIsland(String command, Runnable whenCompleted, Runnable whenAborted) {
        super(ChangeIslandExecutor.INSTANCE, whenCompleted, whenAborted);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
