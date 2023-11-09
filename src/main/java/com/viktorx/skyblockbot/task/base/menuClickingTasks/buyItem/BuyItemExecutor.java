package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.skyblock.flipping.auction.AuctionBrowser;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.MenuClickersSettings;
import com.viktorx.skyblockbot.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BuyItemExecutor extends AbstractMenuClickingExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();
    private BuyItem task;
    private CompletableFuture<String> priceFinder;
    private boolean priceFinderRunning = false;

    BuyItemExecutor() {
        possibleErrors.add("You cannot view this auction");
        possibleErrors.add("This auction wasn't found");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.warn("BuyItem restart happened when state was " + state.getClass().getSimpleName());
        CompletableFuture.runAsync(() -> {
            blockingCloseCurrentInventory();
            SkyblockBot.LOGGER.warn("Can't buy from auction. Restarting task");
            execute(task);
        });
        return new Idle();
    }

    @Override
    public synchronized  <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        CompletableFuture.runAsync(AuctionBrowser.INSTANCE::loadAH);
        priceFinderRunning = false;
        this.task = (BuyItem) task;
        return new LoadingAuctions();
    }

    public synchronized void onTickBuy(MinecraftClient client) {
        state = state.onTick(client);
    }

    protected static class LoadingAuctions implements ExecutorState {
        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (AuctionBrowser.INSTANCE.isAHLoaded()) {
                return new SendingCommand();
            }
            return this;
        }
    }

    protected static class SendingCommand extends WaitingExecutorState {
        private final BuyItemExecutor parent = BuyItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.priceFinderRunning) {
                parent.priceFinder =
                        CompletableFuture.supplyAsync(
                                () -> AuctionBrowser.INSTANCE.getAuctionWithBestPrice(
                                        parent.task.getItemName(),
                                        parent.task.getLoreKeyWords(),
                                        parent.task.getPriceLimit()));
                parent.priceFinderRunning = true;
            }

            if (!parent.priceFinder.isDone()) {
                return this;
            }

            if (waitBeforeAction()) {
                return this;
            }

            parent.priceFinderRunning = false;
            String auctionCommand;

            try {
                auctionCommand = parent.priceFinder.get();
            } catch (InterruptedException | ExecutionException e) {
                SkyblockBot.LOGGER.info("Some weird exception when buying item.");
                parent.task.aborted();
                return new Idle();
            }

            if (auctionCommand == null) {
                SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting!");
                return parent.restart();
            }

            assert client.player != null;
            Utils.sendChatMessage(auctionCommand);

            return new WaitingForNamedMenu(parent, parent.task.getAHMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getBuySlotName())
                        .setNextState(new WaitingForNamedMenu(parent, parent.task.getConfirmMenuName())
                            .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getConfirmSlotName())
                                .setNextState(new CheckingBuyResult())
                            )
                        )
                    );

        }
    }

    protected static class CheckingBuyResult extends WaitingExecutorState {
        private final BuyItemExecutor parent = BuyItemExecutor.INSTANCE;
        private int waitForResultCounter = 0;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (Utils.isStringInRecentChat("You claimed", 5)) {
                parent.task.completed();
                return new Idle();
            } else if (Utils.isStringInRecentChat("Visit the Auction House", 5)) {
                return new ClaimingAuction();
            } else {
                if(waitForResultCounter++ >= MenuClickersSettings.maxWaitForStuff) {
                    SkyblockBot.LOGGER.warn("Error when buying item from ah. Restarting! Line 159");
                    return parent.restart();
                } else {
                    return this;
                }
            }
        }
    }

    protected static class ClaimingAuction extends WaitingExecutorState {
        private final BuyItemExecutor parent = BuyItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {

            if (waitBeforeAction()) {
                return this;
            }

            assert client.player != null;
            Utils.sendChatMessage("/ah");

            return new WaitingForNamedMenu(parent, parent.task.getViewBidsMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getViewBidsSlotName())
                        .setNextState(new WaitingForNamedMenu(parent, parent.task.getViewBidsMenuName())
                            .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getItemName())
                                .setNextState(new WaitingForNamedMenu(parent, parent.task.getClaimMenuName())
                                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getCollectAuctionSlotName())
                                        .setNextState(new WaitingForItem())
                                    )
                                )
                            )
                        )
                    );
        }
    }

    protected static class WaitingForItem implements ExecutorState {
        private final BuyItemExecutor parent = BuyItemExecutor.INSTANCE;
        private int waitForItemCounter = 0;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (SBUtils.isItemInInventory(parent.task.getItemName())) {
                return new WaitForMenuToClose(new Complete(parent));
            } else {
                if(waitForItemCounter++ >= MenuClickersSettings.maxWaitForStuff) {
                    return parent.restart();
                }
                return this;
            }
        }
    }
}
