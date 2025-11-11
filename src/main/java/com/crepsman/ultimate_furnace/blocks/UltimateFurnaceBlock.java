package com.crepsman.ultimate_furnace.blocks;

import com.crepsman.ultimate_furnace.blocks.entity.UltimateFurnaceBlockEntity;
import com.crepsman.ultimate_furnace.item.UltimateFurnaceBlockItem;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import com.crepsman.ultimate_furnace.util.ModProperties;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UltimateFurnaceBlock extends AbstractFurnaceBlock {
	public static final BooleanProperty DAY_MODE;

	public UltimateFurnaceBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState()
			.with(FACING, Direction.NORTH)
			.with(LIT, false)
			.with(DAY_MODE, false)
		);
	}

	@Override
	protected MapCodec<UltimateFurnaceBlock> getCodec() {
		return createCodec(UltimateFurnaceBlock::new);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new UltimateFurnaceBlockEntity(pos, state);
	}


	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIT, DAY_MODE, FACING);
	}


	protected void openScreen(World world, BlockPos pos, PlayerEntity player) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof UltimateFurnaceBlockEntity) {
			player.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
			player.incrementStat(Stats.INTERACT_WITH_FURNACE);
		}
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		if (!world.isClient) {
			updateDayMode(world, state, pos);
		}
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (world.isClient) {
			return null;
		}

		if (type == ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY) {
			return (world1, pos, state1, blockEntity) -> {
				// This cast is safe because we've verified that T is UltimateFurnaceBlockEntity
				UltimateFurnaceBlockEntity.tick(world1, pos, state1, (UltimateFurnaceBlockEntity)blockEntity);
			};
		}

		return null;
	}

	public void updateDayMode(World world, BlockState state, BlockPos pos) {
		boolean isDay = world.getTimeOfDay() % 24000 < 12000;

		if (state.get(DAY_MODE) != isDay) {
			world.setBlockState(pos, state.with(DAY_MODE, isDay), 3);
		}

		scheduleNextTick(world, pos);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		boolean isDay = world.getTimeOfDay() % 24000 < 12000;
		boolean hasDirectSkylight = world.getLightLevel(LightType.SKY, pos.up()) > 0;
		boolean newDayMode = isDay && hasDirectSkylight;

		if (state.get(DAY_MODE) != newDayMode) {
			world.setBlockState(pos, state.with(DAY_MODE, newDayMode), 3);
		}

		scheduleNextTick(world, pos);
	}

	private void scheduleNextTick(World world, BlockPos pos) {
		if (!world.isClient) {
			world.scheduleBlockTick(pos, this, 200); // TickPriority is removed here
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(LIT)) {
			double d = pos.getX() + 0.5;
			double e = pos.getY() + 0.0; // Adjusted height for particles
			double f = pos.getZ() + 0.5;

			if (random.nextDouble() < 0.1) {
				world.playSound(null, d, e, f, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F);			}

			Direction direction = state.get(FACING);
			Direction.Axis axis = direction.getAxis();
			double g = 0.52; // Offset distance
			double h = random.nextDouble() * 0.6 - 0.3; // Horizontal randomization
			double i = axis == Direction.Axis.X ? direction.getOffsetX() * g : h;
			double j = random.nextDouble() * 0.6 + 0.2; // Vertical randomization (centered)
			double k = axis == Direction.Axis.Z ? direction.getOffsetZ() * g : h;

			world.addParticleClient(ParticleTypes.SMOKE, d + i, e + j, f + k, 0.0, 0.0, 0.0);
			world.addParticleClient(ParticleTypes.FLAME, d + i, e + j, f + k, 0.0, 0.0, 0.0);
		}
	}

	// Correct afterBreak override (1.21.5 signature) for custom preserved item drop
	@Override
	public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
		if (!world.isClient && blockEntity instanceof UltimateFurnaceBlockEntity furnace) {
			ItemScatterer.spawn(world, pos, furnace);
			if (!player.isCreative()) {
				ItemStack drop = new ItemStack(this);
				NbtCompound tag = new NbtCompound();
				tag.putInt("Level", furnace.getFurnaceLevel());
				tag.putInt("SmeltCount", furnace.getSmeltCount());
				tag.putInt("StoredPower", furnace.getStoredPower());
				tag.putInt("ItemsPerLevel", FurnaceConfig.getItemsPerLevel());
				int maxPower = FurnaceConfig.getMaxStoredPowerForLevel(furnace.getFurnaceLevel());
				tag.putInt("MaxStoredPower", maxPower);
				drop.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
				UltimateFurnaceBlockItem.applyDynamicLore(drop);
				Block.dropStack(world, pos, drop);
			}
		}
		// Skip super to prevent vanilla drop
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!world.isClient) {
			NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
			if (data != null) {
				var raw = data.getNbt();
				if (raw instanceof NbtCompound tag) {
					BlockEntity be = world.getBlockEntity(pos);
					if (be instanceof UltimateFurnaceBlockEntity furnace) {
						NbtElement lvlE = tag.get("Level");
						if (lvlE instanceof NbtInt lvl) furnace.setLevel(lvl.intValue());
						NbtElement cntE = tag.get("SmeltCount");
						if (cntE instanceof NbtInt cnt) furnace.setSmeltCount(cnt.intValue());
						NbtElement powE = tag.get("StoredPower");
						if (powE instanceof NbtInt pow) furnace.setStoredPower(pow.intValue());
						if (!tag.contains("ItemsPerLevel")) {
							tag.putInt("ItemsPerLevel", FurnaceConfig.getItemsPerLevel());
						}
						if (!tag.contains("MaxStoredPower")) {
							int maxP = FurnaceConfig.getMaxStoredPowerForLevel(furnace.getFurnaceLevel());
							tag.putInt("MaxStoredPower", maxP);
						}
						stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
					}
				}
			}
		}
		super.onPlaced(world, pos, state, placer, stack);
	}


	static {
		DAY_MODE = ModProperties.DAY_MODE;
	}
}
