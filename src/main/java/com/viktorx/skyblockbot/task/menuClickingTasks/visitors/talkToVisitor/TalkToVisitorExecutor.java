package com.viktorx.skyblockbot.task.menuClickingTasks.visitors.talkToVisitor;

import com.viktorx.skyblockbot.CurrentInventory;
import com.viktorx.skyblockbot.RayTraceStuff;
import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class TalkToVisitorExecutor extends AbstractMenuClickingExecutor {
    public static final TalkToVisitorExecutor INSTANCE = new TalkToVisitorExecutor();

    private TalkToVisitorState state = TalkToVisitorState.IDLE;
    private TalkToVisitorState stateBeforePause;
    private TalkToVisitor task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    protected void restart() {
        SkyblockBot.LOGGER.warn("TalkToVisitor task isn't meant to be restarted! Aborting");
        abort();
    }

    public void execute(TalkToVisitor task) {
        if (!state.equals(TalkToVisitorState.IDLE)) {
            SkyblockBot.LOGGER.info("Can't execute talkToVisitor when already executing");
            return;
        }
        SkyblockBot.LOGGER.info("Executing talk to visitor!");
        this.task = task;
        state = TalkToVisitorState.WAITING_FOR_VISITOR;
    }

    public void pause() {
        if (state.equals(TalkToVisitorState.IDLE) || state.equals(TalkToVisitorState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't pause talkToVisitor when already paused or not executing at all");
            return;
        }
        stateBeforePause = state;
        state = TalkToVisitorState.PAUSED;
    }

    public void resume() {
        if (!state.equals(TalkToVisitorState.PAUSED)) {
            SkyblockBot.LOGGER.warn("Can't resume talkToVisitor when not paused");
            return;
        }
        state = stateBeforePause;
    }

    public void abort() {
        state = TalkToVisitorState.IDLE;
    }

    public boolean isExecuting(TalkToVisitor task) {
        return !state.equals(TalkToVisitorState.IDLE) && this.task == task;
    }

    public boolean isPaused() {
        return state.equals(TalkToVisitorState.PAUSED);
    }

    private void onTick(MinecraftClient client) {
        switch (state) {
            case WAITING_FOR_VISITOR -> {
                assert client.world != null;
                if (RayTraceStuff.rayTraceEntityFromPlayer(client.player, client.world, 4.0d) != null) {
                    state = TalkToVisitorState.CLICKING_ON_VISITOR;
                }
            }

            case CLICKING_ON_VISITOR -> {
                if (!waitBeforeCommand()) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    state = TalkToVisitorState.CLICKING_ON_VISITOR_SECOND_TIME;
                }
            }

            case CLICKING_ON_VISITOR_SECOND_TIME -> {
                if (!waitBeforeCommand()) {
                    Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                    state = TalkToVisitorState.WAITING_FOR_MENU;
                }
            }

            case WAITING_FOR_MENU -> {
                if (CurrentInventory.syncIDChanged()) {
                    state = TalkToVisitorState.READING_DATA;
                }
            }

            case READING_DATA -> {
                List<String> lore;
                try {
                    lore = SBUtils.getSlotLore(task.getAcceptOfferStr());
                } catch (TimeoutException e) {
                    SkyblockBot.LOGGER.warn("Can't read data from visitor, timeout exception! Aborting task");
                    abort();
                    return;
                }

                if(lore == null) {
                    SkyblockBot.LOGGER.warn("Lore is null when executing talkToVisitor task, wtf???");
                    abort();
                    return;
                }

                assert client.currentScreen != null;
                task.setVisitorName(client.currentScreen.getTitle().getString());

                String[] nameCount = lore.get(1).split(" x");
                task.setItemName(nameCount[0].strip());
                if(nameCount.length != 1) {
                    task.setItemCount(Integer.parseInt(nameCount[1]));
                } else {
                    task.setItemCount(1);
                }

                SkyblockBot.LOGGER.info("Visitor: " + task.getVisitorName() + ", name: " + task.getItemName() + ", itemCount: " + task.getItemCount());

                state = TalkToVisitorState.CLOSING_VISITOR;
            }

            case CLOSING_VISITOR -> {
                if (!waitBeforeCommand()) {
                    asyncCloseCurrentInventory();
                    state = TalkToVisitorState.IDLE;
                    task.completed();
                }
            }
        }
    }
}
