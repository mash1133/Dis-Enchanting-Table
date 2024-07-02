package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.DisenchantingTableNeoForge;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DisenchantingTableBlock extends BaseEntityBlock {

    public static final MapCodec<DisenchantingTableBlock> CODEC = simpleCodec(DisenchantingTableBlock::new);
    public static final VoxelShape SHAPE = DisenchantingTableBlock.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public DisenchantingTableBlock(Properties p_49224_) {
        super(p_49224_);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DisenchantingTableBlockEntity(blockPos, blockState);
    }

    /* ADDED */

    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL;
    }

    private void handlePlayerInteraction(Level level, BlockPos pos, Player player) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);

            if (entity instanceof DisenchantingTableBlockEntity) {
                ((ServerPlayer) player).openMenu((MenuProvider) entity, pos);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_60503_, Level level, BlockPos pos, Player player, BlockHitResult p_60508_) {
        this.handlePlayerInteraction(level, pos, player);

        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack p_316304_, BlockState p_316362_, Level level, BlockPos pos, Player player, InteractionHand p_316595_, BlockHitResult p_316140_) {
        this.handlePlayerInteraction(level, pos, player);

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        if(level.isClientSide()) {
            return null;
        }

        return createTickerHelper(type, DisenchantingTableNeoForge.DISENCHANTING_TABLE_BLOCK_ENTITY.get(), (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof DisenchantingTableBlockEntity) {
                ((DisenchantingTableBlockEntity) blockEntity).drops();
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
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
