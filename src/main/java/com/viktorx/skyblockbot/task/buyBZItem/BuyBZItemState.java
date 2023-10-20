package com.viktorx.skyblockbot.task.buyBZItem;

public enum BuyBZItemState {
    IDLE,
    SENDING_COMMAND,
    WAITING_FOR_MENU,
    SELLING,
    CONFIRMING,
    WAITING_BEFORE_CLOSING_MENU,
    PAUSED;


    BuyBZItemState() {}
}
