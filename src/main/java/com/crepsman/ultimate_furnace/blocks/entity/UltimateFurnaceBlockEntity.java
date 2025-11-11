package com.crepsman.ultimate_furnace.blocks.entity;

import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.registry.ModScreenHandlers;
import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import com.crepsman.ultimate_furnace.util.FurnaceConfig;
import com.crepsman.ultimate_furnace.util.ModProperties;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBookType;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;

import java.util.Optional;


public class UltimateFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements SidedInventory {
	private static final int MAX_LEVEL = 5;

	private int smeltCount = 0;
	private int level = 1;
	private int burnTime;
	private int storedPower = 0;
	private int cookTime;
	private int cookTimeTotal;

	private float xpBuffer = 0.0f;

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
				return 6; // restore original size
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
		int max = FurnaceConfig.getMaxLevel();
		if (level < max) {
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
			float recipeXp = recipe.value().getExperience();
			if (recipeXp <= 0.0f) recipeXp = (float) FurnaceConfig.getXpPerItem();
			xpBuffer += recipeXp;
			if (this.world instanceof ServerWorld sw && xpBuffer >= 1.0f) {
				int whole = (int) xpBuffer;
				xpBuffer -= whole;
				if (whole > 0) ExperienceOrbEntity.spawn(sw, Vec3d.ofCenter(this.pos), whole);
			}
			this.cookTime = 0;
			this.cookTimeTotal = getCookTime(this.world);
			if (this.storedPower > 0) {
				this.storedPower--;
			}
		} else {
			cookTime = 0;
		}
	}

	public boolean isBurning() {
		// Show as burning if:
		// 1. Has burn time, OR
		// 2. Is actively cooking, OR
		// 3. Has input and either in dayMode OR has stored power
		boolean hasInput = !this.inventory.get(0).isEmpty();
		boolean dayMode = this.world != null &&
			this.world.getBlockState(this.pos).get(ModProperties.DAY_MODE);
		boolean canSmelt = !hasInput ? false :
			(this.inventory.get(2).isEmpty() ||
				this.inventory.get(2).getCount() < this.inventory.get(2).getMaxCount());

		return this.burnTime > 0 || this.cookTime > 0 || (hasInput && canSmelt && (dayMode || this.storedPower > 0));
	}

	private void updateDaytimeBurning(World world, BlockPos pos) {
		if (world == null) return;

		boolean isDay = world.getTimeOfDay() % 24000 < 12000;
		boolean hasDirectSkylight = world.getLightLevel(LightType.SKY, pos.up()) > 0;
		boolean newDayMode = isDay && hasDirectSkylight;

		BlockState currentState = world.getBlockState(pos);
		boolean oldDayMode = currentState.get(ModProperties.DAY_MODE);

		// Store power during day for furnace level > 1
		if (newDayMode && this.level > 1) {
			int powerGainRate = FurnaceConfig.getPowerGainRateForLevel(this.level);
			int maxPower = FurnaceConfig.getMaxStoredPowerForLevel(this.level);
			if (this.storedPower < maxPower) {
				this.storedPower = Math.min(this.storedPower + powerGainRate, maxPower);
				markDirty();
			}
		}

		// Specifically handle day-to-night transition with immediate fire update
		if (oldDayMode && !newDayMode) {
			// When switching to night, we need to immediately check if we should keep burning
			boolean canBurn = false;

			// Only burn at night if we have stored power and valid input
			if (this.storedPower > 0) {
				ItemStack inputStack = this.getStack(0);
				ItemStack outputStack = this.getStack(2);
				boolean hasInput = !inputStack.isEmpty();
				boolean canSmelt = !hasInput ? false :
					(outputStack.isEmpty() || outputStack.getCount() < outputStack.getMaxCount());

				// Only keep burning if we have power and something to smelt
				canBurn = hasInput && canSmelt;
			}

			// Update both day mode and lit state immediately
			world.setBlockState(pos, currentState
					.with(ModProperties.DAY_MODE, false)
					.with(AbstractFurnaceBlock.LIT, canBurn),
				Block.NOTIFY_ALL);
		}
		// Handle other transitions
		else if (currentState.get(ModProperties.DAY_MODE) != newDayMode) {
			world.setBlockState(pos, currentState.with(ModProperties.DAY_MODE, newDayMode), Block.NOTIFY_ALL);
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, UltimateFurnaceBlockEntity blockEntity) {
		if (world == null || pos == null || state == null || blockEntity == null) {
			return;
		}

		blockEntity.updateDaytimeBurning(world, pos);

		boolean wasBurning = blockEntity.isBurning();
		boolean stateChanged = false;

		if (blockEntity.burnTime > 0) {
			--blockEntity.burnTime;
		}

		boolean dayMode = state.get(ModProperties.DAY_MODE);

		ItemStack inputStack = blockEntity.getStack(0);
		ItemStack outputStack = blockEntity.getStack(2);
		boolean hasInput = !inputStack.isEmpty();
		boolean canSmelt = outputStack.getCount() < outputStack.getMaxCount();

		// Update lit state whenever inventory changes to show burning animation immediately
		if (hasInput && canSmelt && (dayMode || blockEntity.storedPower > 0) && !state.get(AbstractFurnaceBlock.LIT)) {
			world.setBlockState(pos, state.with(AbstractFurnaceBlock.LIT, true), Block.NOTIFY_ALL);
			stateChanged = true;
		}

		boolean canUseEnergy = dayMode || blockEntity.storedPower > 0;

		if (hasInput && canSmelt && canUseEnergy) {
			Optional<RecipeEntry<SmeltingRecipe>> recipeEntry = blockEntity.getFirstMatch(new SingleStackRecipeInput(inputStack), world);
			if (recipeEntry.isPresent()) {
				RecipeEntry<SmeltingRecipe> recipe = recipeEntry.get();
				if (recipe != null) {
					// Set cookTimeTotal when we start cooking
					if (blockEntity.cookTime == 0) {
						blockEntity.cookTimeTotal = blockEntity.getCookTime(world);
					}

					if (blockEntity.cookTime < blockEntity.cookTimeTotal) {
						blockEntity.cookTime++;

						// Only consume stored power at night
						if (!dayMode && blockEntity.storedPower > 0) {
							blockEntity.storedPower--;
						}
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
		}

		boolean newDayMode = world.getTimeOfDay() % 24000 < 12000 && world.getLightLevel(LightType.SKY, pos.up()) > 0;

		// Update the block state if burning status changed or day/night changed
		if (wasBurning != blockEntity.isBurning() || dayMode != newDayMode) {
			world.setBlockState(pos, state
				.with(AbstractFurnaceBlock.LIT, blockEntity.isBurning())
				.with(ModProperties.DAY_MODE, newDayMode), Block.NOTIFY_ALL);
			stateChanged = true;
		}

		if (stateChanged) {
			// drop xp orbs based on buffer periodically
			if (!world.isClient && blockEntity.xpBuffer >= 1.0f && world instanceof ServerWorld serverWorld) {
				int whole = (int) blockEntity.xpBuffer;
				blockEntity.xpBuffer -= whole;
				if (whole > 0) ExperienceOrbEntity.spawn(serverWorld, Vec3d.ofCenter(pos), whole);
			}
			blockEntity.markDirty();
		}

		int required = FurnaceConfig.getItemsPerLevel() * blockEntity.getFurnaceLevel();
		if (blockEntity.smeltCount >= required) {
			blockEntity.levelUp();
		}
	}
	private Optional<RecipeEntry<SmeltingRecipe>> getFirstMatch(SingleStackRecipeInput input, World world) {
		if (world instanceof ServerWorld serverWorld) {
			ServerRecipeManager recipeManager = serverWorld.getRecipeManager();
			if (recipeManager == null || input == null) return Optional.empty();
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
		NbtElement e;
		e = nbt.get("SmeltCount"); this.smeltCount = e instanceof NbtInt ei ? ei.intValue() : 0;
		e = nbt.get("Level"); this.level = e instanceof NbtInt li ? li.intValue() : 1;
		e = nbt.get("BurnTime"); this.burnTime = e instanceof NbtInt bi ? bi.intValue() : 0;
		e = nbt.get("StoredPower"); this.storedPower = e instanceof NbtInt pi ? pi.intValue() : 0;
	}

	private int getCookTime(World world) {
		return FurnaceConfig.getCookTimeForLevel(this.level);
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
		if (world instanceof ServerWorld serverWorld) {
			RecipeManager recipeManager = serverWorld.getRecipeManager();
			RegistryKey<RecipePropertySet> key = RecipePropertySet.FURNACE_INPUT;

			if (key == null) {
				return null; // Return early to avoid processing with a null key
			}

			RecipePropertySet propertySet = recipeManager.getPropertySet(key);
			if (propertySet == null) {
				return null; // Return early to prevent further null reference issues
			}

			return new UltimateFurnaceScreenHandler(
				ModScreenHandlers.ULTIMATE_FURNACE_SCREEN_HANDLER,
				RecipeType.SMELTING,
				key,
				RecipeBookType.FURNACE,
				syncId,
				playerInventory,
				this,
				this.propertyDelegate
			);
		} else {
			return null; // Return early to avoid further processing
		}
	}
	public void setStoredPower(int storedPower) {
		this.storedPower = storedPower;
	}

	public int getStoredPower() {
		return storedPower;
	}



	public static int getMaxStoredPower(int level) {
		return FurnaceConfig.getMaxStoredPowerForLevel(level);
	}
}
