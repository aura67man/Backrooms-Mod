package dev.phoenix.backrooms.registry;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.block.FluorescentLightBlock;
import dev.phoenix.backrooms.block.ThresholdBlock;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	// Level 0
	public static final Block DAMP_CARPET = register("damp_carpet", Block::new, props(MapColor.TERRACOTTA_YELLOW, 0.8F).sound(SoundType.WOOL));
	public static final Block WALLPAPER = register("wallpaper", Block::new, props(MapColor.COLOR_YELLOW, 1.5F));
	public static final Block WALLPAPER_BASEBOARD = register("wallpaper_baseboard", Block::new, props(MapColor.COLOR_YELLOW, 1.5F));
	public static final Block CEILING_TILE = register("ceiling_tile", Block::new, props(MapColor.QUARTZ, 1.0F));
	public static final Block FLUORESCENT_LIGHT = register("fluorescent_light", FluorescentLightBlock::new,
		props(MapColor.QUARTZ, 0.6F).sound(SoundType.GLASS).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
	// Poolrooms
	public static final Block POOL_TILE = register("pool_tile", Block::new, props(MapColor.SNOW, 1.5F));
	public static final Block POOL_TILE_BLUE = register("pool_tile_blue", Block::new, props(MapColor.COLOR_LIGHT_BLUE, 1.5F));
	public static final Block POOL_LIGHT = register("pool_light", Block::new, props(MapColor.SNOW, 0.6F).sound(SoundType.GLASS).lightLevel(state -> 15));
	// Car park
	public static final Block GARAGE_CONCRETE = register("garage_concrete", Block::new, props(MapColor.STONE, 2.0F));
	public static final Block GARAGE_FLOOR = register("garage_floor", Block::new, props(MapColor.COLOR_GRAY, 2.0F));
	public static final Block GARAGE_LINE = register("garage_line", RotatedPillarBlock::new, props(MapColor.COLOR_GRAY, 2.0F));
	public static final Block HAZARD_PILLAR = register("hazard_pillar", Block::new, props(MapColor.COLOR_YELLOW, 2.0F));
	public static final Block SODIUM_LAMP = register("sodium_lamp", Block::new, props(MapColor.COLOR_ORANGE, 0.6F).sound(SoundType.GLASS).lightLevel(state -> 13));
	// Shared: the way in / the way out
	public static final Block THRESHOLD = register("threshold", ThresholdBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollision().strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE));
	public static final Block EXIT_SIGN = register("exit_sign", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(-1.0F, 3600000.0F).noLootTable().lightLevel(state -> 9));

	private static BlockBehaviour.Properties props(MapColor color, float strength) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(strength, 6.0F).requiresCorrectToolForDrops();
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Backrooms.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
