package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

	public static BlockEntityType<UltimateFurnaceBlockEntity> ULTIMATE_FURNACE_BLOCK_ENTITY;

	public static void registerModBlockEntities() {
		ULTIMATE_FURNACE_BLOCK_ENTITY = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(UltimateFurnaceMod.MOD_ID, "ultimate_furnace"),
			BlockEntityType.Builder.create(UltimateFurnaceBlockEntity::new, ModBlocks.ULTIMATE_FURNACE).build(null)
		);
	}
}
