package com.gabbymaybelle.dracora.fabric;

import com.gabbymaybelle.dracora.Dracora;
import net.fabricmc.api.ModInitializer;

/** The Fabric entrypoint: everything happens in {@code common}. */
public class DracoraFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Dracora.init();
	}
}
