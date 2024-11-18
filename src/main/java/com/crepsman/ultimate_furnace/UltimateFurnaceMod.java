package com.crepsman.ultimate_furnace;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.registry.ModBlocks;
import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
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

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("setsmeltcount").requires(source -> source.hasPermissionLevel(2))
				.then(argument("position", BlockPosArgumentType.blockPos())
					.then(argument("set", IntegerArgumentType.integer(0, 15000))
						.executes(context -> {
							final int set = IntegerArgumentType.getInteger(context, "set");
							final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");

							Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
							if (block == ModBlocks.ULTIMATE_FURNACE) {
								UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).get();
								entity.setSmeltCount(set);
								context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.ultimate_furnace.set", entity.getSmeltCount()), true);
							} else {
								context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.ultimate_furnace.fail"), false);
							}
							return 1;
						})))
			));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("getsmeltcount")
				.then(argument("position", BlockPosArgumentType.blockPos())
					.executes(context -> {
						final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
						Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
						if (block == ModBlocks.ULTIMATE_FURNACE) {
							UltimateFurnaceBlockEntity entity = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY).get();
							context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.ultimate_furnace.get", entity.getSmeltCount()), true);
						} else {
							context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.ultimate_furnace.fail"), false);
						}
						return 1;
					}))
			));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("skipnight").requires(source -> source.hasPermissionLevel(2))
				.executes(context -> {
					long l;
					l = context.getSource().getWorld().getTimeOfDay() + 24000L;
					context.getSource().getWorld().setTimeOfDay(l - l % 24000L);
					return 1;
				}))
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("skipday").requires(source -> source.hasPermissionLevel(2))
				.executes(context -> {
					long l;
					l = context.getSource().getWorld().getTimeOfDay() + 24000L;
					context.getSource().getWorld().setTimeOfDay(l - l % 12000L);
					return 1;
				}))
		);

		// Register blocks, block entities, and screen handlers
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();
	}

	public static Identifier id(String path) {
		return Identifier.tryParse(MOD_ID, path);
	}
}
