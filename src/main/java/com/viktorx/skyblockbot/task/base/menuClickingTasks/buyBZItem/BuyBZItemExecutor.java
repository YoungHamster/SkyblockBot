package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.IAbstractSignEditScreenMixin;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.MenuClickersSettings;
import com.viktorx.skyblockbot.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;

public class BuyBZItemExecutor extends AbstractMenuClickingExecutor {

    public static BuyBZItemExecutor INSTANCE = new BuyBZItemExecutor();
    private BuyBZItem task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.warn("BuyBZItem restart happened when state was " + state.getClass().getSimpleName());
        CompletableFuture.runAsync(() -> {
            blockingCloseCurrentInventory();
            SkyblockBot.LOGGER.warn("Can't buy " + task.getItemName() + ". Restarting task");
            execute(task);
        });
        return new Idle();
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = (BuyBZItem) task;
        return new SendingCommand();
    }

    public synchronized void onTickBuy(MinecraftClient client) {
        state = state.onTick(client);
    }

    private void typeIntoCurrentScreen(String str) {
        assert MinecraftClient.getInstance().currentScreen != null;
        IAbstractSignEditScreenMixin screen = ((IAbstractSignEditScreenMixin) MinecraftClient.getInstance().currentScreen);
        screen.getMessages()[0] = str;
    }

    protected static class SendingCommand extends WaitingExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }
            Utils.sendChatMessage(parent.task.getBZCommand());

            return new WaitingForNamedMenu(parent, parent.task.getBZMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getSearchItemName())
                            .setNextState(new WaitingForScreenChange()
                                    .setNextState(new Searching())
                            )
                    );
        }
    }

    protected static class Searching extends WaitingExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            parent.typeIntoCurrentScreen(parent.task.getItemName());

            assert client.currentScreen != null;
            client.currentScreen.close();

            /*
             * Depending on amount of items we need to buy execution goes
             * one of two ways. We either buy one item and that's all, or we need to buy more than one
             * so we need to enter custom amount
             */
            ExecutorState buyOneOrEnterAmount;
            if (parent.task.getItemCount() == 1) {
                buyOneOrEnterAmount = new ClickOnSlotOrRestart(parent, parent.task.getBuyOneItemName())
                        .setNextState(new WaitingForItem());
            } else {
                buyOneOrEnterAmount = new ClickOnSlotOrRestart(parent, parent.task.getEnterAmountItemName())
                        .setNextState(new WaitingForScreenChange()
                                .setNextState(new EnteringAmount()
                                )
                        );
            }

            /*
             * No matter what amount of items we need to buy these steps are the same
             */
            return new WaitingForNamedMenu(parent, parent.task.getSearchResultMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getItemName())
                            .setNextState(new WaitingForNamedMenu(parent, parent.task.getItemMenuName())
                                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getBuyInstantlyItemName())
                                            .setNextState(buyOneOrEnterAmount)
                                    )
                            )
                    );
        }
    }

    protected static class EnteringAmount extends WaitingExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            parent.typeIntoCurrentScreen(Integer.toString(parent.task.getItemCount()));

            assert client.currentScreen != null;
            client.currentScreen.close();

            return new WaitingForNamedMenu(parent, parent.task.getBuyCustomAmountMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getBuyCustomAmountItemName())
                            .setNextState(new WaitingForItem()
                            )
                    );
        }
    }

    protected static class WaitingForItem implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;
        private boolean closedInventory = false;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!closedInventory) {
                parent.asyncCloseCurrentInventory();
                closedInventory = true;
            }

            if (SBUtils.isItemInInventory(parent.task.getItemName()) && client.currentScreen == null) {
                parent.task.completed();
                return new Idle();
            }
            return this;
        }
    }

    protected static class WaitingForScreenChange implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;
        protected int waitForScreenLoadingCounter = 0;
        private ExecutorState nextState;

        public WaitingForScreenChange setNextState(ExecutorState nextState) {
            this.nextState = nextState;
            return this;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (client.currentScreen == null) {
                return this;
            }
            if (client.currentScreen.getClass().equals(parent.task.getSearchScreenClass())) {
                return nextState;
            }
            if (waitForScreenLoadingCounter++ > MenuClickersSettings.maxWaitForStuff) {
                return parent.restart();
            }
            return this;
        }
    }
}
