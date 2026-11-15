package dev.phoenix.backrooms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Plain JSON config at config/backrooms.json. Missing keys fall back to the defaults below. */
public class BackroomsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static BackroomsConfig instance = new BackroomsConfig();

	/** Chance (0-1) that pushing into a wall for noclipHoldSeconds drops you into Level 0. */
	public double noclipChance = 0.10;
	/** How long you have to keep walking into the wall before the roll happens. */
	public double noclipHoldSeconds = 5.0;
	/** Set false to stop Fixtures spawning (existing ones are removed). */
	public boolean fixtureEnabled = true;
	/** 0-1 multiplier on natural Fixture spawn attempts. */
	public double fixtureSpawnRate = 0.25;
	/** Set false to stop Tube Moths spawning (existing ones are removed). */
	public boolean tubeMothEnabled = true;
	/** 0-1 multiplier on natural Tube Moth spawn attempts. */
	public double tubeMothSpawnRate = 1.0;

	public static BackroomsConfig get() {
		return instance;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("backrooms.json");
		try {
			if (Files.exists(path)) {
				BackroomsConfig loaded = GSON.fromJson(Files.readString(path), BackroomsConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			}
			instance.noclipChance = clamp(instance.noclipChance);
			instance.fixtureSpawnRate = clamp(instance.fixtureSpawnRate);
			instance.tubeMothSpawnRate = clamp(instance.tubeMothSpawnRate);
			instance.noclipHoldSeconds = Math.max(0.5, instance.noclipHoldSeconds);
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(instance));
		} catch (IOException | RuntimeException e) {
			Backrooms.LOGGER.warn("Could not read {}, using defaults: {}", path, e.toString());
			instance = new BackroomsConfig();
		}
	}

	private static double clamp(double v) {
		return Math.max(0.0, Math.min(1.0, v));
	}
}
