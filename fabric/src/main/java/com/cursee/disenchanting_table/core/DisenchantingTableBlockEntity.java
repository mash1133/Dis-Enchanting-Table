package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableFabric;
import com.cursee.disenchanting_table.core.util.ImplementedInventory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DisenchantingTableBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    public static final int TOTAL_SLOTS = 3;
    private static final int ITEM_INPUT_SLOT = 0;
    private static final int BOOK_INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    protected final ContainerData propertyDelegate;
    private int progress = 0;
    private int maxProgress = 10;

    public @Nullable Player player = null;

    public DisenchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(DisenchantingTableFabric.DISENCHANTING_TABLE_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new ContainerData() {
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
                    case 0: DisenchantingTableBlockEntity.this.progress = value;
                    case 1: DisenchantingTableBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        this.player = player;
        return new DisenchantingTableScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, inventory);
        nbt.putInt("disenchanting_table.progress", progress);
    }

    @Override
    public void load(CompoundTag nbt) {
        ContainerHelper.loadAllItems(nbt, inventory);
        progress = nbt.getInt("disenchanting_table.progress");
        super.load(nbt);
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
        ItemStack inputItem = this.getItem(ITEM_INPUT_SLOT);
        ItemStack inputItemCopy = inputItem.copy();
        ItemStack inputBlankBook = this.getItem(BOOK_INPUT_SLOT);
        ItemStack outputBook = this.getItem(OUTPUT_SLOT);

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
                EnchantmentHelper.setEnchantments(emptyEnchantmentMap, inputItemCopy);

                // remove current items in slots
                this.removeItem(ITEM_INPUT_SLOT, 1);
                this.removeItem(BOOK_INPUT_SLOT, 1);

                // return dis-enchanted item and book with all enchantments
                this.setItem(ITEM_INPUT_SLOT, inputItemCopy);

//                if (this.player != null) {
//                    this.player.sendSystemMessage(Component.literal(this.getItem(ITEM_INPUT_SLOT).getDisplayName().toString()));
//                }

                this.setItem(OUTPUT_SLOT, returnedEnchantedBook);
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

                // System.out.println("taking first enchantment from book");

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
                this.removeItem(ITEM_INPUT_SLOT, 1);
                this.removeItem(BOOK_INPUT_SLOT, 1);

                // return dis-enchanted item and book with all enchantments
                this.setItem(ITEM_INPUT_SLOT, returnedItem);
                this.setItem(OUTPUT_SLOT, returnedEnchantedBook);

                // original implementation end

//                // gather all enchantments from input item as a map
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
//                inputItemEnchantmentsMap.remove(allEnchantmentsFromBookInput.get(0));
//
//                // define our return item with same name as input item
//                // returnedItem = new ItemStack(inputItem.getItem()).setHoverName(inputItem.getHoverName());
//
//                // enchant our returned item with original enchantments, minus the copied enchantment
//                EnchantmentHelper.setEnchantments(inputItemEnchantmentsMap, inputItem);
//
//                // remove current items in slots
//                this.removeItem(ITEM_INPUT_SLOT, 1);
//                this.removeItem(BOOK_INPUT_SLOT, 1);
//
//                // return dis-enchanted item and book with all enchantments
//                this.setItem(ITEM_INPUT_SLOT, inputItem);
//                this.setItem(OUTPUT_SLOT, returnedEnchantedBook);


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
        return !EnchantmentHelper.getEnchantments(this.getItem(ITEM_INPUT_SLOT)).isEmpty() && this.getItem(BOOK_INPUT_SLOT).getItem() == Items.BOOK;
    }

    private boolean outputSlotIsEmpty() {
        return this.getItem(OUTPUT_SLOT).isEmpty();
    }
}
