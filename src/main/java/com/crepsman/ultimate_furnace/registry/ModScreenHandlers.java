package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
	public static final ScreenHandlerType<UltimateFurnaceScreenHandler> ULTIMATE_FURNACE_SCREEN_HANDLER;

	static {
		ULTIMATE_FURNACE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.tryParse("ultimate_furnace", "ultimate_furnace"),
			new ScreenHandlerType<>(UltimateFurnaceScreenHandler::new, FeatureSet.empty())
		);
	}

	public static void registerScreenHandlers() {
		// Registration is handled in static block
	}
}
