package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.keybinds.Keybinds;
import com.viktorx.skyblockbot.mixins.KeyBindingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class SBProfile {
    private List<SBSkill> skills;
    private long bankBalance;
    private long purse;

    public void loadData() throws TimeoutException {
        purse = SBUtils.getPurse();
        bankBalance = SBUtils.getBankBalance();
        loadSkills();
    }

    private void loadSkills() throws TimeoutException {
        MinecraftClient client = MinecraftClient.getInstance();
        Keybinds.asyncPressKeyAfterTick(client.options.hotbarKeys[8]); // selects hotbar slot 9(sb menu)
        Keybinds.asyncPressKeyAfterTick(client.options.rightKey); // opens sb menu with rmb click
        Slot skillsSlot = SBUtils.getSlot("Your Skills");

        int rmb = ((KeyBindingMixin) client.options.attackKey).getBoundKey().getCode();
        client.player.playerScreenHandler.onSlotClick(skillsSlot.id, rmb, SlotActionType.PICKUP, client.player);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bank balance: ").append(bankBalance);
        sb.append("Purse: ").append(purse);
        sb.append("Skills: {");
        skills.forEach(sbSkill -> sb.append(sbSkill.getName())
                .append(": exp-").append(sbSkill.getExp())
                .append(", level-").append(sbSkill.getLevel()) );
        sb.append("}");
        return sb.toString();
    }
}
