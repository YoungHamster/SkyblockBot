package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.Replay;
import com.viktorx.skyblockbot.task.replay.ReplayBotSettings;

import java.util.concurrent.CompletableFuture;

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
    private Task farm;
    private Task currentTask;
    private long durationInMs;

    ComplexFarmingTask() {
        this.getToSkyblock = new ChangeIsland("/play skyblock");
        this.getToCorrectIsland = new ChangeIsland("/warp garden");
        this.farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE);

        this.getToSkyblock.whenCompleted(() -> {
            currentTask = getToCorrectIsland;
            getToCorrectIsland.execute();
        });
        this.getToSkyblock.whenAborted(() -> {
            SkyblockBot.LOGGER.info("Couldn't warp to skyblock!");
            currentTask = null;
        });

        this.getToCorrectIsland.whenCompleted(() -> {
            currentTask = farm;
            farm.execute();
        });
        this.getToCorrectIsland.whenAborted(() -> {
            SkyblockBot.LOGGER.info("Couldn't warp to garden");
            currentTask = null;
        });

        this.farm.whenCompleted(() -> {
            currentTask = farm;
            farm.execute();
        });
        this.farm.whenAborted(() -> {
            if(GlobalExecutorInfo.worldLoading) {

                try {
                    Thread.sleep(ChangeIslandSettings.ticksToWaitForChunks * 50);
                } catch (InterruptedException ignored) {}
                if(GlobalExecutorInfo.worldLoaded) {
                    currentTask = getToCorrectIsland;
                    getToCorrectIsland.execute();
                } else {
                    SkyblockBot.LOGGER.info("Couldn't farm.");
                    currentTask = null;
                }
            }
        });
    }

    public void execute() {

        /*
         * If server isn't skyblock then we start by going to skyblock
         * If it is skyblock but not garden then we start by going to graden
         * If it is garden we just farm
         */
        /*if(!SBUtils.isServerSkyblock()) {
            currentTask = getToSkyblock;
            getToSkyblock.execute();
        } else if (!SBUtils.getIslandOrArea().equals("GARDEN")) {
            currentTask = getToCorrectIsland;
        } else if (!SBUtils.getIslandOrArea().contains("Plot")) {
            getToCorrectIsland.execute();
        } else {
            currentTask = farm;
            farm.execute();
        }*/
        currentTask = farm;
        currentTask.execute();
    }

    public void pause() {
        if(currentTask.isExecuting()) {
            currentTask.pause();
        }
    }

    public void resume() {
        if(currentTask.isExecuting()) {
            currentTask.resume();
        }
    }

    public void abort() {
        if(currentTask.isExecuting()) {
            currentTask.abort();
        }
        currentTask = null;
    }

    public boolean isExecuting() {
        return currentTask != null;
    }

    public boolean isPaused() {
        return currentTask.isPaused();
    }

    public void loadRecordingAsync() {
        CompletableFuture.runAsync(() -> {
            farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE);
        });
    }
}
