package dev.phoenix.backrooms.client;

import dev.phoenix.backrooms.Backrooms;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** A stubby body and two flat wings. UV layout matches moth_texture() in tools/gen_textures.py. */
public class TubeMothModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Backrooms.id("tube_moth"), "main");
	private final ModelPart body;
	private final ModelPart rightWing;
	private final ModelPart leftWing;

	public TubeMothModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.rightWing = this.body.getChild("right_wing");
		this.leftWing = this.body.getChild("left_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition body = mesh.getRoot().addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
		body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 8).addBox(-6.0F, 0.0F, -2.5F, 6.0F, 0.0F, 5.0F), PartPose.offset(-1.0F, -1.0F, 0.0F));
		body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 8).mirror().addBox(0.0F, 0.0F, -2.5F, 6.0F, 0.0F, 5.0F), PartPose.offset(1.0F, -1.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float flap = Mth.cos(state.ageInTicks * 1.3F) * 0.9F;
		this.rightWing.zRot = flap;
		this.leftWing.zRot = -flap;
		this.body.y += Mth.sin(state.ageInTicks * 0.3F) * 0.6F;
		this.body.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
	}
}
