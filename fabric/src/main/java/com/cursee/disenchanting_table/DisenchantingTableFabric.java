package com.cursee.disenchanting_table;

import com.cursee.disenchanting_table.core.DisenchantingTableBlock;
import com.cursee.disenchanting_table.core.DisenchantingTableBlockEntity;
import com.cursee.disenchanting_table.core.DisenchantingTableScreenHandler;
import com.cursee.monolib.core.sailing.Sailing;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DisenchantingTableFabric implements ModInitializer {

    public static final Block DISENCHANTING_TABLE = registerBlock("disenchanting_table",
            new DisenchantingTableBlock(FabricBlockSettings.copyOf(Blocks.ENCHANTING_TABLE).noOcclusion()));

    public static final BlockEntityType<DisenchantingTableBlockEntity> DISENCHANTING_TABLE_BLOCK_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(Constants.MOD_ID, "disenchanting_table_block_entity"),
                    FabricBlockEntityTypeBuilder.create(DisenchantingTableBlockEntity::new,
                            DISENCHANTING_TABLE).build());

    public static final MenuType<DisenchantingTableScreenHandler> DISENCHANTING_TABLE_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, new ResourceLocation(Constants.MOD_ID, "disenchanting_table_screen"),
                    new ExtendedScreenHandlerType<>(DisenchantingTableScreenHandler::new));

    public static final CreativeModeTab DISENCHANTING_TABLE_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(Constants.MOD_ID, "disenchanting_table"),
            FabricItemGroup.builder().title(Component.translatable("itemGroup.disenchanting_table"))
                    .icon(() -> new ItemStack(DISENCHANTING_TABLE)).displayItems((displayContext, entries) -> {
                        entries.accept(DISENCHANTING_TABLE);
                    }).build());

    @Override
    public void onInitialize() {

        DisenchantingTable.init();
        Sailing.register(Constants.MOD_NAME, Constants.MOD_ID, Constants.MOD_VERSION, Constants.MC_VERSION_RAW, Constants.PUBLISHER_AUTHOR, Constants.PRIMARY_CURSEFORGE_MODRINTH);
        DisenchantingTableFabric.registerAll();

        ConfigFabric.initialize();
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Constants.MOD_ID, name), block);
    }
    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Constants.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void registerAll() {}
}
