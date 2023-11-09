package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.BaseTask;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;

public class BuyBZItem extends BaseTask<BuyBZItemExecutor> {

    private String itemName = null;
    private int itemCount = 1;

    public BuyBZItem(String itemName, Runnable whenCompleted, Runnable whenAborted) {
        super(BuyBZItemExecutor.INSTANCE, whenCompleted, whenAborted);
        this.itemName = itemName;
    }

    public BuyBZItem(String itemName, int itemCount, Runnable whenCompleted, Runnable whenAborted) {
        super(BuyBZItemExecutor.INSTANCE, whenCompleted, whenAborted);
        this.itemName = itemName;
        this.itemCount = itemCount;
    }

    public void setItemName(String itemName) {
        if (!BuyBZItemExecutor.INSTANCE.isExecuting(this)) {
            this.itemName = itemName;
        }
    }

    public void setItemCount(int itemCount) {
        if (itemCount < 1) {
            SkyblockBot.LOGGER.warn("Wrong argument for setItemCount() method of BuyBZItem. Arg: " + itemCount);
            return;
        }
        this.itemCount = itemCount;
    }

    @Override
    public void execute() {
        if (itemName == null) {
            SkyblockBot.LOGGER.warn("Can't execute BuyBZItem because itemName is null");
            aborted();
            return;
        }
        BuyBZItemExecutor.INSTANCE.execute(this);
    }

    public String getItemName() {
        return itemName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public String getBZCommand() {
        return "/bz";
    }

    public String getSearchItemName() {
        return "Search";
    }

    public Class<? extends Screen> getSearchScreenClass() {
        return SignEditScreen.class;
    }

    public String getBuyInstantlyItemName() {
        return "Buy Instantly";
    }

    public String getBuyOneItemName() {
        return "Buy only one!";
    }

    public String getEnterAmountItemName() {
        return "Custom Amount";
    }

    public String getBuyCustomAmountItemName() {
        return "Custom Amount";
    }

    public String getBZMenuName() {
        return "Bazaar";
    }

    public String getSearchResultMenuName() {
        if(itemName.length() > 17) {
            return itemName.substring(0, 17);
        } else {
            return itemName;
        }
    }

    public String getItemMenuName() {
        if(itemName.length() > 17) {
            return itemName.substring(0, 17);
        } else {
            return itemName;
        }
    }

    public String getBuyInstantlyMenuName() {
        if(itemName.length() > 17) {
            return itemName.substring(0, 17);
        } else {
            return itemName;
        }
    }

    public String getBuyCustomAmountMenuName() {
        return "Confirm Instant Buy";
    }
}
