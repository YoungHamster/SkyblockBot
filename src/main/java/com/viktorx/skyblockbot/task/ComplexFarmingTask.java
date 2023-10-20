package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.buyItem.BuyItem;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.Replay;
import com.viktorx.skyblockbot.task.replay.ReplayBotSettings;
import com.viktorx.skyblockbot.task.sellSacks.SellSacks;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ComplexFarmingTask {
    public static final ComplexFarmingTask INSTANCE = new ComplexFarmingTask();

    private final Task getToSkyblock;
    private final Task getToGarden;
    private final Task sellSacks;
    private Task farm;
    private Task currentTask;
    private Timer regularPauseTimer;
    private Timer checkGodPotAndCookieTimer;
    private Timer checkSacks;
    private final List<Runnable> runWhenFarmCompleted = new ArrayList<>();
    private final Queue<Task> taskQueue = new ArrayDeque<>();

    void whenGetToSkyblockCompleted() {
        if (!SBUtils.getIslandOrArea().contains("Plot")) {
            currentTask = getToGarden;
        } else {
            currentTask = farm;
        }
        currentTask.execute();
    }

    void whenGetToSkyblockAborted() {
        SkyblockBot.LOGGER.info("Couldn't warp to skyblock!");
        currentTask = null;
    }

    void whenGetToGardenCompleted() {
        currentTask = farm;
        farm.execute();
    }

    void whenGetToGardenAborted() {
        SkyblockBot.LOGGER.info("Couldn't warp to garden");
        currentTask = null;
    }

    void whenSellSacksCompleted() {
        currentTask = farm;
        currentTask.execute();
    }

    void whenSellSacksAborted() {
        currentTask = farm;
        currentTask.execute();
    }

    void whenFarmCompleted() {
        synchronized (runWhenFarmCompleted) {
            for (Runnable task : runWhenFarmCompleted) {
                task.run();
            }
            runWhenFarmCompleted.clear();
        }

        if(!taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
        }
        currentTask.execute();
    }

    void whenFarmAborted() {
        if(GlobalExecutorInfo.worldLoading) {
            SkyblockBot.LOGGER.info("Warped out of garden. Trying to get back");

            while(!GlobalExecutorInfo.worldLoaded) {
                try {
                    Thread.sleep(ChangeIslandSettings.ticksToWaitForChunks);
                } catch (InterruptedException ignored) {}
            }

            if(SBUtils.isServerSkyblock()) {
                currentTask = getToGarden;
            } else {
                currentTask = getToSkyblock;
            }
            currentTask.execute();
        }
    }

    ComplexFarmingTask() {
        this.getToSkyblock = new ChangeIsland("/play skyblock");
        this.getToSkyblock.whenCompleted(this::whenGetToSkyblockCompleted);
        this.getToSkyblock.whenAborted(this::whenGetToSkyblockAborted);

        this.getToGarden = new ChangeIsland("/warp garden");
        this.getToGarden.whenCompleted(this::whenGetToGardenCompleted);
        this.getToGarden.whenAborted(this::whenGetToGardenAborted);

        this.sellSacks = new SellSacks();
        this.sellSacks.whenCompleted(this::whenSellSacksCompleted);
        this.sellSacks.whenCompleted(this::whenSellSacksAborted);

        this.farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE);
        this.farm.whenCompleted(this::whenFarmCompleted);
        this.farm.whenAborted(this::whenFarmAborted);
    }

    public void execute() {
        if(isExecuting()) {
            SkyblockBot.LOGGER.info("Can't start complexFarmingTask when it is already executing");
            return;
        }

        runWhenFarmCompleted.clear();
        taskQueue.clear();

        /*
         * If server isn't skyblock then we start by going to skyblock
         * If it is skyblock but not garden then we start by going to graden
         * If it is garden we just farm
         */
        if(!SBUtils.isServerSkyblock()) {
            currentTask = getToSkyblock;
        } else if (!SBUtils.getIslandOrArea().contains("Plot")) {
            currentTask = getToGarden;
        } else {
            currentTask = farm;
        }

        currentTask.execute();

        /*
         * Basically every durationInMs we tell the farming bot to pause for 10 minutes when it's done the loop
         * After 10 minutes it tells itself to run again when loop is done and starts executing itself again
         *
         * TLDR: pause for 10 minutes every 2 hours but only at the end of the farming loop
         */
        // 120 minutes
        long durationInMs = 1000 * 60 * 120;
        // 20 minutes
        long pauseDuration = 1000 * 60 * 20;

        regularPauseTimer = new Timer(true);
        regularPauseTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SkyblockBot.LOGGER.info("When the current farm loop is done bot is going to take a break");

                synchronized (runWhenFarmCompleted) {
                    runWhenFarmCompleted.add(() -> {
                        SkyblockBot.LOGGER.info("Bot is taking a break");
                        try {
                            Thread.sleep(pauseDuration);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        SkyblockBot.LOGGER.info("Break is over, bot is farming again");
                    });
                }
            }
        },
                durationInMs, durationInMs);

        long timeBetweenChecks = 1000 * 60 * 5; // 5 minutes

        checkGodPotAndCookieTimer = new Timer(true);
        checkGodPotAndCookieTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // TODO check if god pot has less than some number left and same for cookie buff

                synchronized (runWhenFarmCompleted) {
                    runWhenFarmCompleted.add(() -> {
                        currentTask = new BuyItem("God Potion", new String[0]);
                    });
                }
            }
        },
                0, timeBetweenChecks);

        checkSacks = new Timer(true);
        checkSacks.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              if(GlobalExecutorInfo.totalSackCount.get() > GlobalExecutorInfo.totalSackCountLimit) {
                  synchronized (runWhenFarmCompleted) {
                      runWhenFarmCompleted.add(() -> currentTask = sellSacks);
                  }
              }
            }
        },
                0, timeBetweenChecks);
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
        try {
            regularPauseTimer.cancel();
            regularPauseTimer.purge();
        } catch (IllegalStateException e) {
            SkyblockBot.LOGGER.info("Exception when aborting ComplexFarmingTask. Can't cancel regularPauseTimer because it is already cancelled");
        }
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

    public Task getCurrentTask() {
        return currentTask;
    }
}
