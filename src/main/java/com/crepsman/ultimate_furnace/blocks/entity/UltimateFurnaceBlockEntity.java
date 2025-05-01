package com.crepsman.ultimate_furnace.blocks.entity;

import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
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
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.logging.Level;


public class UltimateFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements SidedInventory {
	private static final int MAX_LEVEL = 5;
	private static final int ITEMS_PER_LEVEL = 3000;

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

		if (this.world != null) {
			boolean isDay = this.world.getTimeOfDay() % 24000 < 12000;
			boolean hasDirectSkylight = this.world.getLightLevel(LightType.SKY, pos.up()) > 0;

			if (!state.get(ModProperties.DAY_MODE)) {
				this.world.setBlockState(pos, state.with(ModProperties.DAY_MODE, isDay && hasDirectSkylight), Block.NOTIFY_ALL);
			}
		}
	}

	@Override
	public boolean canExtract(int slot, ItemStack stack, Direction side) {
		return slot != 0; // Prevent extraction from the input slot
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

	public int getFurnaceLevel() {
		return this.level;
	}

	public void setSmeltCount(int count) {
		this.smeltCount = count;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public boolean isBurning() {
		return this.cookTime > 0 || this.storedPower > 0;
	}

	private void updateDaytimeBurning(World world, BlockPos pos) {
		if (world == null) return;

		boolean isDay = world.getTimeOfDay() % 24000 < 12000;
		boolean hasDirectSkylight = world.getLightLevel(LightType.SKY, pos.up()) > 0;
		boolean newDayMode = isDay && hasDirectSkylight;

		if (newDayMode && this.level > 1 && this.storedPower < getMaxStoredPower(this.level)) {
			float powerGainRate = switch (this.level) {
				case 2 -> 1.0f;
				case 3 -> 2.0f;
				case 4 -> 4.0f;
				case 5 -> 5.0f;
				default -> 0.0f;
			};
			this.storedPower = (int) Math.min(this.storedPower + powerGainRate, getMaxStoredPower(this.level));
		}

		BlockState currentState = world.getBlockState(pos);
		if (currentState.get(UltimateFurnaceBlock.DAY_MODE) != newDayMode) {
			world.setBlockState(pos, currentState.with(UltimateFurnaceBlock.DAY_MODE, newDayMode), Block.NOTIFY_ALL);
		}
	}

	private void smeltItem(DynamicRegistryManager registryManager, RecipeEntry<SmeltingRecipe> recipe) {
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
			this.cookTime = 0; // Reset cook time
			this.cookTimeTotal = getCookTime(this.world); // Set cook time based on level

			// Decrement stored power
			if (this.storedPower > 0) {
				this.storedPower--;
			}
		} else {
			cookTime = 0;
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, UltimateFurnaceBlockEntity blockEntity) {
		if (world == null || pos == null || state == null || blockEntity == null) {
			return;
		}

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
				RecipeEntry<SmeltingRecipe> recipe = recipeEntry.get();
				if (recipe != null) {
					if (blockEntity.cookTime < blockEntity.cookTimeTotal) {
						blockEntity.cookTime++;
					} else {
						blockEntity.smeltItem(world.getRegistryManager(), recipe);
						if (blockEntity.level == 1) {
							blockEntity.burnTime = 200; // Set burn time for level 1
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
			if (!hasInput) {
				blockEntity.burnTime = 0;
			}
		}

		boolean dayMode = state.get(ModProperties.DAY_MODE); // Get current day mode
		boolean newDayMode = world.getTimeOfDay() % 24000 < 12000 && world.getLightLevel(LightType.SKY, pos.up()) > 0; // Recalculate day mode

		if (isBurning != blockEntity.isBurning() || dayMode != newDayMode) {
			world.setBlockState(pos, state
				.with(AbstractFurnaceBlock.LIT, blockEntity.isBurning())
				.with(ModProperties.DAY_MODE, newDayMode), Block.NOTIFY_ALL);
			stateChanged = true;
		}

		if (stateChanged) {
			blockEntity.markDirty();
		}

		if (blockEntity.smeltCount >= ITEMS_PER_LEVEL * blockEntity.getFurnaceLevel()) {
			blockEntity.levelUp();
		}

		if (!dayMode) {
			blockEntity.storedPower--;
		}
	}

	private Optional<RecipeEntry<SmeltingRecipe>> getFirstMatch(SingleStackRecipeInput input, World world) {
		if (world instanceof ServerWorld serverWorld) {
			RecipeManager recipeManager = serverWorld.getRecipeManager();
			if (recipeManager == null || input == null) return Optional.empty();

			// Updated method call for 1.21
			return recipeManager.getFirstMatch(RecipeType.SMELTING, input, world);
		}
		return Optional.empty();
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

	private int getCookTime(World world) {
		return switch (this.level) {
			case 1 -> 400; // 50% slower
			case 2 -> 300; // 66% of normal speed
			case 3 -> 200; // Normal speed
			case 4 -> 100; // Twice the speed
			case 5 -> 40;  // Five times the speed
			default -> 200; // Fallback to normal speed
		};
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		return new int[]{0, 2};
	}

	@Override
	protected Text getContainerName() {
		return Text.translatable("container.ultimate_furnace.ultimate_furnace");
	}

	@Override
	public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new UltimateFurnaceScreenHandler(
			ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER,
			RecipeType.SMELTING,
			syncId,
			playerInventory,
			this,
			this.propertyDelegate
		);
	}

	public void setStoredPower(int storedPower) {
		this.storedPower = storedPower;
	}

	public int getStoredPower() {
		return storedPower;
	}



	public static int getMaxStoredPower(int level) {
		return switch (level) {
			case 1 -> 0;
			case 2 -> 6000;
			case 3 -> 8000;
			case 4 -> 12000;
			case 5 -> 18000;
			default -> 0;
		};
	}
}
