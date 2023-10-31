package com.viktorx.skyblockbot.task.base.menuClickingTasks.sellSacks;

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
