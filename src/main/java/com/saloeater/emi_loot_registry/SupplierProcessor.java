package com.saloeater.emi_loot_registry;

import fzzyhmstrs.emi_loot.EMILoot;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import fzzyhmstrs.emi_loot.server.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper class to process custom loot data suppliers.
 * This is separate from the mixin to avoid mixin restrictions on public static methods.
 */
public class SupplierProcessor {

    /**
     * Process a single custom loot data supplier.
     * Called by LootDataSuppliers and invoked from the mixin.
     */
    public static void processCustomSupplier(
            LootDataSupplier supplier,
            Map<ResourceLocation, ChestLootTableSender> chestSenders,
            Map<ResourceLocation, BlockLootTableSender> blockSenders,
            Map<ResourceLocation, MobLootTableSender> mobSenders,
            Map<ResourceLocation, GameplayLootTableSender> gameplaySenders,
            Map<ResourceLocation, ArchaeologyLootTableSender> archaeologySenders
    ) {
        if (supplier == null) return;

        try {
            // Get Forge types from supplier
            ResourceLocation forgeId = supplier.getLootTableId();
            LootContextParamSet forgeContextType = supplier.getContextType();

            // Convert ResourceLocation (Forge) to ResourceLocation (Fabric) for EMI Loot compatibility
            ResourceLocation id = new ResourceLocation(forgeId.getNamespace(), forgeId.getPath());
            ResourceLocation mobId = supplier.getMobId();

            List<LootTableParser.ItemEntryResult> entries = supplier.getEntries();

            if (entries == null || entries.isEmpty()) {
                if (EMILoot.DEBUG) {
                    EMILoot.LOGGER.warn("Supplier for ID " + id + " returned no entries");
                }
                return;
            }

            if (EMILoot.DEBUG) {
                EMILoot.LOGGER.info("Processing custom supplier: " + id + " with " + entries.size() + " entries");
            }

            // Route to appropriate sender based on context type
            if (forgeContextType == LootContextParamSets.CHEST && EMILoot.config.parseChestLoot) {
                processSupplierForChest(id, entries, chestSenders);
            } else if (forgeContextType == LootContextParamSets.BLOCK && EMILoot.config.parseBlockLoot) {
                processSupplierForBlock(id, entries, blockSenders);
            } else if (forgeContextType == LootContextParamSets.ENTITY && EMILoot.config.parseMobLoot) {
                processSupplierForMob(id, entries, mobSenders, mobId);
            } else if ((forgeContextType == LootContextParamSets.FISHING ||
                        forgeContextType == LootContextParamSets.GIFT ||
                        forgeContextType == LootContextParamSets.PIGLIN_BARTER) &&
                       EMILoot.config.parseGameplayLoot) {
                processSupplierForGameplay(id, entries, gameplaySenders);
            } else if (forgeContextType == LootContextParamSets.ARCHAEOLOGY &&
                       EMILoot.config.parseArchaeologyLoot) {
                processSupplierForArchaeology(id, entries, archaeologySenders);
            } else {
                if (EMILoot.DEBUG) {
                    EMILoot.LOGGER.info("Skipping supplier for context type: " + forgeContextType);
                }
            }
        } catch (Exception e) {
            EMILoot.LOGGER.error("Error processing supplier " + supplier.getLootTableId(), e);
        }
    }

    private static void processSupplierForChest(ResourceLocation id, List<LootTableParser.ItemEntryResult> entries,
                                                  Map<ResourceLocation, ChestLootTableSender> chestSenders) {
        ChestLootTableSender sender = chestSenders.getOrDefault(id, new ChestLootTableSender(id));
        SimpleLootPoolBuilder builder = new SimpleLootPoolBuilder(1.0f);
        entries.forEach(builder::addItem);
        sender.addBuilder(builder);
        chestSenders.put(id, sender);
    }

    private static void processSupplierForBlock(ResourceLocation id, List<LootTableParser.ItemEntryResult> entries,
                                                  Map<ResourceLocation, BlockLootTableSender> blockSenders) {
        BlockLootTableSender sender = blockSenders.getOrDefault(id, new BlockLootTableSender(id));
        ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(1.0f, new LinkedList<>(), new LinkedList<>());
        entries.forEach(builder::addItem);
        sender.addBuilder(builder);
        blockSenders.put(id, sender);
    }

    private static void processSupplierForMob(ResourceLocation id, List<LootTableParser.ItemEntryResult> entries,
                                              Map<ResourceLocation, MobLootTableSender> mobSenders, ResourceLocation mobId) {
        MobLootTableSender sender = mobSenders.get(id);
        if (sender == null) {
            if (mobId == null) {
                EMILoot.LOGGER.error("Supplier for ID " + id + " requires mob_id");
                return;
            }
            sender = new MobLootTableSender(id, mobId);
        }
        ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(1.0f, new LinkedList<>(), new LinkedList<>());
        entries.forEach(builder::addItem);
        sender.addBuilder(builder);
        mobSenders.put(id, sender);
    }

    private static void processSupplierForGameplay(ResourceLocation id, List<LootTableParser.ItemEntryResult> entries,
                                                     Map<ResourceLocation, GameplayLootTableSender> gameplaySenders) {
        GameplayLootTableSender sender = gameplaySenders.getOrDefault(id, new GameplayLootTableSender(id));
        ComplexLootPoolBuilder builder = new ComplexLootPoolBuilder(1.0f, new LinkedList<>(), new LinkedList<>());
        entries.forEach(builder::addItem);
        sender.addBuilder(builder);
        gameplaySenders.put(id, sender);
    }

    private static void processSupplierForArchaeology(ResourceLocation id, List<LootTableParser.ItemEntryResult> entries,
                                                        Map<ResourceLocation, ArchaeologyLootTableSender> archaeologySenders) {
        ArchaeologyLootTableSender sender = archaeologySenders.getOrDefault(id, new ArchaeologyLootTableSender(id));
        SimpleLootPoolBuilder builder = new SimpleLootPoolBuilder(1.0f);
        entries.forEach(builder::addItem);
        sender.addBuilder(builder);
        archaeologySenders.put(id, sender);
    }
}
