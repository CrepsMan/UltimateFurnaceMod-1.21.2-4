package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.HotPlateBlock;
import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.function.Function;

public class ModBlocks {
	public static Block ULTIMATE_FURNACE;
	public static Block HOT_PLATE;

	public static void registerModBlocks() {
		ULTIMATE_FURNACE = registerBlockWithItem("ultimate_furnace", UltimateFurnaceBlock::new, AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)).getLeft();
		HOT_PLATE = registerBlockWithItem("hot_plate", HotPlateBlock::new, AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)).getLeft();

		System.out.println("ULTIMATE_FURNACE: " + ULTIMATE_FURNACE);
		System.out.println("HOT_PLATE: " + HOT_PLATE);
	}

	public static <T extends Item> T registerItem(String name, Function<Item.Settings, T> factory) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(UltimateFurnaceMod.MOD_ID, name));
		return Registry.register(
			Registries.ITEM,
			key,
			factory.apply(new Item.Settings().registryKey(key))
		);
	}

	public static <T extends Block> T registerBlock(String name, Function<AbstractBlock.Settings, T> factory) {
		return registerBlock(name, factory, AbstractBlock.Settings.create());
	}

	public static <T extends Block> T registerBlock(String name, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings base) {
		RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(UltimateFurnaceMod.MOD_ID, name));
		return Registry.register(
			Registries.BLOCK,
			key,
			factory.apply(base.registryKey(key))
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
