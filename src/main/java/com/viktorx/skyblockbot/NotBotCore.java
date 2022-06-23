package com.viktorx.skyblockbot;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.client.network.ClientPlayerEntity;

public class NotBotCore {
    public static void run(ClientPlayerEntity client) {
        BaritoneAPI.getProvider().getBaritoneForPlayer(client).getMineProcess().mine(new BlockOptionalMeta("dirt"));
    }

    public static void stop(ClientPlayerEntity client) {
        BaritoneAPI.getProvider().getBaritoneForPlayer(client).getMineProcess().cancel();
    }
}
