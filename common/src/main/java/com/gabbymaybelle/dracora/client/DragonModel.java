package com.gabbymaybelle.dracora.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Dracora's dragon is the Ender Dragon's own model: the parts are vanilla's, baked from the
 * game's Ender Dragon layer (so nothing of its shape is copied here, and a resource pack that
 * reshapes the Ender Dragon reshapes these too). What is Dracora's is how they move. The Ender
 * Dragon only ever flies; this one also stands, walks, sits with its wings folded, and looks
 * where it is looking.
 *
 * <p>The vanilla parts, in model pixels with y counting down: a {@code body} that carries the
 * wings (arm and tip) and the four legs (leg, shin, foot); and, hung from the root, the
 * {@code head} with its {@code jaw}, five {@code neck} and twelve {@code tail} segments, each ten
 * long. Neck and tail are chains: each segment is placed at the end of the one before, as vanilla
 * does it. {@link DragonRenderer} scales the whole model down.
 */
public class DragonModel extends EntityModel<DragonRenderState> {
	private static final int NECK_PARTS = 5;
	private static final int TAIL_PARTS = 12;
	private static final float SEGMENT = 10.0F;
	private static final float DEG = Mth.DEG_TO_RAD;

	/** How far the whole model is lifted so the feet of a standing dragon meet the ground, and how much less when it sits. */
	private static final float STAND_LIFT = 40.4F;
	private static final float SIT_LIFT = 26.0F;
	/** The body sits behind the model's origin in vanilla; this brings its middle over the dragon's own position. */
	private static final float BODY_FORWARD = 24.0F;
	/** A hatchling's head, next to its body's size. */
	private static final float HATCHLING_HEAD = 1.3F;

	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart[] neck = new ModelPart[NECK_PARTS];
	private final ModelPart[] tail = new ModelPart[TAIL_PARTS];
	private final ModelPart body;
	private final ModelPart leftWing;
	private final ModelPart leftWingTip;
	private final ModelPart rightWing;
	private final ModelPart rightWingTip;
	private final Leg leftFront;
	private final Leg rightFront;
	private final Leg leftHind;
	private final Leg rightHind;

	/** One leg: thigh, shin and foot, each hung from the one before. */
	private record Leg(ModelPart leg, ModelPart shin, ModelPart foot) {
		static Leg of(ModelPart body, String name) {
			ModelPart leg = body.getChild(name);
			ModelPart shin = leg.getChild(name + "_tip");
			return new Leg(leg, shin, shin.getChild(name.replace("_leg", "_foot")));
		}

		/** Sets the three joints: the thigh's own angle, then the shin's and the foot's against the part above. */
		void pose(float thigh, float knee, float ankle) {
			leg.xRot = thigh;
			shin.xRot = knee;
			foot.xRot = ankle;
		}
	}

