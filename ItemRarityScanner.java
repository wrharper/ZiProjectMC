import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ItemRarityScanner {

    public static void main(String[] args) {
        Map<String, Double> itemRarityMap = new HashMap<>();

        // Step 1: Calculate natural rarity (blocks, loot, etc.)
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null) continue;

            double rarity = calculateNaturalRarity(item);
            itemRarityMap.put(itemName.toString(), rarity);
        }

        // Step 2: Chain crafted item rarities dynamically
        Map<String, Double> finalRarityMap = chainCraftedItemRarities(itemRarityMap);

        // Step 3: Export results to a JSON file
        writeToJson(finalRarityMap);
    }

    private static double calculateNaturalRarity(Item item) {
        // Check if item is a block and query spawn rates
        if (item instanceof BlockItem) {
            Block block = ((BlockItem) item).getBlock();
            return getBlockSpawnFrequency(block);
        }

        // Query loot tables for non-block items
        double lootTableProbability = getLootTableDropChance(item);
        if (lootTableProbability > 0) {
            return lootTableProbability;
        }

        // Default rarity for items without explicit data
        return 0.5; // Replace with smarter fallback if needed
    }

    private static double getBlockSpawnFrequency(Block block) {
        // Query dynamic spawn frequency from world generation data
        // Replace the following placeholder with Neoforge API methods
        ResourceLocation blockName = block.getRegistryName();
        if (blockName == null) return 0.0;

        // Example for common logic (pseudo-code)
        // return querySpawnRateFromBiomeData(block);
        return 0.0; // Placeholder for real-world generation query
    }

    private static double getLootTableDropChance(Item item) {
        // Query dynamic spawn chance from loot tables
        // Replace the following placeholder with Neoforge API methods
        ResourceLocation itemName = item.getRegistryName();
        if (itemName == null) return 0.0;

        // Example for loot logic (pseudo-code)
        // return queryProbabilityFromLootTables(item);
        return 0.0; // Placeholder for actual loot table query
    }

    private static Map<String, Double> chainCraftedItemRarities(Map<String, Double> itemRarityMap) {
        Map<String, Double> craftedRarityMap = new HashMap<>(itemRarityMap);

        // Query crafting recipes and calculate combined rarity
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemName = item.getRegistryName();
            if (itemName == null || !isCraftedItem(item)) continue;

            double craftedRarity = calculateCraftedItemRarity(item, itemRarityMap);
            craftedRarityMap.put(itemName.toString(), craftedRarity);
        }

        return craftedRarityMap;
    }

    private static double calculateCraftedItemRarity(Item item, Map<String, Double> itemRarityMap) {
        // Replace with logic for recipe analysis
        // Use Neoforge APIs to retrieve recipes and calculate based on component rarity
        return 0.5; // Placeholder, replace with calculated rarity
    }

    private static boolean isCraftedItem(Item item) {
        // Check if the item has a crafting recipe
        // Replace with Neoforge API query for crafting recipe existence
        return true; // Placeholder for real recipe check
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
