package com.cursee.disenchanting_table;

import com.cursee.disenchanting_table.core.DisenchantingTableScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class DisenchantingTableFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        MenuScreens.register(DisenchantingTableFabric.DISENCHANTING_TABLE_SCREEN_HANDLER, DisenchantingTableScreen::new);
    }
}
