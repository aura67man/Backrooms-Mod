package dev.phoenix.backrooms.world.layout;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A liminal level described as a pure function: blockAt(x, y, z). Nothing is stored and nothing
 * depends on neighbouring chunks, so any chunk can be generated in any order and the same
 * function can be asked "where is the nearest exit?" or "where can a player stand?".
 */
public abstract class Layout {
	/** Y of the floor block players stand on in every level. */
	public static final int FLOOR_Y = 32;
	protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
	protected static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();

	public static Layout byStyle(String style) {
		return switch (style) {
			case "blank" -> new BlankLayout();
			case "level_0" -> new Level0Layout();
			default -> throw new IllegalArgumentException("Unknown liminal style: " + style);
		};
	}

	public abstract BlockState blockAt(int x, int y, int z);

	/** Lowest and highest Y that can contain anything other than air. */
	public abstract int minY();

	public abstract int maxY();

	/** The lower threshold block of the exit serving the area around (x, z). */
	public abstract BlockPos exitNear(int x, int z);

	public boolean canStandAt(int x, int z) {
		BlockState floor = this.blockAt(x, FLOOR_Y, z);
		return !floor.isAir() && floor.getFluidState().isEmpty()
			&& this.blockAt(x, FLOOR_Y + 1, z).isAir() && this.blockAt(x, FLOOR_Y + 2, z).isAir() && this.blockAt(x, FLOOR_Y + 3, z).isAir();
	}

	/** Nearest column to (x, z) where a player can stand; returns the feet position. */
	public BlockPos findArrival(int x, int z) {
		for (int radius = 0; radius <= 32; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) == radius && this.canStandAt(x + dx, z + dz)) {
						return new BlockPos(x + dx, FLOOR_Y + 1, z + dz);
					}
				}
			}
		}
		return new BlockPos(x, FLOOR_Y + 1, z);
	}

	/** Cheap, well-mixed positional hash (always non-negative). */
	protected static int hash(int x, int z, int salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 29;
		return (int) (h & 0x7FFFFFFF);
	}

	/** 0-99, for "x percent of cells" decisions. */
	protected static int pct(int x, int z, int salt) {
		return hash(x, z, salt) % 100;
	}
}
