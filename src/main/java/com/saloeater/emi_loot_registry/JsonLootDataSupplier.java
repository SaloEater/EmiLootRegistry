package com.saloeater.emi_loot_registry;

import fzzyhmstrs.emi_loot.parser.LootTableParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.LinkedList;
import java.util.List;

/**
 * A LootDataSupplier created from JSON file data.
 * This allows datapacks to define suppliers without writing Java code.
 */
public class JsonLootDataSupplier implements LootDataSupplier {
    private final ResourceLocation lootTableId;
    private final LootContextParamSet contextType;
    private final List<LootTableParser.ItemEntryResult> entries;
    private final ResourceLocation mobId;

    public JsonLootDataSupplier(ResourceLocation lootTableId, LootContextParamSet contextType, List<LootTableParser.ItemEntryResult> entries, ResourceLocation mobId) {
        this.lootTableId = lootTableId;
        this.contextType = contextType;
        this.entries = entries;
        this.mobId = mobId;
    }

    @Override
    public ResourceLocation getLootTableId() {
        return lootTableId;
    }

    @Override
    public LootContextParamSet getContextType() {
        return contextType;
    }

    @Override
    public List<LootTableParser.ItemEntryResult> getEntries() {
        return entries;
    }

    @Override
    public ResourceLocation getMobId() {
        return mobId;
    }
}
