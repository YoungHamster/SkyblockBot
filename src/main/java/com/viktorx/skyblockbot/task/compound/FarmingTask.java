package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.changeIsland.ChangeIslandSettings;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem.BuyItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.sellSacks.SellSacks;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.useItem.UseItem;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import com.viktorx.skyblockbot.task.base.replay.ReplayBotSettings;
import com.viktorx.skyblockbot.tgBot.TGBotDaemon;
import com.viktorx.skyblockbot.utils.Utils;
import net.minecraft.client.MinecraftClient;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class FarmingTask extends CompoundTask {
    public static final FarmingTask INSTANCE = new FarmingTask(null, null);
    private final Task getToSkyblock;
    private final Task gardenVisitorsTask;
    private final Task farm;
    private final List<Runnable> runWhenFarmCompleted = new ArrayList<>();
    private final Queue<Task> taskQueue = new ArrayBlockingQueue<>(20);
    private final List<Timer> timers = new ArrayList<>();

    FarmingTask(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);

        this.getToSkyblock = new GetToSkyblock(this::whenGetToSkyblockCompleted, this::whenGetToSkyblockAborted);
        this.farm = new Replay(ReplayBotSettings.DEFAULT_RECORDING_FILE, this::whenFarmCompleted, this::whenFarmAborted);

        this.gardenVisitorsTask = new GardenVisitors(this::defaultWhenCompleted, this::whenGardenVisitorsAborted);
    }

    private static void changeFungiMode() {
        String fungiCutterMode = Utils.getLatestMessageContaining("Fungi Cutter Mode");
        if (fungiCutterMode != null && fungiCutterMode.contains("Red Mushrooms")) {
            MinecraftClient.getInstance().options.useKey.setPressed(true);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            MinecraftClient.getInstance().options.useKey.setPressed(false);
        }
    }

    private void whenGetToSkyblockCompleted() {
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + getTaskName());
        printTaskInfoCompl();

        defaultWhenCompleted();
    }

    private void whenGetToSkyblockAborted() {
        printTaskInfoAbort();
    }

    private void defaultWhenCompleted() {
        TGBotDaemon.INSTANCE.queueMessage("Completed task: " + getTaskName());
        printTaskInfoCompl();

        if (!checkIfOnGarden()) {
            currentTask.execute();
            return;
        }

        if (!taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
        } else {
            currentTask = farm;
            changeFungiMode();
        }
        currentTask.execute();
    }

    private void defaultWhenAborted() {
        printTaskInfoAbort();

        if (checkIfOnGarden()) {
            currentTask = farm;
            changeFungiMode();
        }
        currentTask.execute();
    }

    private void whenFarmCompleted() {

        /*
         * This code is for situation when we die at the end of the farm to respawn at the start
         * We have to wait and check every tick if our position is equal to the starting position
         * If we wait for some time and it doesn't happen we abort the task
         */
        int waitTickCounter = 0;
        boolean isPositionCorrect;
        do {
            // account for situation when user wants to abort task while this waiting is happening
            if (currentTask == null) {
                return;
            }

            isPositionCorrect = ((Replay) farm).isPlayerInStartingPosition();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.warn("Interrupted while waiting for respawn after loop, wtf???????");
                e.printStackTrace();
            }
        } while (waitTickCounter++ < ReplayBotSettings.maxTicksToWaitForSpawn && !isPositionCorrect);

        if (!isPositionCorrect) {
            SkyblockBot.LOGGER.error("Farm completed, we waited to respawn, but position isn't correct! Stopping ComplexFarmingTask");
            return;
        }

        // Now we wait half a second for stuff to load
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            SkyblockBot.LOGGER.warn("Interrupted while waiting for respawn after loop, wtf???????");
            e.printStackTrace();
        }

        synchronized (runWhenFarmCompleted) {
            for (Runnable notTask : runWhenFarmCompleted) {
                notTask.run();
            }
            runWhenFarmCompleted.clear();
        }

        defaultWhenCompleted();
    }

    private void whenFarmAborted() {
        printTaskInfoAbort();

        if (GlobalExecutorInfo.worldLoading.get()) {
            SkyblockBot.LOGGER.info("Warped out of garden. Trying to get back");

            while (!GlobalExecutorInfo.worldLoaded.get()) {
                try {
                    Thread.sleep(ChangeIslandSettings.ticksToWaitForChunks);
                } catch (InterruptedException ignored) {
                }
            }

            if (checkIfOnGarden()) {
                currentTask = farm;
            }
            currentTask.execute();
        }
    }

    private void whenGardenVisitorsAborted() {
        printTaskInfoAbort();

        defaultWhenCompleted();
    }

    /**
     * @return true if on garden plot, false otherwise
     * it also does /warp garden if it is in garden, but not on the garden plot and returns true in that case
     */
    private boolean checkIfOnGarden() {
        if (!SBUtils.isServerSkyblock()) {
            currentTask = getToSkyblock;
            return false;
        } else if (!SBUtils.isAreaAPlot()) {
            Utils.sendChatMessage("/warp garden");
            while (!((Replay) farm).isPlayerInStartingPosition()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return true;
    }

    /*
     * I don't want to see which task was completed when i'm not actively debugging,
     * but i always want to see which task was aborted
     */
    private void printTaskInfoCompl() {
        if (GlobalExecutorInfo.debugMode.get()) {
            SkyblockBot.LOGGER.info(getTaskName() + " completed");
        }
    }

    private void printTaskInfoAbort() {
        SkyblockBot.LOGGER.info(getTaskName() + " aborted");
    }

    private void debugExecute() {
        currentTask = gardenVisitorsTask;
        currentTask.execute();
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't start complexFarmingTask when it is already executing");
            return;
        }

        SkyblockBot.LOGGER.info("Debug mode: " + GlobalExecutorInfo.debugMode.get());
        if (GlobalExecutorInfo.debugMode.get()) {
            debugExecute();
            return;
        }

        runWhenFarmCompleted.clear();
        taskQueue.clear();

        if (checkIfOnGarden()) {
            currentTask = farm;
        }
        currentTask.execute();

        timers.forEach(timer -> {
            timer.cancel();
            timer.purge();
        });

        /*
         * Basically every durationInMs we tell the farming bot to pause for 10 minutes when it's done the loop
         * After 10 minutes it tells itself to run again when loop is done and starts executing itself again
         *
         * TLDR: pause for 10 minutes every hour but only at the end of the farming loop
         */
        Timer regularPauseTimer = new Timer(true);
        regularPauseTimer.scheduleAtFixedRate(new RegularPauseTimerTask(),
                FarmingTaskSettings.pauseInterval, FarmingTaskSettings.pauseInterval);
        timers.add(regularPauseTimer);

        /*
         * Checks how much time is left of booster cookie and god potion, queues to buy and use them if necessary
         */
        Timer checkGodPotAndCookieTimer = new Timer(true);
        checkGodPotAndCookieTimer.scheduleAtFixedRate(new CheckGodPotAndCookieTimerTask(),
                0, FarmingTaskSettings.intervalBetweenRegularChecks);
        timers.add(checkGodPotAndCookieTimer);

        /*
         * Checks if sacks have lots of stuff and it's time to sell them, queues to sell them
         */
        Timer checkSacksTimer = new Timer(true);
        checkSacksTimer.scheduleAtFixedRate(new CheckSacksTimerTask(),
                0, FarmingTaskSettings.intervalBetweenRegularChecks);
        timers.add(checkSacksTimer);

        Timer checkVisitorsTimer = new Timer(true);
        checkVisitorsTimer.scheduleAtFixedRate(new CheckVisitorsTimerTask(),
                0, FarmingTaskSettings.checkVisitorsInterval);
        timers.add(checkVisitorsTimer);
    }

    @Override
    public void abort() {
        if (currentTask.isExecuting()) {
            currentTask.abort();
        } else {
            SkyblockBot.LOGGER.warn("Current task is not executing, FarmingTask can't abort anything!");
        }

        for (Timer timer : timers) {
            timer.cancel();
            timer.purge();
        }
        timers.clear();
    }

    public void loadRecordingAsync() {
        if (isExecuting()) {
            synchronized (runWhenFarmCompleted) {
                runWhenFarmCompleted.add(() -> {
                    farm.loadFromFile(ReplayBotSettings.DEFAULT_RECORDING_FILE);
                    ((GardenVisitors) gardenVisitorsTask).reloadRecordings();
                });
            }
        } else {
            CompletableFuture.runAsync(() -> {
                farm.loadFromFile(ReplayBotSettings.DEFAULT_RECORDING_FILE);
                ((GardenVisitors) gardenVisitorsTask).reloadRecordings();
            });
        }
    }

    private class RegularPauseTimerTask extends TimerTask {
        @Override
        public void run() {
            SkyblockBot.LOGGER.info("When the current farm loop is done bot is going to take a break");

            synchronized (runWhenFarmCompleted) {
                runWhenFarmCompleted.add(this::pauseTask);
            }
        }

        private void pauseTask() {
            SkyblockBot.LOGGER.info("Bot is taking a break for " + FarmingTaskSettings.pauseDuration / 60000 + "minutes");
            TGBotDaemon.INSTANCE.queueMessage("Bot is taking a break");
            try {
                Thread.sleep(FarmingTaskSettings.pauseDuration);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            SkyblockBot.LOGGER.info("Break is over, bot is farming again");
            TGBotDaemon.INSTANCE.queueMessage("Break is over, bot is farming again");
        }
    }

    private class CheckGodPotAndCookieTimerTask extends TimerTask {
        private static Task buyGodPot;
        private static Task useGodPot;
        private static Task buyCookie;
        private static Task useCookie;

        {
            buyGodPot = new BuyItem(
                    ItemNames.GOD_POT.getName(),
                    new String[0],
                    FarmingTask.this::defaultWhenCompleted,
                    FarmingTask.this::defaultWhenAborted);

            useGodPot = new UseItem(
                    ItemNames.GOD_POT.getName(),
                    FarmingTask.this::defaultWhenCompleted,
                    FarmingTask.this::defaultWhenAborted);

            buyCookie = new BuyBZItem(
                    ItemNames.BOOSTER_COOKIE.getName(),
                    FarmingTask.this::defaultWhenCompleted,
                    FarmingTask.this::defaultWhenAborted);

            useCookie = new UseItem(
                    ItemNames.BOOSTER_COOKIE.getName(),
                    FarmingTask.this::defaultWhenCompleted,
                    FarmingTask.this::defaultWhenAborted);
        }

        @Override
        public void run() {
            if (!SBUtils.isServerSkyblock()) {
                SkyblockBot.LOGGER.info("Server isn't skyblock. Skipping god pot and cookie check");
                return;
            }

            if (currentTask == null) {
                SkyblockBot.LOGGER.info("Current task is null, can't check god pot and cookie");
            }

            if (SBUtils.getTimeLeftGodPot() < FarmingTaskSettings.godPotBuyThreshold) {
                if (!taskQueue.contains(useGodPot) &&
                        currentTask != buyGodPot &&
                        currentTask != useGodPot) {

                    SkyblockBot.LOGGER.info("Queueing to use god pot. Minutes left: " + SBUtils.getTimeLeftGodPot() / (1000 * 60));

                    if (!SBUtils.isItemInInventory(ItemNames.GOD_POT.getName())) {
                        taskQueue.add(buyGodPot);
                    }

                    taskQueue.add(useGodPot);
                }
            } else {
                if (taskQueue.contains(useGodPot)) {
                    SkyblockBot.LOGGER.info("Wrongfully queued to use god pot, removing that task from queue");
                }
                taskQueue.remove(buyGodPot);
                taskQueue.remove(useGodPot);
            }

            if (SBUtils.getTimeLeftCookieBuff() < FarmingTaskSettings.cookieBuyThreshold) {
                if (!taskQueue.contains(useCookie) &&
                        currentTask != buyCookie &&
                        currentTask != useCookie) {

                    SkyblockBot.LOGGER.info("Queueing to use cookie. Minutes left: " + SBUtils.getTimeLeftCookieBuff() / (1000 * 60));

                    if (!SBUtils.isItemInInventory(ItemNames.BOOSTER_COOKIE.getName())) {
                        taskQueue.add(buyCookie);
                    }

                    taskQueue.add(useCookie);
                }
            } else {
                if (taskQueue.contains(useCookie)) {
                    SkyblockBot.LOGGER.info("Wrongfully queued to use cookie, removing that task from queue");
                }
                taskQueue.remove(buyCookie);
                taskQueue.remove(useCookie);
            }
        }
    }

    private class CheckSacksTimerTask extends TimerTask {
        private SellSacks sellSacks;

        @Override
        public void run() {
            if (GlobalExecutorInfo.carrotCount.get() / 160 > GlobalExecutorInfo.totalSackCountLimit) {
                if (!taskQueue.contains(sellSacks)) {
                    SkyblockBot.LOGGER.info("Queueing to sell sacks");
                    sellSacks = new SellSacks(FarmingTask.this::defaultWhenCompleted, FarmingTask.this::defaultWhenAborted);
                    taskQueue.add(sellSacks);
                }
            }
        }
    }

    private class CheckVisitorsTimerTask extends TimerTask {
        @Override
        public void run() {
            if (SBUtils.getGardenVisitorCount() > 3) {
                if (SBUtils.getPurse() < 3000000) {
                    SkyblockBot.LOGGER.info("Not queueing to handle garden guests, because purse is too low on coins. Purse: " + SBUtils.getPurse());
                    return;
                }
                if (!taskQueue.contains(gardenVisitorsTask) && currentTask != gardenVisitorsTask) {
                    SkyblockBot.LOGGER.info("Queueing to handle garden guests");
                    taskQueue.add(gardenVisitorsTask);
                }
            }
        }
    }
}
