package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableFabric;
import com.cursee.disenchanting_table.core.util.ImplementedInventory;
import com.cursee.monolib.core.MonoLibConfiguration;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.windows.INPUT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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

//    @Override
//    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
//        buf.writeBlockPos(this.worldPosition);
//    }

    @Override
    public Object getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
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

//    @Override
//    protected void saveAdditional(CompoundTag nbt) {
//        super.saveAdditional(nbt);
//        ContainerHelper.saveAllItems(nbt, inventory);
//        nbt.putInt("disenchanting_table.progress", progress);
//    }
//
//    @Override
//    public void load(CompoundTag nbt) {
//        ContainerHelper.loadAllItems(nbt, inventory);
//        progress = nbt.getInt("disenchanting_table.progress");
//        super.load(nbt);
//    }


    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        ContainerHelper.saveAllItems(nbt, inventory, provider);
        nbt.putInt("disenchanting_table.progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        ContainerHelper.loadAllItems(nbt, inventory, provider);
        progress = nbt.getInt("disenchanting_table.progress");
        super.loadAdditional(nbt, provider);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {

        if (level == null || level.isClientSide()) {
            return;
        }

        if (this.hasProperInput() && this.outputSlotIsEmpty()) {

            // debug("progress " + this.progress);

            this.progress++;
            DisenchantingTableBlockEntity.setChanged(level, pos, state);

            if (this.progress >= this.maxProgress) {
                this.newCraftItem(level, pos, state);
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

            if (player.isCrouching() && level.getBlockEntity(pos) != null) {
                level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), ((DisenchantingTableBlockEntity) level.getBlockEntity(pos)).getItem(ITEM_INPUT_SLOT)));
            }

            return true;
        }

        if (player != null && !player.getAbilities().instabuild && player.experienceLevel >= 5) {
            player.setExperienceLevels(player.experienceLevel-5);
            return true;
        }

        return false;
    }

    private void newCraftItem(Level level, BlockPos pos, BlockState state) {

        // we can assume
        // ~ slot 1 is an ItemStack with at least one enchantment -> hasProperInput()
        // ~ slot 2 is an ItemStack of Items.BOOK with size >= 1 -> hasProperInput()
        // ~ slot 3 is an empty ItemStack -> outputSlotIsEmpty()

        // goals
        // ~ remove all enchantments if the input item is not an enchanted book
        // ~ remove one enchantment if the input item is an enchanted book with >= 2 enchantments

        // gather slots and enchantments
        ItemStack inputSlotItem = this.getItem(ITEM_INPUT_SLOT);

        ItemEnchantments inputEnchantments = inputSlotItem.getComponents().get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments outputEnchantments = ItemEnchantments.EMPTY;

        if (inputEnchantments == null) {
            inputEnchantments = inputSlotItem.getEnchantments(); // hacky fix if enchantments not found as data component
        }

        if (!inputEnchantments.isEmpty()) {

            debug("found enchantments on item");

            // ~ remove all enchantments if the input item is not an enchanted book
            if (inputSlotItem.getItem() != Items.ENCHANTED_BOOK && EnchantmentHelper.hasAnyEnchantments(inputSlotItem)) {

                debug("item is not enchanted book");

                if (!takeExperienceFromNearestPlayer(level, pos)) {
                    debug("no player found");
                    return; // do nothing if no player with enough experience was found or no creative player
                }

                this.removeItem(BOOK_INPUT_SLOT, 1);
                debug("removed provided vanilla book from slot");

                // create a new enchanted book, and copy the input enchantments to it.
                ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.setEnchantments(returnedEnchantedBook, inputEnchantments);

                // set the input item's enchantments to be empty
                EnchantmentHelper.setEnchantments(inputSlotItem, ItemEnchantments.EMPTY);

                // return the removed enchantments and original item to the player
                this.setItem(OUTPUT_SLOT, returnedEnchantedBook);
                this.setItem(ITEM_INPUT_SLOT, inputSlotItem);

            }

            // ~ remove one enchantment if the input item is an enchanted book with >= 2 enchantments
            else if (inputSlotItem.getItem() == Items.ENCHANTED_BOOK && inputEnchantments.size() >= 2) {

                debug("item is enchanted book");

                if (!takeExperienceFromNearestPlayer(level, pos)) {
                    debug("no player found");
                    return; // do nothing if no player with enough experience was found or no creative player
                }

                this.removeItem(BOOK_INPUT_SLOT, 1);
                debug("removed provided vanilla book from slot");




                // create variables to hold single enchantment details
                Enchantment[] givenEnchantment = new Enchantment[1];
                Integer[] givenLevel = new Integer[1];

                // extract the last enchantment and level from the input enchantments
                inputEnchantments.entrySet().forEach(enchantment -> {
                    givenEnchantment[0] = enchantment.getKey().value();
                    givenLevel[0] = enchantment.getIntValue();
                });

                debug("Extracted " + givenEnchantment[0].toString() + " with level " + givenLevel[0]);

                ItemEnchantments.Mutable enchantsKeptOnInput = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

                // ignore the extracted enchantment while copying the original enchantments
                inputEnchantments.entrySet().forEach(enchant -> {
                    if (enchant.getKey().value() != givenEnchantment[0]) {
                        enchantsKeptOnInput.set(enchant.getKey(), enchant.getIntValue());
                    }
                });

//                ItemEnchantments.Mutable mutableInputEnchantments = new ItemEnchantments.Mutable(inputEnchantments);
//                mutableInputEnchantments.removeIf(Predicate.isEqual(Holder.direct(givenEnchantment[0])));
                inputEnchantments = enchantsKeptOnInput.toImmutable();




                // overwrite the original output enchantments via ItemEnchantments.Mutable
                ItemEnchantments.Mutable mutableOutputEnchantments = new ItemEnchantments.Mutable(outputEnchantments);
                mutableOutputEnchantments.set(Holder.direct(givenEnchantment[0]), givenLevel[0]);
                outputEnchantments = mutableOutputEnchantments.toImmutable();




                // create a new enchanted book, and copy the output enchantments to it.
                ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.setEnchantments(returnedEnchantedBook, outputEnchantments);

                // set the input item's enchantments to the original enchantments without the removed
                EnchantmentHelper.setEnchantments(inputSlotItem, inputEnchantments);

                // return the removed enchantments and original item to the player
                this.setItem(OUTPUT_SLOT, returnedEnchantedBook);
                this.setItem(ITEM_INPUT_SLOT, inputSlotItem);
            }
        } else {
            debug("FOUND NO ENCHANTMENTS ON ITEM");
        }
    }

    private boolean hasProperInput() {
        return EnchantmentHelper.hasAnyEnchantments(this.getItem(ITEM_INPUT_SLOT)) && this.getItem(BOOK_INPUT_SLOT).getItem() == Items.BOOK;
    }

    private boolean outputSlotIsEmpty() {
        return this.getItem(OUTPUT_SLOT).isEmpty();
    }

    private static void debug(String message) {
        if (DisenchantingTableBlock.debug) {
            System.out.println(message);
        }
    }
}
