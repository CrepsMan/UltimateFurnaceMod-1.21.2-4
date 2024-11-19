package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
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
			context.drawTexture(TEXTURE, this.x + 79, this.y + 34, 176, 14, smeltingProgress + 1, 16);
		}

		// Draw the fuel progress bar based on getFuelProgress()
		float fuelProgress = this.handler.getFuelProgress();
		if (fuelProgress > 0) {
			int scaledFuelHeight = (int) (fuelProgress * 13 / 100);
			context.drawTexture(TEXTURE, this.x + 56, this.y + 36 + 12 - scaledFuelHeight, 176, 12 - scaledFuelHeight, 14, scaledFuelHeight + 1);
		}

		// Draw the smelt count progress bar based on getSmeltCountProgress()
		int smeltCountProgress = this.handler.getSmeltCountProgress();
		if (smeltCountProgress > 0) {
			int scaledSmeltCountWidth = (int) (smeltCountProgress * 161 / 100);
			context.drawTexture(TEXTURE, this.x + 7, this.y + 65, 0, 166, scaledSmeltCountWidth, 5);
		}

		// Draw the copper block overlay when burning
		if (this.handler.isBurning()) {
//			context.drawTexture(TEXTURE, this.x + 56, this.y + 36, 176, 0, 14, 14);
			context.drawTexture(TEXTURE, this.x + 55, this.y + 52, 176, 31, 18, 5);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		super.drawForeground(context, mouseX, mouseY);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		super.drawMouseoverTooltip(context, mouseX, mouseY);

		// Tooltip for the fire icon
		if (mouseX >= this.x + 56 && mouseX <= this.x + 70 && mouseY >= this.y + 36 && mouseY <= this.y + 49) {
			int storedPower = this.handler.getStoredPower();
			int maxPower = switch (this.handler.getLevel()) {
				case 1 -> 0;
				case 2 -> 6000;
				case 3 -> 8000;
				case 4 -> 12000;
				case 5 -> 18000;
				default -> 0;
			};
			String tooltipText = "Stored Power: " + (storedPower * 100 / maxPower) + "%";
			context.drawOrderedTooltip(this.textRenderer, Arrays.asList(Text.literal(tooltipText).asOrderedText()), mouseX, mouseY);
		}

		// Tooltip for the smelt count progress bar
		if (mouseX >= this.x + 7 && mouseX <= this.x + 7 + 161 && mouseY >= this.y + 65 && mouseY <= this.y + 70) {
			int smeltCount = this.handler.getSmeltCount();
			int currentLevel = this.handler.getLevel();
			int maxSmeltCount = this.handler.getMaxSmeltCountForLevel();
			String tooltipText;

			if (currentLevel < 5) {
				tooltipText = smeltCount + " / " + maxSmeltCount + " to level " + (currentLevel + 1);
			} else {
				tooltipText = smeltCount + " / " + maxSmeltCount + " (Max level reached)";
			}

			context.drawOrderedTooltip(this.textRenderer, Arrays.asList(Text.literal(tooltipText).asOrderedText()), mouseX, mouseY);
		}
	}
}
