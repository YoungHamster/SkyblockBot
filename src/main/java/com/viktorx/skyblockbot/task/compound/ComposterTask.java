package com.viktorx.skyblockbot.task.compound;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.ItemNames;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.base.composter.putItems.PutItemsInComposter;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem.BuyBZItem;
import com.viktorx.skyblockbot.task.base.composter.getInfo.GetComposterInfo;
import com.viktorx.skyblockbot.task.base.replay.Replay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class ComposterTask extends CompoundTask {
    private static final String goToComposterRecName = "go_to_composter.bin";
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
        int enchantedSeedCount = (int) Math.floor(organicMatter / 160);
        currentTask = new BuyBZItem(ItemNames.ENCHANTED_SEED.getName(), enchantedSeedCount,
                this::whenBuyMatterCompleted, this::defaultWhenAborted);
        currentTask.execute();
    }

    private void whenBuyMatterCompleted() {
        GetComposterInfo composterInfo = (GetComposterInfo) getComposterInfo;
        float fuel = composterInfo.getMaxFuel() - SBUtils.getComposterFuel();
        int oilBarrelCount = (int) Math.floor(fuel / 10000);
        currentTask = new BuyBZItem(ItemNames.OIL_BARREL.getName(), oilBarrelCount,
                this::whenBuyFuelCompleted, this::defaultWhenAborted);
        currentTask.execute();
    }

    private void whenBuyFuelCompleted() {
        List<Integer> slots = new ArrayList<>();
        assert MinecraftClient.getInstance().player != null;
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();

        for(int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
            String stackName = inventory.getStack(i).getName().getString();
            if(stackName.contains(ItemNames.ENCHANTED_SEED.getName()) || stackName.contains(ItemNames.OIL_BARREL.getName())) {
                slots.add(i);
            }
        }

        currentTask = new PutItemsInComposter(slots,
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
    }
}
