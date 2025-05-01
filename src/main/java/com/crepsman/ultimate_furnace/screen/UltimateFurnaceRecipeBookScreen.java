package com.crepsman.ultimate_furnace.screen;

import net.minecraft.client.gui.screen.recipebook.AbstractFurnaceRecipeBookScreen;
import net.minecraft.item.Item;
import net.minecraft.recipe.book.RecipeBookCategory;

import java.util.Collections;
import java.util.Set;

public class UltimateFurnaceRecipeBookScreen extends AbstractFurnaceRecipeBookScreen {
	public RecipeBookCategory getCategory() {
		return RecipeBookCategory.FURNACE;
	}

	@Override
	protected Set<Item> getAllowedFuels() {
		// Return empty set as this furnace doesn't use conventional fuel
		return Collections.emptySet();
	}
}
