package dev.phoenix.backrooms.registry;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.world.LiminalChunkGenerator;
import dev.phoenix.backrooms.world.layout.Layout;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ModDimensions {
	public static final ResourceKey<Level> LEVEL_0 = key("level_0");
	public static final ResourceKey<Level> POOLROOMS = key("poolrooms");
	public static final ResourceKey<Level> CARPARK = key("carpark");
	private static final Set<ResourceKey<Level>> ALL = Set.of(LEVEL_0, POOLROOMS, CARPARK);

	private static ResourceKey<Level> key(String name) {
		return ResourceKey.create(Registries.DIMENSION, Backrooms.id(name));
	}

	public static boolean isLiminal(Level level) {
		return ALL.contains(level.dimension());
	}

	public static @Nullable Layout layoutOf(ServerLevel level) {
		return level.getChunkSource().getGenerator() instanceof LiminalChunkGenerator generator ? generator.layout() : null;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, Backrooms.id("liminal"), LiminalChunkGenerator.CODEC);
	}

	private ModDimensions() {
	}
}
