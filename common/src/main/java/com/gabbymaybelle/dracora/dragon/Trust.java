package com.gabbymaybelle.dracora.dragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * How far a wild parent trusts one player, and the numbers for earning it. A calm visit is time
 * spent near the parent without scaring it; one counts per Minecraft day, and {@link #FULL} of
 * them earn its egg. Startling the parent loses the visit under way; hurting it loses one earned.
 */
public final class Trust {
	/** Calm visits that earn the egg. */
	public static final int FULL = 3;
	/** Calm ticks near the parent that make a visit: half a minute. */
	public static final int VISIT_TICKS = 600;
	/** How near counts as visiting, in blocks. */
	public static final double VISIT_RANGE = 10.0;
	/** A player sprinting this near scares the parent. */
	public static final double SPRINT_RANGE = 12.0;
	/** A player this near scares it unless they are sneaking. */
	public static final double TOO_CLOSE = 4.0;
	/** The parent looks at the players around it this often, in ticks. */
	public static final int WATCH_EVERY = 10;
	/** How long a scared parent stays in the air, in ticks. */
	public static final int SCARED_TICKS = 600;
	private static final long NEVER = Long.MIN_VALUE;

	private int visits;
	private long lastDay = NEVER;
	private int calm;

	public Trust() {
	}

	/** Trust as it was saved: the visits earned and the day of the last one. */
	public Trust(int visits, long lastDay) {
		this.visits = Math.clamp(visits, 0, FULL);
		this.lastDay = lastDay;
	}

	/** Calm visits earned so far. */
	public int visits() {
		return visits;
	}

	/** The Minecraft day of the last visit that counted. */
	public long lastDay() {
		return lastDay;
	}

	/** Enough visits: the parent lets this player take its egg. */
	public boolean full() {
		return visits >= FULL;
	}

	/**
	 * The player spent these ticks calmly near the parent on this day.
	 *
	 * @return whether that completed a visit
	 */
	public boolean calm(int ticks, long day) {
		if (full() || day == lastDay) {
			return false;
		}
		calm += ticks;
		if (calm < VISIT_TICKS) {
			return false;
		}
		calm = 0;
		lastDay = day;
		visits++;
		return true;
	}

	/** The player scared the parent: the visit under way is lost. */
	public void startled() {
		calm = 0;
	}

	/** The player hurt the parent: an earned visit is lost too. */
	public void hurt() {
		calm = 0;
		visits = Math.max(0, visits - 1);
	}

	/** One player's earned trust, as a parent saves it. */
	public record Saved(UUID player, int visits, long day) {
		public static final Codec<Saved> CODEC = RecordCodecBuilder.create(saved -> saved.group(
				UUIDUtil.CODEC.fieldOf("Player").forGetter(Saved::player),
				Codec.INT.fieldOf("Visits").forGetter(Saved::visits),
				Codec.LONG.fieldOf("Day").forGetter(Saved::day)).apply(saved, Saved::new));
	}

	/** What a player near the parent is doing, as the parent sees it. */
	public enum Sight {
		/** Too far to matter. */
		FAR,
		/** Near enough to visit, and calm. */
		CALM,
		/** Sprinting nearby, or right up close without sneaking. */
		SCARY
	}

	/** How the parent takes a player at this distance, sprinting or not, sneaking or not. */
	public static Sight see(double distance, boolean sprinting, boolean sneaking) {
		if (sprinting && distance <= SPRINT_RANGE || distance < TOO_CLOSE && !sneaking) {
			return Sight.SCARY;
		}
		return distance <= VISIT_RANGE ? Sight.CALM : Sight.FAR;
	}
}
