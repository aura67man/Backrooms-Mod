package dev.phoenix.backrooms.test;

import dev.phoenix.backrooms.BackroomsConfig;
import dev.phoenix.backrooms.block.FluorescentLightBlock;
import dev.phoenix.backrooms.entity.FixtureEntity;
import dev.phoenix.backrooms.entity.TubeMothEntity;
import dev.phoenix.backrooms.registry.ModBlocks;
import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.world.layout.Layout;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
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
		// BRTEST_ONLY=entities (or level0, poolrooms, carpark, server) runs a subset while iterating; unset runs everything
		String only = System.getenv("BRTEST_ONLY");
		if (only == null || !only.equals("server")) {
			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getConnection().waitForChunksRender();
				pass("singleplayer world created");
				world.getServer().runCommand("gamemode creative @a");
				playthrough(context, world.getServer(), world.getConnection(), "singleplayer", only);
			}
			pass("SINGLEPLAYER PLAYTHROUGH COMPLETE");
		}
		if (only == null || only.equals("server")) {
			// A real dedicated server (offline mode, flat world) with this client connected to it over the network stack.
			Properties properties = new Properties();
			properties.setProperty("level-type", "minecraft:flat");
			properties.setProperty("online-mode", "false");
			try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
				TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				pass("dedicated server started and client connected");
				server.runCommand("gamemode creative @a");
				playthrough(context, server, connection, "dedicated", null);
			}
			pass("DEDICATED SERVER PLAYTHROUGH COMPLETE");
		}
		pass("ALL CHECKS COMPLETE");
	}

	private void playthrough(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String mode, String only) {
		HEARD.clear();
		if (only == null || only.contains("level0")) {
			levelZero(context, server, connection, mode);
		}
		if (only == null || only.contains("poolrooms")) {
			poolrooms(context, server, connection, mode);
		}
		if (only == null || only.contains("carpark")) {
			carpark(context, server, connection, mode);
		}
		if (only == null || only.contains("entities")) {
			entities(context, server, connection, mode);
		}
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

	// ------------------------------------------------------------------ checkpoint 5: entities
	private void entities(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String mode) {
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("difficulty normal");
		BlockPos start = server.computeOnServer(s -> findCorridor(ModDimensions.layoutOf(s.getLevel(ModDimensions.LEVEL_0)), 13));
		double px = start.getX() + 0.5;
		double pz = start.getZ() + 0.5;
		float towards = -90; // facing +X, where the entities are summoned
		float away = 90;
		teleport(server, ModDimensions.LEVEL_0, px, 33, pz, towards, 0);
		waitForDimension(context, ModDimensions.LEVEL_0);
		connection.waitForChunksRender();
		server.runCommand("gamemode survival @a");

		// --- Fixture: frozen while watched, creeps closer when not, attacks on contact
		server.runCommand("execute in backrooms:level_0 run summon backrooms:fixture " + (px + 10) + " 33 " + pz + " {Tags:[\"brtest\"]}");
		context.waitTicks(15);
		Vec3 p0 = tagged(server, "brtest");
		context.waitTicks(60);
		Vec3 p1 = tagged(server, "brtest");
		boolean flagged = server.computeOnServer(s -> ((FixtureEntity) entity(s, "brtest")).isWatched());
		check(flagged && p0.distanceTo(p1) < 0.05, mode + ": Fixture stayed frozen for 3s while the player looked at it (moved " + fmt(p0.distanceTo(p1)) + " blocks)");
		shot(context, mode + "_fixture_watched_far");
		teleport(server, ModDimensions.LEVEL_0, px, 33, pz, away, 0);
		context.waitTicks(50);
		Vec3 p2 = tagged(server, "brtest");
		check(p1.x - p2.x > 1.5, mode + ": Fixture crept " + fmt(p1.x - p2.x) + " blocks towards the player in 2.5s while they looked away");
		teleport(server, ModDimensions.LEVEL_0, px, 33, pz, towards, 0);
		context.waitTicks(6);
		Vec3 p3 = tagged(server, "brtest");
		context.waitTicks(40);
		Vec3 p4 = tagged(server, "brtest");
		check(p3.distanceTo(p4) < 0.05, mode + ": Fixture froze again the moment the player looked back (moved " + fmt(p3.distanceTo(p4)) + " blocks)");
		shot(context, mode + "_fixture_watched_closer");
		teleport(server, ModDimensions.LEVEL_0, px, 33, pz, away, 0);
		context.waitFor(mc -> mc.player != null && mc.player.getHealth() < 20.0F, 400);
		float hurt = context.computeOnClient(mc -> mc.player.getHealth());
		check(hurt < 20.0F, mode + ": Fixture reached the player and attacked (health now " + hurt + ")");
		server.runCommand("gamemode creative @a");
		teleport(server, ModDimensions.LEVEL_0, px, 33, pz, towards, 0);
		context.waitTicks(10);
		shot(context, mode + "_fixture_point_blank");
		context.waitFor(mc -> HEARD.contains("backrooms:entity.fixture.ambient"), 800);
		server.runCommand("damage @e[tag=brtest,limit=1] 4");
		context.waitTicks(20);
		server.runCommand("kill @e[tag=brtest]");
		context.waitTicks(30);
		heard("backrooms:entity.fixture.step", "backrooms:entity.fixture.attack", "backrooms:entity.fixture.ambient", "backrooms:entity.fixture.hurt", "backrooms:entity.fixture.death");

		// --- Tube Moth: flies about ignoring the player, retaliates only when hit
		server.runCommand("gamemode survival @a");
		server.runCommand("effect give @a minecraft:instant_health 1 4");
		server.runCommand("execute in backrooms:level_0 run summon backrooms:tube_moth " + (px + 4) + " 34.5 " + pz + " {Tags:[\"brmoth\"]}");
		context.waitTicks(10);
		double travelled = 0;
		double highest = 0;
		Vec3 last = tagged(server, "brmoth");
		for (int i = 0; i < 12; i++) {
			context.waitTicks(10);
			Vec3 now = tagged(server, "brmoth");
			travelled += now.distanceTo(last);
			highest = Math.max(highest, now.y);
			last = now;
		}
		boolean passive = server.computeOnServer(s -> ((Mob) entity(s, "brmoth")).getTarget() == null);
		float before = context.computeOnClient(mc -> mc.player.getHealth());
		check(travelled > 2.0 && highest > 33.6, mode + ": Tube Moth flew " + fmt(travelled) + " blocks in 6s, up to y=" + fmt(highest));
		check(passive && before >= 20.0F, mode + ": Tube Moth ignored the player while left alone (no target, player health " + before + ")");
		lookAt(server, last.add(0, 0.15, 0));
		context.waitTicks(4);
		shot(context, mode + "_tube_moth");
		context.waitFor(mc -> HEARD.contains("backrooms:entity.tube_moth.ambient"), 600);
		server.runCommand("execute as @a run damage @e[tag=brmoth,limit=1] 1 minecraft:player_attack by @s");
		context.waitTicks(10);
		boolean angry = server.computeOnServer(s -> ((Mob) entity(s, "brmoth")).getTarget() instanceof Player);
		check(angry, mode + ": Tube Moth targeted the player after being hit");
		context.waitFor(mc -> mc.player != null && mc.player.getHealth() < before, 400);
		pass(mode + ": the angry Tube Moth bit the player (health now " + context.computeOnClient(mc -> mc.player.getHealth()) + ")");
		server.runCommand("gamemode creative @a");
		server.runCommand("kill @e[tag=brmoth]");
		context.waitTicks(30);
		heard("backrooms:entity.tube_moth.ambient", "backrooms:entity.tube_moth.hurt", "backrooms:entity.tube_moth.death");

		// --- Spawn rules + config: natural spawning in the level, then the toggles
		BackroomsConfig config = BackroomsConfig.get();
		double fixtureRate = config.fixtureSpawnRate;
		server.runOnServer(s -> config.fixtureSpawnRate = 1.0);
		// The game test world starts with mob spawning off. Vanilla only refreshes its cached "spawn monsters"
		// flag when spawn_monsters itself changes, so flip that rule after enabling spawn_mobs.
		server.runCommand("gamerule spawn_mobs true");
		server.runCommand("gamerule spawn_monsters false");
		server.runCommand("gamerule spawn_monsters true");
		int[] spawned = new int[2];
		for (int i = 0; i < 90 && (spawned[0] == 0 || spawned[1] == 0); i++) {
			context.waitTicks(20);
			spawned = server.computeOnServer(s -> countNatural(s.getLevel(ModDimensions.LEVEL_0)));
		}
		check(spawned[0] > 0 && spawned[1] > 0, mode + ": natural spawning in Level 0 produced " + spawned[0] + " Fixture(s) and " + spawned[1] + " Tube Moth(s)");
		server.runOnServer(s -> {
			config.fixtureEnabled = false;
			config.tubeMothEnabled = false;
		});
		context.waitTicks(10);
		int[] afterToggle = server.computeOnServer(s -> countNatural(s.getLevel(ModDimensions.LEVEL_0)));
		check(afterToggle[0] == 0 && afterToggle[1] == 0, mode + ": config toggles removed every Fixture and Tube Moth and stopped new spawns");
		server.runOnServer(s -> {
			config.fixtureEnabled = true;
			config.tubeMothEnabled = true;
			config.fixtureSpawnRate = fixtureRate;
		});
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("kill @e[type=!minecraft:player]");
		teleport(server, Level.OVERWORLD, 0.5, server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0)), 0.5, 0, 0);
		waitForDimension(context, Level.OVERWORLD);
	}

	private static int[] countNatural(ServerLevel level) {
		return new int[]{
			level.getEntities(EntityTypeTest.forClass(FixtureEntity.class), e -> e.isAlive()).size(),
			level.getEntities(EntityTypeTest.forClass(TubeMothEntity.class), e -> e.isAlive()).size()};
	}

	/** A straight, obstacle-free strip of Level 0 running +X, three blocks wide. */
	private static BlockPos findCorridor(Layout layout, int length) {
		for (int x = -200; x < 200; x++) {
			for (int z = -200; z < 200; z++) {
				boolean clear = true;
				for (int dx = -1; dx <= length && clear; dx++) {
					for (int dz = -1; dz <= 1 && clear; dz++) {
						clear = layout.canStandAt(x + dx, z + dz);
					}
				}
				if (clear) {
					return new BlockPos(x, Layout.FLOOR_Y + 1, z);
				}
			}
		}
		throw new AssertionError("no open corridor found");
	}

	static Entity entity(net.minecraft.server.MinecraftServer server, String tag) {
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity.entityTags().contains(tag)) {
					return entity;
				}
			}
		}
		throw new AssertionError("no entity tagged " + tag);
	}

	static Vec3 tagged(TestServerContext server, String tag) {
		return server.computeOnServer(s -> entity(s, tag).position());
	}

	/** Turn the player on the spot to face a point. */
	static void lookAt(TestServerContext server, Vec3 target) {
		server.runCommand("execute as @a at @s run tp @s ~ ~ ~ facing " + target.x + " " + target.y + " " + target.z);
	}

	static String fmt(double value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value);
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
