package dev.phoenix.backrooms.client;

import dev.phoenix.backrooms.Backrooms;
import dev.phoenix.backrooms.entity.TubeMothEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class TubeMothRenderer extends MobRenderer<TubeMothEntity, LivingEntityRenderState, TubeMothModel> {
	private static final Identifier TEXTURE = Backrooms.id("textures/entity/tube_moth.png");

	public TubeMothRenderer(EntityRendererProvider.Context context) {
		super(context, new TubeMothModel(context.bakeLayer(TubeMothModel.LAYER)), 0.15F);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
