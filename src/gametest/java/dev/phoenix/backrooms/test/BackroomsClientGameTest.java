package dev.phoenix.backrooms.test;

import dev.phoenix.backrooms.BackroomsConfig;
import dev.phoenix.backrooms.block.FluorescentLightBlock;
import dev.phoenix.backrooms.registry.ModBlocks;
import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.world.layout.Layout;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scripted playthrough in a real client. Every check logs a [BRTEST] line. */
public class BackroomsClientGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("BRTEST");
	/** Every sound event the client actually handed to the sound engine. */
	private static final Set<String> HEARD = ConcurrentHashMap.newKeySet();

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.getSoundManager().addListener((sound, set, range) -> {
			String id = sound.getIdentifier().toString();
			if (id.startsWith("backrooms:") && HEARD.add(id)) {
				LOG.info("[BRTEST] SOUND client played {}", id);
			}
		}));
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			pass("singleplayer world created");
			world.getServer().runCommand("gamemode creative @a");
			levelZero(context, world.getServer(), world.getConnection(), "singleplayer");
		}
		pass("ALL CHECKS COMPLETE");
	}

	// ------------------------------------------------------------------ checkpoint 2: Level 0 + noclip
	private void levelZero(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String mode) {
		// Entrance: walk into a wall in the overworld and keep pushing. Chance forced to 100% for the test.
		double realChance = BackroomsConfig.get().noclipChance;
		server.runOnServer(s -> BackroomsConfig.get().noclipChance = 1.0);
		int ground = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
		server.runCommand("execute in minecraft:overworld run fill -2 " + ground + " 2 2 " + (ground + 3) + " 2 minecraft:stone");
		teleport(server, Level.OVERWORLD, 0.5, ground, 0.5, 0, 0);
		waitForDimension(context, Level.OVERWORLD);
		connection.waitForChunksRender();
		context.waitTicks(10);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(70);
		shot(context, mode + "_noclip_tell");
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == ModDimensions.LEVEL_0, 200);
		context.getInput().releaseKey(options -> options.keyUp);
		server.runOnServer(s -> BackroomsConfig.get().noclipChance = realChance);
		pass(mode + ": noclip entrance (push into wall ~5s) moved the player to backrooms:level_0");
		connection.waitForChunksRender();
		context.waitTicks(20);
		shot(context, mode + "_level_0_arrival");

		// Generation: carpet, wallpaper, lights and at least one flickering light near the arrival point.
		BlockPos feet = server.computeOnServer(s -> player(s).blockPosition());
		expectBlock(server, ModDimensions.LEVEL_0, feet.below(), ModBlocks.DAMP_CARPET);
		int[] counts = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(ModDimensions.LEVEL_0);
			int walls = 0;
			int lights = 0;
			for (int dx = -24; dx <= 24; dx++) {
				for (int dz = -24; dz <= 24; dz++) {
					walls += level.getBlockState(feet.offset(dx, 1, dz)).is(ModBlocks.WALLPAPER) ? 1 : 0;
					lights += level.getBlockState(feet.offset(dx, 4, dz)).is(ModBlocks.FLUORESCENT_LIGHT) ? 1 : 0;
				}
			}
			return new int[]{walls, lights};
		});
		check(counts[0] > 50 && counts[1] > 4, mode + ": Level 0 generated rooms (" + counts[0] + " wallpaper blocks, " + counts[1] + " light panels within 24 blocks)");

		// Flicker: kick one flickering panel with the random tick it would normally get, then watch it stutter.
		BlockPos flicker = server.computeOnServer(s -> findFlickeringLight(s.getLevel(ModDimensions.LEVEL_0), feet));
		check(flicker != null, mode + ": found a flickering light panel at " + (flicker == null ? "?" : flicker.toShortString()));
		teleport(server, ModDimensions.LEVEL_0, flicker.getX() + 0.5, feet.getY(), flicker.getZ() + 2.5, 180, -50);
		connection.waitForChunksRender();
		server.runOnServer(s -> {
			ServerLevel level = s.getLevel(ModDimensions.LEVEL_0);
			level.getBlockState(flicker).randomTick(level, flicker, level.getRandom());
		});
		int toggles = 0;
		boolean last = true;
		boolean shotDark = false;
		for (int i = 0; i < 200 && toggles < 4; i++) {
			context.waitTick();
			boolean lit = server.computeOnServer(s -> s.getLevel(ModDimensions.LEVEL_0).getBlockState(flicker).getValue(FluorescentLightBlock.LIT));
			if (lit != last) {
				toggles++;
				last = lit;
			}
			if (!lit && !shotDark) {
				context.waitTick();
				shot(context, mode + "_level_0_light_flickered_off");
				shotDark = true;
			}
		}
		check(toggles >= 2, mode + ": flickering light toggled " + toggles + " times while watched");

		// Way back: walk through the nearest exit door.
		walkThroughExit(context, server, connection, ModDimensions.LEVEL_0, mode);
		BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
		check(home.distManhattan(new BlockPos(0, ground, 0)) <= 3, mode + ": exit door returned the player to where they noclipped (" + home.toShortString() + ")");
		heard("backrooms:ambient.level_0", "backrooms:event.noclip_hum", "backrooms:event.transit", "backrooms:block.fluorescent_light.buzz");
	}

	/** Teleport next to the level's nearest exit, wait out the arrival cooldown, and walk into the threshold. */
	private void walkThroughExit(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, ResourceKey<Level> dimension, String mode) {
		BlockPos door = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			return ModDimensions.layoutOf(s.getLevel(dimension)).exitNear(player.getBlockX(), player.getBlockZ());
		});
		expectBlock(server, dimension, door, ModBlocks.THRESHOLD);
		expectBlock(server, dimension, door.above(2), ModBlocks.EXIT_SIGN);
		teleport(server, dimension, door.getX() + 0.5, door.getY(), door.getZ() + 3.5, 180, 0);
		connection.waitForChunksRender();
		context.waitTicks(110);
		shot(context, mode + "_" + dimension.identifier().getPath() + "_exit_door");
		context.getInput().holdKey(options -> options.keyUp);
		context.waitFor(mc -> mc.level != null && mc.level.dimension() != dimension, 200);
		context.getInput().releaseKey(options -> options.keyUp);
		waitForDimension(context, Level.OVERWORLD);
		connection.waitForChunksRender();
		pass(mode + ": walked through the exit door in " + dimension.identifier() + " and came back to the overworld");
	}

	private static BlockPos findFlickeringLight(ServerLevel level, BlockPos near) {
		Layout layout = ModDimensions.layoutOf(level);
		for (int radius = 0; radius < 80; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					BlockPos pos = new BlockPos(near.getX() + dx, Layout.FLOOR_Y + 5, near.getZ() + dz);
					BlockState state = layout.blockAt(pos.getX(), pos.getY(), pos.getZ());
					if (state.is(ModBlocks.FLUORESCENT_LIGHT) && state.getValue(FluorescentLightBlock.FLICKERING) && layout.canStandAt(pos.getX(), pos.getZ() + 2)) {
						return pos;
					}
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ helpers
	static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	/** /tp runs in the overworld's command context unless told otherwise, so always name the dimension. */
	static void teleport(TestServerContext server, ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("execute in " + dimension.identifier() + " run tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch);
	}

	static void pass(String what) {
		LOG.info("[BRTEST] PASS {}", what);
	}

	static void check(boolean condition, String what) {
		if (!condition) {
			LOG.error("[BRTEST] FAIL {}", what);
			throw new AssertionError(what);
		}
		pass(what);
	}

	static void heard(String... sounds) {
		for (String sound : sounds) {
			check(HEARD.contains(sound), "client played sound " + sound);
		}
	}

	static void waitForDimension(ClientGameTestContext context, ResourceKey<Level> dimension) {
		context.waitFor(mc -> mc.level != null && mc.player != null && mc.level.dimension() == dimension, 400);
	}

	static void expectBlock(TestServerContext server, ResourceKey<Level> dimension, BlockPos pos, Block block) {
		Block found = server.computeOnServer(s -> s.getLevel(dimension).getBlockState(pos).getBlock());
		check(found == block, "block at " + pos.toShortString() + " in " + dimension.identifier() + " is " + block.getName().getString() + " (found " + found.getName().getString() + ")");
	}

	static void shot(ClientGameTestContext context, String name) {
		Path path = context.takeScreenshot(name);
		LOG.info("[BRTEST] SCREENSHOT {}", path.getFileName());
	}
}
