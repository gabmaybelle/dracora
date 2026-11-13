package com.gabbymaybelle.dracora.dragon;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * The kinds of place a dragon can be born, each with its own colours. A dragon's colour is one RGB
 * number that tints its grey texture, so a colour here is all a new look costs.
 *
 * <p>End colours never overlap the Overworld's (one of the design rules), and a unit test holds
 * every palette to that. A bred dragon's colour is a blend ({@link Breeding}) and belongs to no
 * birthplace.
 */
public enum Birthplace {
	/** Snowy peaks and frozen plains: pale blues and whites. */
	SNOWY(0xDCEBF5, 0xA9CCE8, 0xF4F7FA),
	/** Jungles: greens and golds. */
	JUNGLE(0x4E9A3C, 0x2F6E2B, 0xD8B23A),
	/** Badlands and savannas: rusts and ochres. */
	WARM(0xB5562C, 0xC98A3A, 0x8A3B24),
	/** Forests and taigas: mossy greens and bark browns. */
	FOREST(0x5E7F3A, 0x7A5A36, 0x3F5E36),
	/** Oceans, beaches and rivers: sea blues and teals. */
	WATER(0x2E6F96, 0x3F9A9A, 0x24507A),
	/** Anywhere else in the Overworld: stone greys and earth. */
	PLAINS(0x8C8C84, 0x9C8466, 0x6E7072),
	/** The End: violets, deep blacks, a pale shimmer-lilac, chorus violet and void blue. */
	END(0x6B3FA0, 0x1E1626, 0xB59CD9, 0x8E5A9E, 0x23284A);

	private final List<Integer> colours;

	Birthplace(int... colours) {
		this.colours = Arrays.stream(colours).boxed().toList();
	}

	/** The colours a dragon born here can have. */
	public List<Integer> colours() {
		return colours;
	}

	/** One of this place's colours, picked by a random roll (any int). */
	public int pick(int roll) {
		return colours.get(Math.floorMod(roll, colours.size()));
	}

	/** The kind of place a biome is, for a dragon born in it. */
	public static Birthplace of(Holder<Biome> biome) {
		if (biome.is(BiomeTags.IS_END)) {
			return END;
		}
		if (biome.is(BiomeTags.SPAWNS_SNOW_FOXES) || biome.is(BiomeTags.SPAWNS_WHITE_RABBITS)) {
			return SNOWY;
		}
		if (biome.is(BiomeTags.IS_JUNGLE)) {
			return JUNGLE;
		}
		if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_SAVANNA)) {
			return WARM;
		}
		if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)) {
			return FOREST;
		}
		if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_RIVER)) {
			return WATER;
		}
		return PLAINS;
	}
}
