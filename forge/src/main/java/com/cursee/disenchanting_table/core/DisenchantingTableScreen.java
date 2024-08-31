package com.cursee.disenchanting_table.core;

import com.cursee.disenchanting_table.Constants;
import com.cursee.disenchanting_table.DisenchantingTable;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class DisenchantingTableScreen extends AbstractContainerScreen<DisenchantingTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,"textures/gui/container/disenchanting.png");

    private final @NotNull Player player;
    private final @NotNull DisenchantingTableMenu menu;

    private int playerExperienceLevels;

    public DisenchantingTableScreen(@NotNull DisenchantingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.menu = menu;
        this.player = inventory.player;
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelY += 9999;
        this.inventoryLabelY += 9999;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, 0, 0, 0.0f);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override @SuppressWarnings("all")
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        if (this.minecraft != null && this.minecraft.player != null && DisenchantingTable.experienceIsRequired) {

            Player player = this.minecraft.player;

            if (!player.getAbilities().instabuild) {

                int color = 0xFFFF6060; // red ARGB
                Component component = Component.literal("Cost: 5 Levels");

                if (player.experienceLevel >= this.menu.cost) {
                    color = 0xFF60FF60; // green ARGB
                }

                int minimumX = this.imageWidth - this.font.width(component)-10;
                int textBGTopLeftY = 67;

                int xMod = 1;
                int yMod = 4;

                graphics.fill(minimumX - 2+xMod, textBGTopLeftY+yMod, this.imageWidth - 8+xMod, 79+yMod, 0xFF45327F);
                graphics.drawString(this.font, component, minimumX+xMod, textBGTopLeftY+2+yMod, color);
            }
        }
    }
}
