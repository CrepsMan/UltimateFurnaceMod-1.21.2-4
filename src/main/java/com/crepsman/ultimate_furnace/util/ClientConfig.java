package com.crepsman.ultimate_furnace.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Environment(EnvType.CLIENT)
public class ClientConfig {
	private static final String FILE_NAME = "ultimate_furnace_client.properties";
	private static boolean loaded = false;
	private static boolean useUnicodeBar = true;
	private static String barColor = "DARK_GREEN"; // Formatting color name

	public static void load() {
		if (loaded || FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path file = configDir.resolve(FILE_NAME);
		Properties props = new Properties();
		if (Files.notExists(file)) {
			useUnicodeBar = true;
			barColor = "DARK_GREEN";
			try (BufferedWriter writer = Files.newBufferedWriter(file)) {
				writer.write("useUnicodeBar=true\n");
				writer.write("barColor=DARK_GREEN\n");
			} catch (IOException ignored) {}
		} else {
			try {
				props.load(Files.newBufferedReader(file));
				useUnicodeBar = Boolean.parseBoolean(props.getProperty("useUnicodeBar", "true"));
				barColor = props.getProperty("barColor", "DARK_GREEN");
			} catch (IOException ignored) {}
		}
		loaded = true;
	}

	public static boolean useUnicodeBar() { return useUnicodeBar; }
	public static String barColor() { return barColor; }
}

