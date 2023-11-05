package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.AbstractVisitorExecutor;
import javafx.util.Pair;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class TalkToVisitorExecutor extends AbstractVisitorExecutor {
    public static final TalkToVisitorExecutor INSTANCE = new TalkToVisitorExecutor();


    private TalkToVisitorExecutor() {
        addState("START_TRACKING_VISITOR");
        addState("TRACKING_VISITOR");
        addState("WAITING_FOR_MENU");
        addState("READING_DATA");
        addState("CLOSING_VISITOR");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void restart() {
        keepTracking.set(false);
        state = getState("IDLE");
        execute(task);
    }

    @Override
    protected void whenMenuOpened() {
        state = getState("READING_DATA");
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        SkyblockBot.LOGGER.info("Executing talk to visitor!");
        this.task = (TalkToVisitor) task;
        state = getState("START_TRACKING_VISITOR");
    }

    private void onTick(MinecraftClient client) {
        switch (getState(state)) {
            case "START_TRACKING_VISITOR" -> whenStartTrackingVisitor();

            case "TRACKING_VISITOR" -> whenTrackingVisitor(client);

            case "WAITING_FOR_MENU" -> waitForMenuOrRestart();

            case "READING_DATA" -> {
                List<String> lore;
                try {
                    lore = SBUtils.getSlotLore(((TalkToVisitor) task).getAcceptOfferStr());
                } catch (TimeoutException e) {
                    SkyblockBot.LOGGER.warn("Can't read data from visitor, timeout exception! Aborting task");
                    abort();
                    return;
                }

                if (lore == null) {
                    SkyblockBot.LOGGER.warn("Lore is null when executing talkToVisitor task, wtf???");
                    abort();
                    return;
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

                    ((TalkToVisitor) task).addItem(new Pair<>(name, count));
                }

                state = getState("CLOSING_VISITOR");
            }

            case "CLOSING_VISITOR" -> {
                if (!waitBeforeAction()) {
                    asyncCloseCurrentInventory();
                    state = getState("IDLE");
                    task.completed();
                }
            }
        }
    }
}
