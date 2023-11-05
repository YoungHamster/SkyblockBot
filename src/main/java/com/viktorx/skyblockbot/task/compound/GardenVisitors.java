package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.giveVisitorItems.GiveVisitorItems;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor.TalkToVisitor;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import javafx.util.Pair;

import java.util.concurrent.TimeoutException;

public class GardenVisitors extends CompoundTask {

    private static final String goToVisitorsRecName = "go_to_visitors.bin";
    private static final String goBackToFarmRecName = "go_back_to_farm.bin";

    private final Task goToVisitors;
    private final Task goBackToFarm;
    private final Task talkToVisitor;
    private final Task giveVisitorItems;
    private String currentVisitor = null;

    public GardenVisitors(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);

        this.goToVisitors = new Replay(goToVisitorsRecName, this::whenGoToVisitorsCompleted, this::whenGoToVisitorsAborted);
        this.goBackToFarm = new Replay(goBackToFarmRecName, this::whenGoBackToFarmCompleted, this::whenGoBackToFarmAborted);

        this.talkToVisitor = new TalkToVisitor(this::whenTalkToVisitorCompleted, this::whenTalkToVisitorAborted);
        this.giveVisitorItems = new GiveVisitorItems(this::whenGiveVisitorItemsCompleted, this::whenGiveVisitorItemsAborted);
    }

    private void whenGoToVisitorsCompleted() {
        currentVisitor = SBUtils.getFirstVisitor();
        ((TalkToVisitor) talkToVisitor).setVisitorName(currentVisitor);
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
        Pair<String, Integer> itemNameCount =  ((TalkToVisitor) talkToVisitor).getNextItem();
        String itemName = itemNameCount.getKey();
        int itemCount = itemNameCount.getValue();

        SkyblockBot.LOGGER.info("Just talked to visitor " + currentVisitor);

        currentTask = new BuyBZItem(itemName, itemCount, this::whenBuyBZItemCompleted, this::whenBuyBZItemAborted);

        if (SBUtils.isItemInInventory(itemName)) {
            try {
                if (SBUtils.getSlot(itemName).getStack().getCount() >= itemCount) {
                    ((GiveVisitorItems) giveVisitorItems).setVisitorName(currentVisitor);
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
        SkyblockBot.LOGGER.warn("TalkToVisitors task aborted! Going back to farm");
        currentTask = goBackToFarm;
        currentTask.execute();
    }

    private void whenBuyBZItemCompleted() {
        if(((TalkToVisitor) talkToVisitor).isItemRemaining()) {
            Pair<String, Integer> itemNameCount =  ((TalkToVisitor) talkToVisitor).getNextItem();
            String itemName = itemNameCount.getKey();
            int itemCount = itemNameCount.getValue();

            ((BuyBZItem) currentTask).setItemName(itemName);
            ((BuyBZItem) currentTask).setItemCount(itemCount);
        } else {
            ((GiveVisitorItems) giveVisitorItems).setVisitorName(currentVisitor);
            currentTask = giveVisitorItems;
        }
        currentTask.execute();
    }

    private void whenBuyBZItemAborted() {
        SkyblockBot.LOGGER.warn("BuyBZItem task aborted! Aborting GardenVisitors task");
        this.aborted();
        currentTask = null;
    }

    private void whenGiveVisitorItemsCompleted() {
        while(true) {
            String firstVisitor = SBUtils.getFirstVisitor();
            if(firstVisitor == null) {
                currentTask = goBackToFarm;
                break;
            } else if(firstVisitor.equals(currentVisitor)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            } else {
                currentVisitor = firstVisitor;
                ((TalkToVisitor) talkToVisitor).setVisitorName(currentVisitor);
                currentTask = talkToVisitor;
                break;
            }
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

    public void reloadRecordings() {
        goToVisitors.loadFromFile(goToVisitorsRecName);
        goBackToFarm.loadFromFile(goBackToFarmRecName);
    }
}
