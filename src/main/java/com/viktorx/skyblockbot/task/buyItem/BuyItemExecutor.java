package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.skyblock.flipping.AuctionBrowser;
import com.viktorx.skyblockbot.task.Task;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class BuyItemExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();

    private BuyItemState state = BuyItemState.IDLE;
    private BuyItemState beforePauseState;
    private BuyItem buyItem;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public void execute(BuyItem task) {
        state = BuyItemState.SENDING_COMMAND;
        this.buyItem = task;
    }

    public void pause() {
        beforePauseState = state;
        state = BuyItemState.PAUSED;
    }

    public void resume() {
        state = beforePauseState;
    }

    public void abort() {
        state = BuyItemState.IDLE;
    }

    public boolean isExecuting(Task task) {
        return !state.equals(BuyItemState.IDLE) && buyItem == task;
    }

    public void onTickBuy(MinecraftClient client) {
        if(!state.equals(BuyItemState.BUYING)) {
            return;
        }
        String auctionCommand = AuctionBrowser.INSTANCE.getAuctionWithBestPrice(buyItem.getItemName(), buyItem.getItemLoreKeyWords());
        if(auctionCommand == null) {
            buyItem.aborted();
            state = BuyItemState.IDLE;
            return;
        }

        assert client.player != null;
        client.player.sendChatMessage(auctionCommand);

        // TODO

    }
}
