package com.crepsman.ultimate_furnace;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.item.UltimateFurnaceBlockItem;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.registry.ModBlocks;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
		FurnaceConfig.load();
		LOGGER.info("Initializing Ultimate Furnace!");
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				literal("ultimatefurnace")
					.then(literal("reload").requires(src -> src.hasPermissionLevel(4)).executes(ctx -> {
						FurnaceConfig.reload();
						for (ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
							updatePlayerFurnaceStacks(player);
						}
						ctx.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.reload"), true);
						return 1;
					}))
					.then(literal("info")
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(ctx -> {
								BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "position");
								Block block = ctx.getSource().getWorld().getBlockState(pos).getBlock();
								if (block != ModBlocks.ULTIMATE_FURNACE) {
									ctx.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
									return 0;
								}
								var opt = ctx.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY);
								if (opt.isEmpty()) {
									ctx.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
									return 0;
								}
								UltimateFurnaceBlockEntity be = opt.get();
								int level = be.getFurnaceLevel();
								int smelt = be.getSmeltCount();
								int stored = be.getStoredPower();
								int maxStoredAt = FurnaceConfig.getMaxStoredPowerForLevel(level);
								int itemsPerAt = FurnaceConfig.getItemsPerLevel();
								int cookAt = FurnaceConfig.getCookTimeForLevel(level);
								int powerGainAt = FurnaceConfig.getPowerGainRateForLevel(level);
								ctx.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.info_at",
									pos.getX(), pos.getY(), pos.getZ(),
									level, smelt, stored, maxStoredAt, itemsPerAt, cookAt, powerGainAt), false);
								return 1;
							}))
					)
					.then(literal("set")
						.requires(source -> source.hasPermissionLevel(4))
						.then(literal("level")
							.then(argument("level", IntegerArgumentType.integer(1, 5))
								.then(argument("position", BlockPosArgumentType.blockPos())
									.executes(context -> {
										final int level = IntegerArgumentType.getInteger(context, "level");
										final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
										Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
										if (block == ModBlocks.ULTIMATE_FURNACE) {
											var opt = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY);
											if (opt.isPresent()) {
												UltimateFurnaceBlockEntity entity = opt.get();
												entity.setLevel(level);
												context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.level", entity.getFurnaceLevel()), true);
											} else {
												context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
											}
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
											var opt = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY);
											if (opt.isPresent()) {
												UltimateFurnaceBlockEntity entity = opt.get();
												entity.setSmeltCount(smeltCount);
												context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.count", entity.getSmeltCount()), true);
											} else {
												context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
											}
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
											var opt = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY);
											if (opt.isPresent()) {
												UltimateFurnaceBlockEntity entity = opt.get();
												entity.setStoredPower(storedPower);
												context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.set.power", entity.getStoredPower()), true);
											} else {
												context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
											}
										} else {
											context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
										}
										return 1;
									}))))
					) // close 'set'
					.then(literal("get")
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(context -> {
								final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
								Block block = context.getSource().getWorld().getBlockState(pos).getBlock();
								if (block == ModBlocks.ULTIMATE_FURNACE) {
									var opt = context.getSource().getWorld().getBlockEntity(pos, ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY);
									if (opt.isPresent()) {
										UltimateFurnaceBlockEntity entity = opt.get();
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.get", entity.getFurnaceLevel(), entity.getSmeltCount(), entity.getStoredPower()), true);
									} else {
										context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
									}
								} else {
									context.getSource().sendFeedback(() -> Text.translatable("commands.ultimate_furnace.fail"), false);
								}
								return 1;
							})))
				);
		});

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(UltimateFurnaceMod::addItemsToFunctionalItemGroup);



	}


	private static void addItemsToFunctionalItemGroup(FabricItemGroupEntries entries) {
		entries.add(ModBlocks.ULTIMATE_FURNACE);
		entries.add(ModBlocks.COPPER_PLATE);
	}

	private static void updatePlayerFurnaceStacks(ServerPlayerEntity player) {
		if (player == null) return;
		ItemStack offHand = player.getOffHandStack();
		updateFurnaceStack(offHand);
		for (int i = 0; i < player.getInventory().size(); i++) {
			updateFurnaceStack(player.getInventory().getStack(i));
		}
	}

	private static void updateFurnaceStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return;
		if (stack.getItem() != ModBlocks.ULTIMATE_FURNACE.asItem()) return;
		var data = stack.get(DataComponentTypes.CUSTOM_DATA);
		var tag = data != null ? data.getNbt() : new net.minecraft.nbt.NbtCompound();
		int level = tag.getInt("Level", 1);
		int maxStored = FurnaceConfig.getMaxStoredPowerForLevel(level);
		tag.putInt("ItemsPerLevel", FurnaceConfig.getItemsPerLevel());
		tag.putInt("MaxStoredPower", maxStored);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
		// Rebuild dynamic lore using newer API
		UltimateFurnaceBlockItem.applyDynamicLore(stack);
	}
}
