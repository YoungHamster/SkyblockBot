package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.IAbstractSignEditScreenMixin;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.MenuClickersSettings;
import com.viktorx.skyblockbot.task.base.replay.ExecutorState;
import com.viktorx.skyblockbot.utils.CurrentInventory;
import com.viktorx.skyblockbot.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;

public class BuyBZItemExecutor extends AbstractMenuClickingExecutor {

    public static BuyBZItemExecutor INSTANCE = new BuyBZItemExecutor();
    private ExecutorState nextState;
    private ExecutorState prevState;
    private BuyBZItem task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.warn("BuyBZItem restart happened when state was " + state.getClass().getSimpleName() +
                " and next state was " + nextState.getClass().getSimpleName());
        CompletableFuture.runAsync(() -> {
            blockingCloseCurrentInventory();
            SkyblockBot.LOGGER.warn("Can't buy " + task.getItemName() + ". Restarting task");
            execute(task);
        });
        return new Idle();
    }

    @Override
    protected ExecutorState whenMenuOpened() {
        return nextState;
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = (BuyBZItem) task;
        currentClickRunning = false;
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

            parent.nextState = new ClickingToSearch();
            return new WaitingForMenu(parent);
        }
    }

    protected static class ClickingToSearch implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.asyncClickOrRestart(parent.task.getSearchItemName())) {
                return this;
            }

            parent.nextState = new Searching();
            return new WaitingForScreenChange();
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

            parent.nextState = new ClickingOnItem();
            return new WaitingForMenu(parent);
        }
    }

    protected static class ClickingOnItem implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.asyncClickOrRestart(parent.task.getItemName())) {
                return this;
            }

            parent.nextState = new ClickingBuyInstantly();
            return new WaitingForMenu(parent);
        }
    }

    protected static class ClickingBuyInstantly implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.asyncClickOrRestart(parent.task.getBuyInstantlyItemName())) {
                return this;
            }

            if (parent.task.getItemCount() == 1) {
                parent.nextState = new BuyingOne();
            } else {
                parent.nextState = new ClickingEnterAmount();
            }
            return new WaitingForMenu(parent);
        }
    }

    protected static class BuyingOne implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.asyncClickOrRestart(parent.task.getBuyOneItemName())) {
                return this;
            }

            parent.asyncCloseCurrentInventory();

            return new WaitingForItem();
        }
    }

    protected static class ClickingEnterAmount implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.asyncClickOrRestart(parent.task.getEnterAmountItemName())) {
                return this;
            }

            parent.prevState = this;
            parent.nextState = new EnteringAmount();
            return new WaitingForScreenChange();
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

            CurrentInventory.syncIDChanged(); // Reseting this for reasons
            assert client.currentScreen != null;
            client.currentScreen.close();

            parent.nextState = new BuyingCustomAmount();
            return new WaitingForMenu(parent);
        }
    }

    protected static class BuyingCustomAmount implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!parent.asyncClickOrRestart(parent.task.getBuyCustomAmountItemName())) {
                return this;
            }

            parent.asyncCloseCurrentInventory();

            return new WaitingForItem();
        }
    }

    protected static class WaitingForItem implements ExecutorState {
        private final BuyBZItemExecutor parent = BuyBZItemExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
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

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (client.currentScreen == null) {
                return this;
            }
            if (client.currentScreen.getClass().equals(parent.task.getSearchScreenClass())) {
                return parent.nextState;
            }
            if (waitForScreenLoadingCounter++ > MenuClickersSettings.maxWaitForScreen) {
                return parent.prevState;
            }
            return this;
        }
    }
}
