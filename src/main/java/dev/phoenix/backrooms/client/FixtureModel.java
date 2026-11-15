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
import net.minecraft.util.Mth;

/**
 * Weighted base, thin pole, pleated shade, and two jointed arms that hang like pull-cords until
 * it starts moving. UV layout matches fixture_texture() in tools/gen_textures.py.
 */
public class FixtureModel extends EntityModel<FixtureRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Backrooms.id("fixture"), "main");
	private final ModelPart shade;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public FixtureModel(ModelPart root) {
		super(root);
		this.shade = root.getChild("shade");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 23.0F, -4.0F, 8.0F, 1.0F, 8.0F), PartPose.ZERO);
		root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(0, 10).addBox(-1.0F, -13.0F, -1.0F, 2.0F, 36.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("shade", CubeListBuilder.create()
			.texOffs(16, 10).addBox(-5.0F, -4.0F, -5.0F, 10.0F, 4.0F, 10.0F)
			.texOffs(32, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 3.0F, 6.0F)
			.texOffs(16, 26).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -13.0F, 0.0F));
		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(8, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 22.0F, 1.0F),
			PartPose.offsetAndRotation(-2.5F, -11.0F, 0.0F, 0.0F, 0.0F, 0.06F));
		rightArm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(16, 32).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 10).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 22.0F, 1.0F),
			PartPose.offsetAndRotation(2.5F, -11.0F, 0.0F, 0.0F, 0.0F, -0.06F));
		leftArm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(FixtureRenderState state) {
		super.setupAnim(state);
		this.shade.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.shade.xRot = state.xRot * Mth.DEG_TO_RAD;
		float speed = Math.min(state.walkAnimationSpeed, 1.0F);
		// arms swing up and reach forward as soon as it starts to move; perfectly still while watched
		float reach = -1.35F * Math.min(1.0F, speed * 3.0F);
		float swing = Mth.cos(state.walkAnimationPos * 0.5F) * 0.15F * speed;
		float idle = state.watched ? 0.0F : Mth.sin(state.ageInTicks * 0.07F) * 0.03F;
		this.rightArm.xRot = reach + swing + idle;
		this.leftArm.xRot = reach - swing - idle;
	}
}
