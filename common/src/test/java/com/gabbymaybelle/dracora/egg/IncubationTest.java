package com.gabbymaybelle.dracora.egg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IncubationTest {
	@Test
	void anEggCracksAtEachThirdAndHatchesAtTheEnd() {
		int third = Incubation.HATCH_WARMTH / 3;
		assertEquals(0, Incubation.stage(0));
		assertEquals(0, Incubation.stage(third - 1));
		assertEquals(1, Incubation.stage(third));
		assertEquals(1, Incubation.stage(2 * third - 1));
		assertEquals(2, Incubation.stage(2 * third));
		assertEquals(2, Incubation.stage(Incubation.HATCH_WARMTH - 1));
		assertEquals(Incubation.HATCHED, Incubation.stage(Incubation.HATCH_WARMTH));
	}

	@Test
	void theStageNeverRunsPastHatchedOrBelowWhole() {
		assertEquals(Incubation.HATCHED, Incubation.stage(Integer.MAX_VALUE));
		assertEquals(0, Incubation.stage(-500));
	}

	@Test
	void warmthChecksLandExactlyOnEachStage() {
		assertTrue(Incubation.HATCH_WARMTH % (Incubation.HATCHED * Incubation.CHECK_EVERY) == 0,
				"each crack stage should be a whole number of warmth checks");
	}
}
