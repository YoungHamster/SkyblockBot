package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem;

public enum BuyBZItemState {
    IDLE,
    SENDING_COMMAND,
    CLICKING_TO_SEARCH,
    SEARCHING,
    CLICKING_ON_ITEM,
    CLICKING_BUY_INSTANTLY,
    CLICKING_ENTER_AMOUNT,
    ENTERING_AMOUNT,
    BUYING_ONE,
    BUYING_CUSTOM_AMOUNT,
    WAITING_FOR_MENU,
    WAITING_FOR_SCREEN_CHANGE,
    RESTARTING,
    PAUSED,
    COMPLETED;


    BuyBZItemState() {}
}
