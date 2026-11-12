package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.dragon.Birthplace;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Stage;
import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import com.gabbymaybelle.dracora.egg.Incubation;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * The egg's checks: it is registered; the rule for what warms an egg; an egg takes its colour
 * from where it is set down, or keeps the one it carries, and drops with it; a warm egg cracks
 * and hatches a hatchling in its colour for whoever set it down, there or away; a cold or
 * unplaced egg waits; and colour, placer and warmth survive a save.
 */
final class EggChecks {
	private static final String TAG = "[dracora_gametest] ";
	private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);
	/** Warmth checks per crack stage. */
	private static final int CHECKS_PER_STAGE = Incubation.HATCH_WARMTH / Incubation.HATCHED / Incubation.CHECK_EVERY;

	private EggChecks() {
	}

	/** The egg's block, item and block entity are registered. */
	static void registered(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Eggs.ID), "the egg block should be registered");
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Eggs.ID), "the egg item should be registered");
		helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(Eggs.ID), "the egg's block entity should be registered");
		helper.assertTrue(Eggs.BLOCK.get() instanceof EggBlock, "the egg block should be Dracora's");
		System.out.println(TAG + "egg: block, item and block entity registered");
		helper.succeed();
	}

	/** The rule itself: lit campfires and lava warm an egg; an unlit campfire, a torch and magma don't. And the crack stages by warmth. */
	static void warmthRule(GameTestHelper helper) {
		BlockState lit = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
		BlockState unlit = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);
		BlockState soul = Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
		System.out.println(TAG + "warms: lit campfire " + Incubation.warms(lit) + ", unlit " + Incubation.warms(unlit) + ", soul campfire " + Incubation.warms(soul)
				+ ", lava " + Incubation.warms(Blocks.LAVA.defaultBlockState()) + ", torch " + Incubation.warms(Blocks.TORCH.defaultBlockState())
				+ ", magma " + Incubation.warms(Blocks.MAGMA_BLOCK.defaultBlockState()));
		helper.assertTrue(Incubation.warms(lit), "a lit campfire should warm an egg");
		helper.assertFalse(Incubation.warms(unlit), "an unlit campfire should not warm an egg");
		helper.assertTrue(Incubation.warms(soul), "a lit soul campfire should warm an egg");
		helper.assertTrue(Incubation.warms(Blocks.LAVA.defaultBlockState()), "lava should warm an egg");
		helper.assertFalse(Incubation.warms(Blocks.TORCH.defaultBlockState()), "a torch should not warm an egg");
		helper.assertFalse(Incubation.warms(Blocks.MAGMA_BLOCK.defaultBlockState()), "magma should not warm an egg");
		helper.assertTrue(Incubation.stage(0) == 0 && Incubation.stage(Incubation.HATCH_WARMTH / 3) == 1
				&& Incubation.stage(Incubation.HATCH_WARMTH) == Incubation.HATCHED, "the crack stages should run from 0 to hatched");
		helper.succeed();
	}

	/**
	 * A plain egg set down by a player takes a colour from that biome's palette and is theirs; an
	 * egg that carries a colour keeps it; and a broken egg drops with its colour and nothing else.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: someone to set the egg down
	static void colour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			BlockPos plainAt = new BlockPos(2, 1, 2);
			BlockPos carriedAt = new BlockPos(6, 1, 6);
			place(helper, player, new ItemStack(Eggs.ITEM.get()), plainAt);
			EggBlockEntity plain = EggBlock.eggAt(level, helper.absolutePos(plainAt));
			helper.assertTrue(plain != null, "a player should be able to set an egg down");
			Birthplace here = Birthplace.of(level.getBiome(helper.absolutePos(plainAt)));
			System.out.println(TAG + "plain egg: colour " + hex(plain.colour()) + " (the room is " + here + "), placer known " + player.getUUID().equals(plain.owner()));
			helper.assertTrue(here.colours().contains(plain.colour()), "a plain egg set down in a " + here + " biome should take one of its colours, not " + hex(plain.colour()));
			helper.assertTrue(player.getUUID().equals(plain.owner()), "the egg should remember who set it down");

			int carriedColour = 0x123456;
			place(helper, player, Eggs.coloured(carriedColour), carriedAt);
			BlockPos carriedPos = helper.absolutePos(carriedAt);
			EggBlockEntity carried = EggBlock.eggAt(level, carriedPos);
			helper.assertTrue(carried != null, "a coloured egg should be placed");
			helper.assertTrue(carried.colour() == carriedColour, "an egg should keep the colour it carries (got " + hex(carried.colour()) + ")");

			carried.setWarmth(5000);
			List<ItemStack> drops = Block.getDrops(level.getBlockState(carriedPos), level, carriedPos, carried);
			helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Eggs.ITEM.get()), "a broken egg should drop one egg (got " + drops + ")");
			DyedItemColor dropped = drops.getFirst().get(DataComponents.DYED_COLOR);
			System.out.println(TAG + "dropped egg: colour " + (dropped == null ? "none" : hex(dropped.rgb())));
			helper.assertTrue(dropped != null && (dropped.rgb() & 0xFFFFFF) == carriedColour, "the dropped egg should carry its colour");
			helper.assertTrue(drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA) == null, "the dropped egg should not carry its placer or warmth");
		} finally {
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/**
	 * Beside a lit campfire, a placed egg cracks at a third and two thirds of the way and hatches at
	 * the end: the egg is gone and a baby dragon in the egg's colour, owned by the placer, stands
	 * there. An egg whose placer is away hatches as theirs all the same.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: someone to set the egg down
	static void hatches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(MIDDLE);
		try {
			int colour = 0x2E6F96;
			helper.setBlock(MIDDLE.east(2), Blocks.CAMPFIRE);
			place(helper, player, Eggs.coloured(colour), MIDDLE);
			helper.assertTrue(EggBlock.eggAt(level, pos) != null, "the egg should be placed");

			check(level, pos, CHECKS_PER_STAGE - 1);
			helper.assertTrue(crack(level, pos) == 0, "the egg should be whole just before a third of the way");
			check(level, pos, 1);
			helper.assertTrue(crack(level, pos) == 1, "the egg should crack at a third of the way (stage " + crack(level, pos) + ")");
			check(level, pos, CHECKS_PER_STAGE);
			helper.assertTrue(crack(level, pos) == 2, "the egg should crack again at two thirds (stage " + crack(level, pos) + ")");
			helper.assertTrue(EggBlock.eggAt(level, pos).colour() == colour, "a cracked egg should keep its colour");
			check(level, pos, CHECKS_PER_STAGE - 1);
			helper.assertTrue(level.getBlockState(pos).is(Eggs.BLOCK.get()), "the egg should not hatch before it has all its warmth");
			helper.assertTrue(helper.getEntities(Dragons.DRAGON.get()).isEmpty(), "no dragon should be about before the egg hatches");
			check(level, pos, 1);

			List<Dragon> dragons = helper.getEntities(Dragons.DRAGON.get());
			System.out.println(TAG + "hatched: egg gone " + level.getBlockState(pos).isAir() + ", dragons " + dragons.size()
					+ (dragons.isEmpty() ? "" : ", stage " + dragons.getFirst().getStage() + ", colour " + hex(dragons.getFirst().getColour())
							+ ", owned by the placer " + dragons.getFirst().isOwnedBy(player)));
			helper.assertTrue(level.getBlockState(pos).isAir(), "the egg should be gone once it hatches");
			helper.assertTrue(dragons.size() == 1, "one dragon should hatch (got " + dragons.size() + ")");
			Dragon hatchling = dragons.getFirst();
			try {
				helper.assertTrue(hatchling.getStage() == Stage.HATCHLING && hatchling.getAge() == Stage.HATCH_AGE, "the hatchling should be a new hatchling, not " + hatchling.getStage());
				helper.assertTrue(hatchling.getColour() == colour, "the hatchling should wear the egg's colour (got " + hex(hatchling.getColour()) + ")");
				helper.assertTrue(hatchling.isTame() && hatchling.isOwnedBy(player), "the hatchling should belong to whoever set the egg down");
			} finally {
				hatchling.discard();
			}

			// The same again for a placer who isn't in the world.
			UUID away = UUID.randomUUID();
			place(helper, player, Eggs.coloured(colour), MIDDLE);
			EggBlock.eggAt(level, pos).setOwner(away);
			check(level, pos, CHECKS_PER_STAGE * Incubation.HATCHED);
			dragons = helper.getEntities(Dragons.DRAGON.get());
			helper.assertTrue(dragons.size() == 1, "an egg whose placer is away should still hatch (got " + dragons.size() + " dragons)");
			Dragon theirs = dragons.getFirst();
			try {
				System.out.println(TAG + "hatched for an absent placer: tame " + theirs.isTame() + ", theirs "
						+ (theirs.getOwnerReference() != null && away.equals(theirs.getOwnerReference().getUUID())));
				helper.assertTrue(theirs.isTame(), "a hatchling whose owner is away should still be tame");
				helper.assertTrue(theirs.getOwnerReference() != null && away.equals(theirs.getOwnerReference().getUUID()), "it should belong to the absent placer");
				helper.assertFalse(theirs.isOwnedBy(player), "it should not belong to someone else who was there");
			} finally {
				theirs.discard();
			}
		} finally {
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/**
	 * An egg gathers no warmth with nothing warm beside it, beside an unlit campfire, with a lit
	 * one out of reach, or when nobody set it down; lava within reach does warm a placed egg.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: someone to set the egg down
	static void waits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(MIDDLE);
		try {
			place(helper, player, Eggs.coloured(0x5E7F3A), MIDDLE);
			EggBlockEntity egg = EggBlock.eggAt(level, pos);
			helper.assertTrue(egg != null, "the egg should be placed");

			check(level, pos, 20);
			helper.assertTrue(egg.warmth() == 0, "an egg with nothing warm beside it should gather no warmth (got " + egg.warmth() + ")");
			helper.setBlock(MIDDLE.east(1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
			check(level, pos, 20);
			helper.assertTrue(egg.warmth() == 0, "an unlit campfire should not warm an egg (got " + egg.warmth() + ")");
			helper.setBlock(MIDDLE.east(1), Blocks.AIR);
			helper.setBlock(MIDDLE.east(Incubation.REACH + 1), Blocks.CAMPFIRE);
			check(level, pos, 20);
			helper.assertTrue(egg.warmth() == 0, "a campfire out of reach should not warm an egg (got " + egg.warmth() + ")");
			helper.setBlock(MIDDLE.east(Incubation.REACH + 1), Blocks.AIR);

			helper.setBlock(MIDDLE.west(Incubation.REACH), Blocks.LAVA);
			check(level, pos, 20);
			System.out.println(TAG + "waiting egg: warmth after 20 cold checks 0, after 20 beside lava " + egg.warmth());
			helper.assertTrue(egg.warmth() == 20 * Incubation.CHECK_EVERY, "lava within reach should warm a placed egg (got " + egg.warmth() + ")");

			// An egg nobody set down (a nest's egg, say) never hatches, lava or not.
			egg.setOwner(null);
			egg.setWarmth(0);
			check(level, pos, 20);
			helper.assertTrue(egg.warmth() == 0, "an egg nobody set down should gather no warmth (got " + egg.warmth() + ")");
			helper.assertTrue(crack(level, pos) == 0, "a waiting egg should stay whole");
		} finally {
			helper.setBlock(MIDDLE.west(Incubation.REACH), Blocks.AIR);
			helper.setBlock(MIDDLE, Blocks.AIR);
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/** Saved and read back into a new egg, the colour, the placer and the warmth come through; players are only sent the colour. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: a placer to save
	static void saves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(MIDDLE);
		try {
			int colour = 0xB5562C;
			place(helper, player, Eggs.coloured(colour), MIDDLE);
			EggBlockEntity egg = EggBlock.eggAt(level, pos);
			helper.assertTrue(egg != null, "the egg should be placed");
			egg.setWarmth(9000);
			CompoundTag tag = egg.saveWithoutMetadata(level.registryAccess());
			EggBlockEntity copy = new EggBlockEntity(pos, level.getBlockState(pos));
			copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			System.out.println(TAG + "egg save: colour " + hex(copy.colour()) + ", placer kept " + player.getUUID().equals(copy.owner()) + ", warmth " + copy.warmth());
			helper.assertTrue(copy.colour() == colour, "the egg's colour should come back from a save (got " + hex(copy.colour()) + ")");
			helper.assertTrue(player.getUUID().equals(copy.owner()), "the egg's placer should come back from a save");
			helper.assertTrue(copy.warmth() == 9000, "the egg's warmth should come back from a save (got " + copy.warmth() + ")");

			CompoundTag sent = egg.getUpdateTag(level.registryAccess());
			helper.assertTrue(sent.getIntOr("Colour", -1) == colour, "players should be sent the egg's colour");
			helper.assertTrue(sent.keySet().size() == 1, "players should be sent only the colour (got " + sent.keySet() + ")");
		} finally {
			helper.setBlock(MIDDLE, Blocks.AIR);
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/** The player sets the egg in their hand down at {@code pos}, the way a real player does. */
	private static void place(GameTestHelper helper, ServerPlayer player, ItemStack egg, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, egg);
		helper.placeAt(player, egg, pos.below(), Direction.UP);
	}

	/** Runs the egg's warmth check this many times, as its scheduled tick would over time. */
	private static void check(ServerLevel level, BlockPos pos, int times) {
		for (int i = 0; i < times; i++) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(Eggs.BLOCK.get())) {
				return;
			}
			state.tick(level, pos, level.getRandom());
		}
	}

	private static int crack(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(Eggs.BLOCK.get()) ? state.getValue(EggBlock.HATCH) : -1;
	}

	private static String hex(int colour) {
		return String.format("%06X", colour);
	}
}
