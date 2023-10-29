package com.viktorx.skyblockbot.task.menuClickingTasks.visitors.talkToVisitor;

public enum TalkToVisitorState {
    IDLE,
    WAITING_FOR_VISITOR,
    CLICKING_ON_VISITOR,
    CLICKING_ON_VISITOR_SECOND_TIME,
    WAITING_FOR_MENU,
    READING_DATA,
    CLOSING_VISITOR,
    PAUSED;


    TalkToVisitorState() {}
}
