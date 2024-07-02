package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableFabric;
import com.cursee.disenchanting_table.core.util.ImplementedInventory;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.windows.INPUT;

import java.util.function.Predicate;

public class DisenchantingTableBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<Object>, ImplementedInventory {

    public static final int TOTAL_SLOTS = 3;
    public static final int ITEM_INPUT_SLOT = 0;
    public static final int BOOK_INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private int progress = 0;
    private int maxProgress = 10; // do not make static or final. may change in the future for recipe handling
    protected final ContainerData propertyDelegate;
    public @Nullable Player player;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

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
    public Component getDisplayName() {
        return Component.literal("Dis-Enchanting Table!");
    }

    @Override
    public Object getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        this.player = player;
        return new DisenchantingTableScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

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

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    private boolean hasProperInput() {
        return EnchantmentHelper.hasAnyEnchantments(this.getItem(ITEM_INPUT_SLOT)) && this.getItem(BOOK_INPUT_SLOT).getItem() == Items.BOOK;
    }

    private boolean outputSlotIsEmpty() {
        return this.getItem(OUTPUT_SLOT).isEmpty();
    }

    private static boolean takeExperienceFromNearestPlayer(Level level, BlockPos pos) {

        // TargetingConditions.forNonCombat() <-- this is important
        @Nullable ServerPlayer player = (ServerPlayer) level.getNearestPlayer(TargetingConditions.forNonCombat(), pos.getX(), pos.getY(), pos.getZ());

        if (player != null && player.getAbilities().instabuild) {

            if (player.isCrouching() && ((DisenchantingTableBlockEntity) level.getBlockEntity(pos)) != null && ((DisenchantingTableBlockEntity) level.getBlockEntity(pos)).getItem(ITEM_INPUT_SLOT) != null) {
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

    private void newCraftItem(Level level, BlockPos pos, BlockState state) {

        // we can assume
        // ~ slot 1 is an ItemStack with at least one enchantment -> hasProperInput()
        // ~ slot 2 is an ItemStack of Items.BOOK with size >= 1 -> hasProperInput()
        // ~ slot 3 is an empty ItemStack -> outputSlotIsEmpty()

        // goals
        // ~ remove all enchantments if the input item is not an enchanted book
        // ~ remove one enchantment if the input item is an enchanted book with >= 2 enchantments

        // current issues
        // !! splitting enchantments from an enchanted book corrupts the current world data

        ItemStack input = this.getItem(ITEM_INPUT_SLOT);

        if (input.getItem() == Items.ENCHANTED_BOOK) {

            ItemEnchantments enchantments = input.getComponents().get(DataComponents.STORED_ENCHANTMENTS);

            // book may be raw item with no stored enchantments present
            if (enchantments != null && enchantments.size() >= 2) {
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);

                if (!takeExperienceFromNearestPlayer(level, pos)) {
                    return; // do nothing if no player with enough experience was found or no creative player
                }

                Object2IntMap.Entry<Holder<Enchantment>>[] extractedEnchantmentEntry = new Object2IntMap.Entry[1];
                enchantments.entrySet().forEach(holderEntry -> extractedEnchantmentEntry[0] = holderEntry);

                EnchantmentInstance extractedEnchantmentInstance = new EnchantmentInstance(extractedEnchantmentEntry[0].getKey(), extractedEnchantmentEntry[0].getIntValue());

                mutable.removeIf(Predicate.isEqual(extractedEnchantmentEntry[0].getKey()));

                EnchantmentHelper.setEnchantments(input, mutable.toImmutable());

                this.setItem(ITEM_INPUT_SLOT, input);
                this.removeItem(BOOK_INPUT_SLOT, 1);
                this.setItem(OUTPUT_SLOT, EnchantedBookItem.createForEnchantment(extractedEnchantmentInstance));
            }
        }
        else {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(input.getEnchantments());

            if (!takeExperienceFromNearestPlayer(level, pos)) {
                return; // do nothing if no player with enough experience was found or no creative player
            }

            this.removeItem(BOOK_INPUT_SLOT, 1);

            // create a new enchanted book, and copy the input enchantments to it.
            ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.setEnchantments(returnedEnchantedBook, mutable.toImmutable());

            // set the input item's enchantments to be empty
            EnchantmentHelper.setEnchantments(input, ItemEnchantments.EMPTY);

            // return the removed enchantments and original item to the player
            this.setItem(OUTPUT_SLOT, returnedEnchantedBook);
            this.setItem(ITEM_INPUT_SLOT, input);
        }
    }
}
