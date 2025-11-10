package com.crepsman.ultimate_furnace.util;

import com.crepsman.ultimate_furnace.registry.ModBlocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class ItemUtils {
	public static ItemStack createConfiguredFurnaceItem(int level, int smeltCount, int storedPower) {
		ItemStack stack = new ItemStack(ModBlocks.ULTIMATE_FURNACE);
		NbtCompound tag = new NbtCompound();
		tag.putInt("Level", level);
		tag.putInt("SmeltCount", smeltCount);
		tag.putInt("StoredPower", storedPower);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
		return stack;
	}
}
