package com.saloeater.emi_loot_registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import fzzyhmstrs.emi_loot.parser.LocationPredicateParser;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import fzzyhmstrs.emi_loot.parser.condition.WeatherCheckConditionParser;
import fzzyhmstrs.emi_loot.util.TextKey;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Loads LootDataSuppliers from JSON files in datapacks.
 * JSON files should be located at: data/<namespace>/emi_loot_suppliers/<type>/<name>.json
 * where <type> is one of: chests, blocks, entities, gameplay, archaeology
 */
public class SupplierLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final String BASE_FOLDER = "emi_loot_suppliers";

    // Stores JSON-loaded suppliers separately from registry
    private static final List<JsonLootDataSupplier> loadedSuppliers = new ArrayList<>();

    public static List<JsonLootDataSupplier> getLoadedSuppliers() {
        return loadedSuppliers;
    }

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> suppliers = new java.util.HashMap<>();

        // Scan for JSON files in all type folders
        for (String type : new String[]{"chests", "blocks", "entities", "gameplay", "archaeology"}) {
            String folder = BASE_FOLDER + "/" + type;
            resourceManager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((resourceLocation, resource) -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    suppliers.put(resourceLocation, json);
                    LOGGER.info("Found supplier JSON: {}", resourceLocation);
                } catch (Exception e) {
                    LOGGER.error("Error reading supplier JSON {}: {}", resourceLocation, e.getMessage());
                }
            });
        }

        return suppliers;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> suppliers, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading {} supplier JSON files", suppliers.size());

        // Clear previously loaded suppliers
        loadedSuppliers.clear();

        suppliers.forEach((fileLocation, json) -> {
            try {
                // Infer context type from folder path
                LootContextParamSet contextType = inferContextType(fileLocation.getPath());
                JsonLootDataSupplier supplier = parseSupplier(json, contextType);
                loadedSuppliers.add(supplier);

                String name = fileLocation.getPath().replace(BASE_FOLDER + "/", "").replace(".json", "");
                LOGGER.info("Loaded supplier '{}' ({}) for loot table {}", name, contextType, supplier.getLootTableId());
            } catch (Exception e) {
                LOGGER.error("Error parsing supplier JSON {}: {}", fileLocation, e.getMessage());
            }
        });

        LootDataSuppliers.loadRegistry();

        LOGGER.info("Successfully loaded {} suppliers from datapacks", loadedSuppliers.size());
    }

    private JsonLootDataSupplier parseSupplier(JsonObject json, LootContextParamSet contextType) {
        // Parse loot table ID
        String lootTableIdStr = json.get("loot_table_id").getAsString();
        ResourceLocation lootTableId = new ResourceLocation(lootTableIdStr);

        ResourceLocation mobId = null;
        if (json.has("mob_id")) {
            String mobIdStr = json.get("mob_id").getAsString();
            mobId = new ResourceLocation(mobIdStr);
        }

        // Parse entries
        JsonArray entriesArray = json.getAsJsonArray("entries");
        List<LootTableParser.ItemEntryResult> entries = new LinkedList<>();

        for (int i = 0; i < entriesArray.size(); i++) {
            JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
            String itemId = entryObj.get("item").getAsString();
            int weight = entryObj.has("weight") ? entryObj.get("weight").getAsInt() : 1;

            // Parse conditions if present
            List<TextKey> conditions = new LinkedList<>();
            if (entryObj.has("conditions")) {
                JsonArray conditionsArray = entryObj.getAsJsonArray("conditions");
                for (JsonElement conditionElement : conditionsArray) {
                    JsonObject conditionObj = conditionElement.getAsJsonObject();
                    TextKey conditionKey = parseCondition(conditionObj);
                    if (conditionKey != null) {
                        conditions.add(conditionKey);
                    }
                }
            }

            ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)));
            entries.add(new LootTableParser.ItemEntryResult(
                stack,
                weight,
                conditions,
                new LinkedList<>()   // TODO: Support functions in future
            ));
        }

        return new JsonLootDataSupplier(lootTableId, contextType, entries, mobId);
    }

    /**
     * Parses a condition JSON object into a TextKey for EMI Loot.
     * Currently supports:
     * - random_chance: {"type": "random_chance", "chance": 0.5}
     */
    private TextKey parseCondition(JsonObject conditionObj) {
        String type = conditionObj.get("type").getAsString();

        return switch (type) {
            case "random_chance" -> {
                double chance = conditionObj.get("chance").getAsDouble();
                // TextKey index 42 is "emi_loot.condition.chance" with one argument
                int index = TextKey.getIndex("emi_loot.condition.chance");
                List<Component> args = new ArrayList<>();
                args.add(Component.literal(String.format("%.1f", chance * 100)));
                yield new TextKey(index, args);
            }
            case "location" -> {
                var locationPredicate = LocationPredicate.fromJson(conditionObj);
                yield TextKey.of("emi_loot.condition.location", LocationPredicateParser.parseLocationPredicate(locationPredicate));
            }
            case "weather" -> {
                var weatherCheck = (new WeatherCheck.Serializer()).deserialize(conditionObj, null);
                var parsed = new WeatherCheckConditionParser().parseCondition(weatherCheck, ItemStack.EMPTY, false);
                yield parsed.get(0).getText();
            }
            case "value_check" ->{
                String key = conditionObj.get("key").getAsString();

                JsonElement valueTranslationJson = conditionObj.get("value_translation");
                JsonElement valuePlainJson = conditionObj.get("value_plain");
                if (valueTranslationJson == null && valuePlainJson == null) {
                    LOGGER.warn("Value check condition missing both translation and plain value for key: {}", key);
                    yield null;
                }

                Component valueComponent;
                if (valuePlainJson == null) {
                    valueComponent = Component.translatable(valueTranslationJson.getAsString());
                } else {
                    valueComponent = Component.literal(valuePlainJson.getAsString());
                }

                yield TextKey.of("emi_loot.condition.value_check", Component.translatable(key), valueComponent);
            }
            default -> {
                LOGGER.warn("Unknown condition type: {}", type);
                yield null;
            }
        };
    }

    /**
     * Infers the context type from the file path.
     * Path format: emi_loot_suppliers/<type>/filename.json
     */
    private LootContextParamSet inferContextType(String path) {
        if (path.contains("/chests/")) {
            return LootContextParamSets.CHEST;
        } else if (path.contains("/blocks/")) {
            return LootContextParamSets.BLOCK;
        } else if (path.contains("/entities/")) {
            return LootContextParamSets.ENTITY;
        } else if (path.contains("/gameplay/")) {
            return LootContextParamSets.FISHING;
        } else if (path.contains("/archaeology/")) {
            return LootContextParamSets.ARCHAEOLOGY;
        } else {
            throw new IllegalArgumentException("Cannot infer context type from path: " + path);
        }
    }
}
