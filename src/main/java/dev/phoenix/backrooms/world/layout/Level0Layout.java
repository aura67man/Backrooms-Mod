package dev.phoenix.backrooms.world.layout;

import dev.phoenix.backrooms.block.FluorescentLightBlock;
import dev.phoenix.backrooms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 0: endless office space. Space is cut into 8x8 cells; each cell edge may or may not carry
 * a wall, most walls have a doorway somewhere along them, and how cramped it gets drifts slowly
 * from open halls to tight mazes. Roughly half the cells have a ceiling light and some of those flicker.
 */
public class Level0Layout extends Layout {
	private static final int CELL = 8;
	/** One exit door per REGION x REGION cells. */
	private static final int REGION = 6;
	private static final int CEILING_Y = FLOOR_Y + 5;

	private final BlockState carpet = ModBlocks.DAMP_CARPET.defaultBlockState();
	private final BlockState wallpaper = ModBlocks.WALLPAPER.defaultBlockState();
	private final BlockState baseboard = ModBlocks.WALLPAPER_BASEBOARD.defaultBlockState();
	private final BlockState ceiling = ModBlocks.CEILING_TILE.defaultBlockState();
	private final BlockState light = ModBlocks.FLUORESCENT_LIGHT.defaultBlockState();
	private final BlockState flickeringLight = this.light.setValue(FluorescentLightBlock.FLICKERING, true);
	private final BlockState threshold = ModBlocks.THRESHOLD.defaultBlockState();
	private final BlockState exitSign = ModBlocks.EXIT_SIGN.defaultBlockState();

	@Override
	public int minY() {
		return FLOOR_Y - 1;
	}

	@Override
	public int maxY() {
		return CEILING_Y + 1;
	}

	@Override
	public BlockState blockAt(int x, int y, int z) {
		if (y < this.minY() || y > this.maxY()) {
			return AIR;
		}
		if (y == this.minY() || y == this.maxY()) {
			return BEDROCK;
		}
		if (y == FLOOR_Y) {
			return this.carpet;
		}
		if (y == CEILING_Y) {
			return this.ceilingAt(x, z);
		}
		BlockState door = this.exitFrameAt(x, y, z);
		if (door != null) {
			return door;
		}
		if (this.isWall(x, z)) {
			return y == FLOOR_Y + 1 ? this.baseboard : this.wallpaper;
		}
		return AIR;
	}

	private BlockState ceilingAt(int x, int z) {
		int cx = Math.floorDiv(x, CELL);
		int cz = Math.floorDiv(z, CELL);
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		if (lx == 4 && (lz == 3 || lz == 4) && pct(cx, cz, 7) < 55) {
			return pct(cx, cz, 8) < 14 ? this.flickeringLight : this.light;
		}
		return this.ceiling;
	}

	/** 0-100: how likely a cell edge here is to be walled. Drifts over a 5-cell scale. */
	private static int density(int cx, int cz) {
		int zone = pct(Math.floorDiv(cx, 5), Math.floorDiv(cz, 5), 1);
		if (zone < 15) {
			return 8; // big open hall with the odd stray wall
		}
		return 30 + zone / 2; // 37% .. 79%
	}

	private static boolean hasWestWall(int cx, int cz) {
		return pct(cx, cz, 2) < density(cx, cz);
	}

	private static boolean hasNorthWall(int cx, int cz) {
		return pct(cx, cz, 3) < density(cx, cz);
	}

	/** True when position `along` (1..7) on a wall is part of that wall's doorway. */
	private static boolean inDoorway(int cx, int cz, int salt, int along) {
		int h = hash(cx, cz, salt);
		int kind = h % 6;
		if (kind == 0) {
			return false; // solid wall
		}
		int width = kind == 1 ? 4 : 2;
		int start = 1 + (h / 6) % (8 - width);
		return along >= start && along < start + width;
	}

	private boolean isWall(int x, int z) {
		int cx = Math.floorDiv(x, CELL);
		int cz = Math.floorDiv(z, CELL);
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		if (lx == 0 && lz == 0) {
			// corner post: solid wherever a wall meets it, plus the occasional lone pillar
			return hasWestWall(cx, cz) || hasNorthWall(cx, cz) || hasWestWall(cx, cz - 1) || hasNorthWall(cx - 1, cz) || pct(cx, cz, 4) < 12;
		}
		if (lx == 0) {
			return hasWestWall(cx, cz) && !inDoorway(cx, cz, 5, lz);
		}
		if (lz == 0) {
			return hasNorthWall(cx, cz) && !inDoorway(cx, cz, 6, lx);
		}
		return false;
	}

	@Override
	public BlockPos exitNear(int x, int z) {
		int rx = Math.floorDiv(x, CELL * REGION);
		int rz = Math.floorDiv(z, CELL * REGION);
		int h = hash(rx, rz, 9);
		int cellX = rx * REGION + h % REGION;
		int cellZ = rz * REGION + (h / REGION) % REGION;
		return new BlockPos(cellX * CELL + 4, FLOOR_Y + 1, cellZ * CELL + 5);
	}

	/** A free-standing door frame in the middle of a cell: two posts, a dark threshold, a lit EXIT sign. */
	private BlockState exitFrameAt(int x, int y, int z) {
		BlockPos door = this.exitNear(x, z);
		if (z != door.getZ() || Math.abs(x - door.getX()) > 1) {
			return null;
		}
		if (x != door.getX()) {
			return y == FLOOR_Y + 1 ? this.baseboard : this.wallpaper;
		}
		if (y <= FLOOR_Y + 2) {
			return this.threshold;
		}
		return y == FLOOR_Y + 3 ? this.exitSign : this.wallpaper;
	}
}
