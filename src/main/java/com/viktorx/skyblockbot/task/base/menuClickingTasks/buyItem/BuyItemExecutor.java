package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.skyblock.flipping.auction.AuctionBrowser;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BuyItemExecutor extends AbstractMenuClickingExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();
    private int nextState;
    private BuyItem task;
    private CompletableFuture<String> priceFinder;
    private boolean priceFinderRunning = false;

    BuyItemExecutor() {
        possibleErrors.add("You cannot view this auction");
        possibleErrors.add("This auction wasn't found");

        addState("LOADING_AUCTIONS");
        addState("SENDING_COMMAND");
        addState("WAITING_FOR_MENU");
        addState("BUYING");
        addState("CONFIRMING_BUY");
        addState("CHECKING_BUY_RESULT");
        addState("RESTARTING"); // if item wasn't bought because some hypixel error, like someone else already bought it we restart
        addState("CLAIMING_AUCTION"); // if item was bought, but didn't go to our inventory, we have to claim it
        addState("CLAIMING_AUCTION_VIEW_BIDS");
        addState("CLAIMING_AUCTION_BID");
        addState("CLIAMING_AUCTION_CLAIM");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    @Override
    protected void restart() {
        blockingCloseCurrentInventory();
        SkyblockBot.LOGGER.warn("Can't buy from auction. Restarting task");
        state = getState("RESTARTING");
    }

    @Override
    protected void whenMenuOpened() {
        state = nextState;
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        CompletableFuture.runAsync(AuctionBrowser.INSTANCE::loadAH);
        priceFinderRunning = false;
        currentClickRunning = false;
        waitTickCounter = 0;
        state = getState("LOADING_AUCTIONS");
        this.task = (BuyItem) task;
    }

    public void onTickBuy(MinecraftClient client) {

        switch (getState(state)) {
            case "LOADING_AUCTIONS" -> {
                if (AuctionBrowser.INSTANCE.isAHLoaded()) {
                    state = getState("SENDING_COMMAND");
                }
            }

            case "SENDING_COMMAND" -> {
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

                if (waitBeforeAction()) {
                    return;
                }

                priceFinderRunning = false;
                String auctionCommand = null;

                try {
                    auctionCommand = priceFinder.get();
                } catch (InterruptedException | ExecutionException e) {
                    SkyblockBot.LOGGER.info("Some weird exception when buying item.");
                    state = getState("IDLE");
                    task.aborted();
                }

                if (auctionCommand == null) {
                    state = getState("RESTARTING");
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting! Line 118");
                    return;
                }

                assert client.player != null;
                Utils.sendChatMessage(auctionCommand);

                nextState = getState("BUYING");
                state = getState("WAITING_FOR_MENU");
            }

            case "BUYING" -> {
                if (!asyncClickOrRestart(task.getBuySlotName())) {
                    return;
                }

                nextState = getState("CONFIRMING_BUY");
                state = getState("WAITING_FOR_MENU");
            }

            case "CONFIRMING_BUY" -> {
                if (!asyncClickOrRestart(task.getConfirmSlotName())) {
                    return;
                }

                nextState = getState("CHECKING_BUY_RESULT");
                state = getState("WAITING_FOR_MENU");
            }

            case "CHECKING_BUY_RESULT" -> {
                if (waitBeforeAction()) {
                    return;
                }

                if (Utils.isStringInRecentChat("You claimed", 5)) {
                    state = getState("IDLE");
                    task.completed();
                } else if (Utils.isStringInRecentChat("Visit the Auction House", 5)) {
                    state = getState("CLAIMING_AUCTION");
                } else {
                    state = getState("RESTARTING");
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting! Line 159");
                }
            }

            case "RESTARTING" -> {
                state = getState("IDLE");
                CompletableFuture.runAsync(() -> execute(task));
            }

            case "CLAIMING_AUCTION" -> {
                if (waitBeforeAction()) {
                    return;
                }

                assert client.player != null;
                Utils.sendChatMessage("/ah");

                nextState = getState("CLAIMING_AUCTION_VIEW_BIDS");
                state = getState("WAITING_FOR_MENU");
            }

            case "CLAIMING_AUCTION_VIEW_BIDS" -> {
                if (!asyncClickOrRestart(task.getViewBidsSlotName())) {
                    return;
                }

                nextState = getState("CLAIMING_AUCTION_BID");
                state = getState("WAITING_FOR_MENU");
            }

            case "CLAIMING_AUCTION_BID" -> {
                if (!asyncClickOrRestart(task.getItemName())) {
                    return;
                }

                nextState = getState("CLIAMING_AUCTION_CLAIM");
                state = getState("WAITING_FOR_MENU");
            }

            case "CLIAMING_AUCTION_CLAIM" -> {
                if (!asyncClickOrRestart(task.getCollectAuctionSlotName())) {
                    return;
                }

                state = getState("IDLE");
                task.completed();
            }

            case "WAITING_FOR_MENU" -> waitForMenuOrRestart();

        }
    }
}
