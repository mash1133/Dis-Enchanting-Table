package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableFabric;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DisenchantingTableBlock extends BaseEntityBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public DisenchantingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisenchantingTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return createTickerHelper(type, DisenchantingTableFabric.DISENCHANTING_TABLE_BLOCK_ENTITY, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    private void doServerInteraction(Level level, BlockPos pos, Player player) {

        MenuProvider screenHandlerFactory = ((DisenchantingTableBlockEntity) level.getBlockEntity(pos));

        if (screenHandlerFactory != null) {
            player.openMenu(screenHandlerFactory);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos pos, Player player, BlockHitResult blockHitResult) {

        if (!level.isClientSide()) {
            this.doServerInteraction(level, pos, player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {

        if (!level.isClientSide()) {
            this.doServerInteraction(level, pos, player);
        }

        return ItemInteractionResult.SUCCESS;
    }

    public static final MapCodec<DisenchantingTableBlock> CODEC = simpleCodec(DisenchantingTableBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DisenchantingTableBlockEntity) {
                Containers.dropContents(world, pos, (DisenchantingTableBlockEntity)blockEntity);
                world.updateNeighbourForOutputSignal(pos,this);
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
        for(int i = 0; i < 3; ++i) {
            int j = randomSource.nextInt(2) * 2 - 1;
            int k = randomSource.nextInt(2) * 2 - 1;
            double d = (double)blockPos.getX() + 0.5 + 0.25 * (double)j;
            double e = (double)((float)blockPos.getY() + randomSource.nextFloat());
            double f = (double)blockPos.getZ() + 0.5 + 0.25 * (double)k;
            double g = (double)(randomSource.nextFloat() * (float)j);
            double h = ((double)randomSource.nextFloat() - 0.5) * 0.125;
            double l = (double)(randomSource.nextFloat() * (float)k);
            level.addParticle(ParticleTypes.PORTAL, d, e, f, g, h, l);
        }
    }
}
