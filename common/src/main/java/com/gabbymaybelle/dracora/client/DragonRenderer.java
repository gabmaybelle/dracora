package com.gabbymaybelle.dracora.client;

import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Stage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Draws a dragon as a small Ender Dragon: vanilla's model (posed by {@link DragonModel}), a grey
 * copy of vanilla's texture tinted by the dragon's own colour ({@link TintableDragonTexture}),
 * and vanilla's glowing eyes on top. Every colour uses the same picture.
 */
public class DragonRenderer extends MobRenderer<Dragon, DragonRenderState, DragonModel> {
	private static final RenderType EYES = RenderTypes.eyes(Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png"));
	/** The Ender Dragon is about seventeen blocks long; a grown Dracora dragon is half that in every direction. */
	public static final float MODEL_SCALE = 0.5F;

	public DragonRenderer(EntityRendererProvider.Context context) {
		super(context, new DragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON)), 1.4F);
		// Renderers are made again whenever resources load, so the grey copy is always of the
		// Ender Dragon texture in use now.
		Minecraft.getInstance().getTextureManager().registerAndLoad(TintableDragonTexture.ID, new TintableDragonTexture());
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(DragonRenderState state) {
		return TintableDragonTexture.ID;
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

	@Override
	protected float getShadowRadius(DragonRenderState state) {
		return super.getShadowRadius(state) * state.ageScale;
	}

	/** The vanilla model scaled down to a grown dragon's size, then to the size of the dragon's stage. */
	@Override
	protected void scale(DragonRenderState state, PoseStack poseStack) {
		float scale = MODEL_SCALE * state.ageScale;
		poseStack.scale(scale, scale, scale);
	}
}
