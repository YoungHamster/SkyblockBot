package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.task.craft.Craft;
import com.viktorx.skyblockbot.task.replay.Replay;

public class ComplexTask {
    /*private Replay replay = null;
    private Craft craft = null;
    private Enchant enchant = null;
    private Reforge reforge = null;
    private BuyBZ buyBZ = null;
    private BuyAH buyAH = null;
    private BuyNPC buyNPC = null;
    private ChangeIsland changeIsland = null;*/
    private final Task task;
    private long durationInMs;

    ComplexTask(Task task) {
        this.task = task;
    }
}
