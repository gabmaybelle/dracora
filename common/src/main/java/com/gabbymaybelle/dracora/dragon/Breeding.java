package com.gabbymaybelle.dracora.dragon;

import java.util.List;

/**
 * What two tamed adults' egg looks like. Its colour is the two parents' colours blended evenly,
 * channel by channel; one egg in {@link #MUTATION_ODDS} is a mutation instead, in one of the rare
 * colours, which no birthplace has. A pair lays one egg per Minecraft day.
 */
public final class Breeding {
	/** One egg in this many is a mutation. */
	public static final int MUTATION_ODDS = 20;
	/** How long a pair waits before it can lay another egg, in ticks: one Minecraft day. */
	public static final int REST_TICKS = Stage.STAGE_TICKS;
	/** Pearl: a warm cream with a blush, so it never reads as a snowy dragon's cool white. */
	public static final int PEARL = 0xF3DCC4;
	/** Obsidian: near black with a violet cast. */
	public static final int OBSIDIAN = 0x140D1C;
	/** The rare colours a mutation can give. */
	public static final List<Integer> MUTATIONS = List.of(PEARL, OBSIDIAN);

	private Breeding() {
	}

	/** The two colours mixed half and half, each of red, green and blue on its own. */
	public static int blend(int a, int b) {
		int red = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF) + 1) / 2;
		int green = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF) + 1) / 2;
		int blue = ((a & 0xFF) + (b & 0xFF) + 1) / 2;
		return red << 16 | green << 8 | blue;
	}

	/**
	 * The egg's colour, from its parents' colours and two random rolls (any ints): the first decides
	 * whether it is a mutation, the second which one.
	 */
	public static int eggColour(int a, int b, int mutationRoll, int pickRoll) {
		if (Math.floorMod(mutationRoll, MUTATION_ODDS) == 0) {
			return MUTATIONS.get(Math.floorMod(pickRoll, MUTATIONS.size()));
		}
		return blend(a, b);
	}

	/** Whether a colour is one of the rare ones. */
	public static boolean isMutation(int colour) {
		return MUTATIONS.contains(colour & 0xFFFFFF);
	}
}
