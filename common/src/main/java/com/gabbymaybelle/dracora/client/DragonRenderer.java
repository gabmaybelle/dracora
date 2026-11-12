package com.gabbymaybelle.dracora.client;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Stage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Draws a dragon: one grey texture, tinted by the dragon's own colour, so every colour uses the
 * same picture.
 */
public class DragonRenderer extends MobRenderer<Dragon, DragonRenderState, DragonModel> {
	private static final Identifier TEXTURE = Dracora.id("textures/entity/dragon/dragon.png");
	/** The model is drawn in pixels; this makes an adult about four blocks from nose to tail. */
	private static final float MODEL_SCALE = 1.5F;

	public DragonRenderer(EntityRendererProvider.Context context) {
		super(context, new DragonModel(context.bakeLayer(DragonModel.LAYER)), 1.2F);
	}

	@Override
	public Identifier getTextureLocation(DragonRenderState state) {
		return TEXTURE;
	}

	@Override
	public DragonRenderState createRenderState() {
		return new DragonRenderState();
	}

	@Override
	public void extractRenderState(Dragon dragon, DragonRenderState state, float partialTicks) {
		super.extractRenderState(dragon, state, partialTicks);
		state.colour = dragon.getColour();
		state.flying = dragon.isFlying();
		state.sitting = dragon.isInSittingPose();
		state.hatchling = dragon.getStage() == Stage.HATCHLING;
	}

	@Override
	protected int getModelTint(DragonRenderState state) {
		return ARGB.opaque(state.colour);
	}

	/** The whole model scaled up, then down again to the size of the dragon's stage. */
	@Override
	protected void scale(DragonRenderState state, PoseStack poseStack) {
		float scale = MODEL_SCALE * state.ageScale;
		poseStack.scale(scale, scale, scale);
	}
}
