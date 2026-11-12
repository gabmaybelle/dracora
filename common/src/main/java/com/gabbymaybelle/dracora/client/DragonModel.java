package com.gabbymaybelle.dracora.client;

import com.gabbymaybelle.dracora.Dracora;
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
 * The dragon, built from boxes the way vanilla builds its mobs: body, a neck and head that follow
 * the player's gaze, a three-part tail, four legs and two-part wings with flat membranes. Wings
 * fold back along the sides on the ground and beat in the air. All the animation is a little
 * maths in {@link #setupAnim}, so there is no animation library to depend on.
 *
 * <p>Sizes are in model pixels; {@link DragonRenderer} scales the whole model up. The texture is
 * 128 by 128 and grey, tinted by the dragon's colour. The texture offsets below place each box on
 * that sheet; {@code tools/draw-placeholders.py} paints the placeholder from the same layout, so
 * change them only together.
 */
public class DragonModel extends EntityModel<DragonRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Dracora.id("dragon"), "main");
	private static final float DEG = Mth.DEG_TO_RAD;
	/** How far the body sinks when sitting, in model pixels. */
	private static final float SIT_DROP = 7.0F;
	/** A hatchling is the adult model made small, with its head this much bigger for its body. */
	private static final float HATCHLING_HEAD = 1.5F;
	/** Folded wings: the arm lifts from the shoulder (negative is up: model y counts down) and the tip folds back down beside it. */
	private static final float WING_FOLD_ARM = -1.1F;
	private static final float WING_FOLD_TIP = 2.6F;

	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart tail1;
	private final ModelPart tail2;
	private final ModelPart tail3;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftWing;
	private final ModelPart leftWingTip;
	private final ModelPart rightWing;
	private final ModelPart rightWingTip;

	public DragonModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.neck = this.body.getChild("neck");
		this.head = this.neck.getChild("head");
		this.tail1 = this.body.getChild("tail1");
		this.tail2 = this.tail1.getChild("tail2");
		this.tail3 = this.tail2.getChild("tail3");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftWing = this.body.getChild("left_wing");
		this.leftWingTip = this.leftWing.getChild("left_wing_tip");
		this.rightWing = this.body.getChild("right_wing");
		this.rightWingTip = this.rightWing.getChild("right_wing_tip");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// The body's centre is 14 px up (y 10 counts down from the top of a 24 px frame).
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -5.0F, -10.0F, 12.0F, 10.0F, 20.0F),
				PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition neck = body.addOrReplaceChild("neck",
				CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -3.0F, -8.0F, 6.0F, 6.0F, 8.0F),
				PartPose.offsetAndRotation(0.0F, -2.0F, -9.0F, -0.6F, 0.0F, 0.0F));
		PartDefinition head = neck.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 7.0F, 7.0F)
						.texOffs(30, 30).addBox(-2.5F, -1.0F, -12.0F, 5.0F, 4.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, 0.6F, 0.0F, 0.0F));
		head.addOrReplaceChild("left_horn",
				CubeListBuilder.create().texOffs(50, 30).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
				PartPose.offsetAndRotation(2.5F, -3.5F, -1.5F, -0.7F, 0.0F, 0.15F));
		head.addOrReplaceChild("right_horn",
				CubeListBuilder.create().texOffs(50, 30).mirror().addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
				PartPose.offsetAndRotation(-2.5F, -3.5F, -1.5F, -0.7F, 0.0F, -0.15F));

		PartDefinition tail1 = body.addOrReplaceChild("tail1",
				CubeListBuilder.create().texOffs(64, 14).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 10.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 9.0F, -0.15F, 0.0F, 0.0F));
		PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
				CubeListBuilder.create().texOffs(96, 0).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 10.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 9.5F, 0.1F, 0.0F, 0.0F));
		tail2.addOrReplaceChild("tail3",
				CubeListBuilder.create().texOffs(96, 14).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 10.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 9.5F, 0.1F, 0.0F, 0.0F));

		// Legs hang from the root, so they stay planted while the body turns its head.
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 44).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F);
		CubeListBuilder mirroredLeg = CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F);
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(4.0F, 14.0F, -6.0F));
		root.addOrReplaceChild("right_front_leg", mirroredLeg, PartPose.offset(-4.0F, 14.0F, -6.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(4.0F, 14.0F, 7.0F));
		root.addOrReplaceChild("right_hind_leg", mirroredLeg, PartPose.offset(-4.0F, 14.0F, 7.0F));

		// Each wing: a bony arm along its front edge and a flat membrane trailing back from it.
		PartDefinition leftWing = body.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(0, 60).addBox(0.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F)
						.texOffs(0, 64).addBox(0.0F, 0.0F, 0.0F, 16.0F, 0.0F, 14.0F),
				PartPose.offset(5.0F, -4.0F, -6.0F));
		leftWing.addOrReplaceChild("left_wing_tip",
				CubeListBuilder.create().texOffs(40, 60).addBox(0.0F, -1.0F, -1.0F, 14.0F, 2.0F, 2.0F)
						.texOffs(64, 64).addBox(0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 12.0F),
				PartPose.offset(16.0F, 0.0F, 0.0F));
		PartDefinition rightWing = body.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(0, 60).mirror().addBox(-16.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F)
						.texOffs(0, 64).mirror().addBox(-16.0F, 0.0F, 0.0F, 16.0F, 0.0F, 14.0F),
				PartPose.offset(-5.0F, -4.0F, -6.0F));
		rightWing.addOrReplaceChild("right_wing_tip",
				CubeListBuilder.create().texOffs(40, 60).mirror().addBox(-14.0F, -1.0F, -1.0F, 14.0F, 2.0F, 2.0F)
						.texOffs(64, 64).mirror().addBox(-14.0F, 0.0F, 0.0F, 14.0F, 0.0F, 12.0F),
				PartPose.offset(-16.0F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(DragonRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;

		// The neck and head share the turn toward whatever the dragon looks at.
		float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * DEG;
		this.neck.yRot = yaw * 0.5F;
		this.head.yRot = yaw * 0.5F;
		this.head.xRot += state.xRot * DEG * 0.7F;
		float headSize = state.hatchling ? HATCHLING_HEAD : 1.0F;
		this.head.xScale = headSize;
		this.head.yScale = headSize;
		this.head.zScale = headSize;
		// A slow breath.
		this.neck.xRot += Mth.sin(age * 0.06F) * 0.04F;

		// The tail sways, each part a little behind the one before.
		this.tail1.yRot = Mth.sin(age * 0.08F) * 0.12F;
		this.tail2.yRot = Mth.sin(age * 0.08F - 0.8F) * 0.16F;
		this.tail3.yRot = Mth.sin(age * 0.08F - 1.6F) * 0.2F;

		if (state.sitting) {
			// Legs folded under and the body lowered onto them.
			this.body.y += SIT_DROP;
			this.leftFrontLeg.y += SIT_DROP;
			this.rightFrontLeg.y += SIT_DROP;
			this.leftHindLeg.y += SIT_DROP;
			this.rightHindLeg.y += SIT_DROP;
			this.leftFrontLeg.xRot = -1.4F;
			this.rightFrontLeg.xRot = -1.4F;
			this.leftHindLeg.xRot = 1.4F;
			this.rightHindLeg.xRot = 1.4F;
		} else {
			// The usual four-legged walk: diagonal pairs swing together.
			float walk = state.walkAnimationPos * 0.6662F;
			float swing = Math.min(state.walkAnimationSpeed, 1.0F) * 1.2F;
			this.leftFrontLeg.xRot = Mth.cos(walk) * swing;
			this.rightHindLeg.xRot = Mth.cos(walk) * swing;
			this.rightFrontLeg.xRot = Mth.cos(walk + Mth.PI) * swing;
			this.leftHindLeg.xRot = Mth.cos(walk + Mth.PI) * swing;
		}

		if (state.flying) {
			// Wings spread and beat; the tips trail the arms slightly.
			float beat = age * 0.35F;
			this.leftWing.zRot = Mth.cos(beat) * 0.7F;
			this.leftWingTip.zRot = Mth.cos(beat - 0.6F) * 0.5F;
		} else {
			// Folded like a resting dragon's: the arm lifts up from the shoulder and the tip folds
			// back down beside it, so both membranes stand along the flank. One rotation axis per
			// part, so the pose doesn't depend on the order rotations are applied in.
			this.leftWing.zRot = WING_FOLD_ARM + Mth.sin(age * 0.06F) * 0.02F;
			this.leftWingTip.zRot = WING_FOLD_TIP;
		}
		this.rightWing.zRot = -this.leftWing.zRot;
		this.rightWingTip.zRot = -this.leftWingTip.zRot;
	}
}
