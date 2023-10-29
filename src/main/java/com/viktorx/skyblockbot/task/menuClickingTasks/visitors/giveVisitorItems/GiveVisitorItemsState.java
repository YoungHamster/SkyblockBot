package com.viktorx.skyblockbot.task.menuClickingTasks.visitors.giveVisitorItems;

public enum GiveVisitorItemsState {
    IDLE,
    CLICKING_ON_VISITOR,
    WAITING_FOR_MENU,
    ACCEPTING_OFFER,
    WAITING_TILL_NPC_LEAVES,
    PAUSED;


    GiveVisitorItemsState() {}
}
