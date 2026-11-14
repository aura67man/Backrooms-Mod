package dev.phoenix.backrooms.world.layout;

import dev.phoenix.backrooms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Flat carpet under a tile ceiling. Only used to prove the dimension plumbing works. */
public class BlankLayout extends Layout {
	@Override
	public BlockState blockAt(int x, int y, int z) {
		if (y == FLOOR_Y - 1) {
			return BEDROCK;
		}
		if (y == FLOOR_Y) {
			return ModBlocks.DAMP_CARPET.defaultBlockState();
		}
		if (y == FLOOR_Y + 5) {
			return (Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4 ? ModBlocks.FLUORESCENT_LIGHT : ModBlocks.CEILING_TILE).defaultBlockState();
		}
		return AIR;
	}

	@Override
	public int minY() {
		return FLOOR_Y - 1;
	}

	@Override
	public int maxY() {
		return FLOOR_Y + 5;
	}

	@Override
	public BlockPos exitNear(int x, int z) {
		return new BlockPos(x, FLOOR_Y + 1, z);
	}
}
