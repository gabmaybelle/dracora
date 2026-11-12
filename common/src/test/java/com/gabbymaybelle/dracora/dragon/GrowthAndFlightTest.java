package com.gabbymaybelle.dracora.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrowthAndFlightTest {
	@Test
	void aDragonIsAHatchlingForADayThenAJuvenileForADayThenGrown() {
		assertEquals(Stage.HATCHLING, Stage.of(Stage.HATCH_AGE));
		assertEquals(Stage.HATCHLING, Stage.of(-Stage.STAGE_TICKS - 1));
		assertEquals(Stage.JUVENILE, Stage.of(-Stage.STAGE_TICKS));
		assertEquals(Stage.JUVENILE, Stage.of(-1));
		assertEquals(Stage.ADULT, Stage.of(0));
		assertEquals(Stage.ADULT, Stage.of(6000));
		assertEquals(24000, Stage.STAGE_TICKS);
		assertEquals(-48000, Stage.HATCH_AGE);
	}

	@Test
	void eachStageIsBiggerAndOnlyTheOlderOnesCarryOrFly() {
		assertTrue(Stage.HATCHLING.scale() < Stage.JUVENILE.scale() && Stage.JUVENILE.scale() < Stage.ADULT.scale());
		assertEquals(1.0F, Stage.ADULT.scale());
		assertFalse(Stage.HATCHLING.canCarry());
		assertTrue(Stage.JUVENILE.canCarry());
		assertTrue(Stage.ADULT.canCarry());
		assertFalse(Stage.HATCHLING.canFly());
		assertFalse(Stage.JUVENILE.canFly());
		assertTrue(Stage.ADULT.canFly());
	}

	@Test
	void aStageNumberThatIsNotOneReadsAsAdult() {
		assertEquals(Stage.HATCHLING, Stage.byIndex(0));
		assertEquals(Stage.ADULT, Stage.byIndex(-1));
		assertEquals(Stage.ADULT, Stage.byIndex(9));
	}

	@Test
	void flightSettlesAtTheTopSpeedAndNeverPassesIt() {
		double speed = 0.0;
		for (int tick = 0; tick < 400; tick++) {
			speed = Flight.nextSpeed(speed, 1.0);
			assertTrue(speed <= Flight.TOP_SPEED + 1.0e-9, "tick " + tick + " went " + speed);
		}
		assertEquals(Flight.TOP_SPEED, speed, 1.0e-6);
		// More input than "full" buys nothing.
		assertEquals(Flight.nextSpeed(0.3, 1.0), Flight.nextSpeed(0.3, 5.0));
	}

	@Test
	void aDragonLeftAloneInTheAirSlowsDown() {
		double speed = Flight.TOP_SPEED;
		for (int tick = 0; tick < 100; tick++) {
			speed = Flight.nextSpeed(speed, 0.0);
		}
		assertTrue(speed < 0.001, "it should have all but stopped, not " + speed);
	}
}
