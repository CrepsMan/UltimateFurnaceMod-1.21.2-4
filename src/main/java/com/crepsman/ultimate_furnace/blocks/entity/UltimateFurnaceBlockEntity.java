package com.crepsman.ultimate_furnace.blocks.entity;

import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import com.crepsman.ultimate_furnace.util.ModProperties;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.Optional;
import java.util.logging.Logger;

public class UltimateFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements SidedInventory {
	private static final Logger LOGGER = Logger.getLogger(UltimateFurnaceBlockEntity.class.getName());
	private static final int MAX_LEVEL = 5;
	private static final int ITEMS_PER_LEVEL = 3000;
	public static int BASE_MAX_STORED_POWER = 10000;

	private int smeltCount = 0;
	private int level = 1;
	private int burnTime;
	private int storedPower = 0;
	private int cookTime;
	private int cookTimeTotal;

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
					case 3: return storedPower;
					case 4: return cookTime;
					case 5: return cookTimeTotal;
					default: return 0;
				}
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case 0: smeltCount = value; break;
					case 1: level = value; break;
					case 2: burnTime = value; break;
					case 3: storedPower = value; break;
					case 4: cookTime = value; break;
					case 5: cookTimeTotal = value; break;
				}
			}

			@Override
			public int size() {
				return 6;
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
			LOGGER.info("Furnace leveled up to level " + level);
		}
	}

	public int getFurnaceLevel() {
		return this.level;
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

	private void updateDaytimeBurning(World world, BlockPos pos) {
		boolean hasSkylight = world.getLightLevel(LightType.SKY, pos.up()) > 0;
		boolean isDay = world.getTimeOfDay() % 24000 < 12000;

		if (isDay && hasSkylight) {
			this.storedPower = Math.min(this.storedPower + (level * 10), BASE_MAX_STORED_POWER * level);
			this.burnTime = Math.max(this.burnTime, 200); // Ensure some burn time remains
		} else {
			this.burnTime = Math.max(0, this.burnTime - 1);
		}

		// Update block state to reflect day/night mode
		if (world.getBlockState(pos).get(ModProperties.DAY_MODE) != (isDay && hasSkylight)) {
			world.setBlockState(pos, world.getBlockState(pos).with(ModProperties.DAY_MODE, isDay && hasSkylight), Block.NOTIFY_ALL);
		}
	}


	public static void tick(World world, BlockPos pos, BlockState state, UltimateFurnaceBlockEntity blockEntity) {
		blockEntity.updateDaytimeBurning(world, pos);

		boolean isBurning = blockEntity.isBurning();
		boolean stateChanged = false;

		if (isBurning) {
			--blockEntity.burnTime;
		}

		ItemStack inputStack = blockEntity.getStack(0);
		ItemStack outputStack = blockEntity.getStack(2);
		boolean hasInput = !inputStack.isEmpty();
		boolean canSmelt = outputStack.getCount() < outputStack.getMaxCount();

		if (hasInput && canSmelt) {
			Optional<RecipeEntry<SmeltingRecipe>> recipeEntry = blockEntity.getFirstMatch(new SingleStackRecipeInput(inputStack), world);
			if (recipeEntry.isPresent()) {
				if (!isBurning && blockEntity.storedPower > 0) {
					blockEntity.burnTime = blockEntity.getCookTime(world);
					blockEntity.cookTimeTotal = blockEntity.burnTime;
					stateChanged = true;
				}

				if (blockEntity.isBurning()) {
					blockEntity.cookTime += blockEntity.level; // Increase cook time based on level
					if (blockEntity.cookTime >= blockEntity.cookTimeTotal) {
						blockEntity.cookTime = 0;
						blockEntity.cookTimeTotal = blockEntity.getCookTime(world);
						if (blockEntity.smeltItem(world.getRegistryManager(), recipeEntry.get())) {
							stateChanged = true;
						}
					}
				} else {
					blockEntity.cookTime = 0;
				}
			} else {
				blockEntity.cookTime = 0;
			}
		} else {
			blockEntity.cookTime = 0;
		}

		if (isBurning != blockEntity.isBurning()) {
			world.setBlockState(pos, state.with(AbstractFurnaceBlock.LIT, blockEntity.isBurning()), Block.NOTIFY_ALL);
			stateChanged = true;
		}

		if (stateChanged) {
			blockEntity.markDirty();
		}

		if (blockEntity.smeltCount >= ITEMS_PER_LEVEL * blockEntity.getFurnaceLevel()) {
			blockEntity.levelUp();
		}
	}
	@Override
	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		super.writeNbt(nbt, lookup);
		nbt.putInt("SmeltCount", this.smeltCount);
		nbt.putInt("Level", this.level);
		nbt.putInt("BurnTime", this.burnTime);
		nbt.putInt("StoredPower", this.storedPower);
	}

	@Override
	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		super.readNbt(nbt, lookup);
		this.smeltCount = nbt.getInt("SmeltCount");
		this.level = nbt.getInt("Level");
		this.burnTime = nbt.getInt("BurnTime");
		this.storedPower = nbt.getInt("StoredPower");
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack, Direction side) {
		if (slot == 0) {
			boolean canInsert = this.world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(stack), this.world).isPresent();
			return canInsert;
		}
		return false;
	}

	private Optional<RecipeEntry<SmeltingRecipe>> getFirstMatch(SingleStackRecipeInput input, World world) {
		return this.world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, input, world);
	}

	private boolean canAcceptRecipeOutput(DynamicRegistryManager registryManager, RecipeEntry<SmeltingRecipe> recipe, DefaultedList<ItemStack> slots, int count) {
		if (!slots.get(0).isEmpty() && recipe != null) {
			ItemStack itemStack = recipe.value().craft(new SingleStackRecipeInput(slots.get(0)), registryManager);
			if (itemStack.isEmpty()) {
				return false;
			} else {
				ItemStack itemStack2 = slots.get(2);
				if (itemStack2.isEmpty()) {
					return true;
				} else if (!ItemStack.areItemsEqual(itemStack2, itemStack)) {
					return false;
				} else if (itemStack2.getCount() < count && itemStack2.getCount() < itemStack2.getMaxCount()) {
					return true;
				} else {
					return itemStack2.getCount() < itemStack.getMaxCount();
				}
			}
		} else {
			return false;
		}
	}

	private boolean smeltItem(DynamicRegistryManager registryManager, RecipeEntry<SmeltingRecipe> recipe) {
		if (recipe != null && canAcceptRecipeOutput(registryManager, recipe, this.inventory, this.getMaxCountPerStack())) {
			ItemStack inputStack = this.inventory.get(0);
			ItemStack outputStack = recipe.value().craft(new SingleStackRecipeInput(inputStack), registryManager);
			ItemStack currentOutputStack = this.inventory.get(2);

			if (currentOutputStack.isEmpty()) {
				this.inventory.set(2, outputStack.copy());
			} else if (ItemStack.areItemsEqual(currentOutputStack, outputStack)) {
				currentOutputStack.increment(outputStack.getCount());
			}

			inputStack.decrement(1);
			smeltCount++;
			return true;
		} else {
			return false;
		}
	}

	private int getCookTime(World world) {
		return this.world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(this.getStack(0)), world)
			.map(recipe -> recipe.value().getCookingTime() / this.level).orElse(200 / this.level); // Decrease cook time based on level
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		return new int[]{0, 2};
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
		return burnTime > 0 ? 100 : 0;
	}
}
