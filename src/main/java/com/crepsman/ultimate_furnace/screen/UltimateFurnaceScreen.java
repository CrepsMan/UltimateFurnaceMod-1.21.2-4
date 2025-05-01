package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.recipebook.AbstractFurnaceRecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;


import java.util.Arrays;
import java.util.List;


public class UltimateFurnaceScreen extends AbstractFurnaceScreen<UltimateFurnaceScreenHandler> {
	private static final Identifier LIT_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/lit_progress");
	private static final Identifier BURN_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/burn_progress");
	private static final Identifier TEXTURE = Identifier.of(UltimateFurnaceMod.MOD_ID, "textures/gui/container/ultimate_furnace.png");
	private static final Identifier LIT_HOT = Identifier.of(UltimateFurnaceMod.MOD_ID, "container/ultimate_furnace/lit_hot");
	private static final Identifier ULTIMATE_BAR = Identifier.of(UltimateFurnaceMod.MOD_ID, "container/ultimate_furnace/ultimate_bar");

	public UltimateFurnaceScreen(UltimateFurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, new UltimateFurnaceRecipeBookScreen(), inventory, title, TEXTURE, LIT_PROGRESS_TEXTURE, BURN_PROGRESS_TEXTURE);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int i = this.x;
		int j = this.y;
		context.drawTexture(TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);

		// Draw the burn item fire icon
		if (this.handler.isBurning()) {
			context.drawGuiTexture(LIT_PROGRESS_TEXTURE, 14, 14, 0, 0, i + 56, j + 36, 14, 14);		}

		// Draw the smelting progress arrow
		int progressPercentage = this.handler.getCookingProgress();
		int l = MathHelper.ceil(progressPercentage * 24.0F / 100.0F);
		context.drawGuiTexture(BURN_PROGRESS_TEXTURE, 24, 16, 0, 0, i + 79, j + 34, l, 16);

		// Draw the ultimate bar
		int smeltCountProgress = this.handler.getSmeltCountProgress();
		if (this.handler.getLevel() == 5) {
			smeltCountProgress = 100;
		}
		if (smeltCountProgress > 0) {
			int scaledSmeltCountWidth = smeltCountProgress * 162 / 100;
			context.drawGuiTexture(ULTIMATE_BAR, 162, 5, 0, 0,this.x + 7, this.y + this.backgroundHeight - 101, scaledSmeltCountWidth, 5);		}

		// Draw the stored power bar
		int storedPowerPercentage = this.handler.getStoredPower();
		int scaledPowerWidth = MathHelper.ceil(storedPowerPercentage * 18.0F / 100.0F);
		context.drawGuiTexture(LIT_HOT, 18, 5, 0, 0, this.x + 55, this.y + 52, scaledPowerWidth, 5);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		super.drawMouseoverTooltip(context, mouseX, mouseY);

		// Tooltip for the stored power bar
		if (mouseX >= this.x + 55 && mouseX <= this.x + 73 && mouseY >= this.y + 52 && mouseY <= this.y + 57) {
			int storedPower = this.handler.getStoredPower();
			int maxPower = UltimateFurnaceBlockEntity.getMaxStoredPower(this.handler.getLevel());
			if (maxPower > 0) {
				String tooltipText = "Stored Power: " + storedPower + "%";
				context.drawOrderedTooltip(this.textRenderer, Arrays.asList(Text.literal(tooltipText).asOrderedText()), mouseX, mouseY);
			}
		}

		// Tooltip for the smelt count progress bar
		if (mouseX >= this.x + 7 && mouseX <= this.x + 7 + 161 && mouseY >= this.y + 65 && mouseY <= this.y + 70) {
			int smeltCount = this.handler.getSmeltCount();
			int currentLevel = this.handler.getLevel();
			int maxSmeltCount = this.handler.getMaxSmeltCountForLevel();
			String tooltipText;

			if (maxSmeltCount > 0) {
				if (currentLevel < 5) {
					tooltipText = smeltCount + " / " + maxSmeltCount + " to level " + (currentLevel + 1);
				} else {
					tooltipText = smeltCount + " (Max level reached)";
				}
				context.drawOrderedTooltip(this.textRenderer, Arrays.asList(Text.literal(tooltipText).asOrderedText()), mouseX, mouseY);
			}
		}
	}
}
