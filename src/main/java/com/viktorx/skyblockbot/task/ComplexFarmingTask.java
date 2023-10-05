package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.Replay;
import com.viktorx.skyblockbot.task.replay.ReplayBotSettings;

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
    private boolean executing = false;

    ComplexFarmingTask() {
        this.getToSkyblock = new ChangeIsland("/play skyblock");
        this.getToCorrectIsland = new ChangeIsland("/warp garden");
        this.farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE);

        this.getToSkyblock.whenCompleted(getToCorrectIsland::execute);
        this.getToSkyblock.whenAborted(() -> {
            SkyblockBot.LOGGER.info("Couldn't warp to skyblock!");
            executing = false;
        });

        this.getToCorrectIsland.whenCompleted(farm::execute);
        this.getToCorrectIsland.whenAborted(() -> {
            SkyblockBot.LOGGER.info("Couldn't warp to garden");
            executing = false;
        });

        this.farm.whenCompleted(farm::execute);
        this.farm.whenAborted(() -> {
            if(GlobalExecutorInfo.worldLoading) {

                try {
                    Thread.sleep(ChangeIslandSettings.ticksToWaitForChunks * 50);
                } catch (InterruptedException ignored) {}
                if(GlobalExecutorInfo.worldLoaded) {
                    getToCorrectIsland.execute();
                } else {
                    SkyblockBot.LOGGER.info("Couldn't farm.");
                    executing = false;
                }
            }
        });
    }

    public void execute() {
        executing = true;
        getToSkyblock.execute();
    }

    public boolean isExecuting() {
        return executing;
    }
}
