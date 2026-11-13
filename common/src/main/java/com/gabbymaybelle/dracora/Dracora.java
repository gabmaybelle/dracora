package com.gabbymaybelle.dracora;

import com.gabbymaybelle.dracora.platform.Services;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dracora: rare dragons, raised from an egg, coloured by where they were born. The loader
 * entrypoints ({@code fabric} and {@code neoforge}) call {@link #init()} once; everything that
 * doesn't care which loader it runs on lives here in {@code common}. {@link Dragons} registers
 * the dragon and its spawn egg, and {@link Eggs} the egg it hatches from;
 * {@link com.gabbymaybelle.dracora.dragon.Dragon} is the dragon itself.
 */
public final class Dracora {
	public static final String MOD_ID = "dracora";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Dracora() {
	}

	public static void init() {
		Config.load();
		Dragons.register();
		Eggs.register();
		NestSpread.register();
		NestStructure.register();
		LOGGER.info("Dracora: ready on {}", Services.PLATFORM.name());
	}

	/** An id in Dracora's own namespace. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
