package com.cursee.disenchanting_table;

import com.cursee.disenchanting_table.core.DisenchantingTableScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class DisenchantingTableFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        // Constants.LOG.info("Hello Fabric world!");
        // CommonClass.init();

        MenuScreens.register(DisenchantingTableFabric.DISENCHANTING_TABLE_SCREEN_HANDLER, DisenchantingTableScreen::new);
    }
}
