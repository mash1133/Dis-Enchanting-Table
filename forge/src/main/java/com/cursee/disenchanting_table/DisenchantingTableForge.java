package com.cursee.disenchanting_table;

import com.cursee.disenchanting_table.core.*;
import com.cursee.monolib.core.sailing.Sailing;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class DisenchantingTableForge {

    public static final DeferredRegister<Block> BLOCK = DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Constants.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);
    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Constants.MOD_ID);

    // Block
    public static final RegistryObject<Block> DISENCHANTING_TABLE_BLOCK = registerBlockAndBlockItem("disenchanting_table", () -> new DisenchantingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).noOcclusion()));

    // BlockEntity
    public static final RegistryObject<BlockEntityType<DisenchantingTableBlockEntity>> DISENCHANTING_TABLE_BLOCK_ENTITY = BLOCK_ENTITY.register("disenchanting_table_block_entity", () ->  BlockEntityType.Builder.of(DisenchantingTableBlockEntity::new, DISENCHANTING_TABLE_BLOCK.get()).build(null));

    // CreativeModeTab
    public static final RegistryObject<CreativeModeTab> DISENCHANTING_TABLE_CREATIVE_MODE_TAB =
            CREATIVE_MODE_TAB.register("disenchanting_table_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(DISENCHANTING_TABLE_BLOCK.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .title(Component.translatable("itemGroup.disenchanting_table"))
                    .displayItems((displayParameters, output) -> output.accept(DISENCHANTING_TABLE_BLOCK.get()))
                    .build());

    // MenuType
    public static final RegistryObject<MenuType<DisenchantingTableMenu>> DISENCHANTING_TABLE_MENU = registerMenuType(DisenchantingTableMenu::new, "disenchanting_table_menu");

    public DisenchantingTableForge() {

        DisenchantingTable.init();

        Sailing.register(Constants.MOD_NAME, Constants.MOD_ID, Constants.MOD_VERSION, Constants.MC_VERSION_RAW, Constants.PUBLISHER_AUTHOR, Constants.PRIMARY_CURSEFORGE_MODRINTH);

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        DisenchantingTableForge.registerAllDeferred(bus);

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

    private static <T extends Block> RegistryObject<T> registerBlockAndBlockItem(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCK.register(name, block);
        DisenchantingTableForge.registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ITEM.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(IContainerFactory<T> factory, String name) {
        return MENU_TYPE.register(name, () -> IForgeMenuType.create(factory));
    }
}