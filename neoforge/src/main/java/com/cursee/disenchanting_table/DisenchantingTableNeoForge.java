package com.cursee.disenchanting_table;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class DisenchantingTableNeoForge {

    public DisenchantingTableNeoForge(IEventBus eventBus) {

        DisenchantingTable.init();
    }
}
