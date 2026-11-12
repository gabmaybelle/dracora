package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Flight;
import com.gabbymaybelle.dracora.dragon.Stage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Growing up and flying: a dragon is a hatchling, then a juvenile, then grown, each a day long
 * and each bigger; only its owner rides it, and only once it is a juvenile; an adult in flight
 * settles at its top speed, hovers when let go and glides down when left; a juvenile hops but
 * can't fly.
 */
final class GrowthChecks {
	private static final String TAG = "[dracora_gametest] ";
	private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

	private GrowthChecks() {
	}

	/** A summoned dragon is grown; set back to a hatchling it is small, and it becomes a juvenile and then an adult as its age passes each mark, by itself. */
	static void grows(GameTestHelper helper) {
		Dragon dragon = Dragons.DRAGON.get().spawn(helper.getLevel(), helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
		helper.assertTrue(dragon != null, "a dragon should spawn");
		helper.assertTrue(dragon.getStage() == Stage.ADULT && !dragon.isBaby(), "a summoned dragon should be grown, not " + dragon.getStage());
		float adultWidth = dragon.getBbWidth();
		dragon.startAsHatchling();
		float hatchlingWidth = dragon.getBbWidth();
		helper.assertTrue(dragon.getStage() == Stage.HATCHLING && dragon.isBaby(), "a new hatchling should be in the hatchling stage, not " + dragon.getStage());
		helper.assertTrue(dragon.getAge() == -2 * 24000, "a hatchling should have two Minecraft days of growing ahead (age " + dragon.getAge() + ")");
		helper.assertTrue(Math.abs(hatchlingWidth - adultWidth * Stage.HATCHLING.scale()) < 0.01F, "a hatchling should be small (width " + hatchlingWidth + ")");

		dragon.setAge(-Stage.STAGE_TICKS - 3);
		helper.assertTrue(dragon.getStage() == Stage.HATCHLING, "three ticks short of a day it should still be a hatchling");
		helper.runAfterDelay(8, () -> {
			float juvenileWidth = dragon.getBbWidth();
			if (dragon.getStage() != Stage.JUVENILE || Math.abs(juvenileWidth - adultWidth * Stage.JUVENILE.scale()) > 0.01F) {
				dragon.discard();
				helper.fail("after its first day a hatchling should be a juvenile, and bigger (stage " + dragon.getStage() + ", width " + juvenileWidth + ")");
				return;
			}
			dragon.setAge(-3);
			helper.runAfterDelay(8, () -> {
				try {
					System.out.println(TAG + "growth: widths hatchling " + hatchlingWidth + ", juvenile " + juvenileWidth + ", adult " + dragon.getBbWidth()
							+ "; stage after both days " + dragon.getStage());
					helper.assertTrue(dragon.getStage() == Stage.ADULT && !dragon.isBaby(), "after its second day it should be grown, not " + dragon.getStage());
					helper.assertTrue(Math.abs(dragon.getBbWidth() - adultWidth) < 0.01F, "a grown dragon should be full size (width " + dragon.getBbWidth() + ")");
				} finally {
					dragon.discard();
				}
				helper.succeed();
			});
		});
	}

	/** A hatchling saved and read back is still a hatchling with the same age. */
	static void stageSaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Dragon dragon = Dragons.DRAGON.get().spawn(level, helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
		Dragon copy = Dragons.DRAGON.get().create(level, EntitySpawnReason.LOAD);
		try {
			dragon.startAsHatchling();
			dragon.setAge(Stage.HATCH_AGE + 500);
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			dragon.saveWithoutId(output);
			CompoundTag tag = output.buildResult();
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			System.out.println(TAG + "stage save: stage " + copy.getStage() + ", age " + copy.getAge());
			helper.assertTrue(copy.getStage() == Stage.HATCHLING, "a hatchling should come back from a save as a hatchling, not " + copy.getStage());
			helper.assertTrue(copy.getAge() == Stage.HATCH_AGE + 500, "its age should come back from a save (got " + copy.getAge() + ")");
		} finally {
			dragon.discard();
			copy.discard();
		}
		helper.succeed();
	}

	/**
	 * Only the owner rides, and only a juvenile or an adult: a stranger's hand does nothing, the
	 * owner's sneaking hand sits it down, their plain hand mounts (and stands it up), and a
	 * hatchling sits instead. A stranger put on its back doesn't steer it.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: an owner and a stranger
	static void rides(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		Dragon dragon = Dragons.DRAGON.get().spawn(level, helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
		try {
			owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			dragon.setNoAi(true);
			dragon.setOnGround(true);
			dragon.tame(owner);

			dragon.mobInteract(stranger, InteractionHand.MAIN_HAND);
			helper.assertTrue(stranger.getVehicle() == null && !dragon.isOrderedToSit(), "a stranger's hand should neither mount nor sit a tamed dragon");

			owner.setShiftKeyDown(true);
			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			helper.assertTrue(dragon.isOrderedToSit() && owner.getVehicle() == null, "its owner's sneaking hand should sit a grown dragon down, not mount it");
			owner.setShiftKeyDown(false);

			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			System.out.println(TAG + "riding: owner on an adult " + (owner.getVehicle() == dragon) + ", steering " + (dragon.getControllingPassenger() == owner)
					+ ", stood up " + !dragon.isOrderedToSit());
			helper.assertTrue(owner.getVehicle() == dragon, "its owner's hand should mount a grown dragon");
			helper.assertFalse(dragon.isOrderedToSit(), "a sitting dragon should stand up for its rider");
			helper.assertTrue(dragon.getControllingPassenger() == owner, "the owner should steer the dragon they ride");
			owner.stopRiding();

			stranger.startRiding(dragon, true, false);
			helper.assertTrue(dragon.getControllingPassenger() == null, "a stranger on its back should not steer it");
			stranger.stopRiding();

			dragon.setAge(-100);
			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			helper.assertTrue(dragon.getStage() == Stage.JUVENILE && owner.getVehicle() == dragon, "its owner should be able to ride a juvenile");

			dragon.startAsHatchling();
			helper.assertTrue(owner.getVehicle() == null, "a dragon that is a hatchling again should set its rider down");
			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			System.out.println(TAG + "riding: owner on a hatchling " + (owner.getVehicle() == dragon) + ", hatchling sits " + dragon.isOrderedToSit());
			helper.assertTrue(owner.getVehicle() == null, "nobody should ride a hatchling");
			helper.assertTrue(dragon.isOrderedToSit(), "its owner's hand should sit a hatchling down");
		} finally {
			owner.stopRiding();
			stranger.stopRiding();
			dragon.discard();
			level.getServer().getPlayerList().remove(owner);
			level.getServer().getPlayerList().remove(stranger);
		}
		helper.succeed();
	}

	/**
	 * An adult takes off with its rider, speeds up to its top speed and no further without
	 * sinking, slows to a hover when let go, and left by its rider falls no faster than a glide.
	 * A fall doesn't hurt it, and it counts as a flying mount.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: a rider
	static void flies(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Dragon dragon = Dragons.DRAGON.get().spawn(level, helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
		try {
			dragon.setNoAi(true);
			dragon.tame(owner);
			Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(MIDDLE.above(2)));
			dragon.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
			dragon.setOnGround(false);
			// The server may not move a dragon a player is steering (their own game does), so the
			// flight is measured with nobody on its back.
			helper.assertTrue(dragon.isFlyingVehicle(), "a grown dragon should count as a flying mount");
			dragon.takeOff();
			helper.assertTrue(dragon.isAirborne(), "a grown dragon should take off");

			Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
			double fastest = 0.0;
			double speed = 0.0;
			double drift = 0.0;
			for (int tick = 0; tick < 150; tick++) {
				dragon.travel(ahead);
				Vec3 moved = dragon.position().subtract(start);
				speed = moved.horizontalDistance();
				fastest = Math.max(fastest, speed);
				drift = Math.max(drift, Math.abs(moved.y));
				dragon.setPos(start);
			}
			double hover = 0.0;
			for (int tick = 0; tick < 80; tick++) {
				dragon.travel(Vec3.ZERO);
				Vec3 moved = dragon.position().subtract(start);
				hover = moved.horizontalDistance();
				drift = Math.max(drift, Math.abs(moved.y));
				dragon.setPos(start);
			}
			System.out.println(TAG + String.format("flight: settles at %.4f blocks a tick (top speed %.2f), fastest %.4f, %.4f after letting go, rose or sank %.6f",
					speed, Flight.TOP_SPEED, fastest, hover, drift));
			helper.assertTrue(speed > Flight.TOP_SPEED * 0.98, "a flying dragon should reach its top speed (got " + speed + ")");
			helper.assertTrue(fastest <= Flight.TOP_SPEED + 0.001, "a flying dragon should never pass its top speed (got " + fastest + ")");
			helper.assertTrue(drift < 0.0001, "level flight should neither rise nor sink (got " + drift + ")");
			helper.assertTrue(hover < 0.01, "a flying dragon should slow to a hover when let go (still " + hover + ")");

			owner.startRiding(dragon, true, false);
			helper.assertTrue(dragon.isAirborne() && dragon.getControllingPassenger() == owner, "a flying dragon should keep flying with its owner on its back");
			owner.stopRiding();
			helper.assertFalse(dragon.isAirborne(), "a dragon left by its rider should stop flying");
			dragon.setDeltaMovement(Vec3.ZERO);
			double fastestFall = 0.0;
			for (int tick = 0; tick < 80; tick++) {
				dragon.setOnGround(false);
				dragon.travel(Vec3.ZERO);
				fastestFall = Math.max(fastestFall, start.y - dragon.position().y);
				dragon.setPos(start);
			}
			System.out.println(TAG + String.format("glide: falls at most %.4f blocks a tick (the glide is %.2f)", fastestFall, Flight.GLIDE_FALL));
			helper.assertTrue(fastestFall > 0.1, "a dragon left in the air should come down (fell " + fastestFall + " a tick)");
			helper.assertTrue(fastestFall <= Flight.GLIDE_FALL + 0.001, "a dragon should fall no faster than a glide (fell " + fastestFall + " a tick)");
			helper.assertFalse(dragon.causeFallDamage(40.0, 1.0F, level.damageSources().fall()), "a fall should not hurt a dragon");
		} finally {
			owner.stopRiding();
			dragon.discard();
			level.getServer().getPlayerList().remove(owner);
		}
		helper.succeed();
	}

	/** A juvenile can't take off; it leaps up and forward, rests before the next leap, and comes down at a glide. It is no flying mount. */
	static void hops(GameTestHelper helper) {
		Dragon dragon = Dragons.DRAGON.get().spawn(helper.getLevel(), helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
		try {
			dragon.setNoAi(true);
			dragon.setAge(-100);
			Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(MIDDLE.above(2)));
			dragon.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
			dragon.setDeltaMovement(Vec3.ZERO);
			dragon.setOnGround(true);
			helper.assertFalse(dragon.isFlyingVehicle(), "a juvenile should not count as a flying mount");
			dragon.takeOff();
			helper.assertFalse(dragon.isAirborne(), "a juvenile should not be able to take off");

			dragon.hop();
			Vec3 leap = dragon.getDeltaMovement();
			dragon.hop();
			Vec3 again = dragon.getDeltaMovement();
			helper.assertTrue(Math.abs(leap.y - Flight.HOP_UP) < 0.001 && Math.abs(leap.z - Flight.HOP_FORWARD) < 0.001,
					"a hop should leap up and forward (got " + leap + ")");
			helper.assertTrue(again.equals(leap), "a juvenile should rest between hops (got " + again + ")");

			double highest = 0.0;
			double height = 0.0;
			double fastestFall = 0.0;
			for (int tick = 0; tick < 80; tick++) {
				dragon.setOnGround(false);
				Vec3 before = dragon.position();
				dragon.travel(Vec3.ZERO);
				double step = dragon.position().y - before.y;
				height += step;
				highest = Math.max(highest, height);
				fastestFall = Math.max(fastestFall, -step);
				dragon.setPos(start);
			}
			System.out.println(TAG + String.format("hop: leaps %.2f up and %.2f forward, peaks %.2f blocks up, then falls at most %.4f a tick", leap.y, leap.z, highest, fastestFall));
			helper.assertTrue(highest > 1.5 && highest < 6.0, "a hop should clear a couple of blocks, not fly (peaked at " + highest + ")");
			helper.assertTrue(height < 0.0, "a juvenile should come back down after a hop (ended " + height + " up)");
			helper.assertTrue(fastestFall <= Flight.GLIDE_FALL + 0.001, "a juvenile should come down at a glide (fell " + fastestFall + " a tick)");
		} finally {
			dragon.discard();
		}
		helper.succeed();
	}
}
