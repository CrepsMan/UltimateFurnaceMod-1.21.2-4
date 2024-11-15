package com.crepsman.ultimate_furnace.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.recipebook.FurnaceRecipeBookScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public class UltimateFurnaceScreen extends AbstractFurnaceScreen<UltimateFurnaceScreenHandler> {
	private static final Identifier TEXTURE = Identifier.of("ultimate_furnace", "textures/gui/container/ultimate_furnace.png");
	private static final Identifier RECIPE_BOOK_TEXTURE = Identifier.of("minecraft", "textures/gui/recipe_book.png");
	private static final Identifier FUEL_BACKGROUND_TEXTURE = Identifier.of("minecraft", "textures/gui/fuel_background.png");

	private final int itemsPerLevel;

	public UltimateFurnaceScreen(UltimateFurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, new FurnaceRecipeBookScreen(), inventory, title, TEXTURE, RECIPE_BOOK_TEXTURE, FUEL_BACKGROUND_TEXTURE);
		this.itemsPerLevel = handler.getItemsPerLevel();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

		// Draw the smelting progress bar based on getSmeltingProgress()
		int smeltingProgress = this.handler.getSmeltingProgress();
		if (smeltingProgress > 0) {
			int scaledProgressWidth = (smeltingProgress * 24) / 100;
			context.drawTexture(TEXTURE, this.x + 79, this.y + 34, 176, 14, scaledProgressWidth + 1, 16);
		}

		int fuelProgress = (int) this.handler.getFuelProgress();
		if (fuelProgress > 0) {
			int scaledFuelHeight = (fuelProgress * 13) / 100;
			context.drawTexture(TEXTURE, this.x + 56, this.y + 36 + 12 - scaledFuelHeight, 176, 12 - scaledFuelHeight, 14, scaledFuelHeight + 1);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		super.drawMouseoverTooltip(context, mouseX, mouseY);

		// Check if the mouse is over the smelt count text area
		if (mouseX >= this.x + 8 && mouseX <= this.x + 8 + this.textRenderer.getWidth("Smelted:") &&
			mouseY >= this.y + this.backgroundHeight - 104 && mouseY <= this.y + this.backgroundHeight - 104 + this.textRenderer.fontHeight) {
			int smeltCount = this.handler.getSmeltCount();
			int currentLevel = this.handler.getLevel();
			String tooltipText = smeltCount + " / " + itemsPerLevel * currentLevel;
			context.drawOrderedTooltip(this.textRenderer, Arrays.asList(Text.literal(tooltipText).asOrderedText()), mouseX, mouseY);
		}
	}
}
