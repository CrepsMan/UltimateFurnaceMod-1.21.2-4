package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ArrayPropertyDelegate;


public class ModScreenHandlers {

	public static final ScreenHandlerType<UltimateFurnaceScreenHandler> ULTIMATE_FURNACE_SCREEN_HANDLER;

	static {
		ULTIMATE_FURNACE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.tryParse("ultimate_furnace:ultimate_furnace"),
			new ScreenHandlerType<>(
				(syncId, playerInventory) -> new UltimateFurnaceScreenHandler(
					ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER,
					RecipeType.SMELTING,
					syncId,
					playerInventory,
					new SimpleInventory(3),
					new ArrayPropertyDelegate(6)
				),
				FeatureSet.empty()
			)
		);
	}
}
