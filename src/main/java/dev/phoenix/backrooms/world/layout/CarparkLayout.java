package dev.phoenix.backrooms.world.layout;

import dev.phoenix.backrooms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sublevel P: an underground car park with no cars and no ramp out. Driving aisles run east-west
 * every 16 blocks with back-to-back rows of painted bays between them, a hazard-striped pillar
 * every 8 blocks, and sodium lamps of which fewer than half still work. Each 64x64 block has one
 * concrete lift core with the way out set into its south face.
 */
public class CarparkLayout extends Layout {
	private static final int ROW = 16;
	private static final int REGION = 64;
	private static final int CEILING_Y = FLOOR_Y + 5;

	private final BlockState concrete = ModBlocks.GARAGE_CONCRETE.defaultBlockState();
	private final BlockState floor = ModBlocks.GARAGE_FLOOR.defaultBlockState();
	private final BlockState lineZ = ModBlocks.GARAGE_LINE.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
	private final BlockState lineX = ModBlocks.GARAGE_LINE.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
	private final BlockState hazard = ModBlocks.HAZARD_PILLAR.defaultBlockState();
	private final BlockState lamp = ModBlocks.SODIUM_LAMP.defaultBlockState();
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
		int row = Math.floorMod(z, ROW);
		boolean aisle = row >= 5 && row <= 11;
		if (y == FLOOR_Y) {
			if (aisle) {
				return this.floor;
			}
			if (row == 0) {
				return this.lineX; // the line between two back-to-back bays
			}
			return Math.floorMod(x, 4) == 0 ? this.lineZ : this.floor;
		}
		if (y == CEILING_Y) {
			boolean fitting = Math.floorMod(x, 8) == 4 && row == 8;
			return fitting && pct(Math.floorDiv(x, 8), Math.floorDiv(z, ROW), 31) < 45 ? this.lamp : this.concrete;
		}
		BlockState core = this.liftCoreAt(x, y, z);
		if (core != null) {
			return core;
		}
		if (Math.floorMod(x, 8) == 0 && row == 0) {
			return y == FLOOR_Y + 1 ? this.hazard : this.concrete;
		}
		// dividing walls cross the bay rows every 64 blocks but always leave the aisles open
		if (Math.floorMod(x, REGION) == 0 && !aisle && pct(Math.floorDiv(x, REGION), Math.floorDiv(z, ROW), 32) < 70) {
			return this.concrete;
		}
		return AIR;
	}

	@Override
	public BlockPos exitNear(int x, int z) {
		int rx = Math.floorDiv(x, REGION);
		int rz = Math.floorDiv(z, REGION);
		int h = hash(rx, rz, 33);
		// the core is 5x5, sits between two pillars and straddles a bay divider; the door is in its south face
		int doorX = rx * REGION + 8 + (h % 6) * 8 + 4;
		int doorZ = rz * REGION + ((h / 6) % 3) * ROW + ROW + 2;
		return new BlockPos(doorX, FLOOR_Y + 1, doorZ);
	}

	private BlockState liftCoreAt(int x, int y, int z) {
		BlockPos door = this.exitNear(x, z);
		int dx = x - door.getX();
		int dz = z - door.getZ();
		if (Math.abs(dx) > 2 || dz > 0 || dz < -4) {
			return null;
		}
		if (dx == 0 && dz == 0) {
			if (y <= FLOOR_Y + 2) {
				return this.threshold;
			}
			if (y == FLOOR_Y + 3) {
				return this.exitSign;
			}
		}
		if (dz == 0 && Math.abs(dx) == 1 && y <= FLOOR_Y + 3) {
			return this.hazard; // striped door surround
		}
		return this.concrete;
	}
}
