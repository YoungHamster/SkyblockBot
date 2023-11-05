package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import com.viktorx.skyblockbot.task.base.BaseTask;

public abstract class AbstractVisitorTask<T extends BaseExecutor> extends BaseTask<T> {
    private String visitorName;

    public AbstractVisitorTask(T executor, Runnable whenCompleted, Runnable whenAborted) {
        super(executor, whenCompleted, whenAborted);
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        SkyblockBot.LOGGER.info("Visitor name: " + visitorName);
        this.visitorName = visitorName;
    }
}
