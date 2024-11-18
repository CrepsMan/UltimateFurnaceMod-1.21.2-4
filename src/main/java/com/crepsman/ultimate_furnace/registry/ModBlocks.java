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
	public static final Block ULTIMATE_FURNACE =registerBlocks("ultimate_furnace", new UltimateFurnaceBlock(Block.Settings.copy(Blocks.FURNACE)));

	public static Block registerBlocks(String name, Block block) {
		registerBlockItems(name, block);
		return Registry.register(Registries.BLOCK, Identifier.of(UltimateFurnaceMod.MOD_ID, name), block);
	}

	public static Item registerBlockItems(String name, Block block) {
		return Registry.register(Registries.ITEM, Identifier.of(UltimateFurnaceMod.MOD_ID, name),
			new BlockItem(block, new Item.Settings()));
	}

	public static void registerModBlocks() {
	}
}
