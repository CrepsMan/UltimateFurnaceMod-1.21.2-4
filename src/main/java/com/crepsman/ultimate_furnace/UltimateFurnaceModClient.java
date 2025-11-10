package com.crepsman.ultimate_furnace;

import com.crepsman.ultimate_furnace.registry.ModBlocks;
import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreen;
import com.crepsman.ultimate_furnace.util.ClientConfig;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;



public class UltimateFurnaceModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER, UltimateFurnaceScreen::new);
		ClientConfig.load();

		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (stack.getItem() == ModBlocks.ULTIMATE_FURNACE.asItem()) {
				NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
				if (data != null) {
					NbtCompound tag = data.getNbt();
					int level = tag.getInt("Level");
					int smelts = tag.getInt("SmeltCount");
					int stored = tag.getInt("StoredPower");
					int maxStored = com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity.getMaxStoredPower(level);
					lines.add(Text.translatable("tooltip.ultimate_furnace.level", level).formatted(Formatting.GRAY));
					int itemsPerLevel = FurnaceConfig.getItemsPerLevel();
					int threshold = Math.max(1, itemsPerLevel * Math.max(1, level));
					lines.add(Text.translatable("tooltip.ultimate_furnace.smelted", smelts).append(Text.literal(" / ")).append(Text.literal(String.valueOf(threshold))).formatted(Formatting.DARK_GRAY));
					lines.add(Text.translatable("tooltip.ultimate_furnace.power", stored, maxStored).formatted(Formatting.GOLD));
					if (maxStored > 0) {
						boolean unicode = ClientConfig.useUnicodeBar();
						Formatting color = safeColor(ClientConfig.barColor());
						int barLen = 20;
						double pct = Math.min(1.0, stored / (double) maxStored);
						int filled = (int) Math.round(barLen * pct);
						StringBuilder bar = new StringBuilder();
						char filledChar = unicode ? '█' : '|';
						char emptyChar = unicode ? '░' : '.';
						for (int i = 0; i < barLen; i++) bar.append(i < filled ? filledChar : emptyChar);
						lines.add(Text.literal(bar.toString()).formatted(color));
					}
				}
			}
		});
	}

	private static Formatting safeColor(String name) {
		try { return Formatting.valueOf(name.toUpperCase()); } catch (Exception e) { return Formatting.DARK_GREEN; }
	}
}
