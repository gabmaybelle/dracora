package com.gabbymaybelle.dracora.dragon;

/**
 * A dragon's three stages of life. A hatchling follows its owner; a juvenile can be ridden for
 * short hops; an adult flies. Growth is time only: each of the first two stages lasts one
 * Minecraft day in loaded chunks, counted by the same age number vanilla's baby animals use
 * (negative while growing, zero and up once grown).
 */
public enum Stage {
	HATCHLING(0.3F),
	JUVENILE(0.6F),
	ADULT(1.0F);

	/** How long the hatchling and juvenile stages each last, in ticks: one Minecraft day. */
	public static final int STAGE_TICKS = 24000;
	/** The age a dragon hatches at: two stages short of grown. */
	public static final int HATCH_AGE = -2 * STAGE_TICKS;

	private final float scale;

	Stage(float scale) {
		this.scale = scale;
	}

	/** The stage a dragon of this age is in. */
	public static Stage of(int age) {
		if (age < -STAGE_TICKS) {
			return HATCHLING;
		}
		return age < 0 ? JUVENILE : ADULT;
	}

	/** The stage by its number (as sent to players), or adult for a number that isn't one. */
	public static Stage byIndex(int index) {
		Stage[] stages = values();
		return index >= 0 && index < stages.length ? stages[index] : ADULT;
	}

	/** Its size next to an adult's. */
	public float scale() {
		return scale;
	}

	/** Whether its owner can ride it: juveniles and adults. */
	public boolean canCarry() {
		return this != HATCHLING;
	}

	/** Whether it can fly with its rider: adults only. A juvenile hops. */
	public boolean canFly() {
		return this == ADULT;
	}
}
