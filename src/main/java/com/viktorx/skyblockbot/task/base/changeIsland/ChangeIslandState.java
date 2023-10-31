package com.viktorx.skyblockbot.task.base.changeIsland;

public enum ChangeIslandState {
    IDLE,
    SENDING_COMMAND,
    WAITING_AFTER_COMMAND,
    WAITING_FOR_WORLD_LOAD,
    PAUSED;


    ChangeIslandState() {}
}
