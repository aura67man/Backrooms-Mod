package dev.phoenix.backrooms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.phoenix.backrooms.world.layout.Layout;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jspecify.annotations.Nullable;

/** Fills chunks straight from a {@link Layout}. One generator class, one layout per level. */
public class LiminalChunkGenerator extends ChunkGenerator {
	public static final MapCodec<LiminalChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
		Codec.STRING.fieldOf("style").forGetter(g -> g.style)
	).apply(i, LiminalChunkGenerator::new));

	private final String style;
	private final Layout layout;

	public LiminalChunkGenerator(BiomeSource biomeSource, String style) {
		super(biomeSource);
		this.style = style;
		this.layout = Layout.byStyle(style);
	}

	public Layout layout() {
		return this.layout;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
		BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		int minY = Math.max(this.layout.minY(), chunk.getMinY());
		int maxY = Math.min(this.layout.maxY(), chunk.getMaxY());
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= maxY; y++) {
					BlockState state = this.layout.blockAt(baseX + x, y, baseZ + z);
					if (!state.isAir()) {
						chunk.setBlockState(pos.set(baseX + x, y, baseZ + z), state);
						oceanFloor.update(x, y, z, state);
						worldSurface.update(x, y, z, state);
					}
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		for (int y = this.layout.maxY(); y >= this.layout.minY(); y--) {
			if (type.isOpaque().test(this.layout.blockAt(x, y, z))) {
				return y + 1;
			}
		}
		return heightAccessor.getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		BlockState[] states = new BlockState[heightAccessor.getHeight()];
		for (int i = 0; i < states.length; i++) {
			states[i] = this.layout.blockAt(x, heightAccessor.getMinY() + i, z);
		}
		return new NoiseColumn(heightAccessor.getMinY(), states);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("Liminal layout: " + this.style);
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
	}

	@Override
	public int getMinY() {
		return 0;
	}

	@Override
	public int getGenDepth() {
		return 64;
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}
}
