package dev.phoenix.backrooms.world;

import com.mojang.serialization.MapCodec;
import dev.phoenix.backrooms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Entrance 4 (hidden Overworld structure): a white-tiled stairwell that has no business being in
 * a field. A lit EXIT sign hangs over the opening (the tell); seven steps lead down to a short
 * landing under a pool light, and the doorway at the end is a threshold into the Poolrooms.
 */
public record WrongStairwellFeature() implements Feature {
	public static final MapCodec<WrongStairwellFeature> CODEC = MapCodec.unit(WrongStairwellFeature::new);
	private static final int STEPS = 7;
	private static final int LANDING = 3;

	@Override
	public MapCodec<WrongStairwellFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		BlockState ground = level.getBlockState(origin.below());
		if (origin.getY() - STEPS - 3 <= level.getMinY() || !ground.getFluidState().isEmpty() || !ground.isSolidRender()
			|| !level.getBlockState(origin).getFluidState().isEmpty()) {
			return false;
		}
		BlockState tile = ModBlocks.POOL_TILE.defaultBlockState();
		int length = STEPS + LANDING;
		for (int i = 0; i <= length + 1; i++) {
			int feetY = origin.getY() - 1 - Math.min(i, STEPS - 1);
			int z = origin.getZ() - i;
			boolean endWall = i == length + 1;
			for (int dx = -1; dx <= 1; dx++) {
				for (int y = feetY - 1; y <= feetY + 3; y++) {
					boolean shell = dx != 0 || y == feetY - 1 || y == feetY + 3 || endWall;
					level.setBlock(new BlockPos(origin.getX() + dx, y, z), shell ? tile : Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		// the tell, the light, and the door that should not lead anywhere
		level.setBlock(origin.above(2), ModBlocks.EXIT_SIGN.defaultBlockState(), 2);
		int bottomY = origin.getY() - STEPS;
		level.setBlock(new BlockPos(origin.getX(), bottomY + 3, origin.getZ() - STEPS - 1), ModBlocks.POOL_LIGHT.defaultBlockState(), 2);
		BlockPos door = new BlockPos(origin.getX(), bottomY, origin.getZ() - length);
		level.setBlock(door, ModBlocks.THRESHOLD.defaultBlockState(), 2);
		level.setBlock(door.above(), ModBlocks.THRESHOLD.defaultBlockState(), 2);
		level.setBlock(door.above(2), ModBlocks.POOL_TILE_BLUE.defaultBlockState(), 2);
		return true;
	}
}
