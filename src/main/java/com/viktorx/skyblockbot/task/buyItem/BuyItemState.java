package com.viktorx.skyblockbot.task.buyItem;

public enum BuyItemState {
    IDLE("idle"),
    SENDING_COMMAND("not_idle"),
    WAITING_FOR_MENU("recording"),
    BUYING("playing"),
    CONFIRMING_BUY("anti_detect_triggered"),
    PAUSED("paused");

    private final String name;

    BuyItemState(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
