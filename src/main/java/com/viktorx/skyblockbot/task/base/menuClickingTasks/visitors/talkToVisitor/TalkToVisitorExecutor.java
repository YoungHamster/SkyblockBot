package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.AbstractVisitorExecutor;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import javafx.util.Pair;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class TalkToVisitorExecutor extends AbstractVisitorExecutor {
    public static final TalkToVisitorExecutor INSTANCE = new TalkToVisitorExecutor();

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected synchronized ExecutorState restart() {
        keepTracking.set(false);
        return execute(task);
    }

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        SkyblockBot.LOGGER.info("Executing talk to visitor!");
        this.task = (TalkToVisitor) task;
        return new StartTrackingVisitor(this);
    }

    @Override
    protected ExecutorState getStateWhenVisitorOpened() {
        return new ReadingData(this);
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    protected static class ReadingData implements ExecutorState {
        private final TalkToVisitorExecutor parent;

        public ReadingData(TalkToVisitorExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            List<String> lore;
            try {
                lore = SBUtils.getSlotLore(((TalkToVisitor) parent.task).getAcceptOfferStr());
            } catch (TimeoutException e) {
                SkyblockBot.LOGGER.warn("Can't read data from visitor, timeout exception! Aborting task");
                parent.abort();
                return new Idle();
            }

            if (lore == null) {
                SkyblockBot.LOGGER.warn("Lore is null when executing talkToVisitor task, wtf???");
                parent.abort();
                return new Idle();
            }

            /*
             * visitors can have more than one item required
             * we loop through lore strings until there's an empty one
             */
            int visitorRequiredItemIterator = 1;
            while (lore.get(visitorRequiredItemIterator).length() > 0) {
                String[] nameCount = lore.get(visitorRequiredItemIterator).split(" x");
                visitorRequiredItemIterator++;

                String name = nameCount[0].strip();
                int count = 1;
                if (nameCount.length != 1) {
                    count = Integer.parseInt(nameCount[1]);
                }

                ((TalkToVisitor) parent.task).addItem(new Pair<>(name, count));
            }

            return new ClosingVisitor(parent);
        }
    }

    protected static class ClosingVisitor extends WaitingExecutorState {
        private final TalkToVisitorExecutor parent;

        public ClosingVisitor(TalkToVisitorExecutor parent) {
            this.parent = parent;
        }

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if (!waitBeforeAction()) {
                parent.asyncCloseCurrentInventory();
                return new WaitForMenuToClose(new Complete(parent));
            }
            return this;
        }
    }
}
