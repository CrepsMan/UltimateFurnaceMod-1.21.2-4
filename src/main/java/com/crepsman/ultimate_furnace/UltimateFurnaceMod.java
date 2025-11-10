package com.crepsman.ultimate_furnace;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.registry.ModBlocks;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import com.crepsman.ultimate_furnace.util.ItemUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.*;

public class UltimateFurnaceMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Ultimate Furnace");
	public static final String MOD_ID = "ultimate_furnace";

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Ultimate Furnace!");
		FurnaceConfig.load();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			// /ultimatefurnace reload
			var reload = literal("reload")
				.requires(src -> src.hasPermissionLevel(4))
				.executes(ctx -> {
					FurnaceConfig.reload();
					ctx.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.reload.success"), true);
					return 1;
				});

			// /ultimatefurnace info <pos>
			var info = literal("info")
				.requires(src -> src.hasPermissionLevel(FurnaceConfig.getInfoPermissionLevel()))
				.then(argument("position", BlockPosArgumentType.blockPos())
					.executes(context -> {
						BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
						Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
						if (block == ModBlocks.ULTIMATE_FURNACE) {
							UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).orElse(null);
							if (entity != null) {
								context.getSource().sendFeedback(() -> Text.translatable(
									"commands.ultimate_furnace.info",
									entity.getFurnaceLevel(), entity.getSmeltCount(), entity.getStoredPower(),
									UltimateFurnaceBlockEntity.getMaxStoredPower(entity.getFurnaceLevel()),
									FurnaceConfig.getItemsPerLevel(),
									FurnaceConfig.getCookTimeForLevel(entity.getFurnaceLevel()),
									FurnaceConfig.getPowerGainRateForLevel(entity.getFurnaceLevel())
								), false);
							} else {
								context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
						}
						return 1;
					}));

			// /ultimatefurnace get <pos>
			var get = literal("get")
				.then(argument("position", BlockPosArgumentType.blockPos())
					.executes(context -> {
						BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
						Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
						if (block == ModBlocks.ULTIMATE_FURNACE) {
							UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).orElse(null);
							if (entity != null) {
								context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.get", entity.getFurnaceLevel(), entity.getSmeltCount(), entity.getStoredPower()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
						}
						return 1;
					}));

			// /ultimatefurnace set <sub>
			var set = literal("set")
				.requires(src -> src.hasPermissionLevel(4))
				.then(literal("level")
					.then(argument("level", IntegerArgumentType.integer(1, FurnaceConfig.getMaxLevel()))
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(context -> {
								int level = IntegerArgumentType.getInteger(context, "level");
								BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
								Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
								if (block == ModBlocks.ULTIMATE_FURNACE) {
									UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).orElse(null);
									if (entity != null) {
										entity.setLevel(level);
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.level", entity.getFurnaceLevel()), true);
									} else context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								} else context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								return 1;
							}))
					))
				.then(literal("smeltcount")
					.then(argument("smeltcount", IntegerArgumentType.integer(0, 15000))
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(context -> {
								int count = IntegerArgumentType.getInteger(context, "smeltcount");
								BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
								Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
								if (block == ModBlocks.ULTIMATE_FURNACE) {
									UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).orElse(null);
									if (entity != null) {
										entity.setSmeltCount(count);
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.count", entity.getSmeltCount()), true);
									} else context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								} else context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								return 1;
							}))
					))
				.then(literal("storepower")
					.then(argument("storedPower", IntegerArgumentType.integer(0, 18000))
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(context -> {
								int power = IntegerArgumentType.getInteger(context, "storedPower");
								BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
								Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
								if (block == ModBlocks.ULTIMATE_FURNACE) {
									UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).orElse(null);
									if (entity != null) {
										entity.setStoredPower(power);
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.power", entity.getStoredPower()), true);
									} else context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								} else context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								return 1;
							}))
					));

			dispatcher.register(literal("ultimatefurnace")
				.then(reload)
				.then(info)
				.then(get)
				.then(set)
			);
		});

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(UltimateFurnaceMod::addItemsToFunctionalItemGroup);
	}


	private static void addItemsToFunctionalItemGroup(FabricItemGroupEntries entries) {
		entries.add(ModBlocks.ULTIMATE_FURNACE);
		entries.add(ModBlocks.COPPER_PLATE);
	}
}
