package dev.phoenix.backrooms.transit;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.registry.ModSounds;
import dev.phoenix.backrooms.world.layout.Layout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Moves players between the real world and the liminal levels, remembering the way home. */
public final class Transit {
	/** Where the player was standing when they first slipped out of reality. Survives relogs and death. */
	public static final AttachmentType<GlobalPos> RETURN_POS = AttachmentRegistry.create(Backrooms.id("return_pos"),
		builder -> builder.persistent(GlobalPos.CODEC).copyOnDeath());
	/** Ticks during which thresholds ignore a player who has just travelled. */
	public static final int COOLDOWN_TICKS = 100;
	private static final List<Runnable> PENDING = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!PENDING.isEmpty()) {
				List<Runnable> batch = new ArrayList<>(PENDING);
				PENDING.clear();
				batch.forEach(Runnable::run);
			}
		});
	}

	/** Run a transit at the end of the tick instead of in the middle of entity movement. */
	public static void defer(Runnable action) {
		PENDING.add(action);
	}

	/** Send the player into a liminal level, arriving at the nearest standable spot to their X/Z. */
	public static boolean enter(ServerPlayer player, ResourceKey<Level> target, BlockPos returnTo) {
		ServerLevel destination = player.level().getServer().getLevel(target);
		Layout layout = destination == null ? null : ModDimensions.layoutOf(destination);
		if (layout == null) {
			Backrooms.LOGGER.warn("Level {} is not available", target.identifier());
			return false;
		}
		return enterAt(player, destination, Vec3.atBottomCenterOf(layout.findArrival(player.getBlockX(), player.getBlockZ())), returnTo);
	}

	public static boolean enterAt(ServerPlayer player, ServerLevel destination, Vec3 position, BlockPos returnTo) {
		if (!ModDimensions.isLiminal(player.level())) {
			player.setAttached(RETURN_POS, GlobalPos.of(player.level().dimension(), returnTo));
		}
		move(player, destination, position);
		return true;
	}

	/** Send the player back to where they came from (or world spawn if that is unknown). */
	public static void leave(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		GlobalPos home = player.removeAttached(RETURN_POS);
		ServerLevel destination = home == null ? null : server.getLevel(home.dimension());
		if (destination == null) {
			home = server.getRespawnData().globalPos();
			destination = server.getLevel(home.dimension());
			if (destination == null) {
				destination = server.overworld();
			}
		}
		move(player, destination, Vec3.atBottomCenterOf(home.pos()));
	}

	private static void move(ServerPlayer player, ServerLevel destination, Vec3 position) {
		ServerLevel origin = player.level();
		origin.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSIT, SoundSource.PLAYERS, 0.8F, 1.0F);
		player.teleportTo(destination, position.x, position.y, position.z, Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		player.setPortalCooldown(COOLDOWN_TICKS);
		destination.playSound(null, position.x, position.y, position.z, ModSounds.TRANSIT, SoundSource.PLAYERS, 0.8F, 1.0F);
		Backrooms.LOGGER.info("{} slipped from {} to {} at {}", player.getPlainTextName(), origin.dimension().identifier(),
			destination.dimension().identifier(), BlockPos.containing(position).toShortString());
	}

	private Transit() {
	}
}
