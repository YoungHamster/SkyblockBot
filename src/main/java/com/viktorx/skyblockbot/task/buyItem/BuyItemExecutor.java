package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.mixins.IChatHudMixin;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.skyblock.flipping.auction.AuctionBrowser;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.Task;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class BuyItemExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();

    private BuyItemState state = BuyItemState.IDLE;
    private BuyItemState nextState;
    private BuyItemState stateBeforePause;
    private BuyItem task;
    private final List<String> possibleErrors = Arrays.asList("You cannot view this auction", "This auction wasn't found");
    private int waitTickCounter = 0;
    private CompletableFuture<String> priceFinder;
    private boolean priceFinderRunning = false;
    private CompletableFuture<Boolean> currentClick;
    private boolean currentClickRunning = false;


    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public void execute(BuyItem task) {
        CompletableFuture.runAsync(AuctionBrowser.INSTANCE::loadAH);
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

        switch (state) {
            case LOADING_AUCTIONS -> {
                if (AuctionBrowser.INSTANCE.isAHLoaded()) {
                    state = BuyItemState.LOOKING_FOR_AUCTION;
                }
            }

            case LOOKING_FOR_AUCTION -> {
                state = BuyItemState.SENDING_COMMAND;
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
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting!");
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

                if (isStringInRecentChat("You claimed")) {
                    state = BuyItemState.IDLE;
                    task.completed();
                } else if (isStringInRecentChat("Visit the Auction House")) {
                    state = BuyItemState.CLAIMING_AUCTION;
                } else {
                    state = BuyItemState.RESTARTING;
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting!");
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
                    state = BuyItemState.RESTARTING;
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting!");
                    return;
                }
                if (CurrentInventory.syncIDChanged()) {
                    state = nextState;
                }
            }

        }
    }

    private boolean isStringInRecentChat(String str) {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        List<ChatHudLine<Text>> messages = ((IChatHudMixin) chat).getMessages();
        if (messages.size() == 0) {
            SkyblockBot.LOGGER.warn("BuyItem ERROR! The message history is empty, it's weird");
            return false;
        }

        int limit = 5;
        if(messages.size() < 5) {
            limit = messages.size();
        }

        for(int i = 0; i < limit; i++) {
            if (messages.get(i).getText().getString().contains(str)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkForPossibleError() {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        List<ChatHudLine<Text>> messages = ((IChatHudMixin) chat).getMessages();
        if (messages.size() == 0) {
            SkyblockBot.LOGGER.warn("BuyItem ERROR! The message history is empty, it's weird");
            return true;
        }
        for (String possibleError : possibleErrors) {
            if (messages.get(0).getText().getString().contains(possibleError)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Returns: true if done and clicked successfully, false if not done yet(or restarting)
     * If it restarts it just changes state to Restarting and onTick method goes with it
     */
    private boolean asyncClickOrRestart(String itemName) {
        if (!currentClickRunning) {
            currentClick = CompletableFuture.supplyAsync(() -> waitAndClick(itemName));
            currentClickRunning = true;
        }

        if (!currentClick.isDone()) {
            return false;
        }
        currentClickRunning = false;

        try {
            if (!currentClick.get()) {
                closeCurrentInvetory();
                SkyblockBot.LOGGER.warn("Can't buy auction. Restarting task");
                state = BuyItemState.RESTARTING;
                return false;
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }

        return true;
    }

    private void closeCurrentInvetory() {
        if (MinecraftClient.getInstance().currentScreen != null) {
            Keybinds.blockingPressKey(MinecraftClient.getInstance().options.inventoryKey);
        }

    }

    private boolean waitAndClick(String slotName) {
        try {
            Thread.sleep(getTimeToWaitBeforeClick());
        } catch (InterruptedException ignored) {
        }

        try {
            SBUtils.leftClickOnSlot(slotName);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /*
     * This class waits half of what other classes wait,
     * because I don't want to lose too many auctions to bots and fast players
     */
    private boolean waitBeforeCommand() {
        if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeClick) {
            return true;
        }
        waitTickCounter = 0;
        return false;
    }

    private long getTimeToWaitBeforeClick() {
        return GlobalExecutorInfo.waitTicksBeforeClick * 50;
    }
}
