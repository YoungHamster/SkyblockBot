package com.viktorx.skyblockbot.task.base.pestKiller;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class PestKiller extends BaseTask<PestKillerExecutor> {
    private final String pestName;

    public PestKiller(String pestName, Runnable whenCompleted, Runnable whenAborted) {
        super(PestKillerExecutor.INSTANCE, whenCompleted, whenAborted);
        this.pestName = pestName;
    }

    public String getPestName() {
        return pestName;
    }
}
