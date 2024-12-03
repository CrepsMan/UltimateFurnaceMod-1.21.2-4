package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class UltimateFurnaceScreen extends AbstractFurnaceScreen<UltimateFurnaceScreenHandler> {
	private static final Identifier LIT_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/lit_progress");
	private static final Identifier BURN_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/burn_progress");
	private static final Text TOGGLE_SMELTABLE_TEXT = Text.translatable("gui.recipebook.toggleRecipes.smeltable");
	private static final Identifier TEXTURE = Identifier.of(UltimateFurnaceMod.MOD_ID, "textures/gui/container/ultimate_furnace.png");
	private static final Identifier LIT_HOT = Identifier.of(UltimateFurnaceMod.MOD_ID, "container/ultimate_furnace/lit_hot.png");
	private static final Identifier ULTIMATE_BAR = Identifier.of(UltimateFurnaceMod.MOD_ID, "textures/gui/container/sprites/container/ultimate_furnace/ultimate_bar.png");
	private static final List<RecipeBookWidget.Tab> TABS;


	public UltimateFurnaceScreen(UltimateFurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title, TOGGLE_SMELTABLE_TEXT, TEXTURE, LIT_PROGRESS_TEXTURE, BURN_PROGRESS_TEXTURE, TABS);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		// Use a function to return the appropriate RenderLayer for GUI texture1

		// Render the main background texture
		int i = this.x;
		int j = this.y;
		context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);

		// Draw the fuel progress bar
		if (this.handler.isBurning()) {
			int k = 14;
			int l = MathHelper.ceil(this.handler.getFuelProgress() * 13.0F) + 1;
			context.drawGuiTexture(RenderLayer::getGuiTextured, LIT_PROGRESS_TEXTURE, 14, 14, 0, 14 - l, i + 56, j + 36 + 14 - l, 14, l);
		}

		// Draw the smelting progress bar
		int k = 24;
		int l = MathHelper.ceil(this.handler.getCookProgress() * 24.0F);
		context.drawGuiTexture(RenderLayer::getGuiTextured, BURN_PROGRESS_TEXTURE, 24, 16, 0, 0, i + 79, j + 34, l, 16);


		// Draw the smelt count progress bar
		int smeltCountProgress = this.handler.getSmeltCountProgress();
		int scaledWidth = smeltCountProgress * 161 / 100; // Scale bar width based on progress
		if (this.handler.getLevel() == 5) {
			scaledWidth = 100; // Full bar at level 5
		}
		if (smeltCountProgress > 0) {

			context.drawGuiTexture(RenderLayer::getGuiTextured, ULTIMATE_BAR, this.x + 7, this.y + 65, 0, 0, scaledWidth, 5, 162, 5);
		}

		// Draw the stored power bar
		int storedPower = this.handler.getStoredPower();
		int maxPower = UltimateFurnaceBlockEntity.getMaxStoredPower(this.handler.getLevel());
		int scaledCopperWidth = maxPower > 0 ? (storedPower * 18 / maxPower) : 0;
		if (scaledCopperWidth > 0) {
			context.drawGuiTexture(RenderLayer::getGuiTextured, LIT_HOT, this.x + 55, this.y + 52, 176, 31, scaledCopperWidth, 5, 256, 256);
		}
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		super.drawMouseoverTooltip(context, mouseX, mouseY);

		// Tooltip for the copper bar under the fire icon
		if (mouseX >= this.x + 55 && mouseX <= this.x + 73 && mouseY >= this.y + 52 && mouseY <= this.y + 57) {
			int storedPower = this.handler.getStoredPower();
			int maxPower = UltimateFurnaceBlockEntity.getMaxStoredPower(this.handler.getLevel());
			if (maxPower > 0) {
				int powerPercentage = Math.min(storedPower * 100 / maxPower, 100); // Cap at 100%
				String tooltipText = "Stored Power: " + powerPercentage + "%";
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

	static {
		TABS = List.of(new RecipeBookWidget.Tab(RecipeBookType.FURNACE), new RecipeBookWidget.Tab(Items.PORKCHOP, RecipeBookCategories.FURNACE_FOOD), new RecipeBookWidget.Tab(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS), new RecipeBookWidget.Tab(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC));
	}
}
