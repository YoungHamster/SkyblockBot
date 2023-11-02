package com.viktorx.skyblockbot.task.base;

import com.viktorx.skyblockbot.SkyblockBot;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseExecutor {

    private final Map<String, Integer> states = new HashMap<>();
    private final Map<Integer, String> reverseStates = new HashMap<>();
    protected BaseTask<?> task;
    protected int state;
    protected int stateBeforePause;

    public BaseExecutor() {
        addState("IDLE");
        addState("PAUSED");

        state = states.get("IDLE");
    }

    protected void addState(String name) {
        states.put(name, states.size());
        reverseStates.put(reverseStates.size(), name);
    }

    protected int getState(String name) {
        return states.get(name);
    }

    protected String getState(Integer i) {
        return reverseStates.get(i);
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
