package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.skyblock.flipping.AuctionBrowser;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.Task;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class BuyItemExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();

    private BuyItemState state = BuyItemState.IDLE;
    private BuyItemState nextState;
    private BuyItemState stateBeforePause;
    private BuyItem task;
    private int waitTickCounter = 0;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public void execute(BuyItem task) {
        AuctionBrowser.INSTANCE.loadAH();
        state = BuyItemState.LOADING_AUCTIONS;
        this.task = task;
    }

    public void pause() {
        stateBeforePause = state;
        state = BuyItemState.PAUSED;
    }

    public void resume() {
        state = stateBeforePause;
    }

    public void abort() {
        state = BuyItemState.IDLE;
    }

    public boolean isExecuting(Task task) {
        return !state.equals(BuyItemState.IDLE) && this.task == task;
    }

    public void onTickBuy(MinecraftClient client) {

        switch(state) {
            case LOADING_AUCTIONS -> {
                if(AuctionBrowser.INSTANCE.isAHLoaded()) {
                    state = BuyItemState.SENDING_COMMAND;
                }
            }

            case SENDING_COMMAND -> {
                String auctionCommand = AuctionBrowser.INSTANCE.getAuctionWithBestPrice(task.getItemName(), task.getItemLoreKeyWords());

                if(auctionCommand == null) {
                    task.aborted();
                    state = BuyItemState.IDLE;
                    return;
                }

                assert client.player != null;
                client.player.sendChatMessage(auctionCommand);

                nextState = BuyItemState.BUYING;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case BUYING -> {
                if(!waitBeforeClick()) {
                    clickOnSlotOrAbort(task.getBuySlotName());

                    nextState = BuyItemState.CONFIRMING_BUY;
                    state = BuyItemState.WAITING_FOR_MENU;
                }
            }

            case CONFIRMING_BUY -> {
                if (!waitBeforeClick()) {
                    clickOnSlotOrAbort(task.getConfirmSlotName());

                    nextState = BuyItemState.CHECKING_BUY_RESULT;
                    state = BuyItemState.WAITING_FOR_MENU;
                }
            }

            case CHECKING_BUY_RESULT -> {
                List<String> messageHistory = client.inGameHud.getChatHud().getMessageHistory();
                if(messageHistory.get(messageHistory.size() - 1).contains("Visit the Auction House to collect you item")) {
                    state = BuyItemState.CLAIMING_AUCTION;
                } else {
                    state = BuyItemState.RESTARTING;
                }
            }

            case RESTARTING -> {
                execute(task);
            }

            case CLAIMING_AUCTION -> {
                assert client.player != null;
                client.player.sendChatMessage("/ah");

                nextState = BuyItemState.CLAIMING_AUCTION_VIEW_BIDS;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case CLAIMING_AUCTION_VIEW_BIDS -> {
                if(!waitBeforeClick()) {
                    clickOnSlotOrAbort(task.getViewBidsSlotName());

                    nextState = BuyItemState.CLAIMING_AUCTION_BID;
                    state = BuyItemState.WAITING_FOR_MENU;
                }
            }

            case CLAIMING_AUCTION_BID -> {
                if(!waitBeforeClick()) {
                    clickOnSlotOrAbort(task.getItemName());

                    nextState = BuyItemState.CLIAMING_AUCTION_CLAIM;
                    state = BuyItemState.WAITING_FOR_MENU;
                }
            }

            case CLIAMING_AUCTION_CLAIM -> {
                if (!waitBeforeClick()) {
                    clickOnSlotOrAbort(task.getConfirmSlotName());

                    state = BuyItemState.IDLE;
                    task.completed();
                }
            }

            case WAITING_FOR_MENU -> {
                if (CurrentInventory.syncIDChanged()) {
                    state = nextState;
                }
            }

        }
    }

    private void clickOnSlotOrAbort(String slotName) {
        try {
            SBUtils.leftClickOnSlot(slotName);
        } catch (TimeoutException e) {
            state = BuyItemState.IDLE;
            task.aborted();
        }
    }

    /*
     * This class waits half of what other classes wait,
     * because I don't want to lose too many auctions to bots and fast players
     */
    private boolean waitBeforeClick() {
        if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeClick / 2) {
            return true;
        }
        waitTickCounter = 0;
        return false;
    }
}
