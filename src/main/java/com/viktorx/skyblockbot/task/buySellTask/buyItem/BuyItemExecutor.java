package com.viktorx.skyblockbot.task.buySellTask.buyItem;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.flipping.auction.AuctionBrowser;
import com.viktorx.skyblockbot.task.Task;
import com.viktorx.skyblockbot.task.buySellTask.BuySellTaskExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BuyItemExecutor extends BuySellTaskExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();

    private BuyItemState state = BuyItemState.IDLE;
    private BuyItemState nextState;
    private BuyItemState stateBeforePause;
    private BuyItem task;
    private final List<String> possibleErrors = Arrays.asList("You cannot view this auction", "This auction wasn't found");
    private CompletableFuture<String> priceFinder;
    private boolean priceFinderRunning = false;


    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    protected void restart() {
        blockingCloseCurrentInventory();
        SkyblockBot.LOGGER.warn("Can't buy from auction. Restarting task");
        state = BuyItemState.RESTARTING;
    }

    public void execute(BuyItem task) {
        if (!state.equals(BuyItemState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute BuyItem when already running");
            return;
        }

        CompletableFuture.runAsync(AuctionBrowser.INSTANCE::loadAH);
        priceFinderRunning = false;
        currentClickRunning = false;
        waitTickCounter = 0;
        state = BuyItemState.LOADING_AUCTIONS;
        this.task = task;
    }

    public void pause() {
        if (state.equals(BuyItemState.IDLE) || state.equals(BuyItemState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't pause BuyItem when idle or already paused");
            return;
        }

        stateBeforePause = state;
        state = BuyItemState.PAUSED;
    }

    public void resume() {
        if (state.equals(BuyItemState.PAUSED)) {
            state = stateBeforePause;
        }
    }

    public void abort() {
        state = BuyItemState.IDLE;
    }

    public boolean isExecuting(Task task) {
        return !state.equals(BuyItemState.IDLE) && this.task == task;
    }

    public void onTickBuy(MinecraftClient client) {

        switch (state) {
            case LOADING_AUCTIONS -> {
                if (AuctionBrowser.INSTANCE.isAHLoaded()) {
                    state = BuyItemState.SENDING_COMMAND;
                }
            }

            case SENDING_COMMAND -> {
                if (!priceFinderRunning) {
                    priceFinder =
                            CompletableFuture.supplyAsync(
                                    () -> AuctionBrowser.INSTANCE.getAuctionWithBestPrice(
                                            task.getItemName(),
                                            task.getItemLoreKeyWords(),
                                            task.getPriceLimit()));
                    priceFinderRunning = true;
                }

                if (!priceFinder.isDone()) {
                    return;
                }

                if (waitBeforeCommand()) {
                    return;
                }

                priceFinderRunning = false;
                String auctionCommand = null;

                try {
                    auctionCommand = priceFinder.get();
                } catch (InterruptedException | ExecutionException e) {
                    SkyblockBot.LOGGER.info("Some weird exception when buying item.");
                    state = BuyItemState.IDLE;
                    task.aborted();
                }

                if (auctionCommand == null) {
                    state = BuyItemState.RESTARTING;
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting! Line 118");
                    return;
                }

                assert client.player != null;
                client.player.sendChatMessage(auctionCommand);

                nextState = BuyItemState.BUYING;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case BUYING -> {
                if (!asyncClickOrRestart(task.getBuySlotName())) {
                    return;
                }

                nextState = BuyItemState.CONFIRMING_BUY;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case CONFIRMING_BUY -> {
                if (!asyncClickOrRestart(task.getConfirmSlotName())) {
                    return;
                }

                nextState = BuyItemState.CHECKING_BUY_RESULT;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case CHECKING_BUY_RESULT -> {
                if (waitBeforeCommand()) {
                    return;
                }

                if (isStringInRecentChat("You claimed", 5)) {
                    state = BuyItemState.IDLE;
                    task.completed();
                } else if (isStringInRecentChat("Visit the Auction House", 5)) {
                    state = BuyItemState.CLAIMING_AUCTION;
                } else {
                    state = BuyItemState.RESTARTING;
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting! Line 159");
                }
            }

            case RESTARTING -> {
                state = BuyItemState.IDLE;
                CompletableFuture.runAsync(() -> execute(task));
            }

            case CLAIMING_AUCTION -> {
                if (waitBeforeCommand()) {
                    return;
                }

                assert client.player != null;
                client.player.sendChatMessage("/ah");

                nextState = BuyItemState.CLAIMING_AUCTION_VIEW_BIDS;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case CLAIMING_AUCTION_VIEW_BIDS -> {
                if (!asyncClickOrRestart(task.getViewBidsSlotName())) {
                    return;
                }

                nextState = BuyItemState.CLAIMING_AUCTION_BID;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case CLAIMING_AUCTION_BID -> {
                if (!asyncClickOrRestart(task.getItemName())) {
                    return;
                }

                nextState = BuyItemState.CLIAMING_AUCTION_CLAIM;
                state = BuyItemState.WAITING_FOR_MENU;
            }

            case CLIAMING_AUCTION_CLAIM -> {
                if (!asyncClickOrRestart(task.getCollectAuctionSlotName())) {
                    return;
                }

                state = BuyItemState.IDLE;
                task.completed();
            }

            case WAITING_FOR_MENU -> {
                if (checkForPossibleError()) {
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting! Line 210");
                    restart();
                    return;
                }
                if (CurrentInventory.syncIDChanged()) {
                    state = nextState;
                }
            }

        }
    }

    private boolean checkForPossibleError() {
        for (String possibleError : possibleErrors) {
            if (isStringInRecentChat(possibleError, 1)) {
                return true;
            }
        }
        return false;
    }
}
