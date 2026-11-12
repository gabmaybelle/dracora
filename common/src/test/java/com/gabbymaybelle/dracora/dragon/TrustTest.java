package com.gabbymaybelle.dracora.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrustTest {
	private static final int LOOKS_PER_VISIT = Trust.VISIT_TICKS / Trust.WATCH_EVERY;

	/** Stays calm for a whole visit's worth of looks on one day; says whether the last look completed a visit. */
	private static boolean visit(Trust trust, long day) {
		boolean counted = false;
		for (int i = 0; i < LOOKS_PER_VISIT; i++) {
			counted = trust.calm(Trust.WATCH_EVERY, day);
		}
		return counted;
	}

	@Test
	void aVisitTakesHalfAMinuteOfCalm() {
		Trust trust = new Trust();
		for (int i = 0; i < LOOKS_PER_VISIT - 1; i++) {
			assertFalse(trust.calm(Trust.WATCH_EVERY, 0), "look " + i + " should not complete a visit");
		}
		assertTrue(trust.calm(Trust.WATCH_EVERY, 0), "the last look should complete the visit");
		assertEquals(1, trust.visits());
	}

	@Test
	void onlyOneVisitCountsADay() {
		Trust trust = new Trust();
		assertTrue(visit(trust, 4));
		assertFalse(visit(trust, 4), "a second visit on the same day should not count");
		assertEquals(1, trust.visits());
		assertTrue(visit(trust, 5), "the next day's visit should count");
		assertEquals(2, trust.visits());
	}

	@Test
	void threeVisitsEarnTheEggAndNoMoreAreCounted() {
		Trust trust = new Trust();
		assertEquals(3, Trust.FULL);
		visit(trust, 1);
		visit(trust, 2);
		assertFalse(trust.full());
		visit(trust, 3);
		assertTrue(trust.full());
		assertFalse(visit(trust, 4));
		assertEquals(Trust.FULL, trust.visits());
	}

	@Test
	void aScareLosesTheVisitUnderWayAndHurtingLosesOneEarned() {
		Trust trust = new Trust();
		visit(trust, 1);
		for (int i = 0; i < LOOKS_PER_VISIT - 1; i++) {
			trust.calm(Trust.WATCH_EVERY, 2);
		}
		trust.startled();
		assertFalse(trust.calm(Trust.WATCH_EVERY, 2), "a scare should start the visit over");
		assertEquals(1, trust.visits(), "a scare should not cost an earned visit");
		trust.hurt();
		assertEquals(0, trust.visits());
		trust.hurt();
		assertEquals(0, trust.visits(), "trust should never go below nothing");
	}

	@Test
	void sprintingNearOrWalkingRightUpIsScaryAndSneakingIsNot() {
		assertEquals(Trust.Sight.SCARY, Trust.see(Trust.SPRINT_RANGE, true, false));
		assertEquals(Trust.Sight.FAR, Trust.see(Trust.SPRINT_RANGE + 0.1, true, false));
		assertEquals(Trust.Sight.SCARY, Trust.see(Trust.TOO_CLOSE - 0.1, false, false));
		assertEquals(Trust.Sight.CALM, Trust.see(Trust.TOO_CLOSE - 0.1, false, true));
		assertEquals(Trust.Sight.SCARY, Trust.see(2.0, true, true), "sprinting is scary however near");
		assertEquals(Trust.Sight.CALM, Trust.see(Trust.TOO_CLOSE, false, false));
		assertEquals(Trust.Sight.CALM, Trust.see(Trust.VISIT_RANGE, false, false));
		assertEquals(Trust.Sight.FAR, Trust.see(Trust.VISIT_RANGE + 0.1, false, false));
	}

	@Test
	void savedTrustComesBackAsItWas() {
		Trust trust = new Trust(2, 7);
		assertEquals(2, trust.visits());
		assertFalse(visit(trust, 7), "the day of the last visit should come back too");
		assertTrue(visit(trust, 8));
		assertTrue(trust.full());
	}
}
