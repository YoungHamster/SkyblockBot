package com.viktorx.skyblockbot.task.base.menuClickingTasks.buyBZItem;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.mixins.IAbstractSignEditScreenMixin;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.MenuClickersSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;

public class BuyBZItemExecutor extends AbstractMenuClickingExecutor {

    public static BuyBZItemExecutor INSTANCE = new BuyBZItemExecutor();
    protected int waitForScreenLoadingCounter = 0;
    private int nextState;
    private int prevState;
    private BuyBZItem task;

    private BuyBZItemExecutor() {
        addState("SENDING_COMMAND");
        addState("CLICKING_TO_SEARCH");
        addState("SEARCHING");
        addState("CLICKING_ON_ITEM");
        addState("CLICKING_BUY_INSTANTLY");
        addState("CLICKING_ENTER_AMOUNT");
        addState("ENTERING_AMOUNT");
        addState("BUYING_ONE");
        addState("BUYING_CUSTOM_AMOUNT");
        addState("WAITING_FOR_MENU");
        addState("WAITING_FOR_SCREEN_CHANGE");
        addState("RESTARTING");
        addState("WAITING_FOR_ITEM");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    @Override
    protected void restart() {
        blockingCloseCurrentInventory();
        SkyblockBot.LOGGER.warn("Can't buy " + task.getItemName() + ". Restarting task");
        state = getState("RESTARTING");
    }

    @Override
    protected void whenMenuOpened() {
        state = nextState;
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        this.task = (BuyBZItem) task;
        waitTickCounter = 0;
        currentClickRunning = false;
        state = getState("SENDING_COMMAND");
    }

    public void onTickBuy(MinecraftClient client) {
        switch (getState(state)) {
            case "SENDING_COMMAND" -> {
                if (waitBeforeAction()) {
                    return;
                }

                assert client.player != null;
                Utils.sendChatMessage(task.getBZCommand());

                nextState = getState("CLICKING_TO_SEARCH");
                state = getState("WAITING_FOR_MENU");
            }

            case "CLICKING_TO_SEARCH" -> {
                if (!asyncClickOrRestart(task.getSearchItemName())) {
                    return;
                }

                nextState = getState("SEARCHING");
                state = getState("WAITING_FOR_SCREEN_CHANGE");
            }

            case "SEARCHING" -> {
                if (waitBeforeAction()) {
                    return;
                }

                typeIntoCurrentScreen(task.getItemName());

                assert client.currentScreen != null;
                client.currentScreen.close();

                nextState = getState("CLICKING_ON_ITEM");
                state = getState("WAITING_FOR_MENU");
            }

            case "CLICKING_ON_ITEM" -> {
                if (!asyncClickOrRestart(task.getItemName())) {
                    return;
                }

                nextState = getState("CLICKING_BUY_INSTANTLY");
                state = getState("WAITING_FOR_MENU");
            }

            case "CLICKING_BUY_INSTANTLY" -> {
                if (!asyncClickOrRestart(task.getBuyInstantlyItemName())) {
                    return;
                }

                if (task.getItemCount() == 1) {
                    nextState = getState("BUYING_ONE");
                } else {
                    nextState = getState("CLICKING_ENTER_AMOUNT");
                }
                state = getState("WAITING_FOR_MENU");
            }

            case "BUYING_ONE" -> {
                if (!asyncClickOrRestart(task.getBuyOneItemName())) {
                    return;
                }

                asyncCloseCurrentInventory();

                state = getState("WAITING_FOR_ITEM");
            }

            case "CLICKING_ENTER_AMOUNT" -> {
                if (!asyncClickOrRestart(task.getEnterAmountItemName())) {
                    return;
                }

                prevState = state;
                nextState = getState("ENTERING_AMOUNT");
                state = getState("WAITING_FOR_SCREEN_CHANGE");
            }

            case "ENTERING_AMOUNT" -> {
                if (waitBeforeAction()) {
                    return;
                }

                typeIntoCurrentScreen(Integer.toString(task.getItemCount()));

                assert client.currentScreen != null;
                client.currentScreen.close();

                nextState = getState("BUYING_CUSTOM_AMOUNT");
                state = getState("WAITING_FOR_MENU");
            }

            case "BUYING_CUSTOM_AMOUNT" -> {
                if (!asyncClickOrRestart(task.getBuyCustomAmountItemName())) {
                    return;
                }

                asyncCloseCurrentInventory();

                state = getState("WAITING_FOR_ITEM");
            }

            case "WAITING_FOR_ITEM" -> {
                if(SBUtils.isItemInInventory(task.getItemName())) {
                    state = getState("IDLE");
                    task.completed();

                }
            }

            case "RESTARTING" -> {
                state = getState("IDLE");
                CompletableFuture.runAsync(() -> execute(task));
            }

            case "WAITING_FOR_MENU" -> waitForMenuOrRestart();

            case "WAITING_FOR_SCREEN_CHANGE" -> {
                if (client.currentScreen == null) {
                    return;
                }
                if (client.currentScreen.getClass().equals(task.getSearchScreenClass())) {
                    state = nextState;
                    return;
                }
                if (waitForScreenLoadingCounter++ > MenuClickersSettings.maxWaitForScreen) {
                    state = prevState;
                }
            }
        }
    }

    private void typeIntoCurrentScreen(String str) {
        assert MinecraftClient.getInstance().currentScreen != null;
        IAbstractSignEditScreenMixin screen = ((IAbstractSignEditScreenMixin) MinecraftClient.getInstance().currentScreen);
        screen.getMessages()[0] = str;
    }
}
