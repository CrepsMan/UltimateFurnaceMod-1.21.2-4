package com.crepsman.ultimate_furnace.util;

import com.crepsman.ultimate_furnace.UltimateFurnaceMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * FurnaceConfig now supports user-friendly array style configuration.
 * New format (ultimate_furnace_config.txt):
 *   max_level=5
 *   items_per_level=3000               # Base smelts required per level (actual threshold = level * items_per_level)
 *   cook_times=400,300,200,100,40      # Ticks per smelt at each level (20 ticks = 1 second). If level exceeds list length last value is reused.
 *   max_stored_power=0,6000,8000,12000,18000   # Capacity of stored daytime power per level.
 *   power_gain_rate=0,1,2,4,5          # Stored power gained per tick in daylight with sky access per level.
 *   info_permission_level=0            # Required permission level to use /ultimatefurnace info (0..4)
 *
 * Backward compatibility: If arrays are missing but old keys like cook_time_level_1 exist,
 * they are gathered into arrays automatically. This lets existing installs upgrade seamlessly.
 */
public class FurnaceConfig {
	private static final String FILE_NAME = "ultimate_furnace_config.txt";
	private static boolean loaded = false;

	// Scalar defaults
	private static final int DEFAULT_MAX_LEVEL = 5;
	private static final int DEFAULT_ITEMS_PER_LEVEL = 3000;
	private static final int DEFAULT_INFO_PERMISSION_LEVEL = 0;

	// Array defaults
	private static final int[] DEFAULT_COOK_TIMES = {400, 300, 200, 100, 40};
	private static final int[] DEFAULT_MAX_STORED_POWER = {0, 6000, 8000, 12000, 18000};
	private static final int[] DEFAULT_POWER_GAIN_RATE = {0, 1, 2, 4, 5};

	// Current values
	private static int maxLevel = DEFAULT_MAX_LEVEL;
	private static int itemsPerLevel = DEFAULT_ITEMS_PER_LEVEL;
	private static int infoPermissionLevel = DEFAULT_INFO_PERMISSION_LEVEL;
	private static int[] cookTimes = Arrays.copyOf(DEFAULT_COOK_TIMES, DEFAULT_COOK_TIMES.length);
	private static int[] maxStoredPower = Arrays.copyOf(DEFAULT_MAX_STORED_POWER, DEFAULT_MAX_STORED_POWER.length);
	private static int[] powerGainRate = Arrays.copyOf(DEFAULT_POWER_GAIN_RATE, DEFAULT_POWER_GAIN_RATE.length);

	public static synchronized void reload() {
		UltimateFurnaceMod.LOGGER.info("Reloading Ultimate Furnace config (array format)...");
		loaded = false;
		load();
	}

