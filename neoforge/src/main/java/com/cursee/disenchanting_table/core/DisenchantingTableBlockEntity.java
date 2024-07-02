package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.Constants;
import com.cursee.disenchanting_table.DisenchantingTableNeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.jarjar.nio.util.Lazy;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.*;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DisenchantingTableBlockEntity extends BlockEntity implements MenuProvider, IAttachmentHolder {

    public static final BlockCapability<ItemHandler, Void> ITEM_HANDLER_NO_CONTEXT = BlockCapability.createVoid( new ResourceLocation(Constants.MOD_ID, "item_handler_no_context"), ItemHandler.class);
    public final IItemHandlerModifiable handler = new IItemHandlerModifiable() {
        @Override
        public void setStackInSlot(int i, ItemStack itemStack) {
            DisenchantingTableBlockEntity.this.itemStackHandler.setStackInSlot(i, itemStack);
        }

        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return DisenchantingTableBlockEntity.this.itemStackHandler.getStackInSlot(i);
        }

        @Override
        public ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
            return DisenchantingTableBlockEntity.this.itemStackHandler.insertItem(i, itemStack, b);
        }

        @Override
        public ItemStack extractItem(int index, int amount, boolean simulate) {
            return DisenchantingTableBlockEntity.this.itemStackHandler.extractItem(index, amount, simulate);
        }

        @Override
        public int getSlotLimit(int i) {
            return DisenchantingTableBlockEntity.this.itemStackHandler.getSlotLimit(i);
        }

        @Override
        public boolean isItemValid(int i, ItemStack itemStack) {
            return DisenchantingTableBlockEntity.this.itemStackHandler.isItemValid(i, itemStack);
        }
    };

    private int progress = 0;
    private int maxProgress = 10;
    protected final ContainerData data;

    public DisenchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(DisenchantingTableNeoForge.DISENCHANTING_TABLE_BLOCK_ENTITY.get(), pos, state);

//        if (this.level != null) {
//            this.handler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
//        }

        this.data = new ContainerData() {

            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> DisenchantingTableBlockEntity.this.progress;
                    case 1 -> DisenchantingTableBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> DisenchantingTableBlockEntity.this.progress = value;
                    case 1 -> DisenchantingTableBlockEntity.this.maxProgress = value;
                };
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    private final ItemStackHandler itemStackHandler = new ItemStackHandler(3) {

        @Override
        protected void onContentsChanged(int slot) {
            DisenchantingTableBlockEntity.super.setChanged();

            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), DisenchantingTableBlock.UPDATE_ALL);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> !EnchantmentHelper.getEnchantments(stack).isEmpty();
                case 1 -> stack.getItem() == Items.BOOK;
                case 2 -> false;
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    public void drops() {
        if (this.level != null) {

            SimpleContainer inventory = new SimpleContainer(itemStackHandler.getSlots());

            for (int i = 0; i < itemStackHandler.getSlots(); i++) {
                inventory.setItem(i, itemStackHandler.getStackInSlot(i));
            }

            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Dis-Enchanting Table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerID, Inventory inventory, Player player) {
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
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemStackHandler.serializeNBT());
        pTag.putInt("disenchanting_table.progress", progress);
        pTag.putInt("disenchanting_table.max_progress", maxProgress);

        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemStackHandler.deserializeNBT(pTag.getCompound("inventory"));
        progress = pTag.getInt("disenchanting_table.progress");
        maxProgress = pTag.getInt("disenchanting_table.max_progress");
    }

    private boolean hasProperInput() {
        return !EnchantmentHelper.getEnchantments(this.itemStackHandler.getStackInSlot(0)).isEmpty() && this.itemStackHandler.getStackInSlot(1).getItem() == Items.BOOK;
    }

    private boolean outputSlotIsEmpty() {
        return this.itemStackHandler.getStackInSlot(2).isEmpty();
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

    private void craftItem(Level level, BlockPos pos, BlockState state) {

        // we already know that slot 1 has enchantments, slot 2 is a vanilla book, and slot 3 is empty

        // intitialize enchanted book to return
        ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);

        // gather input item stacks
        ItemStack inputItem = this.itemStackHandler.getStackInSlot(0);
        ItemStack inputBlankBook = this.itemStackHandler.getStackInSlot(1);
        ItemStack outputBook = this.itemStackHandler.getStackInSlot(2);

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
                this.itemStackHandler.extractItem(0, 1, false);
                this.itemStackHandler.extractItem(1, 1, false);

                // return dis-enchanted item and book with all enchantments
                this.itemStackHandler.setStackInSlot(0, inputItem);
                this.itemStackHandler.setStackInSlot(2, returnedEnchantedBook);
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
                this.itemStackHandler.extractItem(0, 1, false);
                this.itemStackHandler.extractItem(1, 1, false);

                // return dis-enchanted item and book with all enchantments
                this.itemStackHandler.setStackInSlot(0, returnedItem);
                this.itemStackHandler.setStackInSlot(2, returnedEnchantedBook);

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
}
