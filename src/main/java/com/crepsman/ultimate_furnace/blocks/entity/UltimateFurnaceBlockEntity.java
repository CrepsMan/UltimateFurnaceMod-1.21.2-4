package com.crepsman.ultimate_furnace.blocks.entity;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import com.crepsman.ultimate_furnace.blocks.UltimateFurnaceBlock;
import com.crepsman.ultimate_furnace.registry.ModBlockEntities;
import com.crepsman.ultimate_furnace.screen.UltimateFurnaceScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;

public class UltimateFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

	private final PropertyDelegate propertyDelegate;
	private int cookTime = 0;
	private int cookTimeTotal = 300; // Base cook time
	private int nightBurnTime = 4096; // Base burn time at night
	private int currentNightBurnTime = 0;
	private static final int[] TOP_SLOTS = new int[]{0};
	private static final int[] BOTTOM_SLOTS = new int[]{2};
	private int smeltCount = 0;

	// Upgrade thresholds
	private static final int[] UPGRADE_THRESHOLDS = {300, 650, 1200, 5000, 15000}; // Example thresholds
	private static final int[] UPGRADE_COOK_TIME = {250, 200, 150, 75, 20}; // Cook time for each upgrade
	private static final int[] UPGRADE_NIGHT_BURN_TIME = {6144, 7168, 8192, 10240, 13188 }; // Night burn time for each upgrade

	public UltimateFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ULTIMATE_FURNACE_BLOCK_ENTITY, pos, state, RecipeType.SMELTING);

		this.propertyDelegate = new PropertyDelegate() {
			@Override
			public int get(int index) {
				UltimateFurnaceMod.LOGGER.debug("PropertyDelegate get() called with index: " + index);
				switch (index) {
					case 0:
						return UltimateFurnaceBlockEntity.this.currentNightBurnTime;
					case 1:
						return UltimateFurnaceBlockEntity.this.nightBurnTime;
					case 2:
						return UltimateFurnaceBlockEntity.this.cookTime;
					case 3:
						return UltimateFurnaceBlockEntity.this.cookTimeTotal;
					case 4:
						return UltimateFurnaceBlockEntity.this.smeltCount;
					default:
						UltimateFurnaceMod.LOGGER.error("Invalid PropertyDelegate index accessed: " + index);
						return 0;
				}
			}


			@Override
			public void set(int index, int value) {
				switch (index) {
					case 0:
						UltimateFurnaceBlockEntity.this.currentNightBurnTime = value;
						break;
					case 1:
						UltimateFurnaceBlockEntity.this.nightBurnTime = value;
						break;
					case 2:
						UltimateFurnaceBlockEntity.this.cookTime = value;
						break;
					case 3:
						UltimateFurnaceBlockEntity.this.cookTimeTotal = value;
						break;
					case 4:
						UltimateFurnaceBlockEntity.this.smeltCount = value;
						break;
				}
			}

			@Override
			public int size() {
				return 5;
			}
		};
	}

	public static void tick(World world, BlockPos pos, BlockState state, UltimateFurnaceBlockEntity entity) {
		if (world.isClient) return;

		boolean dirty = false;
		boolean isDay = world.isDay();

		// Handle burn time logic for night
		if (isDay) {
			entity.currentNightBurnTime = entity.getNightBurnTime(); // Reset night burn time during the day
		} else {
			if (entity.currentNightBurnTime > 0) {
				int reductionRate = 2;  // Adjust reduction rate as needed
				if (world.getTime() % reductionRate == 0) {
					entity.currentNightBurnTime--;  // Decrease night burn time gradually
				}
			}
		}

		boolean isBurning = entity.isBurning();
		ItemStack input = entity.getInputSlot(); // Get input item stack
		ItemStack output = entity.inventory.get(2); // Output slot

		// Check if there is input and output space
		if (!input.isEmpty() && (output.isEmpty() || output.getCount() < 64)) {
			if (isBurning) {
				Optional<SmeltingRecipe> recipe = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(input), world).map(entry -> (SmeltingRecipe) entry.value());

				if (recipe.isPresent()) {
					entity.cookTime++;
					if (entity.cookTime >= entity.getCookTimeTotal()) {
						ItemStack result = recipe.get().craft(new SingleStackRecipeInput(entity.getInputSlot()), world.getRegistryManager());

						if (entity.smeltItem(result.copy())) {
							entity.incrementSmeltCount();
						}
						entity.cookTime = 0; // Reset cook time after successful smelting
						dirty = true; // Mark the block as dirty for state update
					}
				} else {
					entity.cookTime = 0; // Reset cook time if no valid recipe is found
				}
			} else {
				entity.cookTime = 0; // Reset cook time if not burning
			}
		} else {
			entity.cookTime = 0; // Reset cook time if no input or output full
		}

		// Update property delegate
		entity.propertyDelegate.set(2, entity.cookTime);
		entity.propertyDelegate.set(3, entity.getCookTimeTotal());

		// Update furnace state
		if (dirty || isBurning != state.get(UltimateFurnaceBlock.LIT)) {
			state = state.with(UltimateFurnaceBlock.LIT, isBurning);
			world.setBlockState(pos, state, 3);
			entity.markDirty();
		}
	}


	private int getCookTimeTotal() {
		int upgradeLevel = getUpgradeLevel();
		return upgradeLevel == 0 ? this.cookTimeTotal : UPGRADE_COOK_TIME[upgradeLevel - 1];
	}



	private int getNightBurnTime() {
		int index = getUpgradeLevel();

		// Ensure the index is within bounds
		if (index < 0 || index >= UPGRADE_NIGHT_BURN_TIME.length) {
			// Default to the base burn time or handle the error appropriately
			return UPGRADE_NIGHT_BURN_TIME[0];  // or some other default value
		}

		return UPGRADE_NIGHT_BURN_TIME[index];
	}


	private int getUpgradeLevel() {
		for (int i = UPGRADE_THRESHOLDS.length - 1; i >= 0; i--) {
			if (smeltCount >= UPGRADE_THRESHOLDS[i]) {
				return i + 1; // Returning i+1 to distinguish the base level as 0
			}
		}
		return 0; // Base level with no upgrades
	}


	public void incrementSmeltCount() {
		if (this.smeltCount < 15000) {
			this.smeltCount++;
		}
		markDirty();
	}



	@Override
	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putInt("SmeltCount", this.smeltCount);

	}

	@Override
	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt,registryLookup);
		this.smeltCount = nbt.getInt("SmeltCount");
	}


	@Override
	protected Text getContainerName() {
		return Text.translatable("container.ultimate_furnace.ultimate_furnace");
	}

	public ItemStack getInputSlot() {
		return this.inventory.get(0); // Assuming inventory[0] is the input slot
	}

	public boolean smeltItem(ItemStack output) {
		ItemStack resultStack = this.inventory.get(2); // Output slot

		if (resultStack.isEmpty()) {
			this.inventory.set(2, output.copy());
			this.inventory.get(0).decrement(1);
			return true;
		} else if (resultStack.isOf(output.getItem())) {
			int newCount = resultStack.getCount() + output.getCount();
			if (newCount <= 64) {
				resultStack.increment(output.getCount());
			} else {
				// Only add items needed to fill the output slot to 64
				int itemsToAdd = 64 - resultStack.getCount();
				resultStack.increment(itemsToAdd);
			}
			this.inventory.get(0).decrement(1); // Decrease input stack since the smelting is successful
			return true;
		}
		return false;
	}

	@Override
	public int[] getAvailableSlots(Direction side) {
		if (side == Direction.DOWN) {
			return BOTTOM_SLOTS;
		} else {
			return TOP_SLOTS ;
		}
	}

	//returns the current smelt count
	public int getSmeltCount(){
		return smeltCount;
	}

	//sets the smelt count
	public int setSmeltCount(int set){
		this.propertyDelegate.set(4, set);
		return smeltCount;
	}


	protected boolean isBurning() {
		if (this.world == null) {
			return false;
		}

		// Check if there is fuel progress
		return this.propertyDelegate.get(0) > 0;
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new UltimateFurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
	}
}
