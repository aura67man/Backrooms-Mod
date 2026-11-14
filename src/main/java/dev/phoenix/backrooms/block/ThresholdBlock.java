package dev.phoenix.backrooms.block;

import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.transit.Transit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The dark space inside a door frame. Walk into it inside a liminal level and you go home;
 * walk into one in the real world (the wrong stairwell) and you end up in the Poolrooms.
 */
public class ThresholdBlock extends Block {
	public ThresholdBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (!(level instanceof ServerLevel) || !(entity instanceof ServerPlayer player) || player.isOnPortalCooldown()) {
			return;
		}
		player.setPortalCooldown(Transit.COOLDOWN_TICKS);
		if (ModDimensions.isLiminal(level)) {
			Transit.defer(() -> Transit.leave(player));
		} else {
			// remember the block the player approached from, so coming home does not drop them back inside the door
			double dx = player.getX() - (pos.getX() + 0.5);
			double dz = player.getZ() - (pos.getZ() + 0.5);
			Direction back = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
			BlockPos returnTo = pos.relative(back);
			Transit.defer(() -> Transit.enter(player, ModDimensions.POOLROOMS, returnTo));
		}
	}
}
