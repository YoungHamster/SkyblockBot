package com.viktorx.skyblockbot.task.base;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
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

        this.task = task;
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
        if (state.getClass().equals(Idle.class)) {
            SkyblockBot.LOGGER.warn("Can't abort task when it's already idle!");
        }

        state = new Idle();
        task.aborted();
    }

    public <T extends BaseTask<?>> boolean isExecuting(T task) {
        if (this.task == null) {
            return false;
        }
        return !state.getClass().equals(Idle.class) && this.task.equals(task);
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

    public static abstract class WaitingExecutorState implements ExecutorState {
        protected int waitTickCounter = 0;
        protected int waitTicksBeforeAction = GlobalExecutorInfo.waitTicksBeforeAction;

        protected void setWaitTickLimit(int waitTicks) {
            waitTicksBeforeAction = waitTicks;
        }

        protected boolean waitBeforeAction() {
            if (waitTickCounter++ < waitTicksBeforeAction) {
                return true;
            }
            waitTickCounter = 0;
            return false;
        }
    }
}
