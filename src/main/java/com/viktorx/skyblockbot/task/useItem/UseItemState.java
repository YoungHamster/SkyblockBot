package com.viktorx.skyblockbot.task.useItem;

public enum UseItemState {
    IDLE,
    CHECKING_INVENTORY,
    GOING_TO_HOTBAR_SLOT,
    USING_ITEM,
    GOING_BACK_TO_HOTBAR_SLOT,
    OPENING_INVENTORY,
    MOVING_ITEM_TO_CORRECT_SLOT,
    CLOSING_INVENTORY,
    PAUSED;


    UseItemState() {}
}
