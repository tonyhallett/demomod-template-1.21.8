package tonyhallett.demomod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ModBlocks {
    public static final Block HOPPER_PIPE_BLOCK = register(
            "hopper_pipe",
            HopperPipeBlock::new,
            AbstractBlock.Settings.copy(Blocks.HOPPER),
            settings -> settings.component(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT)
    );

    public static final Block MINECART_COLLISION_KILLER_BLOCK = register(
            "minecart_killer",
            MinecartCollisionKillerBlock::new,
            AbstractBlock.Settings.create().strength(1.0F), settings -> settings);


    private static Block register(
            String name,
            Function<AbstractBlock.Settings, Block> blockFactory,
            AbstractBlock.Settings settings,
            UnaryOperator<Item.Settings> settingsOperator)
    {
        // Create a registry key for the block
        RegistryKey<Block> blockKey = keyOfBlock(name);
        // Create the block instance
        Block block = blockFactory.apply(settings.registryKey(blockKey));

        // Sometimes, you may not want to register an item for the block.
        // Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
        if (settingsOperator != null) {
            // Items need to be registered with a different type of registry key, but the ID
            // can be the same.
            RegistryKey<Item> itemKey = keyOfItem(name);

            BlockItem blockItem = new BlockItem(block, settingsOperator.apply(new Item.Settings().registryKey(itemKey)));
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Demomod.MOD_ID, name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Demomod .MOD_ID, name));
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register((itemGroup) -> {
            itemGroup.add(HOPPER_PIPE_BLOCK.asItem());
            itemGroup.add(MINECART_COLLISION_KILLER_BLOCK.asItem());
        });
    }

}
