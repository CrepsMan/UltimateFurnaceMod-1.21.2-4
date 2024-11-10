package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {

	// Declare the ScreenHandlerType for the Ultimate Furnace
	public static final ScreenHandlerType<UltimateFurnaceScreenHandler> ULTIMATE_FURNACE_SCREEN_HANDLER;

	static {
		// Register the ScreenHandlerType using Registry and the correct identifier
		ULTIMATE_FURNACE_SCREEN_HANDLER = Registry.register(
			Registries.SCREEN_HANDLER,
			Identifier.of("ultimate_furnace", "ultimate_furnace"),
			new ScreenHandlerType<>(UltimateFurnaceScreenHandler::new,null)
		);
	}
}
