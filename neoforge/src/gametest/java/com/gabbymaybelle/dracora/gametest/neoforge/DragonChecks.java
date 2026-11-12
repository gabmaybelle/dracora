package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.dragon.Birthplace;
import com.gabbymaybelle.dracora.dragon.Dragon;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.fml.ModList;

/**
 * The checks: the mod loaded; a dragon lives in the world with its attributes and a colour from
 * the place it appeared; each kind of biome maps to its own palette and the End to the End's;
 * its owner (sneaking) sits it down and stands it up; and colour, owner and sitting survive a save.
 */
final class DragonChecks {
	private static final String TAG = "[dracora_gametest] ";

	private DragonChecks() {
	}

	/** The compat mods asked for (-PexpectMods) are loaded, and the dragon and its egg are registered. */
	static void loaded(GameTestHelper helper) {
		String expected = System.getProperty("dracora.expectMods", "");
		for (String id : expected.split(",")) {
			if (!id.isBlank()) {
				helper.assertTrue(ModList.get().isLoaded(id.trim()), "the compat mod " + id.trim() + " should be loaded");
			}
		}
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Dragons.DRAGON_ID), "the dragon should be registered");
		helper.assertTrue(Dragons.DRAGON_SPAWN_EGG.get() instanceof SpawnEggItem, "the dragon spawn egg should be registered");
		System.out.println(TAG + "loaded: dragon and egg registered; compat mods " + (expected.isBlank() ? "none" : expected));
		helper.succeed();
	}

	/** A dragon spawned in the room is alive 40 ticks later, has 40 health, is wild, and wears a colour from the room's own biome. */
	static void lives(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
		Dragon dragon = Dragons.DRAGON.get().spawn(level, at, EntitySpawnReason.COMMAND);
		helper.assertTrue(dragon != null, "a dragon should spawn");
		Birthplace here = Birthplace.of(level.getBiome(at));
		helper.runAfterDelay(40, () -> {
			try {
				double health = dragon.getAttributeValue(Attributes.MAX_HEALTH);
				System.out.println(TAG + "dragon: alive " + dragon.isAlive() + ", health " + health + ", colour " + hex(dragon.getColour())
						+ ", birthplace " + dragon.birthplace() + " (the room is " + here + "), tame " + dragon.isTame());
				helper.assertTrue(dragon.isAlive(), "the dragon should be alive after 40 ticks");
				helper.assertTrue(health == 40.0, "the dragon should have 40 health (got " + health + ")");
				helper.assertTrue(dragon.birthplace() == here, "a dragon born in a " + here + " biome should wear that palette, not " + dragon.birthplace());
				helper.assertFalse(dragon.isTame(), "a fresh dragon should be wild");
			} finally {
				dragon.discard();
			}
			helper.succeed();
		});
	}

	/** Each kind of biome maps to its own palette: the rule itself, asked for the biomes that stand for each. */
	static void birthplaces(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		Map<ResourceKey<Biome>, Birthplace> expected = Map.of(
				Biomes.SNOWY_SLOPES, Birthplace.SNOWY,
				Biomes.JUNGLE, Birthplace.JUNGLE,
				Biomes.BADLANDS, Birthplace.WARM,
				Biomes.SAVANNA, Birthplace.WARM,
				Biomes.TAIGA, Birthplace.FOREST,
				Biomes.OCEAN, Birthplace.WATER,
				Biomes.PLAINS, Birthplace.PLAINS,
				Biomes.THE_END, Birthplace.END,
				Biomes.END_HIGHLANDS, Birthplace.END);
		expected.forEach((biome, place) -> {
			Holder<Biome> holder = biomes.getOrThrow(biome);
			Birthplace got = Birthplace.of(holder);
			System.out.println(TAG + biome.identifier() + " -> " + got);
			helper.assertTrue(got == place, biome.identifier() + " should be a " + place + " birthplace, not " + got);
		});
		helper.succeed();
	}

	/** With the room's biome turned into the End, a dragon spawned there wears an End colour. */
	static void endColours(GameTestHelper helper) {
		helper.setBiome(Biomes.THE_END);
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
		helper.assertTrue(level.getBiome(at).is(Biomes.THE_END), "the room should now be End biome");
		Dragon dragon = Dragons.DRAGON.get().spawn(level, at, EntitySpawnReason.COMMAND);
		helper.assertTrue(dragon != null, "a dragon should spawn in the End biome");
		try {
			System.out.println(TAG + "End dragon: colour " + hex(dragon.getColour()) + ", birthplace " + dragon.birthplace());
			helper.assertTrue(dragon.birthplace() == Birthplace.END, "a dragon born in the End should wear an End colour, not " + dragon.birthplace());
		} finally {
			dragon.discard();
		}
		helper.succeed();
	}

	/** Tamed by a player, a dragon belongs to them; their sneaking empty hand sits it down and stands it up again. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: taming needs a player
	static void sits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Dragon dragon = Dragons.DRAGON.get().spawn(level, at, EntitySpawnReason.COMMAND);
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			dragon.setNoAi(true);
			dragon.setOnGround(true);
			helper.assertFalse(dragon.isTame(), "a fresh dragon should be wild");
			dragon.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertFalse(dragon.isOrderedToSit(), "a wild dragon should not sit for a stranger");
			dragon.tame(player);
			helper.assertTrue(dragon.isOwnedBy(player), "the tamed dragon should belong to the player");
			// Sneaking: the plain hand mounts a grown dragon (see GrowthChecks.rides).
			player.setShiftKeyDown(true);
			dragon.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(dragon.isOrderedToSit(), "its owner's sneaking empty hand should sit a tamed dragon down");
			dragon.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertFalse(dragon.isOrderedToSit(), "a second use should stand it up");
			System.out.println(TAG + "sitting: owned " + dragon.isOwnedBy(player) + ", sit toggles ok");
		} finally {
			dragon.discard();
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/** Saved and read back into a new dragon, the colour, the owner and the order to sit all come through. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: an owner to save
	static void saves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Dragon dragon = Dragons.DRAGON.get().spawn(level, at, EntitySpawnReason.COMMAND);
		Dragon copy = Dragons.DRAGON.get().create(level, EntitySpawnReason.LOAD);
		try {
			dragon.tame(player);
			dragon.setOrderedToSit(true);
			int colour = dragon.getColour();
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			dragon.saveWithoutId(output);
			CompoundTag tag = output.buildResult();
			helper.assertTrue(tag.getIntOr("Colour", -1) == colour, "the save should carry the colour " + hex(colour));
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
			System.out.println(TAG + "save: colour " + hex(copy.getColour()) + ", owned " + copy.isOwnedBy(player) + ", sitting " + copy.isOrderedToSit());
			helper.assertTrue(copy.getColour() == colour, "the colour should come back from a save (got " + hex(copy.getColour()) + ")");
			helper.assertTrue(copy.isOwnedBy(player), "the owner should come back from a save");
			helper.assertTrue(copy.isOrderedToSit(), "the order to sit should come back from a save");
		} finally {
			dragon.discard();
			copy.discard();
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	private static String hex(int colour) {
		return String.format("%06X", colour);
	}
}
