package dev.phoenix.backrooms.test;

import dev.phoenix.backrooms.registry.ModBlocks;
import dev.phoenix.backrooms.registry.ModDimensions;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scripted playthrough in a real client. Every check logs a [BRTEST] line. */
public class BackroomsClientGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("BRTEST");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			pass("singleplayer world created");
			blankDimension(context, world.getServer(), world.getConnection());
		}
		pass("ALL CHECKS COMPLETE");
	}

	private void blankDimension(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runCommand("execute in backrooms:level_0 run tp @a 8.5 33 8.5");
		waitForDimension(context, ModDimensions.LEVEL_0);
		connection.waitForChunksRender();
		context.waitTicks(20);
		expectBlock(server, ModDimensions.LEVEL_0, new BlockPos(8, 32, 8), ModBlocks.DAMP_CARPET);
		shot(context, "cp1_blank_dimension");
		server.runCommand("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		waitForDimension(context, Level.OVERWORLD);
		pass("travelled to backrooms:level_0 and back to the overworld");
	}

	// ------------------------------------------------------------------ helpers
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

	static void waitForDimension(ClientGameTestContext context, ResourceKey<Level> dimension) {
		context.waitFor(mc -> mc.level != null && mc.player != null && mc.level.dimension() == dimension, 400);
		pass("client is in " + dimension.identifier());
	}

	static void expectBlock(TestServerContext server, ResourceKey<Level> dimension, BlockPos pos, Block block) {
		Block found = server.computeOnServer(s -> s.getLevel(dimension).getBlockState(pos).getBlock());
		check(found == block, "block at " + pos.toShortString() + " in " + dimension.identifier() + " is " + block.getName().getString() + " (found " + found.getName().getString() + ")");
	}

	static void shot(ClientGameTestContext context, String name) {
		Path path = context.takeScreenshot(name);
		LOG.info("[BRTEST] SCREENSHOT {}", path);
	}
}
