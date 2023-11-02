package com.viktorx.skyblockbot.mixins.Network;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.UUID;

/**
 * This is needed to suppress mojangs dumb warnings the like to spam when i play on hypixel
 */
@Mixin(ClientPlayNetworkHandler.class)
public interface IClientPlayNetworkHandlerMixin {
    @Invoker
    boolean callIsSecureChatEnforced();

    @Accessor
    Map<UUID, PlayerListEntry> getPlayerListEntries();

    @Accessor
    static Logger getLOGGER() {
        return null;
    }

    @Invoker
    void callHandlePlayerListAction(PlayerListS2CPacket.Action action, PlayerListS2CPacket.Entry receivedEntry, PlayerListEntry currentEntry);
}
