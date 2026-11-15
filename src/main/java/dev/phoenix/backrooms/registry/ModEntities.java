package dev.phoenix.backrooms.registry;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.entity.FixtureEntity;
import dev.phoenix.backrooms.entity.TubeMothEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final EntityType<FixtureEntity> FIXTURE = register("fixture",
		EntityType.Builder.of(FixtureEntity::new, MobCategory.MONSTER).sized(0.6F, 2.75F).eyeHeight(2.45F).clientTrackingRange(10).notInPeaceful());
	public static final EntityType<TubeMothEntity> TUBE_MOTH = register("tube_moth",
		EntityType.Builder.of(TubeMothEntity::new, MobCategory.AMBIENT).sized(0.4F, 0.3F).eyeHeight(0.15F).clientTrackingRange(8));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Backrooms.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(FIXTURE, FixtureEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TUBE_MOTH, TubeMothEntity.createAttributes());
		// Which biomes they spawn in is data (worldgen/biome/*.json); these rules add the config toggles and rates.
		SpawnPlacements.register(FIXTURE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FixtureEntity::checkSpawnRules);
		SpawnPlacements.register(TUBE_MOTH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TubeMothEntity::checkSpawnRules);
	}

	private ModEntities() {
	}
}
