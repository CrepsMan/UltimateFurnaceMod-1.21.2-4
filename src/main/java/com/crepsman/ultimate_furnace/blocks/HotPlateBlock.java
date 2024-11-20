package com.crepsman.ultimate_furnace.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class HotPlateBlock extends Block {
    public static final BooleanProperty POWERED;
    private static final VoxelShape BASE_SHAPE;

    public HotPlateBlock(Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)this.getDefaultState().with(POWERED, false));
    }

    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean bl = (Boolean)state.get(POWERED);
            if (bl != world.isReceivingRedstonePower(pos)) {
                if (bl) {
                    world.scheduleBlockTick(pos, this, 4);
                } else {
                    world.setBlockState(pos, (BlockState)state.cycle(POWERED), 2);
                }
            }

        }
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (state.get(POWERED)) {
            entity.setFireTicks(20);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && neighborState.isOf(Blocks.WATER) && state.get(POWERED)) {
            world.setBlockState(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()), Blocks.AIR.getDefaultState(), 1);
            world.addParticle(ParticleTypes.CLOUD, pos.getX(), pos.getY() + 0.5, pos.getZ(), 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.25, pos.getY() + 0.5, pos.getZ() + 0.25, 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.25, pos.getY() + 0.5, pos.getZ(), 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX(), pos.getY() + 0.5, pos.getZ() + 0.25, 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX() - 0.25, pos.getY() + 0.5, pos.getZ() + 0.25, 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX()- 0.25, pos.getY() + 0.5, pos.getZ(), 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX(), pos.getY() + 0.5, pos.getZ()- 0.25, 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX()- 0.25, pos.getY() + 0.5, pos.getZ()- 0.25, 0, 0, 0);
            world.addParticle(ParticleTypes.CLOUD, pos.getX()+ 0.25, pos.getY() + 0.5, pos.getZ()- 0.25, 0, 0, 0);
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if ((Boolean)state.get(POWERED) && !world.isReceivingRedstonePower(pos)) {
            world.setBlockState(pos, (BlockState)state.cycle(POWERED), 2);
        }

    }

    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BASE_SHAPE;
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{POWERED});
    }

    static {
        POWERED = RedstoneTorchBlock.LIT;
        BASE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
    }
}
