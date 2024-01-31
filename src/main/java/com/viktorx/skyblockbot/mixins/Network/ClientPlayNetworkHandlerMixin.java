package com.viktorx.skyblockbot.mixins.Network;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.viktorx.skyblockbot.task.GlobalExecutorInfo;
import com.viktorx.skyblockbot.task.base.replay.ReplayExecutor;
import com.viktorx.skyblockbot.utils.CurrentInventory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.viktorx.skyblockbot.SkyblockBot.LOGGER;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow(remap = false)
    private MessageSignatureStorage signatureStorage;

    @Shadow(remap = false)
    @Final
    private DynamicRegistryManager.Immutable combinedDynamicRegistries;

    @Inject(method = "onInventory", at = @At("HEAD"))
    public void interceptInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        CurrentInventory.setSyncID(packet.getSyncId());
        CurrentInventory.setItemStacks(packet.getContents());
    }

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    public void detectServerChangingPosRot(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying() || ReplayExecutor.INSTANCE.isRecording()) {
            ReplayExecutor.INSTANCE.serverChangedPositionRotation = true;
        }
    }

    @Inject(method = "onLookAt", at = @At("HEAD"))
    public void detectServerChangingMyRotation(LookAtS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying()) {
            ReplayExecutor.INSTANCE.serverChangedPositionRotation = true;
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    public void detectServerChangingSlot(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        /*
         * Idk why, but this method gets called twice, once on render thread, and once on netty thread
         * So i have to skip one of those calls to get correct results
         */
        if (RenderSystem.isOnRenderThread()) {
            return;
        }

        /*
         * Anti-detect
         */
        if (ReplayExecutor.INSTANCE.isPlaying()) {
            ReplayExecutor.INSTANCE.serverChangedSlot = true;
        }
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"))
    public void detectServerChangingItem(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
        if (ReplayExecutor.INSTANCE.isPlaying()) {
            ReplayExecutor.INSTANCE.serverChangedItem = true;
        }
    }

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    public void interceptMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        /*
         * Counting cropies
         */
        String message = packet.content().getString();
        if (message.contains("Cropie (Armor Set Bonus)")) {
            GlobalExecutorInfo.cropieCount.addAndGet(1);
        }

        if (message.contains("Sacks") && message.contains("Last 30s.")) {
            int delta = 0;
            String[] split = message.split(" ");

            String increase = split[1];
            increase = increase.substring(1);
            delta += Integer.parseInt(increase.replace(",", ""));

            // if decrease string contains 'last' then no items were taken out of sacks
            // and chat message looks like '[Sacks] +25 items. (Last 30s.)' instead of '[Sacks] +733 items, -594 items. (Last 30s.)'
            String decrease = split[3];
            if (!decrease.contains("Last")) {
                decrease = decrease.substring(1);
                delta -= Integer.parseInt(decrease.replace(",", ""));
            }

            GlobalExecutorInfo.totalSackCount.addAndGet(delta);
        }
    }

    /**
     * @author viktorX
     * @reason original method spams warnings in logs when playing hypixel skyblock
     */
    @Overwrite
    public void onPlayerList(PlayerListS2CPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler netHandler = MinecraftClient.getInstance().getNetworkHandler();
        IClientPlayNetworkHandlerMixin accessorNetHandler = (IClientPlayNetworkHandlerMixin) netHandler;

        NetworkThreadUtils.forceMainThread(packet, netHandler, client);
        Iterator var2 = packet.getPlayerAdditionEntries().iterator();

        PlayerListS2CPacket.Entry entry;
        PlayerListEntry playerListEntry;
        while (var2.hasNext()) {
            entry = (PlayerListS2CPacket.Entry) var2.next();
            playerListEntry = new PlayerListEntry((GameProfile) Objects.requireNonNull(entry.profile()), accessorNetHandler.callIsSecureChatEnforced());
            if (accessorNetHandler.getPlayerListEntries().putIfAbsent(entry.profileId(), playerListEntry) == null) {
                client.getSocialInteractionsManager().setPlayerOnline(playerListEntry);
            }
        }

        var2 = packet.getEntries().iterator();

        while (true) {
            while (var2.hasNext()) {
                entry = (PlayerListS2CPacket.Entry) var2.next();
                playerListEntry = (PlayerListEntry) accessorNetHandler.getPlayerListEntries().get(entry.profileId());
                if (playerListEntry == null) {
                } else {
                    Iterator var5 = packet.getActions().iterator();

                    while (var5.hasNext()) {
                        PlayerListS2CPacket.Action action = (PlayerListS2CPacket.Action) var5.next();
                        accessorNetHandler.callHandlePlayerListAction(action, entry, playerListEntry);
                    }
                }
            }

            return;
        }
    }

    /**
     * @author viktorX
     * @reason Дормоеды ебаные at microsoft think they can decide which chat messages i can see, and which can't
     */
    @Overwrite(remap = false)
    public void onChatMessage(ChatMessageS2CPacket packet) {
        NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) ((Object) this), MinecraftClient.getInstance());
        Optional<MessageBody> optional = packet.body().toBody(this.signatureStorage);
        Optional<MessageType.Parameters> optional2 = packet.serializedParameters().toParameters(this.combinedDynamicRegistries);
        if (optional.isPresent() && optional2.isPresent()) {
            this.signatureStorage.add(optional.get(), packet.signature());
            UUID uUID = packet.sender();
            PlayerListEntry playerListEntry = this.getPlayerListEntry(uUID);

            if (playerListEntry == null) {
                LOGGER.error("Received player chat packet for unknown player with ID: {}", uUID);
                MessageLink messageLink = MessageLink.of(uUID);

                SignedMessage signedMessage = new SignedMessage(messageLink, packet.signature(), optional.get(), packet.unsignedContent(), packet.filterMask());
                MinecraftClient.getInstance().getMessageHandler().onChatMessage(signedMessage, new GameProfile(uUID, "Unknown"), optional2.get());
            } else {
                PublicPlayerSession publicPlayerSession = playerListEntry.getSession();
                MessageLink messageLink;
                if (publicPlayerSession != null) {
                    messageLink = new MessageLink(packet.index(), uUID, publicPlayerSession.sessionId());
                } else {
                    messageLink = MessageLink.of(uUID);
                }

                SignedMessage signedMessage = new SignedMessage(messageLink, packet.signature(), optional.get(), packet.unsignedContent(), packet.filterMask());
                MinecraftClient.getInstance().getMessageHandler().onChatMessage(signedMessage, playerListEntry.getProfile(), optional2.get());
            }
        } else {
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection().disconnect(Text.translatable("multiplayer.disconnect.invalid_packet"));
        }
    }

    @Shadow(remap = false)
    public abstract PlayerListEntry getPlayerListEntry(UUID uuid);
}
