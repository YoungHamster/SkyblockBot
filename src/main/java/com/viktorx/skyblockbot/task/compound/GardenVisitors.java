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

    private final Task goToVisitors;
    private final Task talkToVisitor;
    private final Task giveVisitorItems;
    private String currentVisitor = null;

    public GardenVisitors(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);

        this.goToVisitors = new Replay(goToVisitorsRecName, this::whenGoToVisitorsCompleted, this::whenGoToVisitorsAborted);

        this.talkToVisitor = new TalkToVisitor(this::whenTalkToVisitorCompleted, this::defaultWhenAborted);
        this.giveVisitorItems = new GiveVisitorItems(this::whenGiveVisitorItemsCompleted, this::defaultWhenAborted);
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
    }

    private void whenTalkToVisitorCompleted() {
        whenBuyBZItemCompleted();
    }

    private void defaultWhenAborted() {
        this.aborted();
    }

    private void whenBuyBZItemCompleted() {
        Pair<String, Integer> nextItem = getNextItemToBuy();

        if (nextItem == null) {
            ((GiveVisitorItems) giveVisitorItems).setVisitorName(currentVisitor);
            currentTask = giveVisitorItems;
        } else {
            currentTask = new BuyBZItem(nextItem.getKey(), nextItem.getValue(), this::whenBuyBZItemCompleted, this::defaultWhenAborted);
        }

        currentTask.execute();
    }

    private Pair<String, Integer> getNextItemToBuy() {
        while (((TalkToVisitor) talkToVisitor).isItemRemaining()) {
            Pair<String, Integer> itemNameCount = ((TalkToVisitor) talkToVisitor).getNextItem();
            String itemName = itemNameCount.getKey();
            Integer itemCount = itemNameCount.getValue();

            if (SBUtils.isItemInInventory(itemName)) {
                try {
                    if (SBUtils.getSlot(itemName).getStack().getCount() >= itemCount) {
                        continue;
                    } else {
                        itemCount -= SBUtils.getSlot(itemName).getStack().getCount();
                    }
                } catch (TimeoutException e) {
                    SkyblockBot.LOGGER.info("GardenVisitorsTask whenTalkToVisitorCompleted got TimeoutException\n" +
                            "WTF?? Item is supposed to be in inventory at this point, so the shouldn't be a timeoutException");
                }
            }

            return new Pair<>(itemName, itemCount);
        }
        return null;
    }

    private void whenGiveVisitorItemsCompleted() {
        while (true) {
            String firstVisitor = SBUtils.getFirstVisitor();
            if (firstVisitor == null) {
                this.completed();
                return;
            } else if (firstVisitor.equals(currentVisitor)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            } else {
                currentVisitor = firstVisitor;
                ((TalkToVisitor) talkToVisitor).setVisitorName(currentVisitor);
                currentTask = talkToVisitor;
                break;
            }
        }
        currentTask.execute();
    }

    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        if (SBUtils.getGardenVisitorCount() == 0) {
            SkyblockBot.LOGGER.warn("Can't execute GardenVisitors when there are no visitors!");
            this.aborted();
            return;
        }
        currentTask = goToVisitors;
        goToVisitors.execute();
    }

    public void reloadRecordings() {
        if (!goToVisitors.isExecuting()) {
            goToVisitors.loadFromFile(goToVisitorsRecName);
        } else {
            SkyblockBot.LOGGER.warn("Can't reload goToVisitors recording, because it is being executed");
        }
    }
}
