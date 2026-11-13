package com.gabbymaybelle.dracora.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BreedingTest {
	@Test
	void blendMixesEachChannelHalfAndHalf() {
		assertEquals(0x808080, Breeding.blend(0xFFFFFF, 0x000000));
		assertEquals(0x800080, Breeding.blend(0xFF0000, 0x0000FF));
		assertEquals(0x4E9A3C, Breeding.blend(0x4E9A3C, 0x4E9A3C));
		assertEquals(Breeding.blend(0xDCEBF5, 0xB5562C), Breeding.blend(0xB5562C, 0xDCEBF5));
	}

	@Test
	void oneEggInTwentyIsAMutation() {
		int mutations = 0;
		for (int roll = 0; roll < 2000; roll++) {
			int colour = Breeding.eggColour(0xDCEBF5, 0x4E9A3C, roll, roll / 20);
			if (Breeding.isMutation(colour)) {
				mutations++;
			} else {
				assertEquals(Breeding.blend(0xDCEBF5, 0x4E9A3C), colour);
			}
		}
		assertEquals(100, mutations);
	}

	@Test
	void bothRareColoursCanAppear() {
		assertEquals(Breeding.PEARL, Breeding.eggColour(0, 0, 0, 0));
		assertEquals(Breeding.OBSIDIAN, Breeding.eggColour(0, 0, 0, 1));
	}

	@Test
	void rareColoursBelongToNoBirthplace() {
		for (Birthplace place : Birthplace.values()) {
			for (int colour : Breeding.MUTATIONS) {
				assertFalse(place.colours().contains(colour), place + " has the rare colour " + Integer.toHexString(colour));
			}
		}
		assertTrue(Breeding.MUTATIONS.size() == 2);
	}
}
