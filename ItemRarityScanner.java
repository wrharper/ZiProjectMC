import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
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
    private static final double DEFAULT_RARITY = 0.5; // Default fallback rarity

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

                // Merge mineable and craftable rarities, prioritizing the highest probability source
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
        return 0.0; // Default if not mineable
    }

    private static double calculateCraftableRarity(Item item, Map<String, Double> itemRarityMap) {
        try {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null || visitedItems.contains(itemName)) {
                // Prevent cycles by ignoring already-visited items
                return 0.0;
            }

            visitedItems.add(itemName); // Mark the item as visited

            Recipe<?> recipe = NeoforgeAPI.getCraftingRecipe(itemName); // Fetch crafting recipe
            if (recipe != null) {
                double totalRarity = 0;
                int ingredientCount = 0;
                int maxOutput = recipe.getOutputCount(); // Max items produced by the recipe

                for (Item ingredient : recipe.getIngredients()) {
                    ResourceLocation ingredientName = ingredient.getRegistryName();
                    if (ingredientName == null) continue;

                    // Fetch ingredient rarity or default to a neutral value
                    double ingredientRarity = itemRarityMap.getOrDefault(ingredientName.toString(), DEFAULT_RARITY);

                    // Adjust for proportion and situational factors
                    ingredientRarity = adjustForSituationalIngredients(ingredientRarity, ingredient);

                    totalRarity += ingredientRarity;
                    ingredientCount++;
                }

                visitedItems.remove(itemName); // Unmark item after processing
                double netRarity = totalRarity / ingredientCount; // Average rarity per ingredient
                return netRarity / maxOutput; // Adjust based on max output count
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0; // Default if not craftable
    }

    private static double adjustForSituationalIngredients(double rarity, Item ingredient) {
        // Adjust rarity for auxiliary resource costs (e.g., fuel for smelting)
        if (ingredient.getRegistryName().toString().contains("coal") || ingredient.getRegistryName().toString().contains("blaze_powder")) {
            rarity *= 1.5; // Penalize auxiliary resources
        }

        // Adjust rarity for biome-specific or seasonal resources
        if (ingredient.getRegistryName().toString().contains("snow") || ingredient.getRegistryName().toString().contains("ice")) {
            rarity *= 2.0; // Penalize situational ingredients
        }

        // Add penalties for items often associated with automation
        if (ingredient.getRegistryName().toString().contains("sugarcane") || ingredient.getRegistryName().toString().contains("bamboo")) {
            rarity *= 1.3; // Automation penalty
        }

        return rarity;
    }

    private static double mergeRarities(double mineableRarity, double craftableRarity) {
        // Use the lowest rarity (highest probability source)
        if (mineableRarity > 0 && craftableRarity > 0) {
            return Math.min(mineableRarity, craftableRarity);
        } else if (mineableRarity > 0) {
            return mineableRarity;
        } else if (craftableRarity > 0) {
            return craftableRarity;
        }

        return DEFAULT_RARITY; // Default fallback
    }

    private static double querySpawnRateFromWorldGeneration(ResourceLocation blockName) {
        try {
            return NeoforgeAPI.getBlockSpawnFrequency(blockName); // Query block spawn frequency
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0; // Default for non-spawning blocks
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
