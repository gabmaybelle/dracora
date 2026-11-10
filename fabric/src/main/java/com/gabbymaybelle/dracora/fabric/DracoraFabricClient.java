package com.gabbymaybelle.dracora.fabric;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.client.DragonModel;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** The Fabric client entrypoint: the dragon's model and renderer, in {@code common}'s {@code client} package. */
public class DracoraFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(DragonModel.LAYER, DragonModel::createBodyLayer);
		EntityRendererRegistry.register(Dragons.DRAGON.get(), DragonRenderer::new);
	}
}
