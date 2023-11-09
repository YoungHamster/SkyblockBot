package com.viktorx.skyblockbot.task.base.menuClickingTasks;


import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.utils.CurrentInventory;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.BaseExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMenuClickingExecutor extends BaseExecutor {
    protected final List<String> possibleErrors = new ArrayList<>();

    protected abstract ExecutorState restart();

    protected boolean checkForPossibleError() {
        for (String possibleError : possibleErrors) {
            if (Utils.isStringInRecentChat(possibleError, 1)) {
                return true;
            }
        }
        return false;
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

    protected void asyncCloseCurrentInventory() {
        CompletableFuture.runAsync(this::blockingCloseCurrentInventory);
    }

    protected void blockingCloseCurrentInventory() {
        if (MinecraftClient.getInstance().currentScreen != null) {
            Keybinds.asyncPressKeyAfterTick(InputUtil.GLFW_KEY_ESCAPE);
        }
        while(MinecraftClient.getInstance().currentScreen != null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
    }

    public static class ClickOnSlotOrRestart implements ExecutorState {
        private final AbstractMenuClickingExecutor parent;
        private ExecutorState nextState;
        private final String itemName;
        private boolean currentClickRunning = false;
        private CompletableFuture<Boolean> currentClick;

        /**
         * When using don't forget to set next state, or it will be null
         */
        public ClickOnSlotOrRestart(AbstractMenuClickingExecutor parent, String itemName) {
            this.parent = parent;
            this.itemName = itemName;
        }

        public ClickOnSlotOrRestart setNextState(ExecutorState nextState) {
            this.nextState = nextState;
            return this;
        }

        /*
         * Returns: true if done and clicked successfully, false if not done yet(or restarting)
         * If it restarts it just changes state to Restarting and onTick method goes with it
         */
        public ExecutorState onTick(MinecraftClient client) {
            if (!currentClickRunning) {
                currentClick = CompletableFuture.supplyAsync(() -> waitAndClick(itemName));
                currentClickRunning = true;
            }

            if (!currentClick.isDone()) {
                return this;
            }
            currentClickRunning = false;

            try {
                if (!currentClick.get()) {
                    return parent.restart();
                }
            } catch (InterruptedException | ExecutionException ignored) {}

            return nextState;
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

    }

    public static class WaitingForNamedMenu implements ExecutorState {
        private int waitForMenuCounter = 0;
        private final String menuName;
        private ExecutorState nextState;
        protected final AbstractMenuClickingExecutor parent;
        public WaitingForNamedMenu(AbstractMenuClickingExecutor parent, String menuName) {
            this.parent = parent;
            this.menuName = menuName;
        }

        public WaitingForNamedMenu setNextState(ExecutorState nextState) {
            this.nextState = nextState;
            return this;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(client.currentScreen != null && client.currentScreen.getTitle().getString().contains(menuName)) {
                return nextState;
            } else {
                if(waitForMenuCounter++ > MenuClickersSettings.maxWaitForStuff) {
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
