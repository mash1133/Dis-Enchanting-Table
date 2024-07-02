package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.Constants;
import com.cursee.disenchanting_table.DisenchantingTableNeoForge;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DisenchantingTableBlockEntity extends BlockEntity implements MenuProvider, IAttachmentHolder {

    protected static final int TOTAL_SLOTS = 3;
    protected static final int ITEM_INPUT_SLOT = 0;
    protected static final int BOOK_INPUT_SLOT = 1;
    protected static final int OUTPUT_SLOT = 2;

    public static final BlockCapability<ItemHandler, Void> ITEM_HANDLER_NO_CONTEXT = BlockCapability.createVoid( ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "item_handler_no_context"), ItemHandler.class);
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
                case 0 -> EnchantmentHelper.hasAnyEnchantments(stack);
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

//    @Override
//    public @NotNull CompoundTag getUpdateTag() {
//        return saveWithoutMetadata();
//    }
//
//    @Override
//    protected void saveAdditional(CompoundTag pTag) {
//        pTag.put("inventory", itemStackHandler.serializeNBT());
//        pTag.putInt("disenchanting_table.progress", progress);
//        pTag.putInt("disenchanting_table.max_progress", maxProgress);
//
//        super.saveAdditional(pTag);
//    }
//
//    @Override
//    public void load(CompoundTag pTag) {
//        super.load(pTag);
//        itemStackHandler.deserializeNBT(pTag.getCompound("inventory"));
//        progress = pTag.getInt("disenchanting_table.progress");
//        maxProgress = pTag.getInt("disenchanting_table.max_progress");
//    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider holder) {
        return saveWithoutMetadata(holder);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider holder) {
        pTag.put("inventory", itemStackHandler.serializeNBT(holder));
        pTag.putInt("disenchanting_table.progress", progress);
        pTag.putInt("disenchanting_table.max_progress", maxProgress);
        super.saveAdditional(pTag, holder);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider holder) {
        super.loadAdditional(pTag, holder);
        itemStackHandler.deserializeNBT(holder, pTag.getCompound("inventory"));
        progress = pTag.getInt("disenchanting_table.progress");
        maxProgress = pTag.getInt("disenchanting_table.max_progress");
    }

    private boolean hasProperInput() {
        return EnchantmentHelper.hasAnyEnchantments(this.itemStackHandler.getStackInSlot(0)) && this.itemStackHandler.getStackInSlot(1).getItem() == Items.BOOK;
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

        // we can assume
        // ~ slot 1 is an ItemStack with at least one enchantment -> hasProperInput()
        // ~ slot 2 is an ItemStack of Items.BOOK with size >= 1 -> hasProperInput()
        // ~ slot 3 is an empty ItemStack -> outputSlotIsEmpty()

        // goals
        // ~ remove all enchantments if the input item is not an enchanted book
        // ~ remove one enchantment if the input item is an enchanted book with >= 2 enchantments

        // current issues
        // !! [FIXED?] splitting enchantments from an enchanted book corrupts the current world data

        ItemStack input = this.itemStackHandler.getStackInSlot(ITEM_INPUT_SLOT);

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

                this.itemStackHandler.setStackInSlot(ITEM_INPUT_SLOT, input);
                this.itemStackHandler.extractItem(BOOK_INPUT_SLOT, 1, false);
                this.itemStackHandler.setStackInSlot(OUTPUT_SLOT, EnchantedBookItem.createForEnchantment(extractedEnchantmentInstance));
            }
        }
        else {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(input.getEnchantments());

            if (!takeExperienceFromNearestPlayer(level, pos)) {
                return; // do nothing if no player with enough experience was found or no creative player
            }

            this.itemStackHandler.extractItem(BOOK_INPUT_SLOT, 1, false);

            // create a new enchanted book, and copy the input enchantments to it.
            ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.setEnchantments(returnedEnchantedBook, mutable.toImmutable());

            // set the input item's enchantments to be empty
            EnchantmentHelper.setEnchantments(input, ItemEnchantments.EMPTY);

            // return the removed enchantments and original item to the player
            this.itemStackHandler.setStackInSlot(OUTPUT_SLOT, returnedEnchantedBook);
            this.itemStackHandler.setStackInSlot(ITEM_INPUT_SLOT, input);
        }
    }
}
