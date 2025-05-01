package com.crepsman.ultimate_furnace.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerType;
import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;

import java.util.logging.Logger;

public class UltimateFurnaceScreenHandler extends AbstractFurnaceScreenHandler {
	private static final int ITEMS_PER_LEVEL = 3000; // Define ITEMS_PER_LEVEL
	private final PropertyDelegate customPropertyDelegate;
	private final Inventory inventory;
	private static final Logger LOGGER = Logger.getLogger(UltimateFurnaceScreenHandler.class.getName());

	public UltimateFurnaceScreenHandler(ScreenHandlerType<?> type, RecipeType<? extends AbstractCookingRecipe> recipeType, int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
		// Add RecipeBookCategory parameter for 1.21
		super(type, recipeType, RecipeBookCategory.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
		this.inventory = inventory;
		this.customPropertyDelegate = propertyDelegate;

		this.addProperties(customPropertyDelegate);
		this.slots.set(1, new UltimateFurnaceFuelSlot(this.inventory, 1, 56, 53));
	}


	public int getMaxSmeltCountForLevel() {
		int level = getLevel();
		return level < 5 ? ITEMS_PER_LEVEL * level : ITEMS_PER_LEVEL * 5;
	}

	public int getLevel() {
		return customPropertyDelegate.get(1); // Assuming index 1 is for level
	}

	public int getSmeltCount() {
		return customPropertyDelegate.get(0); // Assuming index 0 is for smelt count
	}

	public int getBurnTime() {
		return customPropertyDelegate.get(2); // Assuming index 2 is for burnTime
	}

	@Override
	protected boolean isFuel(ItemStack itemStack) {
		// Return false as this furnace does not use fuel
		return false;
	}

	public int getSmeltCountProgress() {
		int smeltCount = customPropertyDelegate.get(0);
		int level = customPropertyDelegate.get(1);
		int maxSmeltCount = ITEMS_PER_LEVEL * level;
		return maxSmeltCount > 0 ? smeltCount * 100 / maxSmeltCount : 0;
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
		return ITEMS_PER_LEVEL;
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
