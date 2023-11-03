package com.viktorx.skyblockbot.skyblock;

import com.viktorx.skyblockbot.utils.Utils;

public class SBSkill {

    private final String name;
    private int level;

    public SBSkill(String itemStackName) {
        String[] strings = itemStackName.split(" ");
        name = strings[0];
        level = Utils.convertRomanToInt(strings[1]);
    }


    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":").append(level);

        return sb.toString();
    }
}
