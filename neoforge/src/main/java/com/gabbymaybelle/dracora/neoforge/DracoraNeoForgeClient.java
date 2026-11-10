package com.gabbymaybelle.dracora.neoforge;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.client.DragonModel;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The NeoForge client entrypoint (loaded only on players' computers): the dragon's model and renderer. */
@Mod(value = Dracora.MOD_ID, dist = Dist.CLIENT)
public class DracoraNeoForgeClient {
	public DracoraNeoForgeClient(IEventBus modBus) {
		modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event ->
				event.registerLayerDefinition(DragonModel.LAYER, DragonModel::createBodyLayer));
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
				event.registerEntityRenderer(Dragons.DRAGON.get(), DragonRenderer::new));
	}
}
