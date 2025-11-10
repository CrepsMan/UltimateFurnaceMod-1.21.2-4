package com.crepsman.ultimate_furnace.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerType;
import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;

import java.util.logging.Logger;

public class UltimateFurnaceScreenHandler extends AbstractFurnaceScreenHandler {
	private final PropertyDelegate customPropertyDelegate;
	private final Inventory inventory;
	private static final Logger LOGGER = Logger.getLogger(UltimateFurnaceScreenHandler.class.getName());

	// Primary constructor using RecipeBookCategory (no RecipePropertySet dependency)
	public UltimateFurnaceScreenHandler(ScreenHandlerType<?> type,
									  RecipeType<? extends AbstractCookingRecipe> recipeType,
									  RecipeBookCategory category,
									  int syncId,
									  PlayerInventory playerInventory,
									  Inventory inventory,
									  PropertyDelegate propertyDelegate) {
		super(type, recipeType, category, syncId, playerInventory, inventory, propertyDelegate);
		this.inventory = inventory;
		this.customPropertyDelegate = propertyDelegate;
		this.addProperties(customPropertyDelegate);
		this.slots.set(1, new UltimateFurnaceFuelSlot(this.inventory, 1, 56, 53));
	}

	// Convenience constructor used by registry lambda
	public UltimateFurnaceScreenHandler(ScreenHandlerType<?> type,
									  RecipeType<? extends AbstractCookingRecipe> recipeType,
									  int syncId,
									  PlayerInventory playerInventory,
									  Inventory inventory,
									  PropertyDelegate propertyDelegate) {
		this(type, recipeType, RecipeBookCategory.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
	}

	public int getMaxSmeltCountForLevel() {
		int level = getLevel();
		int itemsPerLevel = FurnaceConfig.getItemsPerLevel();
		int maxLevel = FurnaceConfig.getMaxLevel();
		return level < maxLevel ? itemsPerLevel * level : itemsPerLevel * maxLevel;
	}

	public int getLevel() { return customPropertyDelegate.get(1); }
	public int getSmeltCount() { return customPropertyDelegate.get(0); }
	public int getBurnTime() { return customPropertyDelegate.get(2); }

	@Override
	protected boolean isFuel(ItemStack itemStack) { return false; }

	public int getSmeltCountProgress() {
		int smeltCount = customPropertyDelegate.get(0);
		int level = customPropertyDelegate.get(1);
		int itemsPerLevel = FurnaceConfig.getItemsPerLevel();
		int maxSmeltCount = itemsPerLevel * level;
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

	protected boolean isSmeltable(ItemStack itemStack) { return true; }

	@Override
	public boolean canUse(PlayerEntity player) { return this.inventory.canPlayerUse(player); }
}
