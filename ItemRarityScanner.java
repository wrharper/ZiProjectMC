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
import java.util.Map;

public class ItemRarityScanner {

    public static void main(String[] args) {
        Map<String, Double> itemRarityMap = new HashMap<>();

        // Step 1: Calculate rarity for all items (mineable and/or craftable)
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null) continue;

            double mineableRarity = calculateMineableRarity(item);
            double craftableRarity = calculateCraftableRarity(item, itemRarityMap);

            // Merge mineable and craftable rarities, if applicable
            double finalRarity = mergeRarities(mineableRarity, craftableRarity);
            itemRarityMap.put(itemName.toString(), finalRarity);
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
            Recipe<?> recipe = NeoforgeAPI.getCraftingRecipe(item.getRegistryName());
            if (recipe != null) {
                double totalRarity = 0;
                int ingredientCount = 0;

                for (Item ingredient : recipe.getIngredients()) {
                    ResourceLocation ingredientName = ingredient.getRegistryName();
                    if (ingredientName == null) continue;

                    totalRarity += itemRarityMap.getOrDefault(ingredientName.toString(), 0.5);
                    ingredientCount++;
                }

                return ingredientCount > 0 ? totalRarity / ingredientCount : 0.0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0; // Default if not craftable
    }

    private static double mergeRarities(double mineableRarity, double craftableRarity) {
        if (mineableRarity > 0 && craftableRarity > 0) {
            // Combine the rarities (e.g., weighted average)
            return (mineableRarity + craftableRarity) / 2;
        } else if (mineableRarity > 0) {
            return mineableRarity;
        } else if (craftableRarity > 0) {
            return craftableRarity;
        }

        return 0.5; // Default fallback
    }

    private static double querySpawnRateFromWorldGeneration(ResourceLocation blockName) {
        try {
            return NeoforgeAPI.getBlockSpawnFrequency(blockName);
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
