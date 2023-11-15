package com.viktorx.skyblockbot.task.base.menuClickingTasks.composter.getInfo;

import com.viktorx.skyblockbot.SkyblockBot;
import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.skyblock.SBUtils;
import com.viktorx.skyblockbot.task.base.BaseTask;
import com.viktorx.skyblockbot.task.base.ExecutorState;
import com.viktorx.skyblockbot.task.base.menuClickingTasks.AbstractMenuClickingExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class GetComposterInfoExecutor extends AbstractMenuClickingExecutor {
    public static GetComposterInfoExecutor INSTANCE = new GetComposterInfoExecutor();

    @Override
    public <T extends BaseTask<?>> ExecutorState whenExecute(T task) {
        return new Clicking();
    }

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
    }

    private synchronized void onTick(MinecraftClient client) {
        state = state.onTick(client);
    }

    @Override
    protected ExecutorState restart() {
        CompletableFuture.runAsync(() -> {
            blockingCloseCurrentInventory();
            state = new Clicking();
        });
        return new Idle();
    }

    private static class Clicking extends WaitingExecutorState {
        private final GetComposterInfoExecutor parent = GetComposterInfoExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(!waitBeforeAction()) {
                Keybinds.asyncPressKeyAfterTick(client.options.useKey);
                return new WaitingForNamedMenu(parent, ((GetComposterInfo) parent.task).getComposterMenuName())
                        .setNextState(new ReadingComposterData());
            }
            return this;
        }
    }

    private static class ReadingComposterData extends WaitingExecutorState {
        private final GetComposterInfoExecutor parent = GetComposterInfoExecutor.INSTANCE;

        @Override
        public ExecutorState onTick(MinecraftClient client) {
            if(waitBeforeAction()) {
                return this;
            }

            List<String> loreMatter;
            List<String> loreFuel;
            try {
                loreMatter = SBUtils.getSlotLore("Organic Matter");
                loreFuel = SBUtils.getSlotLore("Fuel");
            } catch (TimeoutException e) {
                SkyblockBot.LOGGER.error("Error when getting composter info, timeout exception when trying to read item lore");
                return parent.restart();
            }
            if (loreMatter == null || loreMatter.size() == 0 || loreFuel == null || loreFuel.size() == 0) {
                SkyblockBot.LOGGER.error("Lore is empty, this shouldn't happen");
                parent.task.aborted();
                return new Idle();
            }

            String[] foo = loreMatter.get(0).split("/");
            String[] bar = loreFuel.get(0).split("/");
            String organicMatterLimit = foo[foo.length - 1].replace("k", "");
            String fuelLimit = bar[bar.length - 1].replace("k", "");

            GetComposterInfo composterInfo = (GetComposterInfo) parent.task;
            composterInfo.setMaxOrganicMatter(Integer.parseInt(organicMatterLimit) * 1000);
            composterInfo.setMaxFuel(Integer.parseInt(fuelLimit) * 1000);

            parent.asyncCloseCurrentInventory();
            return new WaitForMenuToClose(new Completed(parent));
        }
    }
}
