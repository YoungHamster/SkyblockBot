package com.viktorx.skyblockbot.task.buyItem;

import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class BuyItemExecutor {

    public static final BuyItemExecutor INSTANCE = new BuyItemExecutor();

    private boolean executing = false;
    private BuyItem task;

    public void Init() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onTickBuy);
    }

    public void execute(BuyItem task) {
        executing = true;
        this.task = task;
    }

    public void onTickBuy(MinecraftClient client) {
        if(!executing) {
            return;
        }
        PriceDatabase.getInstance().fetchItemPrice(task.getItemName());

    }
}
