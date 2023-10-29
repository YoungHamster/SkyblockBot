package com.viktorx.skyblockbot.task.menuClickingTasks.sellSacks;

public enum SellSacksState {
    IDLE,
    SENDING_COMMAND,
    WAITING_FOR_MENU,
    SELLING,
    CONFIRMING,
    WAITING_BEFORE_CLOSING_MENU,
    PAUSED;


    SellSacksState() {}
}
