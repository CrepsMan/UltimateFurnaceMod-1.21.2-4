package com.crepsman.ultimate_furnace.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.logging.ILogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HotPlateBlock extends Block {
	public static final BooleanProperty POWERED = BooleanProperty.of("powered");
	private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
	private static final Logger LOGGER = Logger.getLogger(HotPlateBlock.class.getName());

	public HotPlateBlock(Settings settings) {
		super(settings);
		System.out.println("Block Identifier: " + Registries.BLOCK.getId(this));
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		if (state.get(POWERED) && world instanceof ServerWorld serverWorld) {
			if (entity == null) {
				LOGGER.log(Level.SEVERE, "Entity is null");
				return;
			}
			entity.setFireTicks(100);
			entity.damage(serverWorld, serverWorld.getDamageSources().inFire(), 2.0F);
		}
		super.onSteppedOn(world, pos, state, entity);
	}

	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
		boolean isPowered = world.isReceivingRedstonePower(pos);
		if (state.get(POWERED) != isPowered) {
			world.setBlockState(pos, state.with(POWERED, isPowered), Block.NOTIFY_LISTENERS);
			world.emitGameEvent(null, isPowered ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
		}
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBlockState(pos.down()).getFluidState().isStill()) {
			for (int i = 0; i < 5; i++) {
				double x = pos.getX() + random.nextDouble();
				double y = pos.getY() + 0.5;
				double z = pos.getZ() + random.nextDouble();
				world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.1, 0.0, 0.01);
			}
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(POWERED)) {
			for (int i = 0; i < 2; i++) {
				double x = pos.getX() + random.nextDouble();
				double y = pos.getY() + 1.0;
				double z = pos.getZ() + random.nextDouble();
				world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
			}
		}
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return BASE_SHAPE;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}
}
