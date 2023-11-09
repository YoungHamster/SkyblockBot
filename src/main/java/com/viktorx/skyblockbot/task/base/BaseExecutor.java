package com.viktorx.skyblockbot.task.base;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.utils.CurrentInventory;
import net.minecraft.client.MinecraftClient;

public abstract class BaseExecutor {

    protected BaseTask<?> task;
    protected ExecutorState state = new Idle();
    protected ExecutorState stateBeforePause;

    public abstract <T extends BaseTask<?>> ExecutorState whenExecute(T task);

    public synchronized <T extends BaseTask<?>> ExecutorState execute(T task) {
        if (!state.getClass().equals(Idle.class)) {
            SkyblockBot.LOGGER.warn("Can't execute " + this.task.getTaskName() + " when already executing");
            return new Idle();
        }

        /*
         * Resetting syncIdChanged in case it changed sometime before this task's execution
         */
        CurrentInventory.syncIDChanged();

        state = whenExecute(task);
        return state;
    }

    public synchronized void pause() {
        if (state.getClass().equals(Idle.class) || state.getClass().equals(Paused.class)) {
            SkyblockBot.LOGGER.warn("Can't pause " + this.task.getTaskName() + " when idle or already paused");
            return;
        }

        stateBeforePause = state;
        state = new Paused();
    }

    public synchronized void resume() {
        if (!state.getClass().equals(Paused.class)) {
            SkyblockBot.LOGGER.warn("Can't resume " + this.task.getTaskName() + " when not paused");
            return;
        }

        state = stateBeforePause;
    }

    public synchronized void abort() {
        state = new Idle();
    }

    public <T extends BaseTask<?>> boolean isExecuting(T task) {
        return !state.getClass().equals(Idle.class) && this.task == task;
    }

    public boolean isPaused() {
        return state.getClass().equals(Paused.class);
    }

    public static class Idle implements ExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            return this;
        }
    }

    public static class Paused implements ExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            return this;
        }
    }
}
