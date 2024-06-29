package com.cursee.disenchanting_table.core;

//import com.cursee.disenchanting_table.DisenchantingTableForge;
import com.cursee.disenchanting_table.DisenchantingTableNeoForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.capabilities.Capabilities;
import net.neoforged.neoforge.items.SlotItemHandler;
//import net.minecraftforge.common.capabilities.ForgeCapabilities;
//import net.minecraftforge.items.SlotItemHandler;

public class DisenchantingTableMenu extends AbstractContainerMenu {

    private final Level level;
    public final int cost = 5;
    private final ContainerData data;
    public boolean playerCanAfford = false;
    private final DisenchantingTableBlockEntity entity;

    public DisenchantingTableMenu(int containerID, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerID, inventory, inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(2));
    }

    public DisenchantingTableMenu(int containerID, Inventory inventory, BlockEntity entity, ContainerData data) {
        super(DisenchantingTableNeoForge.DISENCHANTING_TABLE_MENU.get(), containerID);

        DisenchantingTableMenu.checkContainerSize(inventory, DisenchantingTableBlockEntity.TOTAL_SLOTS); // throws illegal argument if the container has been modified to the wrong size

        this.data = data;
        this.level = inventory.player.level();
        this.entity = (DisenchantingTableBlockEntity) entity;
        this.playerCanAfford = inventory.player.experienceLevel >= cost;

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        this.entity.getCapability(Capabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, DisenchantingTableBlockEntity.ITEM_INPUT_SLOT, 27, 47));
            this.addSlot(new SlotItemHandler(handler, DisenchantingTableBlockEntity.BOOK_INPUT_SLOT, 76, 47));
            this.addSlot(new SlotItemHandler(handler, DisenchantingTableBlockEntity.OUTPUT_SLOT, 134, 47));
        });

        this.addDataSlots(data);
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

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, entity.getBlockPos()), pPlayer, DisenchantingTableNeoForge.DISENCHANTING_TABLE_BLOCK.get());
    }

    // credit to diesieben07 | https://github.com/diesieben07/SevenCommons
    // handles moving stack to either player inventory or tile entity inventory

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TILE_ENTITY_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    @Override @SuppressWarnings("all")
    public ItemStack quickMoveStack(Player player, int slotIndex) {

        Slot slot = slots.get(slotIndex);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY; // EMPTY_ITEM
        }

        ItemStack stack = slot.getItem();
        ItemStack stackCopy = stack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (slotIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(stack, TILE_ENTITY_INVENTORY_FIRST_SLOT_INDEX, TILE_ENTITY_INVENTORY_FIRST_SLOT_INDEX
                    + DisenchantingTableBlockEntity.TOTAL_SLOTS, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        }
        else if (slotIndex < TILE_ENTITY_INVENTORY_FIRST_SLOT_INDEX + DisenchantingTableBlockEntity.TOTAL_SLOTS) {
            // This is a Tile Entity slot so merge the stack into the players inventory
            if (!moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            System.out.println("Invalid slotIndex:" + slotIndex);
            return ItemStack.EMPTY;
        }

        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (stack.getCount() == 0) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);

        return stackCopy;
    }
}
