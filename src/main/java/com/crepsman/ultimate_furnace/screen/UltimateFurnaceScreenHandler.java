package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.slot.Slot;
import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;

import java.util.logging.Logger;

public class UltimateFurnaceScreenHandler extends AbstractFurnaceScreenHandler {
	private static final int ITEMS_PER_LEVEL = 3000; // Define ITEMS_PER_LEVEL
	private final PropertyDelegate customPropertyDelegate;
	private final Inventory inventory;
	private static final Logger LOGGER = Logger.getLogger(UltimateFurnaceScreenHandler.class.getName());

	public UltimateFurnaceScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(3), new CustomPropertyDelegate(6, ITEMS_PER_LEVEL));
	}

	public UltimateFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
		super(ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER, RecipeType.SMELTING, RecipeBookCategory.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
		this.customPropertyDelegate = propertyDelegate;
		this.inventory = inventory;

		this.addProperties(customPropertyDelegate); // Add custom property delegate

		this.slots.set(1, new UltimateFurnaceFuelSlot(this.inventory, 1, 56, 53));
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

	public int getStoredPower() {
		return customPropertyDelegate.get(3); // Assuming index 3 is for storedPower
	}

	@Override
	protected boolean isFuel(ItemStack itemStack) {
		// Return false as this furnace does not use fuel
		return false;
	}

	public int getSmeltCountProgress() {
		int smeltCount = customPropertyDelegate.get(0);
		int level = customPropertyDelegate.get(1);
		return smeltCount * 100 / (ITEMS_PER_LEVEL * level);
	}

	@Override
	protected boolean isSmeltable(ItemStack itemStack) {
		return this.world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(itemStack), this.world).isPresent();
	}

	public int getSmeltingProgress() {
		int progressBarWidth = 24; // Width of the progress arrow
		int cookTime = customPropertyDelegate.get(4);       // Fetch cookTime
		int cookTimeTotal = customPropertyDelegate.get(5);  // Fetch cookTimeTotal

		if (cookTimeTotal == 0) return 0; // Prevent division by zero
		return (int) ((cookTime / (float) cookTimeTotal) * progressBarWidth);
	}



	public float getFuelProgress() {
		int burnTime = customPropertyDelegate.get(2);
		int storedPower = customPropertyDelegate.get(3);
		int maxPower = UltimateFurnaceBlockEntity.BASE_MAX_STORED_POWER;
		return (burnTime > 0 ? burnTime : storedPower) * 100.0f / maxPower;
	}

	public int getItemsPerLevel() {
		return ITEMS_PER_LEVEL;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
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
