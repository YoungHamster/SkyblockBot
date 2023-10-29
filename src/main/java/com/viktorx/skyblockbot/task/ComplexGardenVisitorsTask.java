package com.viktorx.skyblockbot.task;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.replay.Replay;
import com.viktorx.skyblockbot.task.menuClickingTasks.visitors.giveVisitorItems.GiveVisitorItems;
import com.viktorx.skyblockbot.task.menuClickingTasks.visitors.talkToVisitor.TalkToVisitor;

public class ComplexGardenVisitorsTask extends Task {

    private static final String goToVisitorsRecName = "go_to_visitors.bin";
    private static final String goBackToFarmRecName = "go_back_to_farm.bin";

    private Task currentTask;
    private final Task goToVisitors;
    private final Task goBackToFarm;
    private final Task talkToVisitor;
    private final Task buyBZItem;
    private final Task giveVisitorItems;
    private String currentVisitor = null;

    public ComplexGardenVisitorsTask() {
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
        // TODO
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
        ((BuyBZItem) buyBZItem).setItemName(((TalkToVisitor) talkToVisitor).getItemName());
        ((BuyBZItem) buyBZItem).setItemCount(((TalkToVisitor) talkToVisitor).getItemCount());
        currentVisitor = ((TalkToVisitor) talkToVisitor).getVisitorName();

        currentTask = buyBZItem;
        currentTask.execute();
    }

    private void whenTalkToVisitorAborted() {
        // TODO
    }

    private void whenBuyBZItemCompleted() {
        currentTask = giveVisitorItems;
        currentTask.execute();
    }

    private void whenBuyBZItemAborted() {
        // TODO
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

        if(SBUtils.getGardenVisitorCount() == 0) {
            currentTask = goBackToFarm;
        } else {
            currentTask = talkToVisitor;
        }
        currentTask.execute();
    }

    private void whenGiveVisitorItemsAborted() {
        // TODO
    }

    public void execute() {
        if(isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute ComplexGardenVisitorsTask, already in execution");
            return;
        }
        currentTask = goToVisitors;
        goToVisitors.execute();
    }
    public void pause() {
        if(currentTask != null) {
            currentTask.pause();
        }
    }
    public void resume() {
        if(currentTask != null) {
            currentTask.resume();
        }
    }
    public void abort() {
        if(currentTask != null) {
            currentTask.abort();
        }
        currentTask = null;
    }
    public boolean isExecuting() {
        if(currentTask != null) {
            return currentTask.isExecuting();
        }
        return false;
    }
    public boolean isPaused() {
        if(currentTask != null) {
            return currentTask.isPaused();
        }
        return false;
    }

    public void reloadRecordings() {
        goToVisitors.loadFromFile(goToVisitorsRecName);
        goBackToFarm.loadFromFile(goBackToFarmRecName);
    }
}
