package com.gabbymaybelle.dracora.fabric;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import com.gabbymaybelle.dracora.client.EggTint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** The Fabric client entrypoint: the dragon's renderer (it uses vanilla's Ender Dragon model, so there is no model of its own to register) and the egg's tint. */
public class DracoraFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(Dragons.DRAGON.get(), DragonRenderer::new);
		BlockColorRegistry.register(EggTint.LAYERS, Eggs.BLOCK.get());
	}
}
