package com.viktorx.skyblockbot.task.base.menuClickingTasks.assembleCraft;

public enum AssembleCraftState {
    IDLE,
    CHECKING_INGRIDIENTS,
    OPENING_SB_MENU,
    OPENING_CRAFTING_TABLE,
    PUTTING_ITEMS,
    COLLECTING_CRAFT,
    CLOSING_INVENTORY,
    WAITING_FOR_MENU,
    RESTARTING,
    ABORTED,
    PAUSED;


    AssembleCraftState() {}
}
