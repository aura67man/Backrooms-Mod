package dev.phoenix.backrooms.world.layout;

import dev.phoenix.backrooms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Poolrooms: white-tiled halls on a 16-block grid, joined by wide arches. The floor is a
 * patchwork of 8x8 slabs that are either dry walkway, ankle-deep water or a three-deep pool,
 * and water runs straight under the arches. Some halls have a square skylight open to a blank sky.
 */
public class PoolroomsLayout extends Layout {
	private static final int CELL = 16;
	private static final int PATCH = 8;
	/** One exit door per REGION x REGION cells. */
	private static final int REGION = 4;
	private static final int CEILING_Y = FLOOR_Y + 8;
	private static final int ARCH_TOP_Y = FLOOR_Y + 4;
	private static final int DEEP = 3;

	private final BlockState tile = ModBlocks.POOL_TILE.defaultBlockState();
	private final BlockState blueTile = ModBlocks.POOL_TILE_BLUE.defaultBlockState();
	private final BlockState light = ModBlocks.POOL_LIGHT.defaultBlockState();
	private final BlockState water = Blocks.WATER.defaultBlockState();
	private final BlockState threshold = ModBlocks.THRESHOLD.defaultBlockState();
	private final BlockState exitSign = ModBlocks.EXIT_SIGN.defaultBlockState();

	@Override
	public int minY() {
		return FLOOR_Y - 5;
	}

	@Override
	public int maxY() {
		return CEILING_Y;
	}

	@Override
	public BlockState blockAt(int x, int y, int z) {
		if (y < this.minY() || y > this.maxY()) {
			return AIR;
		}
		if (y == this.minY()) {
			return BEDROCK;
		}
		boolean solidWall = this.isWall(x, z) && !isArch(x, z);
		if (y > FLOOR_Y) {
			if (y == CEILING_Y) {
				return this.ceilingAt(x, z);
			}
			BlockState door = this.exitFrameAt(x, y, z);
			if (door != null) {
				return door;
			}
			if (solidWall || (this.isWall(x, z) && y > ARCH_TOP_Y)) {
				return y == ARCH_TOP_Y + 1 ? this.blueTile : this.tile;
			}
			return AIR;
		}
		int depth = solidWall ? 0 : this.poolDepth(x, z);
		int floorTop = FLOOR_Y - depth;
		if (y > floorTop) {
			return this.water;
		}
		if (y == floorTop && depth == DEEP) {
			return Math.floorMod(x, PATCH) == 3 && Math.floorMod(z, PATCH) == 3 ? this.light : this.blueTile;
		}
		return this.tile;
	}

	/** 0 = dry walkway, 1 = shallow water, 3 = deep pool. */
	public int poolDepth(int x, int z) {
		BlockPos door = this.exitNear(x, z);
		if (Math.floorDiv(x, PATCH) == Math.floorDiv(door.getX(), PATCH) && Math.floorDiv(z, PATCH) == Math.floorDiv(door.getZ(), PATCH)) {
			return 0; // the exit always stands on dry tile
		}
		int roll = pct(Math.floorDiv(x, PATCH), Math.floorDiv(z, PATCH), 21);
		return roll < 34 ? 0 : roll < 74 ? 1 : DEEP;
	}

	private static boolean hasSkylight(int cx, int cz) {
		return pct(cx, cz, 25) < 14;
	}

	private boolean isWall(int x, int z) {
		int cx = Math.floorDiv(x, CELL);
		int cz = Math.floorDiv(z, CELL);
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		if (lx == 0 && lz == 0) {
			return true; // a column at every grid corner
		}
		if (lx == 0) {
			return pct(cx, cz, 22) < 55;
		}
		if (lz == 0) {
			return pct(cx, cz, 23) < 55;
		}
		// a lone column in the middle of some halls
		return lx == 8 && lz == 8 && !hasSkylight(cx, cz) && pct(cx, cz, 24) < 30;
	}

	/** Walls are pierced by a 7-wide arch in the middle of each 16-block span. */
	private static boolean isArch(int x, int z) {
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		return (lx == 0 && lz >= 5 && lz <= 11) || (lz == 0 && lx >= 5 && lx <= 11);
	}

	private BlockState ceilingAt(int x, int z) {
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		if (hasSkylight(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)) && lx >= 5 && lx <= 11 && lz >= 5 && lz <= 11) {
			return AIR;
		}
		return lx % 8 == 4 && lz % 8 == 4 && pct(x, z, 26) < 75 ? this.light : this.tile;
	}

	@Override
	public BlockPos exitNear(int x, int z) {
		int rx = Math.floorDiv(x, CELL * REGION);
		int rz = Math.floorDiv(z, CELL * REGION);
		int h = hash(rx, rz, 27);
		int cellX = rx * REGION + h % REGION;
		int cellZ = rz * REGION + (h / REGION) % REGION;
		return new BlockPos(cellX * CELL + 12, FLOOR_Y + 1, cellZ * CELL + 12);
	}

	private BlockState exitFrameAt(int x, int y, int z) {
		BlockPos door = this.exitNear(x, z);
		if (z != door.getZ() || Math.abs(x - door.getX()) > 1 || y > FLOOR_Y + 4) {
			return null;
		}
		if (x != door.getX()) {
			return this.tile;
		}
		if (y <= FLOOR_Y + 2) {
			return this.threshold;
		}
		return y == FLOOR_Y + 3 ? this.exitSign : this.tile;
	}

	/** Centre of the nearest deep pool, for arrivals that fall in from above. */
	public BlockPos findDeepPool(int x, int z) {
		int px = Math.floorDiv(x, PATCH);
		int pz = Math.floorDiv(z, PATCH);
		for (int radius = 0; radius <= 12; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int cx = (px + dx) * PATCH + 4;
					int cz = (pz + dz) * PATCH + 4;
					if (Math.max(Math.abs(dx), Math.abs(dz)) == radius && this.poolDepth(cx, cz) == DEEP
						&& this.blockAt(cx, FLOOR_Y + 1, cz).isAir() && this.blockAt(cx, CEILING_Y - 1, cz).isAir()) {
						return new BlockPos(cx, CEILING_Y - 3, cz);
					}
				}
			}
		}
		return this.findArrival(x, z);
	}
}
