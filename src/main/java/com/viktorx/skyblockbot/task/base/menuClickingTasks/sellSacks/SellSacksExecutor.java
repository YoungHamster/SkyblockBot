package com.viktorx.skyblockbot.task.base.menuClickingTasks.sellSacks;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.utils.Utils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class SellSacksExecutor extends AbstractMenuClickingExecutor {

    public static SellSacksExecutor INSTANCE = new SellSacksExecutor();

    private SellSacks task;

    SellSacksExecutor() {
        possibleErrors.add("You may only use this command after");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    @Override
    protected synchronized ExecutorState restart() {
        SkyblockBot.LOGGER.info("Restarting sellSacks");
        blockingCloseCurrentInventory();
        execute(task);
        return new Idle();
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        this.task = (SellSacks) task;
        SkyblockBot.LOGGER.info("Executing sell sacks");
        return new SendingCommand();
    }

    protected static class SendingCommand implements ExecutorState {
        private final SellSacksExecutor parent;
        public SendingCommand() {
            this.parent = SellSacksExecutor.INSTANCE;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            Utils.sendChatMessage(parent.task.getCommand());
            return new WaitingForNamedMenu(parent, parent.task.getBZMenuName())
                    .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getSellStacksSlotName())
                        .setNextState(new WaitingForNamedMenu(parent, parent.task.getConfirmMenuName())
                            .setNextState(new ClickOnSlotOrRestart(parent, parent.task.getConfirmSlotName())
                                .setNextState(new WaitingBeforeClosingMenu())
                            )
                        )
                    );
        }
    }

    protected static class WaitingBeforeClosingMenu extends WaitingExecutorState {
        private final SellSacksExecutor parent;
        public WaitingBeforeClosingMenu() {
            this.parent = SellSacksExecutor.INSTANCE;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (waitBeforeAction()) {
                return this;
            }

            parent.asyncCloseCurrentInventory();

            SkyblockBot.LOGGER.info("Sold sacks!");
            return new WaitForMenuToClose(new Complete(parent));
        }
    }

}
