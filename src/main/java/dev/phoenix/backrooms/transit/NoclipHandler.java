package dev.phoenix.backrooms.transit;

import dev.phoenix.backrooms.BackroomsConfig;
import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.registry.ModSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Entrance 1 (random): keep walking into a wall while looking at it. After a couple of seconds the
 * wall starts to hum and shed sparks (the tell); after noclipHoldSeconds there is a noclipChance
 * roll, and on a success you fall through into Level 0.
 */
public final class NoclipHandler {
	private static final int TELL_AFTER_TICKS = 30;
	private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

	private static final class State {
		int ticks;
		Vec3 lastPos = Vec3.ZERO;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	private static void tick(ServerPlayer player) {
		State state = STATES.computeIfAbsent(player, p -> new State());
		ServerLevel level = player.level();
		BlockHitResult wall = ModDimensions.isLiminal(level) || player.isSpectator() || player.isPassenger() ? null : wallBeingPushed(player, state);
		state.lastPos = player.position();
		if (wall == null) {
			state.ticks = 0;
			return;
		}
		state.ticks++;
		// just in front of the wall face, so the sparks are not hidden inside the block
		Vec3 at = wall.getLocation().add(Vec3.atLowerCornerOf(wall.getDirection().getUnitVec3i()).scale(0.08));
		if (state.ticks >= TELL_AFTER_TICKS) {
			if (state.ticks % 2 == 0) {
				// forced + always-visible so the tell still shows on "minimal" particle settings
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, at.x, at.y, at.z, 8, 0.45, 0.45, 0.45, 0.05);
				level.sendParticles(ParticleTypes.WHITE_ASH, true, true, at.x, at.y, at.z, 10, 0.5, 0.5, 0.5, 0.0);
				level.sendParticles(ParticleTypes.END_ROD, true, true, at.x, at.y, at.z, 2, 0.4, 0.4, 0.4, 0.01);
			}
			if ((state.ticks - TELL_AFTER_TICKS) % 50 == 0) {
				level.playSound(null, at.x, at.y, at.z, ModSounds.NOCLIP_HUM, SoundSource.AMBIENT, 0.9F, 1.0F);
			}
		}
		BackroomsConfig config = BackroomsConfig.get();
		if (state.ticks >= config.noclipHoldSeconds * 20.0) {
			state.ticks = 0;
			if (player.getRandom().nextDouble() < config.noclipChance) {
				Transit.enter(player, ModDimensions.LEVEL_0, player.blockPosition());
			}
		}
	}

	/** The wall face the player is looking at, if they are holding forward and going nowhere. */
	private static BlockHitResult wallBeingPushed(ServerPlayer player, State state) {
		if (!player.getLastClientInput().forward() || Math.abs(player.getXRot()) > 50.0F) {
			return null;
		}
		Vec3 moved = player.position().subtract(state.lastPos);
		if (moved.x * moved.x + moved.z * moved.z > 0.0004) {
			return null;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
		BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(flat.scale(1.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK || !player.level().getBlockState(hit.getBlockPos()).isCollisionShapeFullBlock(player.level(), hit.getBlockPos())) {
			return null;
		}
		return hit;
	}

	private NoclipHandler() {
	}
}
