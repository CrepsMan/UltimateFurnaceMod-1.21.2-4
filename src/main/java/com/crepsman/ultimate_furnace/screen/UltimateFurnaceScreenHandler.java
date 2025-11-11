package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.recipe.RecipePropertySet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerType;

import java.util.logging.Logger;

public class UltimateFurnaceScreenHandler extends AbstractFurnaceScreenHandler {
	private final PropertyDelegate customPropertyDelegate;
	private final Inventory inventory;
	private static final Logger LOGGER = Logger.getLogger(UltimateFurnaceScreenHandler.class.getName());

	public UltimateFurnaceScreenHandler(ScreenHandlerType<?> type, RecipeType<? extends AbstractCookingRecipe> recipeType, RegistryKey<RecipePropertySet> recipePropertySetKey, RecipeBookType category, int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
		super(type, recipeType, recipePropertySetKey, category, syncId, playerInventory, inventory, propertyDelegate);
		this.inventory = inventory;
		this.customPropertyDelegate = propertyDelegate;

		// do not addProperties again; super already added the provided delegate
		this.slots.set(1, new UltimateFurnaceFuelSlot(this.inventory, 1, 56, 53));
	}

	private int itemsPerLevel() {
		// Use TXT config directly on client; in dev, client/server share the file
		return FurnaceConfig.getItemsPerLevel();
	}

	public int getMaxSmeltCountForLevel() {
		int level = getLevel();
		int per = itemsPerLevel();
		int cap = FurnaceConfig.getMaxLevel();
		return per * Math.min(level, cap);
	}

	public int getLevel() {
		return customPropertyDelegate.get(1);
	}

	public int getSmeltCount() {
		return customPropertyDelegate.get(0);
	}

	public int getBurnTime() {
		return customPropertyDelegate.get(2);
	}

	@Override
	protected boolean isFuel(ItemStack itemStack) {
		return false;
	}

	public int getSmeltCountProgress() {
		int smeltCount = customPropertyDelegate.get(0);
		int level = customPropertyDelegate.get(1);
		int maxSmeltCount = itemsPerLevel() * Math.min(level, FurnaceConfig.getMaxLevel());
		return maxSmeltCount > 0 ? smeltCount * 100 / maxSmeltCount : 0;
	}

	@Override
	public boolean isBurning() {
		int burnTime = customPropertyDelegate.get(2);
		int cookTime = customPropertyDelegate.get(4);
		int storedPower = customPropertyDelegate.get(3);
		boolean hasInput = !inventory.getStack(0).isEmpty();
		if (burnTime > 0) return true;
		if (cookTime > 0) return true;
		return hasInput && storedPower > 0;
	}
	public int getStoredPower() {
		int storedPower = customPropertyDelegate.get(3);
		int maxPower = UltimateFurnaceBlockEntity.getMaxStoredPower(getLevel());
		return maxPower > 0 ? (int) (storedPower * 100.0f / maxPower) : 0;
	}

	public int getCookingProgress() {
		int cookTime = customPropertyDelegate.get(4);
		int cookTimeTotal = customPropertyDelegate.get(5);
		return cookTimeTotal != 0 ? (cookTime * 100) / cookTimeTotal : 0;
	}

	protected boolean isSmeltable(ItemStack itemStack) {
		return true;
	}

	public int getItemsPerLevel() {
		return itemsPerLevel();
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.inventory.canPlayerUse(player);
	}

	private static class CustomPropertyDelegate implements PropertyDelegate {
		private final int[] data;
		private final int itemsPerLevel;

		CustomPropertyDelegate(int size, int itemsPerLevel) {
			this.data = new int[size];
			this.itemsPerLevel = itemsPerLevel;
		}

		@Override
		public int get(int index) {
			return data[index];
		}

		@Override
		public void set(int index, int value) {
			data[index] = value;
		}

		@Override
		public int size() {
			return data.length;
		}
	}
}
