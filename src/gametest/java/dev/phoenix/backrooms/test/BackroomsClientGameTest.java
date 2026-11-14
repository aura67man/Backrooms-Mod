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
			poolrooms(context, world.getServer(), world.getConnection(), "singleplayer");
			carpark(context, world.getServer(), world.getConnection(), "singleplayer");
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

	// ------------------------------------------------------------------ checkpoint 3: Poolrooms + stairwell + End void
	private void poolrooms(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String mode) {
		// Entrance: the hidden Overworld structure. Place one (worldgen places them rarely) and walk down it.
		// The superflat test world is only four blocks deep, so give the staircase a stone plinth to dig into.
		int flat = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 20, 20));
		int ground = flat + 12;
		server.runCommand("execute in minecraft:overworld run fill 14 " + flat + " 4 26 " + (ground - 1) + " 28 minecraft:stone");
		server.runCommand("execute in minecraft:overworld run place feature backrooms:wrong_stairwell 20 " + ground + " 20");
		teleport(server, Level.OVERWORLD, 20.5, ground, 24.5, 180, 15);
		waitForDimension(context, Level.OVERWORLD);
		connection.waitForChunksRender();
		context.waitTicks(110);
		expectBlock(server, Level.OVERWORLD, new BlockPos(20, ground + 2, 20), ModBlocks.EXIT_SIGN);
		expectBlock(server, Level.OVERWORLD, new BlockPos(20, ground - 7, 10), ModBlocks.THRESHOLD);
		shot(context, mode + "_wrong_stairwell");
		context.getInput().holdKey(options -> options.keyUp);
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == ModDimensions.POOLROOMS, 300);
		context.getInput().releaseKey(options -> options.keyUp);
		pass(mode + ": walking down the wrong stairwell moved the player to backrooms:poolrooms");
		connection.waitForChunksRender();
		context.waitTicks(20);
		shot(context, mode + "_poolrooms_arrival");

		BlockPos feet = server.computeOnServer(s -> player(s).blockPosition());
		int[] counts = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(ModDimensions.POOLROOMS);
			int water = 0;
			int tiles = 0;
			int lights = 0;
			for (int dx = -24; dx <= 24; dx++) {
				for (int dz = -24; dz <= 24; dz++) {
					BlockPos floor = new BlockPos(feet.getX() + dx, Layout.FLOOR_Y, feet.getZ() + dz);
					water += level.getBlockState(floor).is(net.minecraft.world.level.block.Blocks.WATER) ? 1 : 0;
					tiles += level.getBlockState(floor).is(ModBlocks.POOL_TILE) ? 1 : 0;
					lights += level.getBlockState(floor.above(8)).is(ModBlocks.POOL_LIGHT) ? 1 : 0;
				}
			}
			return new int[]{water, tiles, lights};
		});
		check(counts[0] > 200 && counts[1] > 200 && counts[2] > 4, mode + ": Poolrooms generated (" + counts[0] + " water, " + counts[1] + " tile, " + counts[2] + " ceiling lights within 24 blocks)");
		walkThroughExit(context, server, connection, ModDimensions.POOLROOMS, mode);
		BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
		check(home.distManhattan(new BlockPos(20, ground - 7, 11)) <= 2, mode + ": exit door returned the player to the bottom of the stairwell (" + home.toShortString() + ")");

		// Entrance: fall out of the bottom of the End.
		teleport(server, Level.END, 300.5, 40, 300.5, 0, 60);
		waitForDimension(context, Level.END);
		context.waitFor(mc -> mc.player != null && mc.player.getY() < -6, 400);
		shot(context, mode + "_end_void_tell");
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == ModDimensions.POOLROOMS, 200);
		pass(mode + ": falling out of the End moved the player to backrooms:poolrooms");
		context.waitFor(mc -> mc.player != null && mc.player.isInWater(), 200);
		float health = server.computeOnServer(s -> player(s).getHealth());
		check(health >= 20.0F, mode + ": the fall ended in a deep pool with no damage (health " + health + ")");
		connection.waitForChunksRender();
		shot(context, mode + "_poolrooms_fell_into_pool");
		walkThroughExit(context, server, connection, ModDimensions.POOLROOMS, mode);
		heard("backrooms:ambient.poolrooms");
	}

	// ------------------------------------------------------------------ checkpoint 4: car park + key fob
	private void carpark(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String mode) {
		boolean craftable = server.computeOnServer(s -> s.getRecipeManager().byKey(
			ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, dev.phoenix.backrooms.Backrooms.id("key_fob"))).isPresent());
		check(craftable, mode + ": key fob crafting recipe is loaded");
		int ground = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 40, -40));
		teleport(server, Level.OVERWORLD, 40.5, ground, -39.5, 90, 0);
		server.runCommand("clear @a");
		server.runCommand("give @a backrooms:key_fob");
		waitForDimension(context, Level.OVERWORLD);
		connection.waitForChunksRender();
		context.waitTicks(20);
		shot(context, mode + "_key_fob_in_hand");
		context.getInput().pressKey(options -> options.keyUse);
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == ModDimensions.CARPARK, 100);
		pass(mode + ": pressing the key fob moved the player to backrooms:carpark");
		connection.waitForChunksRender();
		context.waitTicks(20);
		shot(context, mode + "_carpark_arrival");

		BlockPos feet = server.computeOnServer(s -> player(s).blockPosition());
		int[] counts = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(ModDimensions.CARPARK);
			int lines = 0;
			int pillars = 0;
			int lamps = 0;
			for (int dx = -24; dx <= 24; dx++) {
				for (int dz = -24; dz <= 24; dz++) {
					BlockPos floor = new BlockPos(feet.getX() + dx, Layout.FLOOR_Y, feet.getZ() + dz);
					lines += level.getBlockState(floor).is(ModBlocks.GARAGE_LINE) ? 1 : 0;
					pillars += level.getBlockState(floor.above()).is(ModBlocks.HAZARD_PILLAR) ? 1 : 0;
					lamps += level.getBlockState(floor.above(5)).is(ModBlocks.SODIUM_LAMP) ? 1 : 0;
				}
			}
			return new int[]{lines, pillars, lamps};
		});
		check(counts[0] > 100 && counts[1] > 8 && counts[2] > 2, mode + ": car park generated (" + counts[0] + " bay-line blocks, " + counts[1] + " pillars, " + counts[2] + " working lamps within 24 blocks)");

		// Way back 1: the fob works in reverse.
		context.waitTicks(70);
		context.getInput().pressKey(options -> options.keyUse);
		waitForDimension(context, Level.OVERWORLD);
		BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
		check(home.distManhattan(new BlockPos(40, ground, -40)) <= 1, mode + ": pressing the fob again returned the player to where they started (" + home.toShortString() + ")");

		// Way back 2: the lift core's exit door.
		context.waitTicks(70);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == ModDimensions.CARPARK, 100);
		walkThroughExit(context, server, connection, ModDimensions.CARPARK, mode);
		heard("backrooms:item.key_fob.beep", "backrooms:ambient.carpark");
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
