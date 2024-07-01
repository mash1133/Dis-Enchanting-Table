package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableNeoForge;
import com.cursee.disenchanting_table.core.util.InventoryDirectionEntry;
import com.cursee.disenchanting_table.core.util.InventoryDirectionWrapper;
import com.cursee.disenchanting_table.core.util.WrappedHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//import net.minecraftforge.common.capabilities.Capability;
//import net.minecraftforge.common.capabilities.ForgeCapabilities;
//import net.minecraftforge.common.util.LazyOptional;
//import net.minecraftforge.items.IItemHandler;
//import net.minecraftforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DisenchantingTableBlockEntity extends BlockEntity implements MenuProvider {

    protected static final int TOTAL_SLOTS = 3;
    protected static final int ITEM_INPUT_SLOT = 0;
    protected static final int BOOK_INPUT_SLOT = 1;
    protected static final int OUTPUT_SLOT = 2;

    private int progress = 0;
    private int maxProgress = 10;
    protected final ContainerData data;

    public DisenchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(DisenchantingTableNeoForge.DISENCHANTING_TABLE_BLOCK_ENTITY.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> DisenchantingTableBlockEntity.this.progress;
                    case 1 -> DisenchantingTableBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> DisenchantingTableBlockEntity.this.progress = pValue;
                    case 1 -> DisenchantingTableBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {

        if (level == null || level.isClientSide()) {
            return;
        }

        if (this.hasProperInput() && this.outputSlotIsEmpty()) {

            // System.out.println("progress " + this.progress);

            this.progress++;
            DisenchantingTableBlockEntity.setChanged(level, pos, state);

            if (this.progress >= this.maxProgress) {
                this.craftItem(level, pos, state);
                this.progress = 0;
            }
        }
        else {
            this.progress = 0;
        }
    }

    private static boolean takeExperienceFromNearestPlayer(Level level, BlockPos pos) {

        // TargetingConditions.forNonCombat() <-- this is important
        @Nullable ServerPlayer player = (ServerPlayer) level.getNearestPlayer(TargetingConditions.forNonCombat(), pos.getX(), pos.getY(), pos.getZ());

        if (player != null && player.getAbilities().instabuild) {
            return true;
        }

        if (player != null && !player.getAbilities().instabuild && player.experienceLevel >= 5) {
            player.setExperienceLevels(player.experienceLevel-5);
            return true;
        }

        return false;
    }

    private void craftItem(Level level, BlockPos pos, BlockState state) {

        // we already know that slot 1 has enchantments, slot 2 is a vanilla book, and slot 3 is empty

        // intitialize enchanted book to return
        ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);

        // gather input item stacks
        ItemStack inputItem = this.itemHandler.getStackInSlot(ITEM_INPUT_SLOT);
        ItemStack inputBlankBook = this.itemHandler.getStackInSlot(BOOK_INPUT_SLOT);
        ItemStack outputBook = this.itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (inputItem.getItem() != Items.ENCHANTED_BOOK && !EnchantmentHelper.getEnchantments(inputItem).isEmpty()) {
            // operating on regular item
            // take all enchantments off of item
            // todo: config to take first, last, or random

            if (!DisenchantingTableBlockEntity.takeExperienceFromNearestPlayer(level, pos)) {
                // System.out.println("could not take experience!");
                return;
            }

            if (false) { // begin config tree structure
                // take first enchantment
            }
            else if (false) {
                // take last enchantment
            }
            else if (false) {
                // take random enchantment
            }
            else {
                // take all enchantments

                // System.out.println("taking all enchantments from item");

                // initialize empty map to override input item
                Map<Enchantment, Integer> emptyEnchantmentMap = EnchantmentHelper.getEnchantments(returnedEnchantedBook);

                // copy input enchantments to returned book
                Map<Enchantment, Integer> inputEnchantmentMap = EnchantmentHelper.getEnchantments(inputItem);
                EnchantmentHelper.setEnchantments(inputEnchantmentMap, returnedEnchantedBook);

                // override input enchantment map
                EnchantmentHelper.setEnchantments(emptyEnchantmentMap, inputItem);

                // remove current items in slots
                this.itemHandler.extractItem(ITEM_INPUT_SLOT, 1, false);
                this.itemHandler.extractItem(BOOK_INPUT_SLOT, 1, false);

                // return dis-enchanted item and book with all enchantments
                this.itemHandler.setStackInSlot(ITEM_INPUT_SLOT, inputItem);
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, returnedEnchantedBook);
            }

        }
        else if (inputItem.getItem() == Items.ENCHANTED_BOOK && EnchantmentHelper.getEnchantments(inputItem).size() >= 2) {
            // operating on enchanted book
            // take one enchantment off of enchanted book with 2 or more enchantments
            // todo: config to take last, random, or all but one

            if (!DisenchantingTableBlockEntity.takeExperienceFromNearestPlayer(level, pos)) {
                // System.out.println("could not take experience from player!");
                return;
            }

            if (true) {
                // take first

                // original implementation

                ItemStack returnedItem = null;

                // gather all enchantments from input item as a map
                Map<Enchantment, Integer> inputItemEnchantmentsMap = EnchantmentHelper.getEnchantments(inputItem);

                // initialize arrays to hold the enchantment keys and levels
                List<Enchantment> allEnchantmentsFromBookInput = new ArrayList<>();
                List<Integer> allLevelsFromBookInput = new ArrayList<>();

                // add the enchantments and levels to their respective arrays
                EnchantmentHelper.getEnchantments(inputItem).forEach((enchantment, integer) -> {
                    allEnchantmentsFromBookInput.add(enchantment);
                    allLevelsFromBookInput.add(integer);
                });

                // set up a blank enchantments map by querying our resultingBookItemStack
                Map<Enchantment, Integer> blankEnchantmentsMapForNewBook = EnchantmentHelper.getEnchantments(returnedEnchantedBook);

                // only copy one enchantment from the input enchanted book
                blankEnchantmentsMapForNewBook.put(allEnchantmentsFromBookInput.get(0), allLevelsFromBookInput.get(0));

                // enchant our output
                EnchantmentHelper.setEnchantments(blankEnchantmentsMapForNewBook, returnedEnchantedBook);

                // remove enchantment we copied
                inputItemEnchantmentsMap.remove(allEnchantmentsFromBookInput.get(0));

                // define our return item with same name as input item
                returnedItem = new ItemStack(inputItem.getItem()).setHoverName(inputItem.getHoverName());

                // enchant our returned item with original enchantments - copied enchantment
                EnchantmentHelper.setEnchantments(inputItemEnchantmentsMap, returnedItem);

                // remove current items in slots
                this.itemHandler.extractItem(ITEM_INPUT_SLOT, 1, false);
                this.itemHandler.extractItem(BOOK_INPUT_SLOT, 1, false);

                // return dis-enchanted item and book with all enchantments
                this.itemHandler.setStackInSlot(ITEM_INPUT_SLOT, returnedItem);
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, returnedEnchantedBook);

                // original implementation end

                // System.out.println("taking first enchantment from book");

//                // gather all enchantments from input item as a map
//                Map<Enchantment, Integer> emptyMap = EnchantmentHelper.getEnchantments(returnedEnchantedBook);
//                Map<Enchantment, Integer> inputItemEnchantmentsMap = EnchantmentHelper.getEnchantments(inputItem);
//
//                // initialize arrays to hold copies of the enchantment keys and levels
//                List<Enchantment> allEnchantmentsFromBookInput = new ArrayList<>();
//                List<Integer> allLevelsFromBookInput = new ArrayList<>();
//
//                // add the enchantments and levels to their respective arrays
//                EnchantmentHelper.getEnchantments(inputItem).forEach((enchantment, integer) -> {
//                    allEnchantmentsFromBookInput.add(enchantment);
//                    allLevelsFromBookInput.add(integer);
//                });
//
//                // initialize map to store the enchantment left on the item
//                Map<Enchantment, Integer> returnedEnchantmentMap = EnchantmentHelper.getEnchantments(returnedEnchantedBook);
//
//                // only copy one enchantment from the input enchanted book
//                returnedEnchantmentMap.put(allEnchantmentsFromBookInput.get(0), allLevelsFromBookInput.get(0));
//
//                // enchant our output book
//                EnchantmentHelper.setEnchantments(returnedEnchantmentMap, returnedEnchantedBook);
//
//                // remove enchantment we copied
//                inputItemEnchantmentsMap.remove((Enchantment) allEnchantmentsFromBookInput.get(0), (Integer) allLevelsFromBookInput.get(0));
//
//                // define our return item with same name as input item
//                // returnedItem = new ItemStack(inputItem.getItem()).setHoverName(inputItem.getHoverName());
//
//                // enchant our returned item with original enchantments, minus the copied enchantment
//                EnchantmentHelper.setEnchantments(emptyMap, inputItem);
//                EnchantmentHelper.setEnchantments(inputItemEnchantmentsMap, inputItem);
//
//                // remove current items in slots
//                this.itemHandler.extractItem(ITEM_INPUT_SLOT, 1, false);
//                this.itemHandler.extractItem(BOOK_INPUT_SLOT, 1, false);
//
//                // return dis-enchanted item and book with all enchantments
//                this.itemHandler.setStackInSlot(ITEM_INPUT_SLOT, inputItem);
//                this.itemHandler.setStackInSlot(OUTPUT_SLOT, returnedEnchantedBook);


            }
            else if (false) {
                // take last
            }
            else if (false) {
                // take random
            }
            else {
                // take all but one
            }
        }
    }

    private boolean hasProperInput() {
        return !EnchantmentHelper.getEnchantments(this.itemHandler.getStackInSlot(ITEM_INPUT_SLOT)).isEmpty() && this.itemHandler.getStackInSlot(BOOK_INPUT_SLOT).getItem() == Items.BOOK;
    }

    // ITEM STACK HANDLING

    public final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {

        @Override
        protected void onContentsChanged(int slot) {
            DisenchantingTableBlockEntity.super.setChanged();
            if(level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            /*
             * 0 -> expected input for an enchanted item (tool/armor/book/etc.)
             * 1 -> expected input for a regular book
             * 2 -> output for enchanted book
             */
            return switch (slot) {
                case 0 -> !EnchantmentHelper.getEnchantments(stack).isEmpty(); // only insert if enchanted
                case 1 -> stack.getItem() == Items.BOOK; // only insert regular books
                case 2 -> false; // never insertable
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize() >= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count;
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() < this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    private boolean outputSlotIsEmpty() {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    // END ITEM STACK HANDLING

    public void drops() {
        if (this.level != null) {

            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }

            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerID, @NotNull Inventory inventory, @NotNull Player player) {
        return new DisenchantingTableMenu(containerID, inventory, this, this.data);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

//    @Override
//    public void invalidateCaps() {
//        super.invalidateCaps();
//        lazyItemHandler.invalidate();
//    }

    @Override
    public void invalidateCapabilities() {
        super.invalidateCapabilities();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("disenchanting_table.progress", progress);
        pTag.putInt("disenchanting_table.max_progress", maxProgress);

        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        progress = pTag.getInt("disenchanting_table.progress");
        maxProgress = pTag.getInt("disenchanting_table.max_progress");

    }
}
