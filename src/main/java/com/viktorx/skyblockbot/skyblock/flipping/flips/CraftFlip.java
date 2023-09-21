package com.viktorx.skyblockbot.skyblock.flipping.flips;

public class CraftFlip implements PotentialFlip {
    public static final String type = "CraftFlip";

    private final String itemName;
    private final int soldLast24h; // how many items were sold in last 24h
    private final double median24hprice;
    private final double oneFlipProfit;
    private final double recipePrice;

    // this is supposed to represent the fact that you can only increase the supply of an item by X(currently 10) percent
    // without it significantly changing its price
    private static final double amountSoldCoefficient = 0.1d;

    // median24hPrice is actually just current average price for bazaar items
    public CraftFlip(String itemName, double median24hPrice, double recipePrice, int soldLast24h) {
        this.itemName = itemName;
        this.soldLast24h = soldLast24h;
        this.median24hprice = median24hPrice;

        // accounting for 0.01 is ah tax, bazaar tax is slightly higher
        this.oneFlipProfit = median24hPrice - recipePrice - median24hPrice * 0.01d;
        this.recipePrice = recipePrice;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPotential24hProfit() {
        return soldLast24h * amountSoldCoefficient * oneFlipProfit;
    }

    public double get24hInvestment() {
        return soldLast24h * amountSoldCoefficient * recipePrice;
    }

    public double getOneFlipInvestment() {
        return recipePrice;
    }

    public double getOneFlipProfit() {
        return oneFlipProfit;
    }

    public int comparingBy24hProfit(PotentialFlip second) {
        return Double.compare(getPotential24hProfit(), second.getPotential24hProfit());
    }
}
