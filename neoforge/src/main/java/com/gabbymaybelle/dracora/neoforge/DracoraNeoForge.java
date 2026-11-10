package com.gabbymaybelle.dracora.neoforge;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entrypoint: everything happens in {@code common}; this wires the registry events. */
@Mod(Dracora.MOD_ID)
public class DracoraNeoForge {
	public DracoraNeoForge(IEventBus modBus) {
		NeoForgePlatform platform = (NeoForgePlatform) Services.PLATFORM;
		// init() queues the registrations; the registry events hand them to NeoForge.
		Dracora.init();
		modBus.addListener(RegisterEvent.class, platform::onRegister);
		modBus.addListener(EntityAttributeCreationEvent.class, platform::onAttributes);
		modBus.addListener(RegisterSpawnPlacementsEvent.class, platform::onSpawnPlacements);
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, platform::onCreativeTab);
	}
}
