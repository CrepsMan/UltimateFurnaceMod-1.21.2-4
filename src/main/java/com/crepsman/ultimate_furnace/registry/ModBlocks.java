package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.CopperPlateBlock;
import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.function.Function;

import static net.minecraft.block.Blocks.createLightLevelFromLitBlockState;

public class ModBlocks {
	public static Block ULTIMATE_FURNACE;
	public static Block COPPER_PLATE;

	public static void registerModBlocks() {
		ULTIMATE_FURNACE = registerBlockWithItem("ultimate_furnace", UltimateFurnaceBlock::new, AbstractBlock.Settings.create().requiresTool().strength(3.5F).luminance(createLightLevelFromLitBlockState(13))).getLeft();
		COPPER_PLATE = registerBlockWithItem("copper_plate", CopperPlateBlock::new, AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)).getLeft();
	}

	public static <T extends Item> T registerItem(String name, Function<Item.Settings, T> factory) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(UltimateFurnaceMod.MOD_ID, name));
		return Registry.register(
			Registries.ITEM,
			key.getValue(), // Use the Identifier directly instead of registryKey()
			factory.apply(new Item.Settings())
		);
	}

	public static <T extends Block> T registerBlock(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings base) {
		RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(UltimateFurnaceMod.MOD_ID, name));
		return Registry.register(
			Registries.BLOCK,
			key.getValue(), // Use the Identifier directly instead of registryKey()
			factory.apply(base)
		);
	}

	public static <T extends Block> Pair<T, BlockItem> registerBlockWithItem(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings base) {
		T block = registerBlock(name, factory, base);
		return new Pair<>(
			block,
			registerItem(name, settings -> new BlockItem(block, settings))
		);
	}
}
