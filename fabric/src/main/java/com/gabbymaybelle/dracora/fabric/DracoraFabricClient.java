package com.gabbymaybelle.dracora.fabric;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.client.DragonModel;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import com.gabbymaybelle.dracora.client.EggTint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** The Fabric client entrypoint: the dragon's model and renderer and the egg's tint, in {@code common}'s {@code client} package. */
public class DracoraFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(DragonModel.LAYER, DragonModel::createBodyLayer);
		EntityRendererRegistry.register(Dragons.DRAGON.get(), DragonRenderer::new);
		BlockColorRegistry.register(EggTint.LAYERS, Eggs.BLOCK.get());
	}
}
