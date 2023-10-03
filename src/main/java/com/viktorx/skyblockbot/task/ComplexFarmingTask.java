package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.Replay;

public class ComplexFarmingTask {
    /*private Replay replay = null;
    private Craft craft = null;
    private Enchant enchant = null;
    private Reforge reforge = null;
    private BuyBZ buyBZ = null;
    private BuyAH buyAH = null;
    private BuyNPC buyNPC = null;
    private ChangeIsland changeIsland = null;*/
    public static final ComplexFarmingTask INSTANCE = new ComplexFarmingTask();

    private final Task getToSkyblock;
    private final Task getToCorrectIsland;
    private final Task farm;
    private long durationInMs;
    private boolean running = false;

    ComplexFarmingTask() {
        this.getToSkyblock = new ChangeIsland("/play skyblock");
        this.getToCorrectIsland = new ChangeIsland("/warp garden");
        this.farm = new Replay();

        this.getToSkyblock.whenCompleted(getToCorrectIsland::execute);
        this.getToSkyblock.whenAborted(() -> SkyblockBot.LOGGER.info("Couldn't warp to skyblock!"));
        this.getToCorrectIsland.whenCompleted(farm::execute);
        this.getToCorrectIsland.whenAborted(() -> SkyblockBot.LOGGER.info("Couldn't warp to garden"));
        this.farm.whenCompleted(farm::execute);
        this.farm.whenAborted(() -> {
            if(GlobalExecutorInfo.worldChangeDetected) {
                try {
                    Thread.sleep(ChangeIslandSettings.ticksToWaitForChunks * 50);
                } catch (InterruptedException ignored) {}
                GlobalExecutorInfo.worldChangeDetected = false;
                getToCorrectIsland.execute();
            }
        });
    }

    public void execute() {
        running = true;
        getToSkyblock.execute();
    }

    public boolean isRunning() {
        return running;
    }
}
