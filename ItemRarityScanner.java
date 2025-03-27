import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ItemRarityScanner {
    public static void main(String[] args) {
        // Map to store item rarity values
        Map<String, String> itemRarityMap = new HashMap<>();

        // Iterate through all registered items
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            String itemName = item.getRegistryName().toString();
            String rarity = determineRarity(item); // Custom logic for rarity
            itemRarityMap.put(itemName, rarity);
        }

        // Convert the map to JSON and write to a file
        Gson gson = new Gson();
        JsonObject jsonObject = gson.toJsonTree(itemRarityMap).getAsJsonObject();

        try (FileWriter writer = new FileWriter("item_rarity.json")) {
            gson.toJson(jsonObject, writer);
            System.out.println("item_rarity.json file created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String determineRarity(Item item) {
        // Implement custom logic to assign rarity values
        // Example: basic logic based on item properties
        if (item.isDamageable()) {
            return "Rare";
        } else if (item.getMaxStackSize() > 64) {
            return "Uncommon";
        } else {
            return "Common";
        }
    }
}
