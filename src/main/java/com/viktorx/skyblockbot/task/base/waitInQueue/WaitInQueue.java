package com.viktorx.skyblockbot.task.base.waitInQueue;

import com.viktorx.skyblockbot.task.base.BaseTask;

public class WaitInQueue extends BaseTask<WaitInQueueExecutor> {
    public WaitInQueue() {
        super(WaitInQueueExecutor.INSTANCE);
    }
}
