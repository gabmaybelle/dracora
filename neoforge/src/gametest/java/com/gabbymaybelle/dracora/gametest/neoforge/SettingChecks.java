package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Config;
import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.dragon.Dragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** The settings: nest spacing, flight speed and juvenile riding. */
final class SettingChecks {
	private static final String TAG = "[dracora_gametest] ";
	private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);
	private static final int AREA = 200;

	private SettingChecks() {
	}

	/** How many chunks in a square around the origin would start a nest of this set. */
	private static int nestChunks(ChunkGeneratorStructureState state, RandomSpreadStructurePlacement placement) {
		int count = 0;
		for (int x = -AREA / 2; x < AREA / 2; x++) {
			for (int z = -AREA / 2; z < AREA / 2; z++) {
				if (placement.isStructureChunk(state, x, z)) {
					count++;
				}
			}
		}
		return count;
	}

	/** Settles a dragon's level flight and returns its speed in blocks a tick, measured as the flight check does (nobody on its back). */
	private static double settledSpeed(Dragon dragon, Vec3 start) {
		dragon.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
		dragon.setOnGround(false);
		dragon.takeOff();
		double speed = 0.0;
		for (int tick = 0; tick < 150; tick++) {
			dragon.travel(new Vec3(0.0, 0.0, 1.0));
			speed = dragon.position().subtract(start).horizontalDistance();
			dragon.setPos(start);
		}
		return speed;
	}

	/**
	 * The flight speed setting: a dragon made with it at 10 blocks a second carries 0.5 as its
	 * flying speed (which players' games are sent) and settles at it; one read back from a save
	 * made at another speed takes the setting now in force.
	 */
	static void flightSpeed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int before = Config.flightSpeed;
		Dragon dragon = null;
		Dragon reloaded = null;
		try {
			Config.flightSpeed = 10;
			dragon = Dragons.DRAGON.get().spawn(level, helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
			dragon.setNoAi(true);
			double attribute = dragon.getAttributeValue(Attributes.FLYING_SPEED);
			double settled = settledSpeed(dragon, Vec3.atBottomCenterOf(helper.absolutePos(MIDDLE.above(2))));
			TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			dragon.saveWithoutId(saved);
			CompoundTag tag = saved.buildResult();
			Config.flightSpeed = 20;
			reloaded = Dragons.DRAGON.get().create(level, EntitySpawnReason.LOAD);
			reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			double afterLoad = reloaded.getAttributeValue(Attributes.FLYING_SPEED);
			System.out.println(TAG + String.format("flight speed setting: at 10 blocks a second the attribute is %.3f and it settles at %.4f a tick; "
					+ "saved at 10 and read back at 20 it is %.3f", attribute, settled, afterLoad));
			helper.assertTrue(Math.abs(attribute - 0.5) < 1.0e-6, "at 10 blocks a second the flying speed should be 0.5 a tick, not " + attribute);
			helper.assertTrue(settled > 0.49 && settled <= 0.5001, "at 10 blocks a second it should settle at 0.5 a tick, not " + settled);
			helper.assertTrue(Math.abs(afterLoad - 1.0) < 1.0e-6, "a dragon read back from a save should take the setting now in force (1.0), not " + afterLoad);
		} finally {
			Config.flightSpeed = before;
			if (dragon != null) {
				dragon.discard();
			}
		}
		helper.succeed();
	}

	/** With juvenile riding off, the owner's hand sits a juvenile rather than mounting it, and an adult still carries them. */
	@SuppressWarnings("removal")
	static void juvenileRiding(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Dragon dragon = Dragons.DRAGON.get().spawn(level, helper.absolutePos(MIDDLE), EntitySpawnReason.COMMAND);
		boolean before = Config.juvenileRiding;
		try {
			owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			dragon.setNoAi(true);
			dragon.setOnGround(true);
			dragon.tame(owner);
			dragon.setAge(-100);
			Config.juvenileRiding = false;
			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			boolean rodeJuvenile = owner.getVehicle() == dragon;
			boolean satJuvenile = dragon.isOrderedToSit();
			dragon.setOrderedToSit(false);
			dragon.setAge(0);
			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			boolean rodeAdult = owner.getVehicle() == dragon;
			owner.stopRiding();
			Config.juvenileRiding = true;
			dragon.setAge(-100);
			dragon.mobInteract(owner, InteractionHand.MAIN_HAND);
			boolean rodeWhenOn = owner.getVehicle() == dragon;
			System.out.println(TAG + "juvenile riding setting: off, the juvenile carries " + rodeJuvenile + " and sits " + satJuvenile
					+ ", the adult carries " + rodeAdult + "; on, the juvenile carries " + rodeWhenOn);
			helper.assertFalse(rodeJuvenile, "with juvenile riding off, the owner's hand should not mount a juvenile");
			helper.assertTrue(satJuvenile, "with juvenile riding off, the owner's hand should sit a juvenile down instead");
			helper.assertTrue(rodeAdult, "with juvenile riding off, an adult should still carry its owner");
			helper.assertTrue(rodeWhenOn, "with juvenile riding on, the owner's hand should mount a juvenile");
		} finally {
			Config.juvenileRiding = before;
			owner.stopRiding();
			dragon.discard();
			level.getServer().getPlayerList().remove(owner);
		}
		helper.succeed();
	}

	static void nestSpacing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		int before = Config.nestSpacing;
		try {
			for (String name : new String[] { "nests", "end_nests" }) {
				StructureSet set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
						.getValueOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, Dracora.id(name)));
				helper.assertTrue(set.placement() instanceof RandomSpreadStructurePlacement, name + " should spread like vanilla's random spread, so /locate finds them");
				RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
				Config.nestSpacing = 100;
				int usual = placement.spacing();
				int usualCount = nestChunks(state, placement);
				Config.nestSpacing = 200;
				int wide = placement.spacing();
				int wideCount = nestChunks(state, placement);
				Config.nestSpacing = 50;
				int close = placement.spacing();
				int closeCount = nestChunks(state, placement);
				Config.nestSpacing = 0;
				int offCount = nestChunks(state, placement);
				System.out.println(TAG + "nest spacing (" + name + "): spacing " + usual + " / " + wide + " / " + close + " chunks at 100 / 200 / 50%; nest chunks in "
						+ AREA + "x" + AREA + ": " + usualCount + " / " + wideCount + " / " + closeCount + ", and " + offCount + " at 0");
				helper.assertTrue(wide == usual * 2 && close == usual / 2, name + ": the spacing should double at 200% and halve at 50%");
				helper.assertTrue(wideCount < usualCount && usualCount < closeCount, name + ": wider spacing should mean fewer nest chunks, and closer more");
				helper.assertTrue(offCount == 0, name + ": a spacing of 0 should mean no nests (" + offCount + ")");
			}
		} finally {
			Config.nestSpacing = before;
		}
		helper.succeed();
	}
}
