package com.saloeater.emi_loot_registry;

import fzzyhmstrs.emi_loot.parser.LootTableParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.List;

/**
 * Interface for providing synthetic loot data programmatically.
 * Mods register implementations to create "virtual" loot tables that integrate with EMI Loot.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyLootSupplier implements LootDataSupplier {
 *     public ResourceLocation getLootTableId() {
 *         return new ResourceLocation("mymod", "chests/custom");
 *     }
 *
 *     public LootContextParamSet getContextType() {
 *         return LootContextParamSets.CHEST;
 *     }
 *
 *     public List<LootTableParser.ItemEntryResult> getEntries() {
 *         return List.of(
 *             new LootTableParser.ItemEntryResult(
 *                 new ItemStack(Items.DIAMOND), 1,
 *                 new LinkedList<>(), new LinkedList<>()
 *             )
 *         );
 *     }
 * }
 * }</pre>
 */
public interface LootDataSupplier {
    /**
     * @return The loot table identifier for this synthetic loot source.
     *         Can be a completely new ID or match existing vanilla/datapack tables to extend them.
     */
    ResourceLocation getLootTableId();

    /**
     * @return The context type determining which sender this data goes to:
     *         CHEST, BLOCK, ENTITY, FISHING, GIFT, BARTER, or ARCHAEOLOGY.
     */
    LootContextParamSet getContextType();

    /**
     * @return List of item entries with weights, conditions, and functions.
     *         These will be accumulated into a synthetic loot pool.
     */
    List<LootTableParser.ItemEntryResult> getEntries();

    /**
     * @return Optional mob/entity identifier for entity-specific loot.
     *         Can be null if not applicable to this supplier.
     */
    ResourceLocation getMobId();
}
