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

		// Add custom slots
		this.addSlot(new Slot(inventory, 0, 56, 17)); // Input slot
		this.addSlot(new OutputSlot(inventory, 2, 116, 35)); // Output slot
		this.slots.set(1, new UltimateFurnaceFuelSlot(this.inventory, 1, 56, 53));
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int fromIndex) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(fromIndex);

		if (slot != null && slot.hasStack()) {
			ItemStack originalStack = slot.getStack();
			newStack = originalStack.copy();

			// Ensure that fromIndex is within the valid range of slots
			if (fromIndex < 0 || fromIndex >= this.slots.size()) {
				return ItemStack.EMPTY;
			}

			// Custom logic for transferring items
			if (fromIndex == 2) { // Output slot
				if (!this.insertItem(originalStack, 3, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
				slot.onTakeItem(player, originalStack);
				LOGGER.info("Item smelted and moved to output slot: " + originalStack.getItem().getName().getString());
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

	public int getLevel() {
		return customPropertyDelegate.get(1); // Assuming index 1 is for level
	}

	public int getSmeltCount() {
		return customPropertyDelegate.get(0); // Assuming index 0 is for smelt count
	}

	@Override
	protected boolean isFuel(ItemStack itemStack) {
		// Return false as this furnace does not use fuel
		return false;
	}

	public int getSmeltingProgress() {
		int divisor = ITEMS_PER_LEVEL * this.customPropertyDelegate.get(1);
		if (divisor == 0) {
			return 0;
		}
		return this.customPropertyDelegate.get(0) * 100 / divisor;
	}

	public float getFuelProgress() {
		return this.customPropertyDelegate.get(3) == 1 ? 100.0f : 0.0f;
	}

	public int getItemsPerLevel() {
		return ITEMS_PER_LEVEL;
	}

	public static class OutputSlot extends Slot {
		public OutputSlot(Inventory inventory, int fromIndex, int x, int y) {
			super(inventory, fromIndex, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return false; // Prevent insertion into output slot
		}

		@Override
		public boolean canTakeItems(PlayerEntity playerEntity) {
			return true; // Allow taking items from output slot
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
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

		public int getItemsPerLevel() {
			return itemsPerLevel;
		}
	}
}
