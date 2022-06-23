package com.viktorx.skyblockbot;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BlockOptionalMeta;

public class BotCore {
    public static void run() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().mine(new BlockOptionalMeta("STONE"));
    }

    public static void stop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().cancel();
    }
}