	public static void load() {
		if (loaded) return;
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path file = configDir.resolve(FILE_NAME);

		// Reset to defaults before reading file
		maxLevel = DEFAULT_MAX_LEVEL;
		itemsPerLevel = DEFAULT_ITEMS_PER_LEVEL;
		infoPermissionLevel = DEFAULT_INFO_PERMISSION_LEVEL;
		cookTimes = Arrays.copyOf(DEFAULT_COOK_TIMES, DEFAULT_COOK_TIMES.length);
		maxStoredPower = Arrays.copyOf(DEFAULT_MAX_STORED_POWER, DEFAULT_MAX_STORED_POWER.length);
		powerGainRate = Arrays.copyOf(DEFAULT_POWER_GAIN_RATE, DEFAULT_POWER_GAIN_RATE.length);

		if (Files.notExists(file)) {
			try { createDefaultConfig(file); } catch (IOException e) {
				UltimateFurnaceMod.LOGGER.error("Failed to create default config file", e);
			}
		} else {
			// Temporary collectors for backward compatible per-level keys
			Map<Integer, Integer> legacyCookTimes = new TreeMap<>();
			Map<Integer, Integer> legacyMaxStored = new TreeMap<>();
			Map<Integer, Integer> legacyGainRate = new TreeMap<>();
			try (Stream<String> lines = Files.lines(file)) {
				lines.forEach(rawLine -> {
					String line = rawLine.trim();
					if (line.isEmpty() || line.startsWith("#")) return;
					int idx = line.indexOf('=');
					if (idx <= 0) return;
					String key = line.substring(0, idx).trim();
					String value = line.substring(idx + 1).trim();
					try {
						switch (key) {
							case "max_level" -> maxLevel = parsePositive(value, DEFAULT_MAX_LEVEL);
							case "items_per_level" -> itemsPerLevel = parsePositive(value, DEFAULT_ITEMS_PER_LEVEL);
							case "info_permission_level" -> infoPermissionLevel = clamp(parsePositive(value, DEFAULT_INFO_PERMISSION_LEVEL), 0, 4);
							case "cook_times" -> cookTimes = parseIntArray(value, DEFAULT_COOK_TIMES);
							case "max_stored_power" -> maxStoredPower = parseIntArray(value, DEFAULT_MAX_STORED_POWER);
							case "power_gain_rate" -> powerGainRate = parseIntArray(value, DEFAULT_POWER_GAIN_RATE);
							default -> {
								// Backward compatibility for old per-level keys
								if (key.startsWith("cook_time_level_")) {
									int lvl = parsePositive(key.substring("cook_time_level_".length()), -1);
									if (lvl > 0) legacyCookTimes.put(lvl, parsePositive(value, DEFAULT_COOK_TIMES[0]));
								} else if (key.startsWith("max_stored_power_level_")) {
									int lvl = parsePositive(key.substring("max_stored_power_level_".length()), -1);
									if (lvl > 0) legacyMaxStored.put(lvl, parsePositive(value, DEFAULT_MAX_STORED_POWER[0]));
								} else if (key.startsWith("power_gain_rate_level_")) {
									int lvl = parsePositive(key.substring("power_gain_rate_level_".length()), -1);
									if (lvl > 0) legacyGainRate.put(lvl, parsePositive(value, DEFAULT_POWER_GAIN_RATE[0]));
								}
							}
						}
					} catch (Exception ignored) {}
				});
			} catch (IOException e) {
				UltimateFurnaceMod.LOGGER.error("Failed reading config file", e);
			}
			// If arrays missing (user migrated late) populate from legacy maps
			if (!fileContainsArrayKey(file, "cook_times") && !legacyCookTimes.isEmpty()) {
				cookTimes = mapToDenseArray(legacyCookTimes, DEFAULT_COOK_TIMES[DEFAULT_COOK_TIMES.length - 1]);
			}
			if (!fileContainsArrayKey(file, "max_stored_power") && !legacyMaxStored.isEmpty()) {
				maxStoredPower = mapToDenseArray(legacyMaxStored, DEFAULT_MAX_STORED_POWER[DEFAULT_MAX_STORED_POWER.length - 1]);
			}
			if (!fileContainsArrayKey(file, "power_gain_rate") && !legacyGainRate.isEmpty()) {
				powerGainRate = mapToDenseArray(legacyGainRate, DEFAULT_POWER_GAIN_RATE[DEFAULT_POWER_GAIN_RATE.length - 1]);
			}
		}
		loaded = true;
		UltimateFurnaceMod.LOGGER.info("Ultimate Furnace config loaded: max_level=" + maxLevel + ", arrays: cook_times=" + Arrays.toString(cookTimes));
	}

	private static boolean fileContainsArrayKey(Path file, String key) {
		try (Stream<String> lines = Files.lines(file)) {
			return lines.anyMatch(l -> l.trim().startsWith(key + "="));
		} catch (IOException e) {
			return false;
		}
	}

