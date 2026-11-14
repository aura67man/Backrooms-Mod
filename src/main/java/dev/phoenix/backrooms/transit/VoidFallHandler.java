package dev.phoenix.backrooms.transit;

import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.registry.ModSounds;
import dev.phoenix.backrooms.world.layout.Layout;
import dev.phoenix.backrooms.world.layout.PoolroomsLayout;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Entrance 2 (location): fall out of the bottom of the End. Below the islands the air starts to
 * drip and echo (the tell), and instead of dying in the void you drop into a deep pool in the Poolrooms.
 */
public final class VoidFallHandler {
	private static final int TELL_BELOW_Y = 0;
	private static final int CATCH_BELOW_Y = -32;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.level().dimension() == Level.END && player.getY() < TELL_BELOW_Y && !player.isSpectator()) {
					tick(player);
				}
			}
		});
	}

	private static void tick(ServerPlayer player) {
		ServerLevel level = player.level();
		if (player.getY() >= CATCH_BELOW_Y) {
			level.sendParticles(ParticleTypes.FALLING_WATER, player.getX(), player.getY() - 2.0, player.getZ(), 12, 1.5, 2.0, 1.5, 0.0);
			if (player.tickCount % 15 == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AMBIENT_POOLROOMS, SoundSource.AMBIENT, 0.7F, 1.0F);
			}
			return;
		}
		ServerLevel poolrooms = level.getServer().getLevel(ModDimensions.POOLROOMS);
		Layout layout = poolrooms == null ? null : ModDimensions.layoutOf(poolrooms);
		if (layout instanceof PoolroomsLayout pools) {
			BlockPos above = pools.findDeepPool(player.getBlockX(), player.getBlockZ());
			// coming home from here would be straight back into the void, so "home" is the End's arrival platform
			Transit.enterAt(player, poolrooms, Vec3.atBottomCenterOf(above), ServerLevel.END_SPAWN_POINT);
		}
	}

	private VoidFallHandler() {
	}
}
