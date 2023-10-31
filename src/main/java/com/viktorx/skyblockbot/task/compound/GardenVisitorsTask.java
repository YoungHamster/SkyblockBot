package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems.GiveVisitorItems;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor.TalkToVisitor;
import com.viktorx.skyblockbot.task.base.replay.Replay;

import java.util.concurrent.TimeoutException;

public class GardenVisitorsTask extends Task {

    private static final String goToVisitorsRecName = "go_to_visitors.bin";
    private static final String goBackToFarmRecName = "go_back_to_farm.bin";

    private Task currentTask;
    private final Task goToVisitors;
    private final Task goBackToFarm;
    private final Task talkToVisitor;
    private final Task buyBZItem;
    private final Task giveVisitorItems;
    private String currentVisitor = null;

    public GardenVisitorsTask() {
        this.goToVisitors = new Replay(goToVisitorsRecName);
        this.goToVisitors.whenCompleted(this::whenGoToVisitorsCompleted);
        this.goToVisitors.whenAborted(this::whenGoToVisitorsAborted);

        this.goBackToFarm = new Replay(goBackToFarmRecName);
        this.goBackToFarm.whenCompleted(this::whenGoBackToFarmCompleted);
        this.goBackToFarm.whenAborted(this::whenGoBackToFarmAborted);

        this.talkToVisitor = new TalkToVisitor();
        this.talkToVisitor.whenCompleted(this::whenTalkToVisitorCompleted);
        this.talkToVisitor.whenAborted(this::whenTalkToVisitorAborted);

        this.buyBZItem = new BuyBZItem();
        this.buyBZItem.whenCompleted(this::whenBuyBZItemCompleted);
        this.buyBZItem.whenAborted(this::whenBuyBZItemAborted);

        this.giveVisitorItems = new GiveVisitorItems();
        this.giveVisitorItems.whenCompleted(this::whenGiveVisitorItemsCompleted);
        this.giveVisitorItems.whenAborted(this::whenGiveVisitorItemsAborted);
    }

    private void whenGoToVisitorsCompleted() {
        currentTask = talkToVisitor;
        currentTask.execute();
    }

    private void whenGoToVisitorsAborted() {
        SkyblockBot.LOGGER.warn("GoToVisitors task aborted! Aborting GardenVisitors task");
        this.aborted();
        currentTask = null;
    }

    private void whenGoBackToFarmCompleted() {
        currentTask = null;
        this.completed();
    }

    private void whenGoBackToFarmAborted() {
        currentTask = null;
        this.aborted();
    }

    private void whenTalkToVisitorCompleted() {
        String itemName = ((TalkToVisitor) talkToVisitor).getItemName();
        int itemCount = ((TalkToVisitor) talkToVisitor).getItemCount();
        ((BuyBZItem) buyBZItem).setItemName(itemName);
        ((BuyBZItem) buyBZItem).setItemCount(itemCount);
        currentVisitor = ((TalkToVisitor) talkToVisitor).getVisitorName();

        currentTask = buyBZItem;

        if (SBUtils.isItemInInventory(itemName)) {
            try {
                if (SBUtils.getSlot(itemName).getStack().getCount() >= itemCount) {
                    currentTask = giveVisitorItems;
                }
            } catch (TimeoutException e) {
                SkyblockBot.LOGGER.info("GardenVisitorsTask whenTalkToVisitorCompleted got TimeoutException\n" +
                        "WTF?? Item is supposed to be in inventory at this point, so the shouldn't be a timeoutException");
            }
        }
        currentTask.execute();
    }

    private void whenTalkToVisitorAborted() {
        SkyblockBot.LOGGER.warn("TalkToVisitors task aborted! Aborting GardenVisitors task");
        this.aborted();
        currentTask = null;
    }

    private void whenBuyBZItemCompleted() {
        currentTask = giveVisitorItems;
        currentTask.execute();
    }

    private void whenBuyBZItemAborted() {
        SkyblockBot.LOGGER.warn("BuyBZItem task aborted! Aborting GardenVisitors task");
        this.aborted();
        currentTask = null;
    }

    private void whenGiveVisitorItemsCompleted() {
        while (SBUtils.isGardenVisitorInQueue(currentVisitor)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                SkyblockBot.LOGGER.info("Interrupted when waiting for garden visitor to leave, wtf?");
            }
        }
        currentVisitor = null;

        if (SBUtils.getGardenVisitorCount() == 0) {
            currentTask = goBackToFarm;
        } else {
            currentTask = talkToVisitor;
        }
        currentTask.execute();
    }

    private void whenGiveVisitorItemsAborted() {
        SkyblockBot.LOGGER.warn("GiveVisitorsItems task aborted! Aborting GardenVisitors task");
        this.aborted();
        currentTask = null;
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        currentTask = goToVisitors;
        goToVisitors.execute();
    }

    public void pause() {
        if (currentTask != null) {
            currentTask.pause();
        }
    }

    public void resume() {
        if (currentTask != null) {
            currentTask.resume();
        }
    }

    public void abort() {
        if (currentTask != null) {
            currentTask.abort();
        }
        currentTask = null;
    }

    public boolean isExecuting() {
        if (currentTask != null) {
            return currentTask.isExecuting();
        }
        return false;
    }

    public boolean isPaused() {
        if (currentTask != null) {
            return currentTask.isPaused();
        }
        return false;
    }

    public void reloadRecordings() {
        goToVisitors.loadFromFile(goToVisitorsRecName);
        goBackToFarm.loadFromFile(goBackToFarmRecName);
    }
}
