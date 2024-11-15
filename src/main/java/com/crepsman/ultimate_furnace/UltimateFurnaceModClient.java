package com.crepsman.ultimate_furnace;

import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;



public class UltimateFurnaceModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER, UltimateFurnaceScreen::new);

	}

}

