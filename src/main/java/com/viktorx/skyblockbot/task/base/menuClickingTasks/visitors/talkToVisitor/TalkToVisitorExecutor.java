package com.viktorx.skyblockbot.task.base.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.RayTraceStuff;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import javafx.util.Pair;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class TalkToVisitorExecutor extends AbstractMenuClickingExecutor {
    public static final TalkToVisitorExecutor INSTANCE = new TalkToVisitorExecutor();

    private TalkToVisitor task;

    private TalkToVisitorExecutor() {
        addState("WAITING_FOR_VISITOR");
        addState("CLICKING_ON_VISITOR");
        addState("CLICKING_ON_VISITOR_SECOND_TIME");
        addState("WAITING_FOR_MENU");
        addState("READING_DATA");
        addState("CLOSING_VISITOR");
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    @Override
    protected void restart() {
        SkyblockBot.LOGGER.warn("TalkToVisitor task isn't meant to be restarted! Aborting");
        abort();
    }

    @Override
    protected void whenMenuOpened() {
        state = getState("READING_DATA");
    }

    @Override
    public <T extends BaseTask<?>> void whenExecute(T task) {
        SkyblockBot.LOGGER.info("Executing talk to visitor!");
        this.task = (TalkToVisitor) task;
        state = getState("WAITING_FOR_VISITOR");
    }

    private void onTick(MinecraftClient client) {
        switch (getState(state)) {
            case "WAITING_FOR_VISITOR" -> {
                assert client.world != null;
                if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4.0d) != null) {
                    state = getState("CLICKING_ON_VISITOR");
                }
            }

            case "CLICKING_ON_VISITOR" -> {
                if (!waitBeforeAction()) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    state = getState("CLICKING_ON_VISITOR_SECOND_TIME");
                }
            }

            case "CLICKING_ON_VISITOR_SECOND_TIME" -> {
                if (!waitBeforeAction()) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    state = getState("WAITING_FOR_MENU");
                }
            }

            case "WAITING_FOR_MENU" -> waitForMenuOrRestart();

            case "READING_DATA" -> {
                List<String> lore;
                try {
                    lore = SBUtils.getSlotLore(task.getAcceptOfferStr());
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

                assert client.currentScreen != null;
                task.setVisitorName(client.currentScreen.getTitle().getString());

                /*
                 * visitors can have more than one item required
                 */
                int visitorRequiredItemIterator = 1;
                while (lore.get(visitorRequiredItemIterator).contains(" x")) {
                    String[] nameCount = lore.get(visitorRequiredItemIterator).split(" x");

                    String name = nameCount[0].strip();
                    int count = 1;
                    if (nameCount.length != 1) {
                        count = Integer.parseInt(nameCount[1]);
                    }

                    task.addItem(new Pair<>(name, count));
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
