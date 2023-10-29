package com.viktorx.skyblockbot.task.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.Task;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;

public class BuyBZItem extends Task {

    private String itemName = null;
    private int itemCount = 1;

    public void setItemName(String itemName) {
        if(!BuyBZItemExecutor.INSTANCE.isExecuting(this)) {
            this.itemName = itemName;
        }
    }

    public void setItemCount(int itemCount) {
        if(itemCount < 1) {
            SkyblockBot.LOGGER.warn("Wrong argument for setItemCount() method of BuyBZItem. Arg: " + itemCount);
            return;
        }
        this.itemCount = itemCount;
    }

    public void execute() {
        if(itemName == null) {
            SkyblockBot.LOGGER.warn("Can't execute BuyBZItem because itemName is null");
            aborted();
            return;
        }
        BuyBZItemExecutor.INSTANCE.execute(this);
    }

    public void pause() {
        BuyBZItemExecutor.INSTANCE.pause();
    }

    public void resume() {
        BuyBZItemExecutor.INSTANCE.resume();
    }

    public void abort() {
        BuyBZItemExecutor.INSTANCE.abort();
    }

    public boolean isExecuting() {
        return BuyBZItemExecutor.INSTANCE.isExecuting(this);
    }

    public boolean isPaused() {
        return BuyBZItemExecutor.INSTANCE.isPaused();
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
}
