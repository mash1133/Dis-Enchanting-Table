package com.cursee.disenchanting_table.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class DisenchantingTableEntityRenderer implements BlockEntityRenderer<DisenchantingTableBlockEntity> {

    public DisenchantingTableEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DisenchantingTableBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        pPoseStack.pushPose();
        pPoseStack.popPose();
    }
}
