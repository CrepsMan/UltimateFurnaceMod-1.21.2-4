package com.crepsman.ultimate_furnace.blocks;

import com.crepsman.ultimate_furnace.util.ModProperties;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class CopperPlateBlock extends Block implements Waterloggable {
	public static final BooleanProperty HOT;
	public static final BooleanProperty COLD;
	public static final BooleanProperty WATERLOGGED;
	private static final VoxelShape BASE_SHAPE;


	public CopperPlateBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(HOT, false).with(COLD, false).with(WATERLOGGED, false));
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		if (state.get(HOT) && world instanceof ServerWorld serverWorld) {
			entity.setFireTicks(100);
			entity.damage(serverWorld, serverWorld.getDamageSources().inFire(), 2.0F);
			entity.addVelocity(0, 0.5, 0);
		}else if (state.get(COLD)) {
			entity.setInPowderSnow(true);
		}
		super.onSteppedOn(world, pos, state, entity);
	}

	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
		boolean isHOT = sourceBlock == Blocks.MAGMA_BLOCK;
		boolean isCOLD = sourceBlock == Blocks.PACKED_ICE;
		if (isCOLD&& !isHOT) {
			state.cycle(COLD);
		} else if (isHOT && !isCOLD) {
			state.cycle(HOT);
		}

		if (isHOT) {
			BlockPos[] adjacentPositions = {
				pos.north(), pos.south(), pos.east(), pos.west(), pos.down(), pos.up()
			};

			for (BlockPos adjacentPos : adjacentPositions) {
				if (world.getBlockState(adjacentPos).getFluidState().isStill()) {
					world.scheduleBlockTick(pos, this, 20);
					break;
				}
			}
		}
	}

	@Nullable
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER).with(HOT, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(HOT)) {
			boolean waterEvaporated = false;

			// Check a circular area with a radius of 3 blocks
			for (int dx = -3; dx <= 3; dx++) {
				for (int dy = -3; dy <= 3; dy++) {
					for (int dz = -3; dz <= 3; dz++) {
						if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 3) {
							BlockPos adjacentPos = pos.add(dx, dy, dz);
							BlockState adjacentState = world.getBlockState(adjacentPos);
							FluidState fluidState = adjacentState.getFluidState();
							if (!fluidState.isEmpty()) {
								world.setBlockState(adjacentPos, Blocks.AIR.getDefaultState(), 3);
								world.playSound(null, adjacentPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
								waterEvaporated = true;

								// Spawn particles where the water disappears
								for (int i = 0; i < 10; i++) {
									double x = adjacentPos.getX() + random.nextDouble();
									double y = adjacentPos.getY() + 0.5;
									double z = adjacentPos.getZ() + random.nextDouble();
									world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
									world.spawnParticles(ParticleTypes.BUBBLE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
								}
							}
						}
					}
				}
			}

			if (state.get(WATERLOGGED)) {
				world.setBlockState(pos, state.with(WATERLOGGED, false), 3);
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
				waterEvaporated = true;

				// Spawn particles where the water disappears
				for (int i = 0; i < 10; i++) {
					double x = pos.getX() + random.nextDouble();
					double y = pos.getY() + 0.5;
					double z = pos.getZ() + random.nextDouble();
					world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
					world.spawnParticles(ParticleTypes.BUBBLE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
					world.spawnParticles(ParticleTypes.DRIPPING_WATER, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
				}
			}
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(HOT)) {
			for (int i = 0; i < 2; i++) {
				double x = pos.getX() + random.nextDouble();
				double y = pos.getY() + 0.625;
				double z = pos.getZ() + random.nextDouble();
				world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
				world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.01, 0.0);
			}
		}else if (state.get(COLD)) {
			for (int i = 0; i < 2; i++) {
				double x = pos.getX() + random.nextDouble();
				double y = pos.getY() + 0.625;
				double z = pos.getZ() + random.nextDouble();
				world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, 0.01, 0.0);
			}
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
		if (neighborState.getFluidState().getFluid() == Fluids.WATER || neighborState.getFluidState().getFluid() == Fluids.FLOWING_WATER || (neighborState.contains(Properties.WATERLOGGED) && neighborState.get(WATERLOGGED)))  {
			tickView.scheduleBlockTick(pos, this, 2);
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
