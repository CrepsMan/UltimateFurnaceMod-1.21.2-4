package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.recipebook.RecipeBookType;
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
		super(handler, net.minecraft.client.recipebook.RecipeBookType.FURNACE, inventory, title, TEXTURE, RECIPE_BOOK_TEXTURE, FUEL_BACKGROUND_TEXTURE, tru);
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

		// Draw the fuel progress bar based on whether the furnace is burning
		if (this.handler.getBurnTime() > 0) {
			context.drawTexture(TEXTURE, this.x + 56, this.y + 36, 176, 0, 14, 13);
		}

		// Draw the smelt count progress bar based on getSmeltCountProgress()
		int smeltCountProgress = this.handler.getSmeltCountProgress();
		if (this.handler.getLevel() == 5) {
			smeltCountProgress = 100; // Keep the bar full at level 5
		}
		if (smeltCountProgress > 0) {
			int scaledSmeltCountWidth = (int) (smeltCountProgress * 161 / 100);
			context.drawTexture(TEXTURE, this.x + 7, this.y + 65, 0, 166, scaledSmeltCountWidth, 5);
		}

		// Draw the copper bar under the fire icon based on storedPower
		int storedPower = this.handler.getStoredPower();
		int maxPower = UltimateFurnaceBlockEntity.getMaxStoredPower(this.handler.getLevel());
		int scaledCopperWidth = maxPower > 0 ? (int) (storedPower * 18 / maxPower) : 0;
		if (scaledCopperWidth > 0) {
			context.drawTexture(TEXTURE, this.x + 55, this.y + 52, 176, 31, scaledCopperWidth, 5);
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
}
