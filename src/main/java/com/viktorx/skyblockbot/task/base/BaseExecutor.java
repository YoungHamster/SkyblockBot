package com.viktorx.skyblockbot.task.base;

import com.viktorx.skyblockbot.SkyblockBot;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseExecutor {

    protected final Map<String, Integer> states = new HashMap<>();
    protected BaseTask<?> task;
    protected int state;
    protected int stateBeforePause;

    public BaseExecutor() {
        states.put("IDLE", 1);
        states.put("PAUSED", 2);

        state = states.get("IDLE");
    }

    public abstract <T extends BaseTask<?>> void whenExecute(T task);

    public <T extends BaseTask<?>> void execute(T task) {
        if (state != states.get("IDLE")) {
            SkyblockBot.LOGGER.warn("Can't execute " + this.task.getClassName() + " when already executing");
            return;
        }

        whenExecute(task);
    }

    public void pause() {
        if (state == states.get("IDLE") || state == states.get("PAUSED")) {
            SkyblockBot.LOGGER.warn("Can't pause " + this.task.getClassName() + " when idle or already paused");
            return;
        }

        stateBeforePause = state;
        state = states.get("PAUSED");
    }

    public void resume() {
        if (state != states.get("PAUSED")) {
            SkyblockBot.LOGGER.warn("Can't resume " + this.task.getClassName() + " when not paused");
            return;
        }

        state = stateBeforePause;
    }

    public void abort() {
        state = states.get("IDLE");
    }

    public <T extends BaseTask<?>> boolean isExecuting(T task) {
        return state != states.get("IDLE") && this.task == task;
    }

    public boolean isPaused() {
        return state == states.get("PAUSED");
    }
}
