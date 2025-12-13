package com.saloeater.emi_loot_registry.mixin;

import com.saloeater.emi_loot_registry.LootDataSuppliers;
import fzzyhmstrs.emi_loot.EMILoot;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import fzzyhmstrs.emi_loot.server.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.Map;

/**
 * Mixin into LootTableParser to add custom loot data supplier support.
 * Injects after vanilla loot tables are parsed but before post-processing.
 */
@Mixin(value = LootTableParser.class, remap = false)
public class LootTableParserMixin {

    @Shadow
    private static Map<ResourceLocation, ChestLootTableSender> chestSenders;

    @Shadow
    private static Map<ResourceLocation, BlockLootTableSender> blockSenders;

    @Shadow
    private static Map<ResourceLocation, MobLootTableSender> mobSenders;

    @Shadow
    private static Map<ResourceLocation, GameplayLootTableSender> gameplaySenders;

    @Shadow
    private static Map<ResourceLocation, ArchaeologyLootTableSender> archaeologySenders;

    /**
     * Inject into parseLootTables after vanilla tables are processed.
     * This is where we process all registered custom suppliers.
     */
    @Inject(method = "parseLootTables", at = @At(value = "TAIL", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V", shift = At.Shift.BEFORE))
    private static void processCustomLootSuppliers(LootDataManager manager, Map<LootDataId<?>, ?> tables, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (EMILoot.DEBUG) {
            EMILoot.LOGGER.info("Processing custom loot data suppliers via mixin");
        }
        LootDataSuppliers.registerSenders(chestSenders, blockSenders, mobSenders, gameplaySenders, archaeologySenders);
    }
}
