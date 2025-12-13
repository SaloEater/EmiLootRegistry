package com.saloeater.emi_loot_registry;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.slf4j.Logger;

/**
 * Main mod class for EMI Loot Registry.
 * Provides a custom Forge registry for LootDataSupplier types.
 */
@Mod("emi_loot_registry")
public class EmiLootRegistry {
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * DeferredRegister for LootDataSupplierType entries.
     * Other mods can use this to register their own suppliers.
     */
    public static final DeferredRegister<LootDataSupplierType<?>> LOOT_DATA_SUPPLIERS =
        DeferredRegister.<LootDataSupplierType<?>>create(LootDataSuppliers.REGISTRY_KEY, "emi_loot_registry");

    // Example suppliers can be registered here by other mods
    // See ExampleDataPack folder for datapack-based examples

    public EmiLootRegistry() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the registry itself
        modBus.addListener(this::createRegistries);

        // Register our supplier entries
        LOOT_DATA_SUPPLIERS.register(modBus);

        // Initialize registry reference after common setup
        modBus.addListener(this::commonSetup);

        // Register datapack reload listener
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    /**
     * Creates the custom registry for LootDataSupplierType.
     * Called during NewRegistryEvent.
     */
    private void createRegistries(NewRegistryEvent event) {
        LOGGER.info("Creating LootDataSupplier registry");
        // Create the registry - the supplier will be resolved after the event
        event.create(new RegistryBuilder<LootDataSupplierType<?>>()
            .setName(LootDataSuppliers.REGISTRY_KEY.location())
            .disableSaving()  // Suppliers are code-only, not data
            .disableSync()    // Don't need to sync to clients
        );
    }

    /**
     * Common setup phase - retrieve and store the registry reference.
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Get the registry from Forge's registry manager
            LootDataSuppliers.REGISTRY = net.minecraftforge.registries.RegistryManager.ACTIVE
                .getRegistry(LootDataSuppliers.REGISTRY_KEY);

            if (LootDataSuppliers.REGISTRY != null) {
                LOGGER.info("LootDataSupplier registry initialized with {} entries",
                    LootDataSuppliers.REGISTRY.getKeys().size());
            } else {
                LOGGER.error("LootDataSupplier registry not initialized!");
            }
        });
    }

    /**
     * Register reload listener for JSON-based suppliers from datapacks.
     */
    private void addReloadListeners(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(new SupplierLoader());
        LOGGER.info("Registered SupplierLoader for datapack reloading");
    }
}
