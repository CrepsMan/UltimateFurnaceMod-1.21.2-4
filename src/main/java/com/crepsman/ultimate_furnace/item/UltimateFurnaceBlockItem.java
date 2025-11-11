package com.crepsman.ultimate_furnace.item;

import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.text.Style;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class UltimateFurnaceBlockItem extends BlockItem {
	public UltimateFurnaceBlockItem(net.minecraft.block.Block block, Settings settings) {
		super(block, settings);
	}

	// Removed deprecated appendTooltip override. Dynamic details are now injected into LoreComponent server-side when item is dropped.

	public static void applyDynamicLore(ItemStack stack) {
		NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) return;
		var tag = data.getNbt();
		if (!(tag instanceof net.minecraft.nbt.NbtCompound)) return;
		net.minecraft.nbt.NbtCompound ctag = (net.minecraft.nbt.NbtCompound) tag;
		boolean hasPreservedKeys = ctag.contains("Level") || ctag.contains("SmeltCount") || ctag.contains("StoredPower");
		int level = ctag.getInt("Level", 1);
		int smelt = ctag.getInt("SmeltCount", 0);
		int power = ctag.getInt("StoredPower", 0);
		boolean meaningful = level > 1 || smelt > 0 || power > 0;
		if (!hasPreservedKeys || !meaningful) return;

		NumberFormat nf = NumberFormat.getIntegerInstance();
		int itemsPerLevel = FurnaceConfig.getItemsPerLevel();
		NbtElement itemsEl = ctag.get("ItemsPerLevel");
		if (itemsEl instanceof NbtInt ni) itemsPerLevel = ni.intValue();
		int required = itemsPerLevel * Math.min(level, FurnaceConfig.getMaxLevel());
		int maxPower = FurnaceConfig.getMaxStoredPowerForLevel(level);
		NbtElement maxEl = ctag.get("MaxStoredPower");
		if (maxEl instanceof NbtInt mi) maxPower = mi.intValue();

		List<Text> lines = new ArrayList<>();
		lines.add(Text.translatable("tooltip.ultimate_furnace.level", level).fillStyle(Style.EMPTY.withColor(0xFFFFFF)));
		lines.add(Text.translatable("tooltip.ultimate_furnace.progress").fillStyle(Style.EMPTY.withColor(0xAAAAAA)));
		lines.add(barLine(smelt, required, 20, 0x55FF55, 0x2E2E2E));
		lines.add(Text.translatable("tooltip.ultimate_furnace.count_pair", nf.format(smelt), nf.format(required)).fillStyle(Style.EMPTY.withColor(0x888888)));
		lines.add(Text.translatable("tooltip.ultimate_furnace.power_label").fillStyle(Style.EMPTY.withColor(0xAAAAAA)));
		lines.add(barLine(power, maxPower, 20, 0x55FFFF, 0x2E2E2E));
		lines.add(Text.translatable("tooltip.ultimate_furnace.count_pair", nf.format(power), nf.format(maxPower)).fillStyle(Style.EMPTY.withColor(0x888888)));

		stack.set(DataComponentTypes.LORE, new LoreComponent(lines));
	}

	private static MutableText barLine(int value, int max, int width, int colorFull, int colorEmpty) {
		if (max <= 0) max = 1;
		int filled = Math.min(width, (int) Math.round((value / (double) max) * width));
		String full = "■".repeat(filled);
		String empty = "■".repeat(width - filled);
		MutableText text = Text.empty();
		if (!full.isEmpty()) text = text.append(Text.literal(full).fillStyle(Style.EMPTY.withColor(colorFull)));
		if (!empty.isEmpty()) text = text.append(Text.literal(empty).fillStyle(Style.EMPTY.withColor(colorEmpty)));
		return text;
	}
}