	private static int parsePositive(String s, int def) {
		try {
			int v = Integer.parseInt(s.trim());
			return v > 0 ? v : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int clamp(int v, int min, int max) { return Math.min(max, Math.max(min, v)); }

	private static int[] parseIntArray(String value, int[] def) {
		String[] parts = value.split(",");
		List<Integer> out = new ArrayList<>();
		for (String p : parts) {
			p = p.trim();
			if (p.isEmpty()) continue;
			try { out.add(Integer.parseInt(p)); } catch (NumberFormatException ignored) {}
		}
		if (out.isEmpty()) return Arrays.copyOf(def, def.length);
		int[] arr = new int[out.size()];
		for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);
		return arr;
	}

	private static int[] mapToDenseArray(Map<Integer, Integer> map, int fallback) {
		int maxKey = map.keySet().stream().max(Integer::compareTo).orElse(1);
		int[] arr = new int[maxKey];
		for (int i = 1; i <= maxKey; i++) {
			arr[i - 1] = map.getOrDefault(i, fallback);
		}
		return arr;
	}

	private static void createDefaultConfig(Path file) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			writer.write("# Ultimate Furnace Configuration (Array Format)\n");
			writer.write("\n");
			writer.write("# max_level: Highest attainable furnace level. Must be >= 1.\n");
			writer.write("max_level=" + DEFAULT_MAX_LEVEL + "\n\n");
			writer.write("# items_per_level: Base smelts needed per level.\n");
			writer.write("#   Threshold to reach next level = current_level * items_per_level\n");
			writer.write("items_per_level=" + DEFAULT_ITEMS_PER_LEVEL + "\n\n");
			writer.write("# cook_times:\n");
			writer.write("#   20 ticks = 1 second. Lower = faster smelting. Last value reused if level exceeds list length.*\n");
			writer.write("cook_times=" + join(DEFAULT_COOK_TIMES) + "\n\n");
			writer.write("# max_stored_power:\n");
			writer.write("#   Higher values allow more daytime charge to accumulate and be spent at night.*\n");
			writer.write("max_stored_power=" + join(DEFAULT_MAX_STORED_POWER) + "\n\n");
			writer.write("# power_gain_rate:\n");
			writer.write("#   Requires sky access. 20 ticks = 1 second. Last value reused if level exceeds list length.*\n");
			writer.write("power_gain_rate=" + join(DEFAULT_POWER_GAIN_RATE) + "\n\n");
			writer.write("# info_permission_level: Minecraft permission level required for /ultimatefurnace info (0..4).\n");
			writer.write("info_permission_level=" + DEFAULT_INFO_PERMISSION_LEVEL + "\n\n");
			writer.write("# NOTES:\n");
			writer.write("# - To extend levels beyond max_level, raise max_level AND append additional values to arrays.\n");
			writer.write("# - If arrays are shorter than max_level, the last array value is reused for higher levels.\n");
			writer.write("# - Use /ultimatefurnace reload in-game to apply changes without restarting.\n");
		}
	}

	private static String join(int[] arr) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) sb.append(',');
			sb.append(arr[i]);
		}
		return sb.toString();
	}

	// Public API getters -----------------------------------------------------
	public static int getMaxLevel() { return maxLevel; }
	public static int getItemsPerLevel() { return itemsPerLevel; }
	public static int getInfoPermissionLevel() { return infoPermissionLevel; }

	public static int getCookTimeForLevel(int level) {
		if (level <= 0) return cookTimes[0];
		return cookTimes[level - 1 < cookTimes.length ? level - 1 : cookTimes.length - 1];
	}

	public static int getMaxStoredPowerForLevel(int level) {
		if (level <= 0) return maxStoredPower[0];
		return maxStoredPower[level - 1 < maxStoredPower.length ? level - 1 : maxStoredPower.length - 1];
	}

	public static int getPowerGainRateForLevel(int level) {
		if (level <= 0) return powerGainRate[0];
		return powerGainRate[level - 1 < powerGainRate.length ? level - 1 : powerGainRate.length - 1];
	}
}
