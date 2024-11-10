package com.crepsman.ultimate_furnace.screen;

import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.slot.Slot;

public class UltimateFurnaceScreenHandler extends AbstractFurnaceScreenHandler {
	private final PropertyDelegate customPropertyDelegate;
	private final Inventory inventory;

	public UltimateFurnaceScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(3), new CustomPropertyDelegate(5));
	}

	public UltimateFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
		super(ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER, RecipeType.SMELTING, RecipeBookCategory.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
		this.customPropertyDelegate = propertyDelegate;
		this.inventory = inventory;

		this.addProperties(customPropertyDelegate); // Add custom property delegate
		this.slots.clear(); // Clear previous slots

		// Add custom slots
		this.addSlot(new Slot(inventory, 0, 56, 17)); // Input slot
		this.addSlot(new Slot(inventory, 1, 56, 53)); // Fuel slot
		this.addSlot(new OutputSlot(inventory, 2, 116, 35)); // Output slot

		// Player inventory (rows 1-3)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}

		// Player hotbar (row 0)
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	// Custom transfer stack logic
	public ItemStack customTransferStack(PlayerEntity player, int fromIndex) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(fromIndex);

		if (slot != null && slot.hasStack()) {
			ItemStack originalStack = slot.getStack();
			newStack = originalStack.copy();

			// Custom logic for transferring items
			if (fromIndex == 2) { // Output slot
				if (!this.insertItem(originalStack, 3, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
				slot.onTakeItem(player, originalStack);
			} else if (fromIndex != 1 && fromIndex != 0) { // Not fuel or input
				if (this.isSmeltable(originalStack)) {
					if (!this.insertItem(originalStack, 0, 1, false)) {
						return ItemStack.EMPTY;
					}
				} else if (this.isFuel(originalStack)) {
					if (!this.insertItem(originalStack, 1, 2, false)) {
						return ItemStack.EMPTY;
					}
				} else if (fromIndex >= 3 && fromIndex < 30) {
					if (!this.insertItem(originalStack, 30, this.slots.size(), false)) {
						return ItemStack.EMPTY;
					}
				} else if (fromIndex >= 30 && fromIndex < this.slots.size()) {
					if (!this.insertItem(originalStack, 3, 30, false)) {
						return ItemStack.EMPTY;
					}
				}
			} else if (!this.insertItem(originalStack, 3, this.slots.size(), false)) {
				return ItemStack.EMPTY;
			}

			if (originalStack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}

			if (originalStack.getCount() == newStack.getCount()) {
				return ItemStack.EMPTY;
			}
		}
		return newStack;
	}

	@Override
	public float getCookProgress() {
		int cookTime = customPropertyDelegate.get(2);
		int cookTimeTotal = customPropertyDelegate.get(3);
		return cookTimeTotal == 0 ? 0 : (float) cookTime / cookTimeTotal * 24;
	}

	public float getFuelProgress() {
		int burnTime = customPropertyDelegate.get(0);
		int currentBurnTime = customPropertyDelegate.get(1);
		return currentBurnTime != 0 && burnTime != 0 ? (float) burnTime / currentBurnTime * 13 : 0;
	}

	public boolean isBurning() {
		return customPropertyDelegate.get(0) > 0;
	}

	public int getSmeltCount() {
		return customPropertyDelegate.get(4);
	}

	// Output slot to prevent item insertion
	public static class OutputSlot extends Slot {
		public OutputSlot(Inventory inventory, int fromIndex, int x, int y) {
			super(inventory, fromIndex, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false;
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
	}

	// Helper custom delegate to simplify data storage
	private static class CustomPropertyDelegate implements PropertyDelegate {
		private final int[] data;

		CustomPropertyDelegate(int size) {
			this.data = new int[size];
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
