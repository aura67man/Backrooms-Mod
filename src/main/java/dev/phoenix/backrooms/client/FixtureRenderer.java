package dev.phoenix.backrooms.client;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.entity.FixtureEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FixtureRenderer extends MobRenderer<FixtureEntity, FixtureRenderState, FixtureModel> {
	private static final Identifier TEXTURE = Backrooms.id("textures/entity/fixture.png");

	public FixtureRenderer(EntityRendererProvider.Context context) {
		super(context, new FixtureModel(context.bakeLayer(FixtureModel.LAYER)), 0.4F);
	}

	@Override
	public Identifier getTextureLocation(FixtureRenderState state) {
		return TEXTURE;
	}

	@Override
	public FixtureRenderState createRenderState() {
		return new FixtureRenderState();
	}

	@Override
	public void extractRenderState(FixtureEntity entity, FixtureRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.watched = entity.isWatched();
	}
}
