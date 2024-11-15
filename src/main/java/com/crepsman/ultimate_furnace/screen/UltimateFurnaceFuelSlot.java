package com.crepsman.ultimate_furnace.screen;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class UltimateFurnaceFuelSlot extends Slot {

	public UltimateFurnaceFuelSlot(Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		// Prevent any item from being inserted as fuel
		return false;
	}

	@Override
	public int getMaxItemCount(ItemStack stack) {
		return 0;  // No fuel, so max count is zero
	}

	public boolean canBeHighlighted() {
		// Override this to prevent the highlight from showing on this slot
		return false;
	}
}
