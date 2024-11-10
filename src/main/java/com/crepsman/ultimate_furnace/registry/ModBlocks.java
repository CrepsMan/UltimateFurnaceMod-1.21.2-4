package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.Blocks;

public class ModBlocks {
	public static final Block ULTIMATE_FURNACE = new UltimateFurnaceBlock(Block.Settings.copy(Blocks.FURNACE));

	public static void registerModBlocks() {
		Registry.register(Registries.BLOCK, Identifier.of(UltimateFurnaceMod.MOD_ID, "ultimate_furnace"), ULTIMATE_FURNACE);
	}

	public static void registerModBlockItems() {
		Registry.register(Registries.ITEM, Identifier.of(UltimateFurnaceMod.MOD_ID, "ultimate_furnace"),
			new BlockItem(ULTIMATE_FURNACE, new Item.Settings()));
	}
}
