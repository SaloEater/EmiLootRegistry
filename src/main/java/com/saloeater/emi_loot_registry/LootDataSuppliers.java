package com.saloeater.emi_loot_registry;

import fzzyhmstrs.emi_loot.EMILoot;
import fzzyhmstrs.emi_loot.server.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Central registry holder for LootDataSupplier types.
 * Manages the custom Forge registry and provides processing coordination.
 */
public class LootDataSuppliers {
    /**
     * Registry key for the custom loot data supplier registry.
     */
    public static final ResourceKey<Registry<LootDataSupplierType<?>>> REGISTRY_KEY =
        ResourceKey.createRegistryKey(new ResourceLocation("emi_loot_registry", "loot_data_supplier"));

    /**
     * Registry instance. Initialized by Forge registry setup.
     */
    public static net.minecraftforge.registries.IForgeRegistry<LootDataSupplierType<?>> REGISTRY;

    private static Map<ResourceLocation, ChestLootTableSender> chestSenders;
    private static Map<ResourceLocation, BlockLootTableSender> blockSenders;
    private static Map<ResourceLocation, MobLootTableSender> mobSenders;
    private static Map<ResourceLocation, GameplayLootTableSender> gameplaySenders;
    private static Map<ResourceLocation, ArchaeologyLootTableSender> archaeologySenders;

    /**
     * Called by the mixin to process all registered suppliers after vanilla loot tables are parsed.
     * Iterates through the registry and creates suppliers, passing them to the processor for handling.
     * Also processes JSON-loaded suppliers from datapacks.
     */
    public static void registerSenders(
            Map<ResourceLocation, ChestLootTableSender> chestSenders,
            Map<ResourceLocation, BlockLootTableSender> blockSenders,
            Map<ResourceLocation, MobLootTableSender> mobSenders,
            Map<ResourceLocation, GameplayLootTableSender> gameplaySenders,
            Map<ResourceLocation, ArchaeologyLootTableSender> archaeologySenders
    ) {
        LootDataSuppliers.chestSenders = chestSenders;
        LootDataSuppliers.blockSenders = blockSenders;
        LootDataSuppliers.mobSenders = mobSenders;
        LootDataSuppliers.gameplaySenders = gameplaySenders;
        LootDataSuppliers.archaeologySenders = archaeologySenders;
    }

    public static void loadRegistry() {
        int count = 0;

        // Process registry-based suppliers
        if (REGISTRY != null) {
            for (LootDataSupplierType<?> supplierType : REGISTRY) {
                try {
                    LootDataSupplier supplier = supplierType.create();
                    // Pass to helper class to process the supplier
                    SupplierProcessor.processCustomSupplier(supplier, chestSenders, blockSenders, mobSenders, gameplaySenders, archaeologySenders);
                    count++;
                } catch (Exception e) {
                    EMILoot.LOGGER.error("Failed to create or process loot data supplier", e);
                }
            }
        } else {
            EMILoot.LOGGER.warn("LootDataSuppliers registry not initialized");
        }

        // Process JSON-loaded suppliers from datapacks
        for (JsonLootDataSupplier supplier : SupplierLoader.getLoadedSuppliers()) {
            try {
                SupplierProcessor.processCustomSupplier(supplier, chestSenders, blockSenders, mobSenders, gameplaySenders, archaeologySenders);
                count++;
            } catch (Exception e) {
                EMILoot.LOGGER.error("Failed to process JSON loot data supplier for " + supplier.getLootTableId(), e);
            }
        }

        if (count > 0 && EMILoot.DEBUG) {
            EMILoot.LOGGER.info("Processed " + count + " custom loot data suppliers");
        }
    }
}
