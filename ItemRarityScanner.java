import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemRarityScanner {

    private static final Set<ResourceLocation> visitedItems = new HashSet<>(); // Track visited items to prevent loops
    private static final Map<ResourceLocation, Double> memoizedRarities = new HashMap<>(); // Memoization cache for performance
    private static final double DEFAULT_RARITY = Double.MAX_VALUE; // Default fallback rarity for unavailable data

    public static void main(String[] args) {
        Map<String, Double> itemRarityMap = new HashMap<>();

        // Step 1: Calculate rarity for all items (mineable and/or craftable)
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null) continue;

            // Prevent duplicate calculations with memoization
            if (!memoizedRarities.containsKey(itemName)) {
                double mineableRarity = calculateMineableRarity(item);
                double craftableRarity = calculateCraftableRarity(item, itemRarityMap);

                // Merge mineable and craftable rarities, prioritizing the most restrictive odds
                double finalRarity = mergeRarities(mineableRarity, craftableRarity);
                memoizedRarities.put(itemName, finalRarity);
                itemRarityMap.put(itemName.toString(), finalRarity);
            }
        }

        // Step 2: Export results to a JSON file
        writeToJson(itemRarityMap);
    }

    private static double calculateMineableRarity(Item item) {
        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();
            return querySpawnRateFromWorldGeneration(block.getRegistryName());
        }
        return DEFAULT_RARITY; // Default if not mineable
    }

    private static double calculateCraftableRarity(Item item, Map<String, Double> itemRarityMap) {
        try {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null || visitedItems.contains(itemName)) {
                return DEFAULT_RARITY; // Prevent cycles
            }

            visitedItems.add(itemName); // Mark the item as visited

            Recipe<?> recipe = NeoforgeAPI.getCraftingRecipe(itemName); // Fetch crafting recipe dynamically
            if (recipe != null) {
                double totalOdds = 0;
                int ingredientCount = 0;
                int maxOutput = recipe.getOutputCount(); // Max items produced by the recipe

                for (Item ingredient : recipe.getIngredients()) {
                    ResourceLocation ingredientName = ingredient.getRegistryName();
                    if (ingredientName == null) continue;

                    double ingredientRarity = itemRarityMap.getOrDefault(ingredientName.toString(), DEFAULT_RARITY);

                    // Dynamically adjust for situational factors
                    ingredientRarity = adjustForSituationalIngredients(ingredientRarity, ingredient);

                    totalOdds += ingredientRarity;
                    ingredientCount++;
                }

                visitedItems.remove(itemName); // Unmark item after processing
                double netRarity = totalOdds / ingredientCount; // Average rarity per ingredient
                return netRarity * maxOutput; // Final odds for crafting
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return DEFAULT_RARITY; // Default for uncategorized items
    }

    private static double adjustForSituationalIngredients(double rarity, Item ingredient) {
        ResourceLocation ingredientName = ingredient.getRegistryName();
        if (ingredientName == null) return rarity;

        // Query spawn odds or biome distribution
        double spawnOdds = querySpawnOdds(ingredientName);
        if (spawnOdds > 0) {
            rarity *= spawnOdds; // Adjust rarity based on full spawn odds
        }

        // Estimate acquisition time for smelting or processing
        double acquisitionTime = calculateAcquisitionTime(ingredientName);
        rarity *= acquisitionTime; // Scale rarity by acquisition time in seconds

        return rarity;
    }

    private static double querySpawnOdds(ResourceLocation ingredientName) {
        try {
            return NeoforgeAPI.getSpawnProbability(ingredientName); // Fetch dynamic spawn probabilities
        } catch (Exception e) {
            e.printStackTrace();
        }

        return DEFAULT_RARITY; // Default fallback for items with unknown spawn odds
    }

    private static double calculateAcquisitionTime(ResourceLocation ingredientName) {
        try {
            if (NeoforgeAPI.requiresSmelting(ingredientName)) {
                double smeltingTime = NeoforgeAPI.getSmeltingTime(ingredientName); // Fetch smelting time
                double fuelModifier = NeoforgeAPI.getFuelCostModifier(ingredientName); // Adjust for fuel efficiency
                return smeltingTime * fuelModifier; // Total smelting acquisition time
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1; // Default minimal acquisition time
    }

    private static double mergeRarities(double mineableRarity, double craftableRarity) {
        return Math.min(mineableRarity, craftableRarity); // Use the most restrictive (highest odds) rarity
    }

    private static double querySpawnRateFromWorldGeneration(ResourceLocation blockName) {
        try {
            double spawnProbability = NeoforgeAPI.getBlockSpawnProbability(blockName); // Query dynamic spawn rates
            if (spawnProbability > 0) {
                return 1 / spawnProbability; // Convert probability to odds
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return DEFAULT_RARITY; // Default for blocks without valid spawn rates
    }

    private static void writeToJson(Map<String, Double> itemRarityMap) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.toJsonTree(itemRarityMap).getAsJsonObject();

        try (FileWriter writer = new FileWriter("item_rarity.json")) {
            gson.toJson(jsonObject, writer);
            System.out.println("item_rarity.json file created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
