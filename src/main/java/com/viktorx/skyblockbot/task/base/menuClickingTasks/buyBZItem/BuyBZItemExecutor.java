package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.IAbstractSignEditScreenMixin;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
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

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.warn("BuyBZItem restart happened when state was " + state.getClass().getSimpleName());
        state = new Idle();
        CompletableFuture.runAsync(() -> {
            blockingCloseCurrentInventory();
            SkyblockBot.LOGGER.warn("Can't buy " + ((BuyBZItem) task).getItemName() + ". Restarting task");
            execute(task);
        });
        return new Idle();
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = task;
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

            BuyBZItem buyTask = (BuyBZItem) parent.task;

            int freeSpace = 0;
            for(int i = 0; i < GlobalExecutorInfo.inventorySlotCount; i++) {
                if(client.player.getInventory().getStack(i).getName().getString().equals("Air")) {
                    freeSpace += 64;
                }
            }
            if(freeSpace < buyTask.getItemCount()) {
                SkyblockBot.LOGGER.error("Not enough inventory space to fit "
                        + buyTask.getItemCount() + " " + buyTask.getItemName()
                        + ", aborting buyBZItem task. Inventory can fit: " + freeSpace + " items");
                parent.task.aborted();
                return new Idle();
            }

            Utils.sendChatMessage(buyTask.getBZCommand());

            return new WaitingForNamedMenu(parent, buyTask.getBZMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, buyTask.getSearchItemName())
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

            BuyBZItem buyTask = (BuyBZItem) parent.task;
            parent.typeIntoCurrentScreen(buyTask.getItemName());

            assert client.currentScreen != null;
            client.currentScreen.close();

            /*
             * Depending on amount of items we need to buy execution goes
             * one of two ways. We either buy one item and that's all, or we need to buy more than one
             * so we need to enter custom amount
             */
            ExecutorState buyOneOrEnterAmount;
            if (buyTask.getItemCount() == 1) {
                buyOneOrEnterAmount = new ClickOnSlotOrRestart(parent, buyTask.getBuyOneItemName())
                        .setNextState(new WaitingForItem());
            } else {
                buyOneOrEnterAmount = new ClickOnSlotOrRestart(parent, buyTask.getEnterAmountItemName())
                        .setNextState(new WaitingForScreenChange()
                                .setNextState(new EnteringAmount()
                                )
                        );
            }

            /*
             * No matter what amount of items we need to buy these steps are the same
             */
            return new WaitingForNamedMenu(parent, buyTask.getSearchResultMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, buyTask.getItemName())
                        .setNextState(new WaitingForNamedMenu(parent, buyTask.getItemMenuName())
                            .setNextState(new ClickOnSlotOrRestart(parent, buyTask.getBuyInstantlyItemName())
                                .setNextState(new WaitingForNamedMenu(parent, buyTask.getBuyInstantlyMenuName())
                                    .setNextState(buyOneOrEnterAmount)
                                            )
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

            BuyBZItem buyTask = (BuyBZItem) parent.task;
            parent.typeIntoCurrentScreen(Integer.toString(buyTask.getItemCount()));

            assert client.currentScreen != null;
            client.currentScreen.close();

            return new WaitingForNamedMenu(parent, buyTask.getBuyCustomAmountMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, buyTask.getBuyCustomAmountItemName())
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

            BuyBZItem buyTask = (BuyBZItem) parent.task;
            if (SBUtils.isItemInInventory(buyTask.getItemName()) && client.currentScreen == null) {
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

            BuyBZItem buyTask = (BuyBZItem) parent.task;
            if (client.currentScreen.getClass().equals(buyTask.getSearchScreenClass())) {
                return nextState;
            }

            if (waitForScreenLoadingCounter++ > MenuClickersSettings.maxWaitForStuff) {
                return parent.restart();
            }
            return this;
        }
    }
}
