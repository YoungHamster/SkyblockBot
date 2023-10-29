package com.viktorx.skyblockbot.task.replay;

public enum ReplayBotState {
    IDLE("idle"),
    NOT_IDLE("not_idle"),
    RECORDING("recording"),
    PLAYING("playing"),
    PAUSED("paused"),
    ANTI_DETECT_TRIGGERED("anti_detect_triggered"),
    PREPARING_TO_START("adjusting_head_before_starting"),
    UNPRESSING_BUTTONS_BEFORE_START("unpressing_buttons_before_start");

    private final String name;

    ReplayBotState(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
