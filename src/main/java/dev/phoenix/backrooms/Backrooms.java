package dev.phoenix.backrooms;

import dev.phoenix.backrooms.registry.ModBlocks;
import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.registry.ModItems;
import dev.phoenix.backrooms.registry.ModSounds;
import dev.phoenix.backrooms.transit.NoclipHandler;
import dev.phoenix.backrooms.transit.Transit;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backrooms implements ModInitializer {
	public static final String MOD_ID = "backrooms";
	public static final Logger LOGGER = LoggerFactory.getLogger("Backrooms");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		BackroomsConfig.load();
		ModSounds.init();
		ModBlocks.init();
		ModItems.init();
		ModDimensions.init();
		Transit.init();
		NoclipHandler.init();
		LOGGER.info("Backrooms: Thresholds initialised");
	}
}
