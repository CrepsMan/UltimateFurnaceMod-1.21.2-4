package com.crepsman.ultimate_furnace.registry;

import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
	public static final ScreenHandlerType<UltimateFurnaceScreenHandler> ULTIMATE_FURNACE_SCREEN_HANDLER;

	static {
		ULTIMATE_FURNACE_SCREEN_HANDLER = Registry.register(
			RegistryKeys.SCREEN_HANDLER,
			Identifier.tryParse("ultimate_furnace", "ultimate_furnace"),
			new ScreenHandlerType<>(UltimateFurnaceScreenHandler::new)
		);
	}

	// Method to register screen handlers (can be extended in the future)
	public static void registerScreenHandlers() {
		// Registration is handled in static block
	}
}
