package com.viktorx.skyblockbot.skyblock.flipping.flips;

import com.viktorx.skyblockbot.skyblock.flipping.CraftPriceCalculator;
import com.viktorx.skyblockbot.skyblock.flipping.PriceDatabase;
import org.apache.commons.lang3.tuple.Pair;

public class FlipFactory {
    public PotentialFlip createFlip(String type, String itemName) {
        if (type.equals(CraftFlip.type)) {
            Pair<Double, Integer> priceVol24h = PriceDatabase.instance.fetchPriceTradeVol(itemName);
            Double recipePrice = CraftPriceCalculator.instance.getRecipePrice(itemName);
            if (priceVol24h == null || recipePrice == null) {
                return null;
            }
            return new CraftFlip(itemName, priceVol24h.getLeft(), recipePrice, priceVol24h.getRight());
        }
        return null;
    }
}
