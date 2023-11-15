package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.getInfo.GetComposterInfo;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.putItems.PutItemsInComposter;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import com.viktorx.skyblockbot.utils.Utils;

public class ComposterTask extends CompoundTask {
    private static final String goToComposterRecName = "go_to_composter.bin";
    private static final String organicMatterSource = ItemNames.BOX_OF_SEEDS.getName();
    private static final String fuelSource = ItemNames.OIL_BARREL.getName();
    private final Task goToComposter;
    private final Task getComposterInfo;

    public ComposterTask(Runnable whenCompleted, Runnable whenAborted) {
        super(whenCompleted, whenAborted);

        this.goToComposter = new Replay(goToComposterRecName, this::whenGoToComposterCompleted, this::defaultWhenAborted);
        this.getComposterInfo = new GetComposterInfo(this::whenGetComposterInfoCompleted, this::defaultWhenAborted);
    }

    private void whenGoToComposterCompleted() {
        currentTask = getComposterInfo;
        currentTask.execute();
    }

    private void whenGetComposterInfoCompleted() {
        GetComposterInfo composterInfo = (GetComposterInfo) getComposterInfo;
        float organicMatter = composterInfo.getMaxOrganicMatter() - SBUtils.getComposterOrganicMatter();
        int boxOfSeedsCount = Math.round(organicMatter / 25600) - Utils.countItemInInventory(organicMatterSource);

        if(boxOfSeedsCount <= 0) {
            whenBuyMatterCompleted();
            return;
        }

        currentTask = new BuyBZItem(organicMatterSource, boxOfSeedsCount,
                this::whenBuyMatterCompleted, this::defaultWhenAborted);
        currentTask.execute();
    }

    private void whenBuyMatterCompleted() {
        GetComposterInfo composterInfo = (GetComposterInfo) getComposterInfo;
        float fuel = composterInfo.getMaxFuel() - SBUtils.getComposterFuel();
        int oilBarrelCount = Math.round(fuel / 10000) - Utils.countItemInInventory(fuelSource);

        if(oilBarrelCount <= 0) {
            whenBuyFuelCompleted();
            return;
        }

        currentTask = new BuyBZItem(fuelSource, oilBarrelCount,
                this::whenBuyFuelCompleted, this::defaultWhenAborted);
        currentTask.execute();
    }

    private void whenBuyFuelCompleted() {
        currentTask = new PutItemsInComposter(organicMatterSource, fuelSource,
                this::whenPutItemsInComposterCompleted, this::defaultWhenAborted);
        currentTask.execute();
    }

    private void whenPutItemsInComposterCompleted() {
        this.completed();
    }

    private void defaultWhenAborted() {
        this.aborted();
    }

    @Override
    public void execute() {
        if (isExecuting()) {
            SkyblockBot.LOGGER.info("Can't execute GardenVisitorsTask, already in execution");
            return;
        }
        currentTask = goToComposter;
        currentTask.execute();
    }
}
