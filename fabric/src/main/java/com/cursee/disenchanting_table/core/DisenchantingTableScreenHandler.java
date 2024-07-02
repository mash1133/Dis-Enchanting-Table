package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableFabric;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DisenchantingTableScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;
    public final DisenchantingTableBlockEntity blockEntity;

    public DisenchantingTableScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(2));
    }

    public DisenchantingTableScreenHandler(int syncId, Inventory playerInventory,
                                      BlockEntity blockEntity, ContainerData arrayPropertyDelegate) {
        super(DisenchantingTableFabric.DISENCHANTING_TABLE_SCREEN_HANDLER, syncId);
        checkContainerSize(((Container) blockEntity), DisenchantingTableBlockEntity.TOTAL_SLOTS);
        this.inventory = (Container)blockEntity;
        this.propertyDelegate = arrayPropertyDelegate;
        this.blockEntity = ((DisenchantingTableBlockEntity) blockEntity);

        this.addSlot(new Slot(inventory, 0, 27, 47));
        this.addSlot(new Slot(inventory, 1, 76, 47));
        this.addSlot(new Slot(inventory, 2, 134, 47));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addDataSlots(arrayPropertyDelegate);
    }

    public DisenchantingTableScreenHandler(int i, Inventory inventory, Object o) {
        this.inventory = inventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
