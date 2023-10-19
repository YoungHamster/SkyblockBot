package com.viktorx.skyblockbot.task.buyItem;

public enum BuyItemState {
    IDLE,
    SENDING_COMMAND,
    WAITING_FOR_MENU,
    BUYING,
    CONFIRMING_BUY,
    PAUSED;


    BuyItemState() {

    }
}
