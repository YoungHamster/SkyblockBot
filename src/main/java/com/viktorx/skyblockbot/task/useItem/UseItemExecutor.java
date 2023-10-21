package com.viktorx.skyblockbot.task.useItem;

public class UseItemExecutor {

    public static UseItemExecutor INSTANCE = new UseItemExecutor();

    private UseItem task;
    private UseItemState state = UseItemState.IDLE;

    public boolean isExecuting(UseItem task) {
        return !state.equals(UseItemState.IDLE) && this.task.equals(task);
    }

    public boolean isPaused() {
        return state.equals(UseItemState.PAUSED);
    }
}