	/** {@code root} is the baked vanilla Ender Dragon layer. */
	public DragonModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.jaw = this.head.getChild("jaw");
		for (int i = 0; i < NECK_PARTS; i++) {
			this.neck[i] = root.getChild("neck" + i);
		}
		for (int i = 0; i < TAIL_PARTS; i++) {
			this.tail[i] = root.getChild("tail" + i);
		}
		this.body = root.getChild("body");
		this.leftWing = this.body.getChild("left_wing");
		this.leftWingTip = this.leftWing.getChild("left_wing_tip");
		this.rightWing = this.body.getChild("right_wing");
		this.rightWingTip = this.rightWing.getChild("right_wing_tip");
		this.leftFront = Leg.of(this.body, "left_front_leg");
		this.rightFront = Leg.of(this.body, "right_front_leg");
		this.leftHind = Leg.of(this.body, "left_hind_leg");
		this.rightHind = Leg.of(this.body, "right_hind_leg");
	}

	@Override
	public void setupAnim(DragonRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float beat = age * 0.3F;
		boolean flying = state.flying;

		this.root.z = -BODY_FORWARD;
		if (flying) {
			// The body rides up and down a little with each wing beat.
			this.root.y = -STAND_LIFT + Mth.sin(beat - 1.0F) * 3.0F;
		} else {
			this.root.y = -(state.sitting ? SIT_LIFT : STAND_LIFT);
		}

		legs(state, flying);
		wings(age, beat, flying);
		neckAndHead(state, age, beat, flying);
		tail(state, age, beat, flying);
	}

	private void legs(DragonRenderState state, boolean flying) {
		if (flying) {
			// Tucked up and back, as the Ender Dragon carries them.
			this.leftFront.pose(1.3F, -0.5F, 0.75F);
			this.rightFront.pose(1.3F, -0.5F, 0.75F);
			this.leftHind.pose(1.0F, 0.5F, 0.75F);
			this.rightHind.pose(1.0F, 0.5F, 0.75F);
		} else if (state.sitting) {
			// Folded under the lowered body, feet flat.
			this.leftFront.pose(-0.9F, 1.8F, -0.9F);
			this.rightFront.pose(-0.9F, 1.8F, -0.9F);
			this.leftHind.pose(-1.25F, 2.2F, -0.95F);
			this.rightHind.pose(-1.25F, 2.2F, -0.95F);
		} else {
			// Standing on four bent legs; diagonal pairs swing together in a walk.
			float walk = state.walkAnimationPos * 0.5F;
			float swing = Math.min(state.walkAnimationSpeed, 1.0F) * 0.45F;
			float a = Mth.cos(walk) * swing;
			float b = Mth.cos(walk + Mth.PI) * swing;
			this.leftFront.pose(-0.35F + a, 0.7F, -0.35F - a);
			this.rightFront.pose(-0.35F + b, 0.7F, -0.35F - b);
			this.leftHind.pose(-0.9F + b, 1.65F, -0.75F - b);
			this.rightHind.pose(-0.9F + a, 1.65F, -0.75F - a);
		}
	}

	private void wings(float age, float beat, boolean flying) {
		if (flying) {
			// The Ender Dragon's beat: the arm sweeps and the tip follows a little behind it.
			this.leftWing.xRot = 0.125F - Mth.cos(beat) * 0.2F;
			this.leftWing.yRot = -0.25F;
			this.leftWing.zRot = -(Mth.sin(beat) + 0.125F) * 0.8F;
			this.leftWingTip.zRot = (Mth.sin(beat + 2.0F) + 0.5F) * 0.75F;
		} else {
			// Folded: the arm is raised and swept back along the body, and the tip doubles back
			// down beside it. A slow breath moves them a little.
			this.leftWing.xRot = 0.0F;
			this.leftWing.yRot = -0.9F;
			this.leftWing.zRot = -1.0F + Mth.sin(age * 0.06F) * 0.02F;
			this.leftWingTip.zRot = 2.55F;
		}
		this.rightWing.xRot = this.leftWing.xRot;
		this.rightWing.yRot = -this.leftWing.yRot;
		this.rightWing.zRot = -this.leftWing.zRot;
		this.rightWingTip.zRot = -this.leftWingTip.zRot;
	}

	/** The neck is a chain from the shoulders to the head. On the ground it rises in a curve; in the air it stretches forward and ripples with the beat. */
	private void neckAndHead(DragonRenderState state, float age, float beat, boolean flying) {
		float yaw = Mth.clamp(state.yRot, -70.0F, 70.0F) * DEG;
		float pitch = Mth.clamp(state.xRot, -45.0F, 45.0F) * DEG;
		float x = this.neck[0].x;
		float y = this.neck[0].y;
		float z = this.neck[0].z;
		float breath = Mth.sin(age * 0.06F) * 0.03F;
		for (int i = 0; i < NECK_PARTS; i++) {
			ModelPart part = this.neck[i];
			float rise;
			if (flying) {
				rise = Mth.cos(i * 0.45F + beat) * 0.12F;
			} else {
				// Steep at the shoulders, levelling out toward the head.
				rise = -0.75F + i * 0.17F + breath;
			}
			part.xRot = rise;
			part.yRot = yaw * (i + 1) / (NECK_PARTS + 1.0F);
			part.x = x;
			part.y = y;
			part.z = z;
			x -= Mth.sin(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
			y += Mth.sin(part.xRot) * SEGMENT;
			z -= Mth.cos(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
		}
		this.head.x = x;
		this.head.y = y;
		this.head.z = z;
		this.head.yRot = yaw;
		this.head.xRot = pitch * 0.8F + (flying ? 0.0F : 0.15F);
		this.jaw.xRot = flying ? (Mth.sin(beat) + 1.0F) * 0.1F : 0.04F + breath;
		float headSize = state.hatchling ? HATCHLING_HEAD : 1.0F;
		this.head.xScale = headSize;
		this.head.yScale = headSize;
		this.head.zScale = headSize;
	}

	/** The tail is a chain behind the body. On the ground it curves down and trails; in the air it streams out and ripples. It sways, each segment a little after the one before. */
	private void tail(DragonRenderState state, float age, float beat, boolean flying) {
		float x = this.tail[0].x;
		float y = this.tail[0].y;
		float z = this.tail[0].z;
		float ripple = 0.0F;
		for (int i = 0; i < TAIL_PARTS; i++) {
			ModelPart part = this.tail[i];
			float droop;
			if (flying) {
				ripple += Mth.sin(i * 0.45F + beat) * 0.05F;
				droop = ripple;
			} else if (state.sitting) {
				// Less far to fall when the body is down.
				droop = i < 6 ? 0.45F : Math.max(0.0F, 0.45F - (i - 5) * 0.12F);
			} else {
				droop = i < 8 ? 0.55F : Math.max(0.0F, 0.55F - (i - 7) * 0.16F);
			}
			float sway = Mth.sin(age * 0.07F - i * 0.35F) * 0.012F * (i + 1);
			// Vanilla's tail segments face backward: half a turn, plus the sway.
			part.yRot = Mth.PI + sway;
			part.xRot = droop;
			part.x = x;
			part.y = y;
			part.z = z;
			y += Mth.sin(part.xRot) * SEGMENT;
			z -= Mth.cos(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
			x -= Mth.sin(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
		}
	}
}
