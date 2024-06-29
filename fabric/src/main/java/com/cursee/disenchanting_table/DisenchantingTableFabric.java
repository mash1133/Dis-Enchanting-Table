package com.cursee.disenchanting_table;

import net.fabricmc.api.ModInitializer;

public class DisenchantingTableFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        
        DisenchantingTable.init();
    }
}
