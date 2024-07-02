package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableForge;
import com.cursee.disenchanting_table.core.util.InventoryDirectionEntry;
import com.cursee.disenchanting_table.core.util.InventoryDirectionWrapper;
import com.cursee.disenchanting_table.core.util.WrappedHandler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DisenchantingTableBlockEntity extends BlockEntity implements MenuProvider {

    protected static final int TOTAL_SLOTS = 3;
    protected static final int ITEM_INPUT_SLOT = 0;
    protected static final int BOOK_INPUT_SLOT = 1;
    protected static final int OUTPUT_SLOT = 2;

    private int progress = 0;
    private int maxProgress = 10;
    protected final ContainerData data;

    public DisenchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(DisenchantingTableForge.DISENCHANTING_TABLE_BLOCK_ENTITY.get(), pos, state);

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

    private boolean hasProperInput() {
        return EnchantmentHelper.hasAnyEnchantments(this.itemHandler.getStackInSlot(ITEM_INPUT_SLOT)) && this.itemHandler.getStackInSlot(BOOK_INPUT_SLOT).getItem() == Items.BOOK;
    }

    // ITEM STACK HANDLING

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {

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
                case 0 -> EnchantmentHelper.hasAnyEnchantments(stack); // only insert if enchanted
                case 1 -> stack.getItem() == Items.BOOK; // only insert regular books
                case 2 -> false; // never insertable
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final Map<Direction, LazyOptional<WrappedHandler>> directionWrappedHandlerMap =
        new InventoryDirectionWrapper(itemHandler,
            new InventoryDirectionEntry(Direction.NORTH, BOOK_INPUT_SLOT, true),
            new InventoryDirectionEntry(Direction.EAST, BOOK_INPUT_SLOT, true),
            new InventoryDirectionEntry(Direction.SOUTH, BOOK_INPUT_SLOT, true),
            new InventoryDirectionEntry(Direction.WEST, BOOK_INPUT_SLOT, true),
            new InventoryDirectionEntry(Direction.DOWN, BOOK_INPUT_SLOT, true),
            new InventoryDirectionEntry(Direction.UP, BOOK_INPUT_SLOT, true)
        ).directionsMap;

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize() >= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count;
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() < this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    private boolean outputSlotIsEmpty() {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {

        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            if(side == null) {
                return lazyItemHandler.cast();
            }

            if(directionWrappedHandlerMap.containsKey(side)) {
                Direction localDir = this.getBlockState().getValue(DisenchantingTableBlock.FACING);

                if(side == Direction.DOWN ||side == Direction.UP) {
                    return directionWrappedHandlerMap.get(side).cast();
                }

                return switch (localDir) {
                    default -> directionWrappedHandlerMap.get(side.getOpposite()).cast();
                    case EAST -> directionWrappedHandlerMap.get(side.getClockWise()).cast();
                    case SOUTH -> directionWrappedHandlerMap.get(side).cast();
                    case WEST -> directionWrappedHandlerMap.get(side.getCounterClockWise()).cast();
                };
            }
        }

        return super.getCapability(cap, side);
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

//    @Override
//    public @NotNull CompoundTag getUpdateTag() {
//        return saveWithoutMetadata();
//    }
//
//    @Override
//    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
//        super.onDataPacket(net, pkt);
//    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

//    @Override
//    protected void saveAdditional(CompoundTag pTag) {
//        pTag.put("inventory", itemHandler.serializeNBT());
//        pTag.putInt("disenchanting_table.progress", progress);
//        pTag.putInt("disenchanting_table.max_progress", maxProgress);
//
//        super.saveAdditional(pTag);
//    }
//
//    @Override
//    public void load(CompoundTag pTag) {
//        super.load(pTag);
//        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
//        progress = pTag.getInt("disenchanting_table.progress");
//        maxProgress = pTag.getInt("disenchanting_table.max_progress");
//
//    }


    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider provider) {
        pTag.put("inventory", itemHandler.serializeNBT(provider));
        pTag.putInt("disenchanting_table.progress", progress);
        pTag.putInt("disenchanting_table.max_progress", maxProgress);
        super.saveAdditional(pTag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider provider) {
        super.loadAdditional(pTag, provider);
        itemHandler.deserializeNBT(provider, pTag.getCompound("inventory"));
        progress = pTag.getInt("disenchanting_table.progress");
        maxProgress = pTag.getInt("disenchanting_table.max_progress");
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

        ItemStack input = this.itemHandler.getStackInSlot(ITEM_INPUT_SLOT);

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

                this.itemHandler.setStackInSlot(ITEM_INPUT_SLOT, input);
                this.itemHandler.extractItem(BOOK_INPUT_SLOT, 1, false);
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, EnchantedBookItem.createForEnchantment(extractedEnchantmentInstance));
            }
        }
        else {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(input.getEnchantments());

            if (!takeExperienceFromNearestPlayer(level, pos)) {
                return; // do nothing if no player with enough experience was found or no creative player
            }

            this.itemHandler.extractItem(BOOK_INPUT_SLOT, 1, false);

            // create a new enchanted book, and copy the input enchantments to it.
            ItemStack returnedEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.setEnchantments(returnedEnchantedBook, mutable.toImmutable());

            // set the input item's enchantments to be empty
            EnchantmentHelper.setEnchantments(input, ItemEnchantments.EMPTY);

            // return the removed enchantments and original item to the player
            this.itemHandler.setStackInSlot(OUTPUT_SLOT, returnedEnchantedBook);
            this.itemHandler.setStackInSlot(ITEM_INPUT_SLOT, input);
        }
    }
}
