package com.crepsman.ultimate_furnace;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.registry.ModBlocks;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.item.ItemGroups;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class UltimateFurnaceMod implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("Ultimate Furnace");
	public static final String MOD_ID = "ultimate_furnace";

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Ultimate Furnace!");
		// Register blocks, block entities, and screen handlers
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(UltimateFurnaceMod::addItemsToFunctionalItemGroup);


		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("ultimatefurnace")
				.then(literal("set")
					.requires(source -> source.hasPermissionLevel(4)) // Only operators can use this command
					.then(literal("level")
						.then(argument("level", IntegerArgumentType.integer(1, 5))
							.then(argument("position", BlockPosArgumentType.blockPos())
								.executes(context -> {
									final int level = IntegerArgumentType.getInteger(context, "level");
									final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");

									Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
									if (block == ModBlocks.ULTIMATE_FURNACE) {
										UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).get();
										entity.setLevel(level);
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.level", entity.getFurnaceLevel()), true);
									} else {
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
									}
									return 1;
								}))))
					.then(literal("smeltcount")
						.then(argument("smeltcount", IntegerArgumentType.integer(0, 15000))
							.then(argument("position", BlockPosArgumentType.blockPos())
								.executes(context -> {
									final int smeltCount = IntegerArgumentType.getInteger(context, "smeltcount");
									final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");

									Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
									if (block == ModBlocks.ULTIMATE_FURNACE) {
										UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).get();
										entity.setSmeltCount(smeltCount);
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.count", entity.getSmeltCount()), true);
									} else {
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
									}
									return 1;
								}))))
					.then(literal("storedPower")
						.then(argument("storedPower", IntegerArgumentType.integer(0, 18000))
							.then(argument("position", BlockPosArgumentType.blockPos())
								.executes(context -> {
									final int storedPower = IntegerArgumentType.getInteger(context, "storedPower");
									final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");

									Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
									if (block == ModBlocks.ULTIMATE_FURNACE) {
										UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).get();
										entity.setStoredPower(storedPower);
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.power", entity.getStoredPower()), true);
									} else {
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
									}
									return 1;
								})))))
					.then(literal("get")
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(context -> {
								final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
								Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
								if (block == ModBlocks.ULTIMATE_FURNACE) {
									UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).get();
									context.getSource().sendFeedback(() -> Text.literal("Level: " + entity.getFurnaceLevel() + ", Smelt Count: " + entity.getSmeltCount() + ", Stored Power: " + entity.getStoredPower()), true);
								} else {
									context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								}
								return 1;
							})))
				));



	}

	private static void addItemsToFunctionalItemGroup(FabricItemGroupEntries entries) {
		entries.add(ModBlocks.ULTIMATE_FURNACE);
		entries.add(ModBlocks.COPPER_PLATE);
	}

	public static Identifier id(String path) {
		return Identifier.tryParse(MOD_ID, path);
	}
}
