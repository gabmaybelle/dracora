package com.gabbymaybelle.dracora.egg;

import com.gabbymaybelle.dracora.Dracora;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * How an egg hatches: it gathers warmth while something warm (a lit campfire, lava) is beside it,
 * cracks at a third and at two thirds of the way, and hatches when it has enough. A cold egg
 * waits and keeps the warmth it has.
 */
public final class Incubation {
	/** The warmth an egg needs, in ticks spent beside something warm: one Minecraft day. */
	public static final int HATCH_WARMTH = 24000;
	/** How often an egg looks for warmth, in ticks. It does nothing in between. */
	public static final int CHECK_EVERY = 100;
	/** The crack stage that means "hatch now"; the egg block shows the stages below it. */
	public static final int HATCHED = 3;
	/** How far warmth reaches: this many blocks to each side, and one up or down. */
	public static final int REACH = 2;

	/** The blocks that warm an egg. One with a "lit" state only counts while it is lit. */
	public static final TagKey<Block> WARMS_EGGS = TagKey.create(Registries.BLOCK, Dracora.id("warms_eggs"));

	private Incubation() {
	}

	/** The crack stage for an amount of warmth: 0 (whole), 1, 2, or {@link #HATCHED}. */
	public static int stage(int warmth) {
		if (warmth <= 0) {
			return 0;
		}
		return (int) Math.min(HATCHED, (long) warmth * HATCHED / HATCH_WARMTH);
	}

	/** Whether a block gives off warmth an egg can use. */
	public static boolean warms(BlockState state) {
		return state.is(WARMS_EGGS) && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT));
	}

	/** Whether anything warm is within reach of the egg at {@code pos}. */
	public static boolean isWarm(BlockGetter level, BlockPos pos) {
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-REACH, -1, -REACH), pos.offset(REACH, 1, REACH))) {
			if (warms(level.getBlockState(near))) {
				return true;
			}
		}
		return false;
	}
}
