package dev.phoenix.backrooms.client;

import dev.phoenix.backrooms.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class BackroomsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(FixtureModel.LAYER, FixtureModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(TubeMothModel.LAYER, TubeMothModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.FIXTURE, FixtureRenderer::new);
		EntityRendererRegistry.register(ModEntities.TUBE_MOTH, TubeMothRenderer::new);
	}
}
