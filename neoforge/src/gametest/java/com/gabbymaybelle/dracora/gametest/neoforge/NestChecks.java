package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.dragon.Birthplace;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Trust;
import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The nest's checks: the nest as it generates holds a guarded egg and a parent that takes it as
 * its own; calm visits, one a day, earn the egg and nothing else gets it; sprinting, rushing up
 * and hurting scare the parent and cost trust; a scared parent takes to the air, and one whose
 * egg is gone leaves; and all of it survives a save.
 */
final class NestChecks {
	private static final String TAG = "[dracora_gametest] ";
	private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);
	private static final int LOOKS_PER_VISIT = Trust.VISIT_TICKS / Trust.WATCH_EVERY;

	private NestChecks() {
	}

	/**
	 * The nest's template set down as world generation does it: a guarded egg with no colour yet
	 * in the middle and one wild adult beside it, which within a couple of seconds has found the
	 * egg, guards it and has given it its own colour, one of this biome's.
	 */
	static void generates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos corner = helper.absolutePos(new BlockPos(0, 1, 0));
		BlockPos eggAt = corner.offset(4, 1, 4);
		StructureTemplate nest = level.getStructureTemplateManager().get(Dracora.id("nest")).orElse(null);
		helper.assertTrue(nest != null, "the nest's template should be in the jar");
		helper.assertTrue(nest.getSize().getX() == 9 && nest.getSize().getZ() == 9, "the nest should be nine blocks across (got " + nest.getSize() + ")");
		nest.placeInWorld(level, corner, corner, new StructurePlaceSettings().setFinalizeEntities(true), level.getRandom(), Block.UPDATE_ALL);

		EggBlockEntity egg = EggBlock.eggAt(level, eggAt);
		List<Dragon> dragons = helper.getEntities(Dragons.DRAGON.get());
		System.out.println(TAG + "nest placed: egg " + (egg != null) + ", guarded " + (egg != null && egg.guarded()) + ", dragons " + dragons.size());
		helper.assertTrue(egg != null && egg.guarded(), "the nest should hold a guarded egg in the middle");
		helper.assertTrue(egg.owner() == null && !egg.hasColour(), "a nest's egg should start with no placer and no colour");
		helper.assertTrue(level.getBlockState(eggAt.below()).is(Blocks.HAY_BLOCK), "the egg should lie on hay");
		helper.assertTrue(level.getBlockState(corner.offset(0, 1, 4)).is(Blocks.COBBLESTONE) || level.getBlockState(corner.offset(0, 1, 4)).is(Blocks.MOSSY_COBBLESTONE),
				"the nest should have a rim");
		helper.assertTrue(dragons.size() == 1, "one parent should come with the nest (got " + dragons.size() + ")");
		Dragon parent = dragons.getFirst();
		helper.assertTrue(parent.isGuarding() && !parent.isTame() && !parent.isBaby(), "the parent should be a wild adult that guards");
		Birthplace here = Birthplace.of(level.getBiome(eggAt));
		helper.assertTrue(here.colours().contains(parent.getColour()), "the parent should wear one of this biome's colours");

		helper.runAfterDelay(45, () -> {
			try {
				System.out.println(TAG + "nest after 45 ticks: parent's nest " + (eggAt.equals(parent.nest())) + ", egg colour " + hex(egg.colour()) + ", parent " + hex(parent.getColour())
						+ ", parent " + String.format("%.1f", Math.sqrt(parent.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(eggAt)))) + " blocks from the egg");
				helper.assertTrue(eggAt.equals(parent.nest()), "the parent should have found its egg");
				helper.assertTrue(egg.hasColour() && egg.colour() == parent.getColour(), "the egg should take its parent's colour");
				helper.assertTrue(parent.hasHome() && parent.getHomePosition().equals(eggAt), "the parent should stay around its nest");
				helper.assertFalse(parent.isLeaving(), "a parent with its egg should stay");
			} finally {
				parent.discard();
				clear(helper);
			}
			helper.succeed();
		});
	}

	/**
	 * A player who stays near and calm earns a visit after half a minute, one a day; the third
	 * earns the egg. Until then the egg doesn't give under their hand and drops nothing however it
	 * breaks; after, they can break it and it drops with its colour. Another player has earned nothing.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: visitors
	static void visits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer visitor = helper.makeMockServerPlayerInLevel();
		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		BlockPos eggAt = helper.absolutePos(MIDDLE);
		Dragon parent = nestWithParent(helper);
		try {
			EggBlockEntity egg = EggBlock.eggAt(level, eggAt);
			BlockState state = level.getBlockState(eggAt);
			helper.assertTrue(egg.guarded() && egg.colour() == parent.getColour(), "a guarded egg should wear its parent's colour");
			stand(visitor, parent, 6.0);
			stand(stranger, parent, 6.0);

			look(parent, visitor, 1, LOOKS_PER_VISIT - 1);
			helper.assertTrue(parent.trustOf(visitor.getUUID()).visits() == 0, "a visit should take the whole half minute");
			look(parent, visitor, 1, 1);
			helper.assertTrue(parent.trustOf(visitor.getUUID()).visits() == 1, "half a minute of calm should count as a visit");
			look(parent, visitor, 1, LOOKS_PER_VISIT);
			helper.assertTrue(parent.trustOf(visitor.getUUID()).visits() == 1, "only one visit should count a day");
			look(parent, visitor, 2, LOOKS_PER_VISIT);
			helper.assertTrue(parent.trustOf(visitor.getUUID()).visits() == 2, "the next day's visit should count");

			float before = state.getDestroyProgress(visitor, level, eggAt);
			List<ItemStack> taken = Block.getDrops(state, level, eggAt, egg, visitor, ItemStack.EMPTY);
			List<ItemStack> blown = Block.getDrops(state, level, eggAt, egg);
			System.out.println(TAG + "two visits: lets " + egg.lets(visitor.getUUID()) + ", breaking progress " + before + ", drops by hand " + taken.size() + ", otherwise " + blown.size());
			helper.assertFalse(egg.lets(visitor.getUUID()), "two visits should not earn the egg");
			helper.assertTrue(before == 0.0F, "a guarded egg should not give under an untrusted hand (progress " + before + ")");
			helper.assertTrue(taken.isEmpty() && blown.isEmpty(), "a guarded egg should drop nothing before it is earned");

			look(parent, visitor, 3, LOOKS_PER_VISIT);
			float after = state.getDestroyProgress(visitor, level, eggAt);
			taken = Block.getDrops(state, level, eggAt, egg, visitor, ItemStack.EMPTY);
			blown = Block.getDrops(state, level, eggAt, egg);
			System.out.println(TAG + "three visits: full " + parent.trustOf(visitor.getUUID()).full() + ", lets " + egg.lets(visitor.getUUID()) + ", breaking progress " + after
					+ ", drops by hand " + taken.size() + ", otherwise " + blown.size() + ", stranger let " + egg.lets(stranger.getUUID()));
			helper.assertTrue(parent.trustOf(visitor.getUUID()).full() && egg.lets(visitor.getUUID()), "three visits should earn the egg");
			helper.assertTrue(after > 0.0F, "an earned egg should break for the player who earned it");
			helper.assertTrue(taken.size() == 1 && taken.getFirst().is(Eggs.ITEM.get()), "an earned egg should drop for the player who earned it");
			DyedItemColor colour = taken.getFirst().get(DataComponents.DYED_COLOR);
			helper.assertTrue(colour != null && (colour.rgb() & 0xFFFFFF) == parent.getColour(), "the egg taken should carry the parent's colour");
			helper.assertTrue(blown.isEmpty(), "an earned egg should still drop nothing when broken any other way");
			helper.assertFalse(egg.lets(stranger.getUUID()), "one player's trust should not let another take the egg");
			helper.assertTrue(state.getDestroyProgress(stranger, level, eggAt) == 0.0F, "the egg should not give under a stranger's hand");
			helper.assertTrue(Block.getDrops(state, level, eggAt, egg, stranger, ItemStack.EMPTY).isEmpty(), "the egg should drop nothing for a stranger");
			helper.assertTrue(parent.trustOf(stranger.getUUID()).visits() == 0, "the stranger should have earned nothing");

			// An ordinary egg, set down by anyone, is nobody's to guard.
			egg.setGuarded(false);
			helper.assertTrue(egg.lets(stranger.getUUID()) && state.getDestroyProgress(stranger, level, eggAt) > 0.0F, "an unguarded egg should break for anyone");
		} finally {
			parent.discard();
			clear(helper);
			level.getServer().getPlayerList().remove(visitor);
			level.getServer().getPlayerList().remove(stranger);
		}
		helper.succeed();
	}

	/**
	 * What scares a parent: a player walking right up, or sprinting nearby; not one sneaking up,
	 * walking at a distance, or sprinting far off. A scare loses the visit under way; hurting it
	 * loses an earned visit and the egg with it. A player it trusts fully no longer scares it.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: a visitor
	static void scares(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos eggAt = helper.absolutePos(MIDDLE);
		Dragon parent = nestWithParent(helper);
		Dragon second = null;
		try {
			EggBlockEntity egg = EggBlock.eggAt(level, eggAt);
			Trust trust = parent.trustOf(player.getUUID());

			stand(player, parent, Trust.TOO_CLOSE - 1.0);
			player.setShiftKeyDown(true);
			look(parent, player, 1, 1);
			helper.assertFalse(parent.isScared(), "a player sneaking up close should not scare it");
			player.setShiftKeyDown(false);
			stand(player, parent, Trust.SPRINT_RANGE + 1.0);
			player.setSprinting(true);
			look(parent, player, 1, 1);
			helper.assertFalse(parent.isScared(), "a player sprinting far off should not scare it");
			player.setSprinting(false);
			stand(player, parent, Trust.VISIT_RANGE + 1.0);
			look(parent, player, 1, LOOKS_PER_VISIT);
			helper.assertTrue(trust.visits() == 0, "a player too far away should earn nothing");

			stand(player, parent, 6.0);
			// With the sneaking look above, one look short of a visit.
			look(parent, player, 1, LOOKS_PER_VISIT - 2);
			player.setSprinting(true);
			look(parent, player, 1, 1);
			boolean sprintScared = parent.isScared();
			player.setSprinting(false);
			look(parent, player, 1, LOOKS_PER_VISIT);
			System.out.println(TAG + "scares: sprinting at 6 blocks scared it " + sprintScared + ", visits after " + trust.visits() + " (calm while it is in the air earns nothing)");
			helper.assertTrue(sprintScared, "a player sprinting nearby should scare it");
			helper.assertTrue(trust.visits() == 0, "calm time while the parent is scared should earn nothing");

			// A second parent, to see walking right up, hurting, and full trust.
			parent.discard();
			second = nestWithParent(helper);
			egg = EggBlock.eggAt(level, eggAt);
			trust = second.trustOf(player.getUUID());
			stand(player, second, 6.0);
			look(second, player, 1, LOOKS_PER_VISIT);
			look(second, player, 2, LOOKS_PER_VISIT);
			look(second, player, 3, LOOKS_PER_VISIT);
			helper.assertTrue(trust.full() && egg.lets(player.getUUID()), "three visits should earn the egg");
			stand(player, second, Trust.TOO_CLOSE - 1.0);
			player.setSprinting(true);
			look(second, player, 4, 1);
			player.setSprinting(false);
			helper.assertFalse(second.isScared(), "a player it trusts fully should not scare it, even sprinting right up");

			float health = second.getHealth();
			second.hurtServer(level, level.damageSources().playerAttack(player), 2.0F);
			System.out.println(TAG + "hurt: health " + health + " to " + second.getHealth() + ", scared " + second.isScared() + ", visits " + trust.visits() + ", lets " + egg.lets(player.getUUID())
					+ ", fights back " + (second.getTarget() != null));
			helper.assertTrue(second.getHealth() < health, "the hit should land");
			helper.assertTrue(second.isScared(), "being hurt should scare it");
			helper.assertTrue(trust.visits() == Trust.FULL - 1, "hurting it should cost an earned visit (now " + trust.visits() + ")");
			helper.assertFalse(egg.lets(player.getUUID()), "hurting it should lose the egg again");
			helper.assertTrue(second.getTarget() == null, "it should never fight back");

			Dragon third = second;
			helper.runAfterDelay(30, () -> {
				try {
					double rose = third.getY() - eggAt.getY();
					System.out.println(TAG + "scared parent after 30 ticks: in the air " + third.isAirborne() + ", " + String.format("%.1f", rose) + " blocks above its nest");
					helper.assertTrue(third.isAirborne(), "a scared parent should take to the air");
					helper.assertTrue(rose > 2.0, "a scared parent should climb away (rose " + rose + ")");
				} finally {
					third.discard();
					clear(helper);
					level.getServer().getPlayerList().remove(player);
				}
				helper.succeed();
			});
		} catch (RuntimeException | Error failed) {
			parent.discard();
			if (second != null) {
				second.discard();
			}
			clear(helper);
			level.getServer().getPlayerList().remove(player);
			throw failed;
		}
	}

	/** A parent whose egg is gone stops guarding, takes to the air and leaves. */
	static void leaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos eggAt = helper.absolutePos(MIDDLE);
		Dragon parent = nestWithParent(helper);
		helper.assertTrue(parent.isGuarding() && !parent.isLeaving(), "a parent with its egg should guard it");
		level.setBlock(eggAt, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		helper.runAfterDelay(40, () -> {
			try {
				double rose = parent.getY() - eggAt.getY();
				System.out.println(TAG + "egg gone, 40 ticks on: leaving " + parent.isLeaving() + ", guarding " + parent.isGuarding() + ", in the air " + parent.isAirborne() + ", "
						+ String.format("%.1f", rose) + " blocks up");
				helper.assertTrue(parent.isLeaving() && !parent.isGuarding(), "a parent whose egg is gone should leave");
				helper.assertTrue(parent.isAirborne() && rose > 2.0, "a leaving parent should fly off (rose " + rose + ")");
				helper.assertFalse(parent.hasHome(), "a leaving parent should have nothing to stay near");
			} finally {
				parent.discard();
				clear(helper);
			}
			helper.succeed();
		});
	}

	/** A parent's nest and the trust it has given come back from a save, and so does the egg's guard; players are sent the guard and who may take it. */
	static void saves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos eggAt = helper.absolutePos(MIDDLE);
		Dragon parent = nestWithParent(helper);
		Dragon copy = Dragons.DRAGON.get().create(level, EntitySpawnReason.LOAD);
		try {
			UUID friend = UUID.randomUUID();
			UUID passerby = UUID.randomUUID();
			Trust trust = parent.trustOf(friend);
			for (int day = 1; day <= Trust.FULL; day++) {
				for (int i = 0; i < LOOKS_PER_VISIT; i++) {
					trust.calm(Trust.WATCH_EVERY, day);
				}
			}
			parent.trustOf(passerby);
			EggBlockEntity egg = EggBlock.eggAt(level, eggAt);
			egg.setTrusted(friend, true);

			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			parent.saveWithoutId(output);
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
			System.out.println(TAG + "parent save: guarding " + copy.isGuarding() + ", nest kept " + eggAt.equals(copy.nest()) + ", friend's visits " + copy.trustOf(friend).visits()
					+ ", passer-by's " + copy.trustOf(passerby).visits());
			helper.assertTrue(copy.isGuarding() && eggAt.equals(copy.nest()), "the parent's nest should come back from a save");
			helper.assertTrue(copy.trustOf(friend).full(), "earned trust should come back from a save");
			helper.assertTrue(copy.trustOf(friend).lastDay() == Trust.FULL, "the day of the last visit should come back from a save");
			helper.assertTrue(copy.trustOf(passerby).visits() == 0, "a passer-by should come back with nothing");
			helper.assertTrue(copy.hasHome() && copy.getHomePosition().equals(eggAt), "the parent should still stay around its nest");

			CompoundTag tag = egg.saveWithoutMetadata(level.registryAccess());
			EggBlockEntity eggCopy = new EggBlockEntity(eggAt, level.getBlockState(eggAt));
			eggCopy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			helper.assertTrue(eggCopy.guarded() && eggCopy.lets(friend) && !eggCopy.lets(passerby), "the egg's guard and who may take it should come back from a save");
			CompoundTag sent = egg.getUpdateTag(level.registryAccess());
			EggBlockEntity seen = new EggBlockEntity(eggAt, level.getBlockState(eggAt));
			seen.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), sent));
			System.out.println(TAG + "egg sent to players: " + sent.keySet() + "; their game lets the friend " + seen.lets(friend) + ", a passer-by " + seen.lets(passerby));
			helper.assertTrue(seen.guarded() && seen.lets(friend) && !seen.lets(passerby) && seen.colour() == egg.colour(),
					"players should be sent the egg's colour, its guard and who may take it");
		} finally {
			parent.discard();
			copy.discard();
			clear(helper);
		}
		helper.succeed();
	}

	/** An egg in the middle of the room and a wild adult beside it that guards it. */
	private static Dragon nestWithParent(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos eggAt = helper.absolutePos(MIDDLE);
		level.setBlock(eggAt, Eggs.BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
		Dragon parent = Dragons.DRAGON.get().spawn(level, eggAt.east(2), EntitySpawnReason.COMMAND);
		helper.assertTrue(parent != null && EggBlock.eggAt(level, eggAt) != null, "a dragon and an egg should be there");
		parent.setOnGround(true);
		parent.guard(eggAt);
		return parent;
	}

	/** Stands the player this far east of the dragon, on its level. */
	private static void stand(ServerPlayer player, Dragon dragon, double blocks) {
		player.snapTo(dragon.getX() + blocks, dragon.getY(), dragon.getZ());
	}

	/** The parent looks at the player this many times, as it does every half second. */
	private static void look(Dragon parent, ServerPlayer player, long day, int times) {
		for (int i = 0; i < times; i++) {
			parent.notice(player, day);
		}
	}

	private static void clear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(8, 2, 8)))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static String hex(int colour) {
		return String.format("%06X", colour);
	}
}
