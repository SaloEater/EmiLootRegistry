package com.saloeater.emi_loot_registry;

/**
 * Registry type wrapping LootDataSupplier factories.
 * Follows the same pattern as LootConditionType and LootFunctionType.
 *
 * <p>This type is registered in the custom Forge registry and provides
 * a factory method to create supplier instances during loot table parsing.
 */
public interface LootDataSupplierType<T extends LootDataSupplier> {
    /**
     * Create a new instance of the supplier.
     * Called during loot table parsing to generate synthetic loot data.
     *
     * @return A new supplier instance
     */
    T create();
}
