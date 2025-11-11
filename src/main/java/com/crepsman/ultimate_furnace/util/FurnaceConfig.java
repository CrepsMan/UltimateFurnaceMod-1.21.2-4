package com.crepsman.ultimate_furnace.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class FurnaceConfig {
	private static Path CONFIG_PATH;

	private static int maxLevel = 5;
	private static int itemsPerLevel = 3000;
	private static int[] cookTimes = new int[]{400,300,200,100,40};
	private static int[] maxStoredPower = new int[]{0,6000,8000,12000,18000};
	private static int[] powerGainRate = new int[]{0,1,2,4,5};
	private static double xpPerItem = 0.1D;

	private FurnaceConfig() { }

	public static void load() {
		try {
			CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimate_furnace_config.txt");
			if (!Files.exists(CONFIG_PATH)) {
				saveDefaults();
			}
			Map<String, String> map = readKeyValues(CONFIG_PATH);
			maxLevel = parseInt(map.get("max_level"), maxLevel, 1, 128);
			itemsPerLevel = parseInt(map.get("items_per_level"), itemsPerLevel, 1, Integer.MAX_VALUE);
			xpPerItem = parseDouble(map.get("xp_per_item"), xpPerItem, 0.0, 100.0);
			cookTimes = parseIntArray(map.get("cook_times"), cookTimes, maxLevel);
			maxStoredPower = parseIntArray(map.get("max_stored_power"), maxStoredPower, maxLevel);
			powerGainRate = parseIntArray(map.get("power_gain_rate"), powerGainRate, maxLevel);
		} catch (Exception ignored) { }
	}

	private static Map<String, String> readKeyValues(Path path) throws IOException {
		Map<String, String> out = new HashMap<>();
		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				int idx = line.indexOf('=');
				if (idx < 0) continue;
				String k = line.substring(0, idx).trim();
				String v = line.substring(idx + 1).trim();
				out.put(k, v);
			}
		}
		return out;
	}

	private static void saveDefaults() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("# Ultimate Furnace Config (TXT)\n");
		sb.append("# key=value, arrays are comma-separated, one-based per-level arrays up to max_level\n\n");
		sb.append("max_level=").append(maxLevel).append('\n');
		sb.append("items_per_level=").append(itemsPerLevel).append('\n');
		sb.append("xp_per_item=").append(xpPerItem).append('\n');
		sb.append("cook_times=").append(join(cookTimes)).append('\n');
		sb.append("max_stored_power=").append(join(maxStoredPower)).append('\n');
		sb.append("power_gain_rate=").append(join(powerGainRate)).append('\n');
		try (BufferedWriter bw = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
			bw.write(sb.toString());
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

	private static int parseInt(String s, int def, int min, int max) {
		try {
			int v = Integer.parseInt(s);
			return Math.max(min, Math.min(max, v));
		} catch (Exception e) { return def; }
	}

	private static double parseDouble(String s, double def, double min, double max) {
		try {
			double v = Double.parseDouble(s);
			return Math.max(min, Math.min(max, v));
		} catch (Exception e) { return def; }
	}

	private static int[] parseIntArray(String s, int[] fallback, int expected) {
		if (s == null || s.isEmpty()) return Arrays.copyOf(fallback, expected);
		String[] parts = s.split(",");
		int[] out = new int[Math.max(expected, parts.length)];
		for (int i = 0; i < out.length; i++) out[i] = i < fallback.length ? fallback[i] : (i > 0 ? out[i-1] : fallback[0]);
		for (int i = 0; i < Math.min(parts.length, expected); i++) {
			try { out[i] = Integer.parseInt(parts[i].trim()); } catch (Exception ignored) { }
		}
		return out;
	}

	public static int getMaxLevel() { return maxLevel; }
	public static int getItemsPerLevel() { return itemsPerLevel; }
	public static int getCookTimeForLevel(int level) { return cookTimes[Math.min(Math.max(level-1,0), cookTimes.length-1)]; }
	public static int getMaxStoredPowerForLevel(int level) { return maxStoredPower[Math.min(Math.max(level-1,0), maxStoredPower.length-1)]; }
	public static int getPowerGainRateForLevel(int level) { return powerGainRate[Math.min(Math.max(level-1,0), powerGainRate.length-1)]; }
	public static double getXpPerItem() { return xpPerItem; }

	public static void reload() { load(); }
}
