package com.gabbymaybelle.dracora.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.ARGB;
import org.junit.jupiter.api.Test;

class TintableDragonTextureTest {
	@Test
	void theDarkestGreyBecomesAMidGreyAndTheBrightestWhite() {
		assertEquals(96, TintableDragonTexture.stretch(10, 10, 60));
		assertEquals(255, TintableDragonTexture.stretch(60, 10, 60));
		// Strays outside the picture's usual range are held to the ends.
		assertEquals(96, TintableDragonTexture.stretch(0, 10, 60));
		assertEquals(255, TintableDragonTexture.stretch(200, 10, 60));
	}

	@Test
	void brighterInStaysBrighterOut() {
		int last = -1;
		for (int grey = 10; grey <= 60; grey++) {
			int out = TintableDragonTexture.stretch(grey, 10, 60);
			assertTrue(out >= last, "grey " + grey + " came out darker than the one before");
			last = out;
		}
	}

	@Test
	void aPixelsGreyIsItsBrightnessWhateverItsColour() {
		assertEquals(0, TintableDragonTexture.grey(ARGB.color(255, 0, 0, 0)));
		assertEquals(255, TintableDragonTexture.grey(ARGB.color(255, 255, 255, 255)));
		assertEquals(TintableDragonTexture.grey(ARGB.color(255, 40, 40, 40)), TintableDragonTexture.grey(ARGB.color(10, 40, 40, 40)));
		assertTrue(TintableDragonTexture.grey(ARGB.color(255, 0, 200, 0)) > TintableDragonTexture.grey(ARGB.color(255, 0, 0, 200)), "green reads brighter than blue");
	}
}
