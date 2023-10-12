package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.skyblock.flipping.AuctionBrowser;
import com.viktorx.skyblockbot.task.Task;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class BuyItemExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();

    private boolean executing = false;
    private BuyItem buyItem;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public void execute(BuyItem task) {
        executing = true;
        this.buyItem = task;
    }

    public void pause() {

    }

    public void resume() {

    }

    public void abort() {
        executing = false;
    }

    public boolean isExecuting(Task task) {
        return executing && buyItem == task;
    }

    public void onTickBuy(MinecraftClient client) {
        if(!executing) {
            return;
        }
        String auctionCommand = AuctionBrowser.INSTANCE.getAuctionWithBestPrice(buyItem.getItemName());
        if(auctionCommand == null) {
            buyItem.aborted();
            executing = false;
            return;
        }

        assert client.player != null;
        client.player.sendChatMessage(auctionCommand);

        // TODO

    }
}
