package com.viktorx.skyblockbot.skyblock;

import java.util.List;

public class SBProfile {
    private List<SBSkill> skills;
    private long bankBalance;
    private long purse;

    public void loadData() {
        purse = SBUtils.getPurse();
        bankBalance = SBUtils.getBankBalance();
    }
}
