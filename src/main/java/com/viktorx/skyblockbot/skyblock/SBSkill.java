package com.viktorx.skyblockbot.skyblock;

public class SBSkill {

    private final String name;
    private int level;
    private float exp;

    public SBSkill(String name) {
        this.name = name;
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

    public float getExp() {
        return exp;
    }

    public void setExp(float exp) {
        this.exp = exp;
    }
}
