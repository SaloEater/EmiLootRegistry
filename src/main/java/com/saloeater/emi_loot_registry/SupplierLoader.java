package com.saloeater.emi_loot_registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Loads LootDataSuppliers from JSON files in datapacks.
 * JSON files should be located at: data/<namespace>/emi_loot_suppliers/<name>.json
 */
public class SupplierLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final String FOLDER = "emi_loot_suppliers";

    // Stores JSON-loaded suppliers separately from registry
    private static final List<JsonLootDataSupplier> loadedSuppliers = new ArrayList<>();

    public static List<JsonLootDataSupplier> getLoadedSuppliers() {
        return loadedSuppliers;
    }

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> suppliers = new java.util.HashMap<>();
        
        // Scan for JSON files in all datapacks
        resourceManager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).forEach((resourceLocation, resource) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                suppliers.put(resourceLocation, json);
                LOGGER.info("Found supplier JSON: {}", resourceLocation);
            } catch (Exception e) {
                LOGGER.error("Error reading supplier JSON {}: {}", resourceLocation, e.getMessage());
            }
        });
        
        return suppliers;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> suppliers, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading {} supplier JSON files", suppliers.size());

        // Clear previously loaded suppliers
        loadedSuppliers.clear();

        suppliers.forEach((fileLocation, json) -> {
            try {
                JsonLootDataSupplier supplier = parseSupplier(json);
                loadedSuppliers.add(supplier);

                String name = fileLocation.getPath().replace(FOLDER + "/", "").replace(".json", "");
                LOGGER.info("Loaded supplier '{}' for loot table {}", name, supplier.getLootTableId());
            } catch (Exception e) {
                LOGGER.error("Error parsing supplier JSON {}: {}", fileLocation, e.getMessage());
            }
        });

        LootDataSuppliers.loadRegistry();

        LOGGER.info("Successfully loaded {} suppliers from datapacks", loadedSuppliers.size());
    }

    private JsonLootDataSupplier parseSupplier(JsonObject json) {
        // Parse loot table ID
        String lootTableIdStr = json.get("loot_table_id").getAsString();
        ResourceLocation lootTableId = new ResourceLocation(lootTableIdStr);
        
        // Parse context type
        String contextTypeStr = json.get("context_type").getAsString();
        LootContextParamSet contextType = parseContextType(contextTypeStr);
        
        // Parse entries
        JsonArray entriesArray = json.getAsJsonArray("entries");
        List<LootTableParser.ItemEntryResult> entries = new LinkedList<>();
        
        for (int i = 0; i < entriesArray.size(); i++) {
            JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
            String itemId = entryObj.get("item").getAsString();
            int weight = entryObj.has("weight") ? entryObj.get("weight").getAsInt() : 1;
            
            ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)));
            entries.add(new LootTableParser.ItemEntryResult(
                stack,
                weight,
                new LinkedList<>(),  // TODO: Support conditions in future
                new LinkedList<>()   // TODO: Support functions in future
            ));
        }
        
        return new JsonLootDataSupplier(lootTableId, contextType, entries);
    }

    private LootContextParamSet parseContextType(String type) {
        return switch (type.toUpperCase()) {
            case "CHEST" -> LootContextParamSets.CHEST;
            case "BLOCK" -> LootContextParamSets.BLOCK;
            case "ENTITY", "MOB" -> LootContextParamSets.ENTITY;
            case "FISHING" -> LootContextParamSets.FISHING;
            case "ARCHAEOLOGY" -> LootContextParamSets.ARCHAEOLOGY;
            default -> throw new IllegalArgumentException("Unknown context type: " + type);
        };
    }
}
