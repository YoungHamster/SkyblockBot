package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.task.Task;

public class CraftTask extends Task {
    private Task currentTask;
    private final Task buyBZItem;
    private final Task buyItem;
    private final Task assembleCraft;
}
