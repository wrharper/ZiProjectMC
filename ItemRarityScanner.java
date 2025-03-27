import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
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

        // Step 1: Calculate rarity for natural items
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null) continue;

            double rarity = calculateNaturalRarity(item);
            itemRarityMap.put(itemName.toString(), rarity);
        }

        // Step 2: Dynamically calculate rarity for crafted items
        Map<String, Double> finalRarityMap = chainCraftedItemRarities(itemRarityMap);

        // Step 3: Export results to a JSON file
        writeToJson(finalRarityMap);
    }

    private static double calculateNaturalRarity(Item item) {
        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();
            return querySpawnRateFromWorldGeneration(block.getRegistryName());
        }

        double lootTableProbability = getLootTableDropChance(item);
        if (lootTableProbability > 0) {
            return lootTableProbability;
        }

        return 0.5; // Default fallback for undefined items
    }

    private static double querySpawnRateFromWorldGeneration(ResourceLocation blockName) {
        try {
            // Query biome-specific or dimension-specific spawn data for blocks
            return NeoforgeAPI.getBlockSpawnFrequency(blockName); // Example Neoforge method
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }

    private static double getLootTableDropChance(Item item) {
        try {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null) return 0.0;

            LootTable lootTable = NeoforgeAPI.getLootTable(itemName); // Example Neoforge method
            if (lootTable != null) {
                return lootTable.getDropProbability(itemName); // Example Neoforge method
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }

    private static Map<String, Double> chainCraftedItemRarities(Map<String, Double> itemRarityMap) {
        Map<String, Double> craftedRarityMap = new HashMap<>(itemRarityMap);

        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null || !isCraftedItem(item)) continue;

            double craftedRarity = calculateCraftedItemRarity(item, itemRarityMap);
            craftedRarityMap.put(itemName.toString(), craftedRarity);
        }

        return craftedRarityMap;
    }

    private static double calculateCraftedItemRarity(Item item, Map<String, Double> itemRarityMap) {
        try {
            Recipe<?> recipe = NeoforgeAPI.getCraftingRecipe(item.getRegistryName()); // Example Neoforge method
            if (recipe != null) {
                double totalRarity = 0;
                int ingredientCount = 0;

                for (Item ingredient : recipe.getIngredients()) { // Iterate through ingredients
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

        return 0.5;
    }

    private static boolean isCraftedItem(Item item) {
        try {
            return NeoforgeAPI.hasCraftingRecipe(item.getRegistryName()); // Example Neoforge method
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
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
