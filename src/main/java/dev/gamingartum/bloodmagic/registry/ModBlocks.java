package dev.gamingartum.bloodmagic.registry;

import dev.gamingartum.bloodmagic.BloodMagic;
import dev.gamingartum.bloodmagic.block.BloodAltarBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

    private static final ResourceKey<Block> BLOOD_ALTAR_KEY = ResourceKey.create(Registries.BLOCK, BloodMagic.id("blood_altar"));

    public static final Block BLOOD_ALTAR = new BloodAltarBlock(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .strength(3.0f, 6.0f)
            .requiresCorrectToolForDrops()
            .setId(BLOOD_ALTAR_KEY)
    );

    public static void initialize() {
        register(BLOOD_ALTAR_KEY, BLOOD_ALTAR);
    }

    private static void register(ResourceKey<Block> key, Block block) {
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, key, block);
        Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, key.identifier()));
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, key.identifier(), new BlockItem(block, props));
    }
}
