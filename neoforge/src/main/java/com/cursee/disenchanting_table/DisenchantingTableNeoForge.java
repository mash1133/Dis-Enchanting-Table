package com.cursee.disenchanting_table;

import com.cursee.disenchanting_table.core.*;
import com.cursee.monolib.core.sailing.Sailing;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class DisenchantingTableNeoForge {

    public static final DeferredRegister<Block> BLOCK = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Constants.MOD_ID);
    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(BuiltInRegistries.MENU, Constants.MOD_ID);

    // Block
    public static final DeferredHolder<Block, Block> DISENCHANTING_TABLE_BLOCK = registerBlockAndBlockItem("disenchanting_table", () -> new DisenchantingTableBlock(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).noOcclusion()));

    // BlockEntity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisenchantingTableBlockEntity>> DISENCHANTING_TABLE_BLOCK_ENTITY = BLOCK_ENTITY.register("disenchanting_table_block_entity", () ->  BlockEntityType.Builder.of(DisenchantingTableBlockEntity::new, DISENCHANTING_TABLE_BLOCK.get()).build(null));

    // CreativeModeTab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DISENCHANTING_TABLE_CREATIVE_MODE_TAB =
            CREATIVE_MODE_TAB.register("disenchanting_table_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(DISENCHANTING_TABLE_BLOCK.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .title(Component.translatable("itemGroup.disenchanting_table"))
                    .displayItems((displayParameters, output) -> output.accept(DISENCHANTING_TABLE_BLOCK.get()))
                    .build());

    // MenuType
    public static final DeferredHolder<MenuType<?>, MenuType<DisenchantingTableMenu>> DISENCHANTING_TABLE_MENU = registerMenuType(DisenchantingTableMenu::new, "disenchanting_table_menu");

    public DisenchantingTableNeoForge() {

        DisenchantingTable.init();

        Sailing.register(Constants.MOD_NAME, Constants.MOD_ID, Constants.MOD_VERSION, Constants.MC_VERSION_RAW, Constants.PUBLISHER_AUTHOR, Constants.PRIMARY_CURSEFORGE_MODRINTH);

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        DisenchantingTableNeoForge.registerAllDeferred(bus);

        bus.addListener(this::addCreative);
    }


    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(DISENCHANTING_TABLE_BLOCK_ENTITY.get(),
                    DisenchantingTableEntityRenderer::new);
        }
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(DISENCHANTING_TABLE_MENU.get(), DisenchantingTableScreen::new);
            });
        }
    }


    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(DISENCHANTING_TABLE_BLOCK.get());
        }
    }

    private static void registerAllDeferred(IEventBus bus) {
        BLOCK.register(bus);
        BLOCK_ENTITY.register(bus);
        CREATIVE_MODE_TAB.register(bus);
        ITEM.register(bus);
        MENU_TYPE.register(bus);
    }

    private static <T extends Block> DeferredHolder<Block, T> registerBlockAndBlockItem(String name, Supplier<T> block) {
        DeferredHolder<Block, T> toReturn = BLOCK.register(name, block);
        DisenchantingTableNeoForge.registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Item, BlockItem> registerBlockItem(String name, DeferredHolder<Block, T> block) {
        return ITEM.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(IContainerFactory<T> factory, String name) {
        return MENU_TYPE.register(name, () -> IMenuTypeExtension.create(factory));
    }
}