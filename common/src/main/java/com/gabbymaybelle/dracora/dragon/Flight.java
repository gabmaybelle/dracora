package com.gabbymaybelle.dracora.dragon;

/**
 * The numbers behind a ridden dragon's movement. An adult in the air is pushed along its rider's
 * line of sight and slowed by drag each tick, so its speed settles at its flying speed attribute
 * and can never pass it. {@link #TOP_SPEED} is that attribute's default, about what an elytra
 * glides at; the server's {@code flightSpeed} setting replaces it on every dragon.
 */
public final class Flight {
	/** The default top flying speed, in blocks per tick (15 blocks a second); the setting overrides it. */
	public static final double TOP_SPEED = 0.75;
	/** What is left of a flying dragon's speed after each tick, as vanilla's flying mobs have it. */
	public static final double DRAG = 0.91;
	/** The fastest any dragon falls, in blocks per tick: spread wings turn a fall into a glide. */
	public static final double GLIDE_FALL = 0.25;
	/** A juvenile's hop: how hard it leaps up, and how hard forward, in blocks per tick. */
	public static final double HOP_UP = 0.75;
	public static final double HOP_FORWARD = 0.5;
	/** An adult's push off the ground when it takes off, in blocks per tick. */
	public static final double TAKE_OFF_UP = 0.5;
	/** The share of its walking speed a dragon keeps with a rider steering it on the ground (a steered mob moves much faster than one walking by itself). */
	public static final double RIDDEN_WALK = 0.6;
	/** How fast a wild dragon flies by itself, in blocks per tick (9 blocks a second). */
	public static final double WILD_SPEED = 0.45;
	/** Ticks between a juvenile's hops. */
	public static final int HOP_REST = 10;

	private Flight() {
	}

	/** The push per tick, at full input, that settles at {@code topSpeed} under {@link #DRAG}. */
	public static double push(double topSpeed) {
		return topSpeed * (1.0 - DRAG);
	}

	/** The speed a dragon moves at on the next tick, from its speed now and the share of full input (0 to 1) it is given. */
	public static double nextSpeed(double speed, double input) {
		return speed * DRAG + push(TOP_SPEED) * Math.clamp(input, 0.0, 1.0);
	}
}
