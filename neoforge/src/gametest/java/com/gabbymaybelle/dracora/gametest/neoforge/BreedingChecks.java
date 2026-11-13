package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.dragon.Breeding;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Stage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Breeding: two tamed adults fed raw fish lay an egg in the blend of their colours (or, one in
 * twenty, a rare colour) and then rest a Minecraft day; nothing else is food, a wild or young
 * dragon is never in love, and fish never hurries a young one's growth.
 */
final class BreedingChecks {
	private static final String TAG = "[dracora_gametest] ";
	private static final int SNOWY = 0xDCEBF5;
	private static final int JUNGLE = 0x2F6E2B;

	private BreedingChecks() {
	}

	private static Dragon adult(GameTestHelper helper, BlockPos at, int colour, ServerPlayer owner) {
		Dragon dragon = Dragons.DRAGON.get().spawn(helper.getLevel(), helper.absolutePos(at), EntitySpawnReason.COMMAND);
		helper.assertTrue(dragon != null, "a dragon should spawn");
		dragon.setColour(colour);
		if (owner != null) {
			dragon.tame(owner);
		}
		return dragon;
	}

	private static void feed(Dragon dragon, ServerPlayer player, ItemStack food) {
		player.setItemInHand(InteractionHand.MAIN_HAND, food);
		dragon.mobInteract(player, InteractionHand.MAIN_HAND);
	}

	/** Salmon and cod put a tamed adult in love; beef does not, and neither does fish for a wild adult. */
	@SuppressWarnings("removal")
	static void fish(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Dragon salmon = adult(helper, new BlockPos(2, 1, 2), SNOWY, owner);
		Dragon cod = adult(helper, new BlockPos(6, 1, 2), SNOWY, owner);
		Dragon beef = adult(helper, new BlockPos(2, 1, 6), SNOWY, owner);
		Dragon wild = adult(helper, new BlockPos(6, 1, 6), SNOWY, null);
		try {
			feed(salmon, owner, new ItemStack(Items.SALMON));
			feed(cod, owner, new ItemStack(Items.COD));
			feed(beef, owner, new ItemStack(Items.COOKED_BEEF));
			feed(wild, owner, new ItemStack(Items.SALMON));
			System.out.println(TAG + "fish: in love salmon " + salmon.isInLove() + ", cod " + cod.isInLove() + ", beef " + beef.isInLove() + ", wild " + wild.isInLove());
			helper.assertTrue(salmon.isInLove(), "raw salmon should put a tamed adult in love");
			helper.assertTrue(cod.isInLove(), "raw cod should put a tamed adult in love");
			helper.assertFalse(beef.isInLove(), "beef is not a dragon's food");
			helper.assertFalse(wild.isInLove(), "a wild dragon should not fall in love");
		} finally {
			for (Dragon dragon : List.of(salmon, cod, beef, wild)) {
				dragon.discard();
			}
			level.getServer().getPlayerList().remove(owner);
		}
		helper.succeed();
	}

	/** Fish neither puts a young dragon in love nor makes it grow faster. */
	@SuppressWarnings("removal")
	static void young(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Dragon dragon = adult(helper, new BlockPos(4, 1, 4), SNOWY, owner);
		try {
			dragon.setAge(-100);
			feed(dragon, owner, new ItemStack(Items.SALMON, 4));
			int afterJuvenile = dragon.getAge();
			dragon.startAsHatchling();
			feed(dragon, owner, new ItemStack(Items.SALMON, 4));
			System.out.println(TAG + "young: juvenile age after fish " + afterJuvenile + ", hatchling age after fish " + dragon.getAge() + ", in love " + dragon.isInLove());
			helper.assertTrue(afterJuvenile == -100, "fish should not hurry a juvenile's growth (age " + afterJuvenile + ")");
			helper.assertTrue(dragon.getAge() == Stage.HATCH_AGE, "fish should not hurry a hatchling's growth (age " + dragon.getAge() + ")");
			helper.assertFalse(dragon.isInLove(), "a young dragon should not fall in love");
		} finally {
			dragon.discard();
			level.getServer().getPlayerList().remove(owner);
		}
		helper.succeed();
	}

	/**
	 * Two tamed adults side by side, fed salmon, find each other and lay one egg item in the blend
	 * of their colours (or a rare one); both then rest a day, so fish does nothing until it passes.
	 */
	@SuppressWarnings("removal")
	static void lays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		Dragon a = adult(helper, new BlockPos(3, 1, 4), SNOWY, owner);
		Dragon b = adult(helper, new BlockPos(5, 1, 4), JUNGLE, owner);
		feed(a, owner, new ItemStack(Items.SALMON));
		feed(b, owner, new ItemStack(Items.SALMON));
		helper.succeedWhen(() -> {
			List<ItemEntity> eggs = level.getEntitiesOfClass(ItemEntity.class, a.getBoundingBox().inflate(8.0), e -> e.getItem().is(Eggs.ITEM.get()));
			helper.assertTrue(eggs.size() == 1, "the pair should lay one egg (" + eggs.size() + " so far)");
			DyedItemColor dyed = eggs.get(0).getItem().get(DataComponents.DYED_COLOR);
			int colour = dyed == null ? -1 : dyed.rgb() & 0xFFFFFF;
			int blend = Breeding.blend(SNOWY, JUNGLE);
			System.out.println(TAG + "lays: egg " + String.format("%06X", colour) + " (blend " + String.format("%06X", blend) + "), ages " + a.getAge() + " and " + b.getAge());
			helper.assertTrue(colour == blend || Breeding.isMutation(colour), String.format("the egg should be the blend %06X or a rare colour, not %06X", blend, colour));
			helper.assertTrue(a.getAge() > Breeding.REST_TICKS - 200 && b.getAge() > Breeding.REST_TICKS - 200,
					"both parents should rest about a day (ages " + a.getAge() + " and " + b.getAge() + ")");
			helper.assertTrue(a.getStage() == Stage.ADULT, "a resting parent is still an adult");
			feed(a, owner, new ItemStack(Items.SALMON));
			helper.assertFalse(a.isInLove(), "a resting parent should not fall in love again");
			a.setAge(0);
			feed(a, owner, new ItemStack(Items.SALMON));
			helper.assertTrue(a.isInLove(), "once the day has passed it should fall in love again");
			eggs.forEach(ItemEntity::discard);
			a.discard();
			b.discard();
			level.getServer().getPlayerList().remove(owner);
		});
	}
}
