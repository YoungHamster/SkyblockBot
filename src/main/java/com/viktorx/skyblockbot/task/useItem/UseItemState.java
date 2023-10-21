package com.viktorx.skyblockbot.task.useItem;

public enum UseItemState {
    IDLE,
    SENDING_COMMAND,
    WAITING_FOR_MENU,
    SELLING,
    CONFIRMING,
    WAITING_BEFORE_CLOSING_MENU,
    PAUSED;


    UseItemState() {}
}
