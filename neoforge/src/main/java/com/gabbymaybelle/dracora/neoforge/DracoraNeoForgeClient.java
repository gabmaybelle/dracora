package com.gabbymaybelle.dracora.neoforge;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import com.gabbymaybelle.dracora.client.EggTint;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** The NeoForge client entrypoint (loaded only on players' computers): the dragon's renderer (it uses vanilla's Ender Dragon model, so there is no model of its own to register) and the egg's tint. */
@Mod(value = Dracora.MOD_ID, dist = Dist.CLIENT)
public class DracoraNeoForgeClient {
	public DracoraNeoForgeClient(IEventBus modBus) {
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
				event.registerEntityRenderer(Dragons.DRAGON.get(), DragonRenderer::new));
		modBus.addListener(RegisterColorHandlersEvent.BlockTintSources.class, event ->
				event.register(EggTint.LAYERS, Eggs.BLOCK.get()));
	}
}
