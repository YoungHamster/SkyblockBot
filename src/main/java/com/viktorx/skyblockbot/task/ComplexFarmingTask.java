package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.ScreenshotDaemon;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.buySellTask.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.buySellTask.buyItem.BuyItem;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.Replay;
import com.viktorx.skyblockbot.task.replay.ReplayBotSettings;
import com.viktorx.skyblockbot.task.buySellTask.sellSacks.SellSacks;
import com.viktorx.skyblockbot.task.useItem.UseItem;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class ComplexFarmingTask {
    public static final ComplexFarmingTask INSTANCE = new ComplexFarmingTask();

    private final Task getToSkyblock;
    private final Task getToGarden;
    private final Task sellSacks;
    private Task farm;
    private final Task buyItem;
    private final Task buyBZItem;
    private final Task useItem;
    private Task currentTask;
    private Timer regularPauseTimer;
    private Timer checkGodPotAndCookieTimer;
    private Timer checkSacksTimer;
    private final List<Runnable> runWhenFarmCompleted = new ArrayList<>();
    private final Queue<Task> taskQueue = new ArrayBlockingQueue<>(20);

    void whenGetToSkyblockCompleted() {
        ScreenshotDaemon.INSTANCE.queueMessage("Completed task: " + getCurrentTaskName());

        if (!SBUtils.getIslandOrArea().contains(ComplexFarmingTaskSettings.gardenName)) {
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

    void whenGetToGardenAborted() {
        SkyblockBot.LOGGER.info("Couldn't warp to garden");
        currentTask = null;
    }

    void defaultWhenCompleted() {
        ScreenshotDaemon.INSTANCE.queueMessage("Completed task: " + getCurrentTaskName());

        if (!taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
        } else {
            currentTask = farm;
        }
        currentTask.execute();
    }

    void defaultWhenAborted() {
        if (canFarm()) {
            SkyblockBot.LOGGER.warn(getCurrentTaskName() + " task aborted.");
            currentTask = farm;
            currentTask.execute();
        }
    }

    void whenFarmCompleted() {
        ScreenshotDaemon.INSTANCE.queueMessage("Completed task: " + getCurrentTaskName());

        synchronized (runWhenFarmCompleted) {
            for (Runnable notTask : runWhenFarmCompleted) {
                notTask.run();
            }
            runWhenFarmCompleted.clear();
        }

        defaultWhenCompleted();
    }

    void whenFarmAborted() {
        if (GlobalExecutorInfo.worldLoading.get()) {
            SkyblockBot.LOGGER.info("Warped out of garden. Trying to get back");

            while (!GlobalExecutorInfo.worldLoaded.get()) {
                try {
                    Thread.sleep(ChangeIslandSettings.ticksToWaitForChunks);
                } catch (InterruptedException ignored) {
                }
            }

            if (SBUtils.isServerSkyblock()) {
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
        this.getToGarden.whenCompleted(this::defaultWhenCompleted);
        this.getToGarden.whenAborted(this::whenGetToGardenAborted);

        this.sellSacks = new SellSacks();
        this.sellSacks.whenCompleted(this::defaultWhenCompleted);
        this.sellSacks.whenCompleted(this::defaultWhenAborted);

        this.farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE);
        this.farm.whenCompleted(this::whenFarmCompleted);
        this.farm.whenAborted(this::whenFarmAborted);

        this.buyItem = new BuyItem();
        this.buyItem.whenCompleted(this::defaultWhenCompleted);
        this.buyItem.whenAborted(this::defaultWhenAborted);

        this.buyBZItem = new BuyBZItem();
        this.buyBZItem.whenCompleted(this::defaultWhenCompleted);
        this.buyBZItem.whenAborted(this::defaultWhenAborted);

        this.useItem = new UseItem();
        this.useItem.whenCompleted(this::defaultWhenCompleted);
        this.useItem.whenAborted(this::defaultWhenAborted);
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't start complexFarmingTask when it is already executing");
            return;
        } else {
            ((UseItem) useItem).setItemName("Bread");
            currentTask = useItem;
            currentTask.execute();
        }

        if(useItem.isExecuting()) {
            return;
        }

        runWhenFarmCompleted.clear();
        taskQueue.clear();

        /*
         * If server isn't skyblock then we start by going to skyblock
         * If it is skyblock but not garden then we start by going to graden
         * If it is garden we just farm
         */
        if (!SBUtils.isServerSkyblock()) {
            currentTask = getToSkyblock;
        } else if (!SBUtils.getIslandOrArea().contains(ComplexFarmingTaskSettings.gardenName)) {
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

        regularPauseTimer = new Timer(true);
        regularPauseTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              SkyblockBot.LOGGER.info("When the current farm loop is done bot is going to take a break");

              synchronized (runWhenFarmCompleted) {
                  runWhenFarmCompleted.add(() -> {
                      SkyblockBot.LOGGER.info("Bot is taking a break");
                      try {
                          Thread.sleep(ComplexFarmingTaskSettings.pauseDuration);
                      } catch (InterruptedException e) {
                          throw new RuntimeException(e);
                      }
                      SkyblockBot.LOGGER.info("Break is over, bot is farming again");
                  });
              }
            }
        },
                ComplexFarmingTaskSettings.pauseInterval, ComplexFarmingTaskSettings.pauseInterval);

        /*
         * Checks how much time is left of booster cookie and god potion, queues to buy and use them if necessary
         */

        checkGodPotAndCookieTimer = new Timer(true);
        checkGodPotAndCookieTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!SBUtils.isServerSkyblock()) {
                    SkyblockBot.LOGGER.info("Server isn't skyblock. Skipping god pot and cookie check");
                    return;
                }

                if(SBUtils.getTimeLeftGodPot() < ComplexFarmingTaskSettings.godPotBuyThreshold) {
                    if(!taskQueue.contains(buyItem)) {
                        SkyblockBot.LOGGER.info("Queueing to buy god pot. Minutes left: " + SBUtils.getTimeLeftGodPot() / (1000 * 60));

                        ((BuyItem) buyItem).setItemInfo(ItemNames.GOD_POT.getName(), new String[0]);
                        taskQueue.add(buyItem);
                        ((UseItem) useItem).setItemName(ItemNames.GOD_POT.getName());
                        taskQueue.add(useItem);
                    }
                }

                if(SBUtils.getTimeLeftCookieBuff() < ComplexFarmingTaskSettings.cookieBuyThreshold) {
                    SkyblockBot.LOGGER.info("Queueing to buy cookie. Minutes left: " + SBUtils.getTimeLeftCookieBuff() / (1000 * 60));

                    ((BuyBZItem) buyBZItem).setItemName(ItemNames.BOOSTER_COOKIE.getName());
                    taskQueue.add(buyBZItem);
                    ((UseItem) useItem).setItemName(ItemNames.BOOSTER_COOKIE.getName());
                    taskQueue.add(useItem);
                }
            }
        },
                0, ComplexFarmingTaskSettings.intervalBetweenRegularChecks);

        /*
         * Checks if sacks have lots of stuff and it's time to sell them, queues to sell them
         */
        checkSacksTimer = new Timer(true);
        checkSacksTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (GlobalExecutorInfo.carrotCount.get() / 160 > GlobalExecutorInfo.totalSackCountLimit) {
                    if(!taskQueue.contains(sellSacks)) {
                        taskQueue.add(sellSacks);
                    }
                }
            }
        },
                0, ComplexFarmingTaskSettings.intervalBetweenRegularChecks);
    }

    private boolean canFarm() {
        return SBUtils.getIslandOrArea().contains(ComplexFarmingTaskSettings.gardenName)
                && SBUtils.getTimeLeftCookieBuff() > ComplexFarmingTaskSettings.cookieBuyThreshold / 2
                && SBUtils.getTimeLeftGodPot() > ComplexFarmingTaskSettings.godPotBuyThreshold / 2;
    }

    public void pause() {
        if (currentTask.isExecuting()) {
            currentTask.pause();
        }
    }

    public void resume() {
        if (currentTask.isExecuting()) {
            currentTask.resume();
        }
    }

    public void abort() {
        if (currentTask.isExecuting()) {
            currentTask.abort();
        }
        currentTask = null;
        try {
            if (regularPauseTimer != null) {
                regularPauseTimer.cancel();
                regularPauseTimer.purge();
            }

            if (checkGodPotAndCookieTimer != null) {
                checkGodPotAndCookieTimer.cancel();
                checkGodPotAndCookieTimer.purge();
            }

            if (checkSacksTimer != null) {
                checkSacksTimer.cancel();
                checkSacksTimer.purge();
            }
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
        if(isExecuting()) {
            synchronized (runWhenFarmCompleted) {
                runWhenFarmCompleted.add(() -> farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE));
            }
        } else {
            CompletableFuture.runAsync(() -> farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE));
        }
    }

    public String getCurrentTaskName() {
        if(currentTask == null) {
            return "null. No task is currently executing";
        }
        String taskName = currentTask.getClass().getName();
        String[] foo = taskName.split("\\.");
        taskName = foo[foo.length - 1];
        return taskName;
    }
}
