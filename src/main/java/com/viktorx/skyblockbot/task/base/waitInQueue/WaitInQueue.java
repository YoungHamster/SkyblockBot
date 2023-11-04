package com.viktorx.skyblockbot.task.base.waitInQueue;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class WaitInQueue extends BaseTask<WaitInQueueExecutor> {
    public WaitInQueue(Runnable whenCompleted, Runnable whenAborted) {
        super(WaitInQueueExecutor.INSTANCE, whenCompleted, whenAborted);
    }
}
