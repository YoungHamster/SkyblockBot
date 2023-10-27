package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.Utils;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.buySellTask.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.buySellTask.buyItem.BuyItem;
import com.viktorx.skyblockbot.task.buySellTask.sellSacks.SellSacks;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIsland;
import com.viktorx.skyblockbot.task.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.replay.Replay;
import com.viktorx.skyblockbot.task.replay.ReplayBotSettings;
import com.viktorx.skyblockbot.task.useItem.UseItem;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class ComplexFarmingTask extends Task {
    public static final ComplexFarmingTask INSTANCE = new ComplexFarmingTask();

    private final Task getOutOfLimbo;
    private final Task getToSkyblock;
    private final Task getToGarden;
    private final Task sellSacks;
    private final Task buyItem;
    private final Task buyBZItem;
    private final Task useItem;
    private final Task gardenGuestsTask;
    private final List<Runnable> runWhenFarmCompleted = new ArrayList<>();
    private final Queue<Task> taskQueue = new ArrayBlockingQueue<>(20);
    private Task farm;
    private Task currentTask;
    private List<Timer> timers = new ArrayList<>();

    ComplexFarmingTask() {
        this.getOutOfLimbo = new ChangeIsland("/lobby");
        this.getOutOfLimbo.whenCompleted(this::whenGetOutOfLimboCompleted);
        this.getOutOfLimbo.whenAborted(this::whenGetOutOfLimboAborted);

        this.getToSkyblock = new ChangeIsland("/play skyblock");
        this.getToSkyblock.whenCompleted(this::whenGetToSkyblockCompleted);
        this.getToSkyblock.whenAborted(this::whenGetToSkyblockAborted);

        this.getToGarden = new ChangeIsland("/warp garden");
        this.getToGarden.whenCompleted(this::defaultWhenCompleted);
        this.getToGarden.whenAborted(this::whenGetToGardenAborted);

        this.sellSacks = new SellSacks();
        this.sellSacks.whenCompleted(this::defaultWhenCompleted);
        this.sellSacks.whenAborted(this::defaultWhenAborted);

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

        this.gardenGuestsTask = new ComplexGardenGuestsTask();
    }

    void whenGetOutOfLimboCompleted() {
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + getCurrentTaskName());
        currentTask = getToSkyblock;
        currentTask.execute();
    }

    void whenGetOutOfLimboAborted() {
        TGBotDaemon.INSTANCE.queueMessage("Failed task: " + getCurrentTaskName());
        SkyblockBot.LOGGER.info("Failed to get out of limbo, waiting 10 minutes to retry");
        Timer retryGetOutOfLimbo = new Timer(true);
        retryGetOutOfLimbo.schedule(new TimerTask() {
            @Override
            public void run() {
                currentTask.execute();
            }
        }, ComplexFarmingTaskSettings.retryGetOutOfLimboDelay);
    }

    void whenGetToSkyblockCompleted() {
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + getCurrentTaskName());

        if (!SBUtils.getIslandOrArea().contains(ComplexFarmingTaskSettings.gardenName)) {
            currentTask = getToGarden;
        } else {
            defaultWhenCompleted();
            return;
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
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + getCurrentTaskName());

        if (!taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
        } else {
            currentTask = farm;
        }
        currentTask.execute();
    }

    void defaultWhenAborted() {
        SkyblockBot.LOGGER.warn(getCurrentTaskName() + " task aborted.");
        currentTask = farm;
        currentTask.execute();
    }

    void whenFarmCompleted() {
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
                if(Utils.isStringInRecentChat("You were spawned in limbo", 3)
                   || Utils.isStringInRecentChat("Вы АФК", 3)) {
                    currentTask = getOutOfLimbo;
                } else {
                    currentTask = getToSkyblock;
                }
            }
            currentTask.execute();
        }
    }

    private void debugExecute() {
        currentTask = farm;
        currentTask.execute();
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't start complexFarmingTask when it is already executing");
            return;
        }

        SkyblockBot.LOGGER.info("Debuge mode: " + GlobalExecutorInfo.debugMode.get());
        if(GlobalExecutorInfo.debugMode.get()) {
            debugExecute();
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
         * TLDR: pause for 10 minutes every hour but only at the end of the farming loop
         */
        Timer regularPauseTimer = new Timer(true);
        regularPauseTimer.scheduleAtFixedRate(new RegularPauseTimerTask(),
                ComplexFarmingTaskSettings.pauseInterval, ComplexFarmingTaskSettings.pauseInterval);
        timers.add(regularPauseTimer);

        /*
         * Checks how much time is left of booster cookie and god potion, queues to buy and use them if necessary
         */
        Timer checkGodPotAndCookieTimer = new Timer(true);
        checkGodPotAndCookieTimer.scheduleAtFixedRate(new CheckGodPotAndCookieTimerTask(),
                0, ComplexFarmingTaskSettings.intervalBetweenRegularChecks);
        timers.add(checkGodPotAndCookieTimer);

        /*
         * Checks if sacks have lots of stuff and it's time to sell them, queues to sell them
         */
        Timer checkSacksTimer = new Timer(true);
        checkSacksTimer.scheduleAtFixedRate(new CheckSacksTimerTask(),
                0, ComplexFarmingTaskSettings.intervalBetweenRegularChecks);
        timers.add(checkSacksTimer);

        Timer checkGuestsTimer = new Timer(true);
        checkGuestsTimer.scheduleAtFixedRate(new CheckGuestsTimerTask(),
                0, ComplexFarmingTaskSettings.checkGuestsInterval);
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

        for(Timer timer : timers) {
            timer.cancel();
            timer.purge();
        }
        timers.clear();
    }

    public boolean isExecuting() {
        return currentTask != null;
    }

    public boolean isPaused() {
        return currentTask.isPaused();
    }

    public void loadRecordingAsync() {
        if (isExecuting()) {
            synchronized (runWhenFarmCompleted) {
                runWhenFarmCompleted.add(() -> farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE));
            }
        } else {
            CompletableFuture.runAsync(() -> farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE));
        }
    }

    public String getCurrentTaskName() {
        if (currentTask == null) {
            return "null. No task is currently executing";
        }
        String taskName = currentTask.getClass().getName();
        String[] foo = taskName.split("\\.");
        taskName = foo[foo.length - 1];
        return taskName;
    }

    private class RegularPauseTimerTask extends TimerTask {
        @Override
        public void run() {
            SkyblockBot.LOGGER.info("When the current farm loop is done bot is going to take a break");

            synchronized (runWhenFarmCompleted) {
                runWhenFarmCompleted.add(() -> {
                    SkyblockBot.LOGGER.info("Bot is taking a break for " + ComplexFarmingTaskSettings.pauseDuration / 60000 + "minutes");
                    TGBotDaemon.INSTANCE.queueMessage("Bot is taking a break");
                    try {
                        Thread.sleep(ComplexFarmingTaskSettings.pauseDuration);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    SkyblockBot.LOGGER.info("Break is over, bot is farming again");
                    TGBotDaemon.INSTANCE.queueMessage("Break is over, bot is farming again");
                });
            }
        }
    }

    private class CheckGodPotAndCookieTimerTask extends TimerTask {
        @Override
        public void run() {
            if (!SBUtils.isServerSkyblock()) {
                SkyblockBot.LOGGER.info("Server isn't skyblock. Skipping god pot and cookie check");
                return;
            }

            if (currentTask == null) {
                SkyblockBot.LOGGER.info("Current task is null, can't check god pot and cookie");
            }

            SkyblockBot.LOGGER.info("Time left god pot: " + SBUtils.getTimeLeftGodPot()
                    + "\n Time left cookie: " + SBUtils.getTimeLeftCookieBuff());
            if (SBUtils.getTimeLeftGodPot() < ComplexFarmingTaskSettings.godPotBuyThreshold) {
                if (!taskQueue.contains(useItem) &&
                        !currentTask.getClass().equals(buyItem.getClass()) &&
                        !currentTask.getClass().equals(useItem.getClass())) {

                    SkyblockBot.LOGGER.info("Queueing to use god pot. Minutes left: " + SBUtils.getTimeLeftGodPot() / (1000 * 60));

                    if (!SBUtils.isItemInInventory(ItemNames.GOD_POT.getName())) {
                        ((BuyItem) buyItem).setItemInfo(ItemNames.GOD_POT.getName(), new String[0]);
                        taskQueue.add(buyItem);
                    }
                    ((UseItem) useItem).setItemName(ItemNames.GOD_POT.getName());
                    taskQueue.add(useItem);
                }
            }

            if (SBUtils.getTimeLeftCookieBuff() < ComplexFarmingTaskSettings.cookieBuyThreshold) {
                if (!taskQueue.contains(useItem) &&
                        !currentTask.getClass().equals(buyBZItem.getClass()) &&
                        !currentTask.getClass().equals(useItem.getClass())) {

                    SkyblockBot.LOGGER.info("Queueing to use cookie. Minutes left: " + SBUtils.getTimeLeftCookieBuff() / (1000 * 60));

                    if (!SBUtils.isItemInInventory(ItemNames.BOOSTER_COOKIE.getName())) {
                        ((BuyBZItem) buyBZItem).setItemName(ItemNames.BOOSTER_COOKIE.getName());
                        taskQueue.add(buyBZItem);
                    }
                    ((UseItem) useItem).setItemName(ItemNames.BOOSTER_COOKIE.getName());
                    taskQueue.add(useItem);
                }
            }
        }
    }

    private class CheckSacksTimerTask extends TimerTask {
        @Override
        public void run() {
            if (GlobalExecutorInfo.carrotCount.get() / 160 > GlobalExecutorInfo.totalSackCountLimit) {
                if (!taskQueue.contains(sellSacks)) {
                    SkyblockBot.LOGGER.info("Queueing to sell sacks");
                    taskQueue.add(sellSacks);
                }
            }
        }
    }

    private class CheckGuestsTimerTask extends TimerTask {
        @Override
        public void run() {
            if(SBUtils.getGardenGuestCount() > 3) {
                if (!taskQueue.contains(gardenGuestsTask)) {
                    SkyblockBot.LOGGER.info("Queueing to handle garden guests");
                    taskQueue.add(gardenGuestsTask);
                }
            }
        }
    }
}
