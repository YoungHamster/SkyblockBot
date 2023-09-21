package com.viktorx.skyblockbot.skyblock.flipping.flips;

public interface PotentialFlip {
    String getItemName();

    double getPotential24hProfit();

    double get24hInvestment();

    double getOneFlipInvestment();

    double getOneFlipProfit();

    int comparingBy24hProfit(PotentialFlip second);
}
