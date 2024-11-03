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
		this(syncId, playerInventory, new SimpleInventory(3), new PropertyDelegate() {
			private final int[] data = new int[5];

			@Override
			public int get(int fromIndex) {
				return data[fromIndex];
			}

			@Override
			public void set(int fromIndex, int value) {
				data[fromIndex] = value;
			}

			@Override
			public int size() {
				return data.length;
			}
		});
	}

	// Override transferSlot to use custom logic
	public ItemStack quickMove(PlayerEntity player, int fromIndex) {
		// Call custom transfer logic here
		return customTransferStack(player, fromIndex);
	}

	public UltimateFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
		super(ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER, RecipeType.SMELTING, RecipeBookCategory.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
		this.customPropertyDelegate = propertyDelegate;
		this.addProperties(this.customPropertyDelegate);
		this.slots.clear();
		this.inventory = inventory;

		// Add the input slot
		this.addSlot(new Slot(inventory, 0, 56, 17)); // Input slot

		// Add the output slot
		this.addSlot(new OutputSlot(inventory, 2, 116, 35)); // Output slot

		int playerInventoryStartX = 8;
		int playerInventoryStartY = 84;
		int hotbarStartY = 142;

		// Add player inventory slots
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, playerInventoryStartX + col * 18, playerInventoryStartY + row * 18));
			}
		}

		// Add player hotbar slots
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, playerInventoryStartX + col * 18, hotbarStartY));
		}
	}

	public ItemStack customTransferStack(PlayerEntity player, int fromIndex) {
		ItemStack newStack = ItemStack.EMPTY;

		// Prevent out-of-bounds access
		if (fromIndex < 0 || fromIndex >= this.slots.size()) {
			return ItemStack.EMPTY;
		}

		Slot slot = this.slots.get(fromIndex);

		if (slot != null && slot.hasStack()) {
			ItemStack originalStack = slot.getStack();
			newStack = originalStack.copy();

			// Output slot (fromIndex 1)
			if (fromIndex == 1) { // Output slot
				if (!this.insertItem(originalStack, 2, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
				slot.onTakeItem(player, originalStack); // Notify the slot that the item was taken
			}

			// Input slot (fromIndex 0)
			else if (fromIndex == 0) { // Input slot
				if (!this.insertItem(originalStack, 2, this.slots.size(), true)) { // Move to player inventory/hotbar
					return ItemStack.EMPTY;
				}
				slot.onTakeItem(player, originalStack); // Notify the slot that the item was taken
			}
			// Player inventory/hotbar handling
			else if (fromIndex >= 2 && fromIndex < this.slots.size()) {
				// If it's smeltable, we attempt to move it to the input slot
				if (this.isSmeltable(originalStack)) {
					if (!this.insertItem(originalStack, 0, 1, false)) { // Input slot
						return ItemStack.EMPTY;
					}
				} else if (fromIndex >= 2 && fromIndex < 30) {
					// Move items from the player's inventory to the hotbar
					if (!this.insertItem(originalStack, 30, this.slots.size(), false)) {
						return ItemStack.EMPTY;
					}
				} else if (fromIndex >= 30 && fromIndex < this.slots.size()) {
					// Move items from the hotbar to the inventory
					if (!this.insertItem(originalStack, 2, 30, false)) {
						return ItemStack.EMPTY;
					}
				}
			}

			// Remove the item from the slot if it's empty
			if (originalStack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}

			// If the item count hasn't changed, return empty
			if (originalStack.getCount() == newStack.getCount()) {
				return ItemStack.EMPTY;
			}

			// Notify the slot that the item was taken
			if (fromIndex == 1) {
				slot.onTakeItem(player, originalStack);
			}
		}

		return newStack;
	}


	@Override
	public float getCookProgress() {
		int cookTime = this.customPropertyDelegate.get(2); // This could cause the issue if fromIndex exceeds bounds
		int cookTimeTotal = this.customPropertyDelegate.get(3);

		if (cookTimeTotal == 0) {
			cookTimeTotal = 1; // Avoid division by zero
		}

		return (int) ((double) cookTime / cookTimeTotal * 24); // 24 is the width of the arrow progress indicator
	}

	public float getFuelProgress() {
		int burnTime = this.customPropertyDelegate.get(0);
		int currentBurnTime = this.customPropertyDelegate.get(1);
		return currentBurnTime != 0 && burnTime != 0 ? burnTime * 13 / currentBurnTime : 0;
	}

	public boolean isBurning() {
		return this.customPropertyDelegate.get(0) > 0;
	}

	public int getSmeltCount() {
		return this.customPropertyDelegate.get(4);
	}

	public static class OutputSlot extends Slot {
		public OutputSlot(Inventory inventory, int fromIndex, int x, int y) {
			super(inventory, fromIndex, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false; // Prevent players from placing any items into the output slot
		}
	}

	@Override
	public boolean isFuel(ItemStack stack) {
		// Return false for all items, making nothing act as fuel
		return false;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.inventory.canPlayerUse(player);
	}
}
