package com.viktorx.skyblockbot.task.base.menuClickingTasks;


import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.utils.CurrentInventory;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMenuClickingExecutor extends BaseExecutor {
    protected final List<String> possibleErrors = new ArrayList<>();
    protected boolean currentClickRunning = false;
    protected int waitForMenuCounter = 0;
    private CompletableFuture<Boolean> currentClick;

    protected abstract ExecutorState restart();
    protected abstract ExecutorState whenMenuOpened();

    protected boolean checkForPossibleError() {
        for (String possibleError : possibleErrors) {
            if (Utils.isStringInRecentChat(possibleError, 1)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Returns: true if done and clicked successfully, false if not done yet(or restarting)
     * If it restarts it just changes state to Restarting and onTick method goes with it
     */
    protected boolean asyncClickOrRestart(String itemName) {
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
                restart();
                return false;
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }

        return true;
    }

    protected void asyncCloseCurrentInventory() {
        CompletableFuture.runAsync(this::blockingCloseCurrentInventory);
    }

    protected void blockingCloseCurrentInventory() {
        if (MinecraftClient.getInstance().currentScreen != null) {
            Keybinds.blockingPressKey(MinecraftClient.getInstance().options.inventoryKey);
        }
        while(MinecraftClient.getInstance().currentScreen != null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
    }

    protected boolean waitAndClick(String slotName) {
        try {
            Thread.sleep(getTimeToWaitBeforeClick());
        } catch (InterruptedException ignored) {
        }

        try {
            /*
             * This line is here to clear info about sync id changes before button click
             * Otherwise sometimes we get false-positives when checking if the menu was opened after clicking on slot,
             * that is supposed to open new menu in different menu tasks
             */
            CurrentInventory.syncIDChanged();

            SBUtils.leftClickOnSlot(slotName);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected long getTimeToWaitBeforeClick() {
        return GlobalExecutorInfo.waitTicksBeforeAction * 50L;
    }

    public static abstract class WaitingExecutorState implements ExecutorState {
        protected int waitTickCounter = 0;

        protected boolean waitBeforeAction() {
            if (waitTickCounter++ < GlobalExecutorInfo.waitTicksBeforeAction) {
                return true;
            }
            waitTickCounter = 0;
            return false;
        }
    }

    public static class WaitingForMenu implements ExecutorState {
        private int waitForMenuCounter = 0;
        protected final AbstractMenuClickingExecutor parent;

        public WaitingForMenu(AbstractMenuClickingExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(CurrentInventory.syncIDChanged()) {
                return parent.whenMenuOpened();
            } else {
                if(waitForMenuCounter++ > MenuClickersSettings.maxWaitForScreen) {
                    return parent.restart();
                }
            }
            return this;
        }
    }

    public static class WaitForMenuToClose implements ExecutorState {
        private final ExecutorState nextState;

        public WaitForMenuToClose(ExecutorState nextState) {
            this.nextState = nextState;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(client.currentScreen == null) {
                return nextState;
            }
            return this;
        }
    }

    public static class Complete implements ExecutorState {
        private final AbstractMenuClickingExecutor parent;
        public Complete(AbstractMenuClickingExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            parent.task.completed();
            return new Idle();
        }
    }
}
