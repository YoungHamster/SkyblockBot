package com.viktorx.skyblockbot.task.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.mixins.ISelectionManagerMixin;
import com.viktorx.skyblockbot.mixins.ISignEditScreenMixin;
import com.viktorx.skyblockbot.task.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.menuClickingTasks.BuySellSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

public class BuyBZItemExecutor extends AbstractMenuClickingExecutor {

    public static BuyBZItemExecutor INSTANCE = new BuyBZItemExecutor();
    protected int waitForScreenLoadingCounter = 0;
    private BuyBZItemState state = BuyBZItemState.IDLE;
    private BuyBZItemState nextState;
    private BuyBZItemState prevState;
    private BuyBZItemState stateBeforePause;
    private BuyBZItem task;
    private int waitBetweenLettersCounter = 0;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    protected void restart() {
        blockingCloseCurrentInventory();
        SkyblockBot.LOGGER.warn("Can't buy " + task.getItemName() + ". Restarting task");
        state = BuyBZItemState.RESTARTING;
    }

    public void execute(BuyBZItem task) {
        if (!state.equals(BuyBZItemState.IDLE)) {
            SkyblockBot.LOGGER.warn("Can't execute buyBZItem, already running");
            return;
        }

        this.task = task;
        waitBetweenLettersCounter = 0;
        waitTickCounter = 0;
        currentClickRunning = false;
        state = BuyBZItemState.SENDING_COMMAND;
    }

    public void pause() {
        if (state.equals(BuyBZItemState.IDLE) || state.equals(BuyBZItemState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't pause buyBZItem when not running or paused");
            return;
        }

        stateBeforePause = state;
        state = BuyBZItemState.PAUSED;
    }

    public void resume() {
        if (state.equals(BuyBZItemState.PAUSED)) {
            state = stateBeforePause;
        } else {
            SkyblockBot.LOGGER.warn("Can't resume when not paused!");
        }
    }

    public void abort() {
        state = BuyBZItemState.IDLE;
    }

    public boolean isExecuting(BuyBZItem task) {
        return !state.equals(BuyBZItemState.IDLE) && this.task.equals(task);
    }

    public boolean isPaused() {
        return state.equals(BuyBZItemState.PAUSED);
    }

    public void onTickBuy(MinecraftClient client) {
        switch (state) {
            case SENDING_COMMAND -> {
                if (waitBeforeCommand()) {
                    return;
                }

                assert client.player != null;
                client.player.sendChatMessage(task.getBZCommand());

                nextState = BuyBZItemState.CLICKING_TO_SEARCH;
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case CLICKING_TO_SEARCH -> {
                if (!asyncClickOrRestart(task.getSearchItemName())) {
                    return;
                }

                nextState = BuyBZItemState.SEARCHING;
                state = BuyBZItemState.WAITING_FOR_SCREEN_CHANGE;
            }

            case SEARCHING -> {
                if (waitBetweenLetters()) {
                    return;
                }

                typeIntoCurrentScreen(task.getItemName());

                assert client.currentScreen != null;
                client.currentScreen.close();

                nextState = BuyBZItemState.CLICKING_ON_ITEM;
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case CLICKING_ON_ITEM -> {
                if (!asyncClickOrRestart(task.getItemName())) {
                    return;
                }

                nextState = BuyBZItemState.CLICKING_BUY_INSTANTLY;
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case CLICKING_BUY_INSTANTLY -> {
                if (!asyncClickOrRestart(task.getBuyInstantlyItemName())) {
                    return;
                }

                if (task.getItemCount() == 1) {
                    nextState = BuyBZItemState.BUYING_ONE;
                } else {
                    nextState = BuyBZItemState.CLICKING_ENTER_AMOUNT;
                }
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case BUYING_ONE -> {
                if (!asyncClickOrRestart(task.getBuyOneItemName())) {
                    return;
                }

                asyncCloseCurrentInventory();

                nextState = BuyBZItemState.COMPLETED;
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case CLICKING_ENTER_AMOUNT -> {
                if (!asyncClickOrRestart(task.getEnterAmountItemName())) {
                    return;
                }

                prevState = state;
                nextState = BuyBZItemState.ENTERING_AMOUNT;
                state = BuyBZItemState.WAITING_FOR_SCREEN_CHANGE;
            }

            case ENTERING_AMOUNT -> {
                if (waitBetweenLetters()) {
                    return;
                }

                typeIntoCurrentScreen(Integer.toString(task.getItemCount()));

                assert client.currentScreen != null;
                client.currentScreen.close();

                nextState = BuyBZItemState.BUYING_CUSTOM_AMOUNT;
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case BUYING_CUSTOM_AMOUNT -> {
                if (!asyncClickOrRestart(task.getBuyCustomAmountItemName())) {
                    return;
                }

                asyncCloseCurrentInventory();

                nextState = BuyBZItemState.COMPLETED;
                state = BuyBZItemState.WAITING_FOR_MENU;
            }

            case RESTARTING -> {
                state = BuyBZItemState.IDLE;
                CompletableFuture.runAsync(() -> execute(task));
            }

            case WAITING_FOR_MENU -> {
                if (CurrentInventory.syncIDChanged()) {
                    state = nextState;
                }
            }

            case WAITING_FOR_SCREEN_CHANGE -> {
                if (client.currentScreen == null) {
                    return;
                }
                if (client.currentScreen.getClass().equals(task.getSearchScreenClass())) {
                    state = nextState;
                    return;
                }
                if (waitForScreenLoadingCounter++ > BuySellSettings.maxWaitForScreen) {
                    state = prevState;
                }
            }

            case COMPLETED -> {
                SkyblockBot.LOGGER.info("BuyBZItem completed!");
                state = BuyBZItemState.IDLE;
                task.completed();
            }
        }
    }

    private void typeIntoCurrentScreen(String str) {
        assert MinecraftClient.getInstance().currentScreen != null;
        ISignEditScreenMixin screen = ((ISignEditScreenMixin) MinecraftClient.getInstance().currentScreen);
        screen.getText()[0] = str;
    }

    /*
     * wait 3 ticks between letters
     */
    private boolean waitBetweenLetters() {
        if (waitBetweenLettersCounter++ < BuySellSettings.waitTicksBetweenLetters) {
            return true;
        }
        waitBetweenLettersCounter = 0;
        return false;
    }
}
