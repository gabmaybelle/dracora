package com.gabbymaybelle.dracora;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NestSpacingTest {
	@Test
	void theUsualSettingKeepsTheUsualDistances() {
		assertEquals(24, Config.spacingAt(24, 100));
		assertEquals(8, Config.separationAt(24, 8, 100));
		assertEquals(36, Config.spacingAt(36, 100));
		assertEquals(12, Config.separationAt(36, 12, 100));
	}

	@Test
	void theSettingScalesBothDistances() {
		assertEquals(48, Config.spacingAt(24, 200));
		assertEquals(16, Config.separationAt(24, 8, 200));
		assertEquals(12, Config.spacingAt(24, 50));
		assertEquals(4, Config.separationAt(24, 8, 50));
	}

	@Test
	void separationAlwaysStaysBelowSpacing() {
		for (int percent = 1; percent <= Config.NEST_SPACING_MAX; percent++) {
			for (int[] set : new int[][] { { 24, 8 }, { 36, 12 }, { 3, 2 } }) {
				int spacing = Config.spacingAt(set[0], percent);
				int separation = Config.separationAt(set[0], set[1], percent);
				assertTrue(spacing >= 2 && separation >= 0 && separation < spacing,
						"at " + percent + "% spacing " + spacing + " and separation " + separation);
			}
		}
	}
}
