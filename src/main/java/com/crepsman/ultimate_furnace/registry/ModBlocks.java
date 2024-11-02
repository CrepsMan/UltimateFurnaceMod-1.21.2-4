package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.state.property.Properties;
import com.crepsman.ultimate_furnace.UltimateFurnaceMod;

import java.util.function.ToIntFunction;

public class ModBlocks {

	public static final Block ULTIMATE_FURNACE = registerBlock(
		"ultimate_furnace",
		new UltimateFurnaceBlock(AbstractBlock.Settings.create().requiresTool().strength(3.5F).luminance(createLightLevelFromLitBlockState()))
	);

	private static ToIntFunction<BlockState> createLightLevelFromLitBlockState() {
		return (state) -> state.get(Properties.LIT) ? 13 : 0;
	}

	private static Block registerBlock(String name, Block block) {
		return Registry.register(Registries.BLOCK, UltimateFurnaceMod.id(name), block);
	}

	public static void registerModBlocks() {
		// Register all blocks here
		// Register the Ultimate Furnace block
		registerBlock("ultimate_furnace", ULTIMATE_FURNACE);
	}
}
