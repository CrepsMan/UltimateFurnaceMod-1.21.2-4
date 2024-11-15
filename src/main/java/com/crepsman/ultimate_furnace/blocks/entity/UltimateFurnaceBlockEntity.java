package com.crepsman.ultimate_furnace.blocks.entity;

import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.logging.Logger;

public class UltimateFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements SidedInventory {
	private static final Logger LOGGER = Logger.getLogger(UltimateFurnaceBlockEntity.class.getName());
	private static final int MAX_LEVEL = 5;
	private static final int ITEMS_PER_LEVEL = 3000;
	private static final int MAX_STORED_POWER = 10000;

	private int smeltCount = 0;
	private int level = 1;
	private boolean isDaytimeBurning = false;
	private int burnTime;
	private int storedPower = 0;

	private final PropertyDelegate propertyDelegate;

	public UltimateFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY, pos, state, RecipeType.SMELTING);
		this.propertyDelegate = new PropertyDelegate() {
			@Override
			public int get(int index) {
				switch (index) {
					case 0: return smeltCount;
					case 1: return level;
					case 2: return burnTime;
					case 3: return isDaytimeBurning ? 1 : 0;
					case 4: return storedPower;
					default: return 0;
				}
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case 0: smeltCount = value; break;
					case 1: level = value; break;
					case 2: burnTime = value; break;
					case 3: isDaytimeBurning = value == 1; break;
					case 4: storedPower = value; break;
				}
			}

			@Override
			public int size() {
				return 5;
			}
		};
	}

	public int getSmeltCount() {
		return smeltCount;
	}

	private void levelUp() {
		if (level < MAX_LEVEL) {
			level++;
			smeltCount = 0;
		}
	}

	public void setSmeltCount(int count) {
		this.smeltCount = count;
	}

	public boolean isBurning() {
		return this.burnTime > 0;
	}

	public void resetSmelting() {
		this.smeltCount = 0;
	}

	public void smelt() {
		if (this.isBurning()) {
			this.smeltCount++;
			LOGGER.info("Smelting item. Smelt count: " + this.smeltCount);
			if (this.smeltCount >= ITEMS_PER_LEVEL * this.level) {
				this.levelUp();
			}
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, UltimateFurnaceBlockEntity blockEntity) {
		if (!world.isClient()) {
			blockEntity.updateDaytimeBurning(world);
			if (blockEntity.isDaytimeBurning) {
				blockEntity.storedPower = Math.min(blockEntity.storedPower + 1, MAX_STORED_POWER * blockEntity.level);
				if (blockEntity.burnTime > 0) {
					blockEntity.burnTime--;
					blockEntity.smelt();
				} else if (blockEntity.storedPower > 0) {
					blockEntity.burnTime = 1;
					blockEntity.storedPower--;
				}
			} else {
				if (blockEntity.storedPower > 0) {
					blockEntity.burnTime = 1;
					blockEntity.storedPower--;
				} else {
					blockEntity.burnTime = 0;
				}
			}
		}
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, Direction side) {
		boolean canInsert = slot != 1 && super.canInsert(slot, stack, side);
		if (canInsert) {
			LOGGER.info("Item inserted into slot: " + slot + ", Item: " + stack.getItem().getName().getString());
		}
		return canInsert;
	}

	private void updateDaytimeBurning(World world) {
		isDaytimeBurning = world.isDay() && world.getLightLevel(pos.up()) == 15;
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		return new int[] {0, 2};
	}

	@Override
	protected Text getContainerName() {
		return Text.literal("Ultimate Furnace");
	}

	@Override
	public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new UltimateFurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
	}

	public int getFuelProgress() {
		return isDaytimeBurning ? 100 : 0;
	}
}
