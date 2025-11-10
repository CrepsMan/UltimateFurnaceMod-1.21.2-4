package com.crepsman.ultimate_furnace.blocks;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.util.ModProperties;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;


public class CopperPlateBlock extends Block implements Waterloggable {
	public static final BooleanProperty HOT;
	public static final BooleanProperty COLD;
	public static final BooleanProperty WATERLOGGED;
	private static final VoxelShape BASE_SHAPE;

	public static final TagKey<Block> WARM_BLOCK_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(UltimateFurnaceMod.MOD_ID, "warm_block"));
	public static final TagKey<Block> COLD_BLOCK_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(UltimateFurnaceMod.MOD_ID, "cold_block"));

	public CopperPlateBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(HOT, false).with(COLD, false).with(WATERLOGGED, false));
	}


	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (state.get(HOT) && world instanceof ServerWorld serverWorld) {
			entity.setFireTicks(100);
			entity.damage(serverWorld, serverWorld.getDamageSources().inFire(),2.0F);
			entity.addVelocity(0,0.25,0);
		}else if (state.get(COLD)) {
			entity.setInPowderSnow(true);
			entity.slowMovement(state, new Vec3d(0.9, 1.5, 0.9));
		}
		super.onEntityCollision(state, world, pos, entity);
	}

	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
		updateState(world, pos, state);

		BlockPos[] adjacentPositions = {
			pos.north(), pos.south(), pos.east(), pos.west(), pos.down(), pos.up()
		};

		for (BlockPos adjacentPos : adjacentPositions) {
			if (world.getBlockState(adjacentPos).getFluidState().isStill()) {
				world.scheduleBlockTick(pos, this, 5);
				break;
			}
		}
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		updateState(world, pos, state);
		world.scheduleBlockTick(pos, this, 3);
	}

	private void updateState(World world, BlockPos pos, BlockState state) {
		BlockState blockBelow = world.getBlockState(pos.down());
		boolean isWarmBlock = blockBelow.isIn(WARM_BLOCK_TAG);
		boolean isColdBlock = blockBelow.isIn(COLD_BLOCK_TAG);
		// enforce mutual exclusivity: HOT takes priority over COLD if both detected
		if (isWarmBlock && isColdBlock) {
			isColdBlock = false;
		}
		// avoid unnecessary world updates if state is already correct
		if (state.get(HOT) == isWarmBlock && state.get(COLD) == isColdBlock) return;
		world.setBlockState(pos, state.with(HOT, isWarmBlock).with(COLD, isColdBlock), 3);
	}

	@Nullable
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER).with(HOT, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		updateState(world, pos, state);
		if (state.get(HOT)) {
			int radius = 3;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius) {
							BlockPos adjacentPos = pos.add(dx, dy, dz);
							BlockState adjacentState = world.getBlockState(adjacentPos);
							FluidState fluidState = adjacentState.getFluidState();
							if ((fluidState.getFluid() == Fluids.WATER || fluidState.getFluid() == Fluids.FLOWING_WATER) && (!adjacentState.contains(Properties.WATERLOGGED) || !adjacentState.get(Properties.WATERLOGGED))) {
								world.setBlockState(adjacentPos, Blocks.AIR.getDefaultState(), 3);
								world.playSound(null, adjacentPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
								spawnEvapParticles(world, adjacentPos, random);
							} else if (adjacentState.contains(Properties.WATERLOGGED) && adjacentState.get(Properties.WATERLOGGED)) {
								world.setBlockState(adjacentPos, adjacentState.with(Properties.WATERLOGGED, false), 3);
								world.playSound(null, adjacentPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
								spawnEvapParticles(world, adjacentPos, random);
							} else if (adjacentState.isIn(COLD_BLOCK_TAG)) {
								world.setBlockState(adjacentPos, Blocks.AIR.getDefaultState(), 3);
								world.playSound(null, adjacentPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
								spawnColdRemovalParticles(world, adjacentPos, random);
							}
						}
					}
				}
			}
			if (state.get(WATERLOGGED)) {
				world.setBlockState(pos, state.with(WATERLOGGED, false), 3);
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
				spawnEvapParticles(world, pos, random);
			}
		} else if (state.get(COLD)) {
			// Check a 1-block area around the block (excluding the block above)
			for (Direction direction : Direction.values()) {
				if (direction != Direction.UP) {
					BlockPos adjacentPos = pos.offset(direction);
					BlockState adjacentState = world.getBlockState(adjacentPos);
					FluidState fluidState = adjacentState.getFluidState();
					if (fluidState.getFluid() == Fluids.WATER && fluidState.isStill() && !adjacentState.contains(Properties.WATERLOGGED)) {
						world.setBlockState(adjacentPos, Blocks.ICE.getDefaultState(), 3);
						world.playSound(null, adjacentPos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
						for (int i = 0; i < 10; i++) {
							double x = adjacentPos.getX() + random.nextDouble();
							double y = adjacentPos.getY() + 0.5;
							double z = adjacentPos.getZ() + random.nextDouble();
							world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
						}
					}
				}
			}
		}
	}

	private void spawnEvapParticles(ServerWorld world, BlockPos pos, Random random) {
		for (int i = 0; i < 10; i++) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + 0.5;
			double z = pos.getZ() + random.nextDouble();
			world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 5, 0.0, 0.1, 0.0, 0.01);
			world.spawnParticles(ParticleTypes.BUBBLE, x, y, z, 5, 0.0, 0.1, 0.0, 0.01);
		}
	}

	private void spawnColdRemovalParticles(ServerWorld world, BlockPos pos, Random random) {
		for (int i = 0; i < 10; i++) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + 0.5;
			double z = pos.getZ() + random.nextDouble();
			world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.0, 0.1, 0.0, 0.01);
			world.spawnParticles(ParticleTypes.BUBBLE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
			world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 2, 0.1, 0.1, 0.1, 0.01);
			world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 4, 0.0, 0.1, 0.0, 0.01);
		}
	}


	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return BASE_SHAPE;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(HOT, WATERLOGGED, COLD);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
		if (neighborState.getFluidState().getFluid() == Fluids.WATER || neighborState.getFluidState().getFluid() == Fluids.FLOWING_WATER || (neighborState.contains(Properties.WATERLOGGED) && neighborState.get(WATERLOGGED)) || neighborState.isIn(COLD_BLOCK_TAG)) {
			tickView.scheduleBlockTick(pos, this, 3);
		}
		return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}

	static {
		HOT = ModProperties.HOT;
		COLD = ModProperties.COLD;
		WATERLOGGED = Properties.WATERLOGGED;
		BASE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
	}
}
