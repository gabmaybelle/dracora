package com.gabbymaybelle.dracora.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The palettes keep to the design rules: End colours are the End's alone, and every colour can be born. */
class BirthplaceTest {
	@Test
	void endColoursNeverAppearInTheOverworld() {
		Set<Integer> overworld = new HashSet<>();
		for (Birthplace place : Birthplace.values()) {
			if (place != Birthplace.END) {
				overworld.addAll(place.colours());
			}
		}
		for (int colour : Birthplace.END.colours()) {
			assertFalse(overworld.contains(colour), String.format("End colour %06X is also an Overworld colour", colour));
		}
	}

	@Test
	void everyPlaceHasColoursAndNoneIsTheUnsetMarker() {
		for (Birthplace place : Birthplace.values()) {
			assertFalse(place.colours().isEmpty(), place + " has no colours");
			for (int colour : place.colours()) {
				assertNotEquals(Dragon.NO_COLOUR, colour, place + " uses the unset marker as a colour");
				assertEquals(colour & 0xFFFFFF, colour, place + " colour has bits above RGB");
			}
		}
	}

	@Test
	void anyRollPicksFromThePlacesOwnPalette() {
		for (Birthplace place : Birthplace.values()) {
			for (int roll : new int[] { 0, 1, 2, 3, -1, -7, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
				assertTrue(place.colours().contains(place.pick(roll)), place + " roll " + roll);
			}
		}
	}

	@Test
	void everyColourInAPaletteCanBePicked() {
		for (Birthplace place : Birthplace.values()) {
			Set<Integer> picked = new HashSet<>();
			for (int roll = 0; roll < place.colours().size(); roll++) {
				picked.add(place.pick(roll));
			}
			assertEquals(Set.copyOf(place.colours()), picked, place.toString());
		}
	}
}
