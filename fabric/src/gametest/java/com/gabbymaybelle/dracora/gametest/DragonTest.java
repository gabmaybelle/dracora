package com.gabbymaybelle.dracora.gametest;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import com.gabbymaybelle.dracora.client.EggTint;
import com.gabbymaybelle.dracora.dragon.Birthplace;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mod in a real game: two dragons stand in front of the player, one born on the flat world's
 * plains (tamed and sitting) and one on a patch turned into End biome (standing), each wearing
 * its birthplace's colour, with an egg in a jungle colour between them beside a campfire. The
 * player's game gets the colours, the sitting pose, Dracora's renderer and the egg's tint, and
 * all of it comes back after the world is saved, closed and opened again.
 * Run with {@code ./gradlew :fabric:runClientGameTest}; screenshots land in
 * fabric/build/run/clientGameTest/screenshots.
 */
public class DragonTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();
	private UUID plainsDragon;
	private UUID endDragon;
	private int plainsColour;
	private int endColour;
	private BlockPos eggPos;
	private int eggColour;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		loaded();
		TestWorldSave save;
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			// Keep the screenshots clean: no command chatter, and the taming advancement granted now
			// rather than popping up over the dragons later.
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule announceAdvancements false");
			server.runCommand("gamerule doMobSpawning false");
			server.runCommand("gamemode creative @a");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			server.runCommand("advancement grant @a only minecraft:husbandry/tame_an_animal");
			// Face south, looking slightly down, where the dragons will stand.
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 12");
			ctx.waitTicks(5);
			server.runOnServer(this::summon);
			ctx.waitTicks(20);
			server.runOnServer(this::onServer);
			ctx.runOnClient(mc -> onClient(mc, "in a new world"));
			world.getConnection().waitForChunksRender();
			ctx.takeScreenshot("dragons");
			save = world.getWorldSave();
		}
		// Saved and closed: open it again and check the dragons came back as they were.
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			ctx.waitTicks(20);
			world.getServer().runOnServer(this::afterReopen);
			ctx.runOnClient(mc -> onClient(mc, "after reopening"));
			ctx.takeScreenshot("dragons_after_reopening");
		}
		if (!failures.isEmpty()) {
			throw new AssertionError(failures.size() + " check(s) failed:\n - " + String.join("\n - ", failures));
		}
	}

	/** The compat mods asked for (-PexpectMods) are loaded, and the dragon and its egg are registered. */
	private void loaded() {
		String expected = System.getProperty("dracora.expectMods", "");
		for (String id : expected.split(",")) {
			if (!id.isBlank()) {
				check(FabricLoader.getInstance().isModLoaded(id.trim()), "the compat mod " + id.trim() + " is loaded");
			}
		}
		check(BuiltInRegistries.ENTITY_TYPE.containsKey(Dragons.DRAGON_ID), "the dragon is registered");
		check(Dragons.DRAGON_SPAWN_EGG.get() instanceof SpawnEggItem, "the dragon spawn egg is registered");
		check(BuiltInRegistries.BLOCK.containsKey(Eggs.ID) && BuiltInRegistries.ITEM.containsKey(Eggs.ID), "the egg block and its item are registered");
	}

	/** Two dragons in front of the player: one on the plains, tamed and sitting; one on a patch of End biome. */
	private void summon(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		BlockPos base = player.blockPosition();
		BlockPos plainsPos = base.offset(4, 0, 10);
		BlockPos endPos = base.offset(-5, 0, 11);
		// The flat world is all plains; give the End dragon's spot its own End biome first.
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
				String.format("fillbiome %d %d %d %d %d %d minecraft:the_end",
						endPos.getX() - 3, endPos.getY() - 3, endPos.getZ() - 3, endPos.getX() + 3, endPos.getY() + 6, endPos.getZ() + 3));

		Dragon plains = Dragons.DRAGON.get().spawn(level, plainsPos, EntitySpawnReason.COMMAND);
		Dragon end = Dragons.DRAGON.get().spawn(level, endPos, EntitySpawnReason.COMMAND);
		check(plains != null && end != null, "both dragons spawn");
		if (plains == null || end == null) {
			return;
		}
		for (Dragon dragon : new Dragon[] { plains, end }) {
			dragon.setNoAi(true);
			// Side on to the player, so the screenshot shows the wings, legs and tail.
			dragon.snapTo(dragon.getX(), dragon.getY(), dragon.getZ(), dragon == plains ? 90.0F : -90.0F, 0.0F);
			dragon.setYBodyRot(dragon.getYRot());
			dragon.setYHeadRot(dragon.getYRot());
		}
		plainsDragon = plains.getUUID();
		endDragon = end.getUUID();
		plainsColour = plains.getColour();
		endColour = end.getColour();
		check(plains.birthplace() == Birthplace.PLAINS, String.format("a dragon born on plains has a plains colour, not %06X", plainsColour));
		check(end.birthplace() == Birthplace.END, String.format("a dragon born in the End has an End colour, not %06X", endColour));

		plains.tame(player);
		plains.setOrderedToSit(true);
		// The AI is off, so set the pose the sit goal would.
		plains.setInSittingPose(true);

		// Between them, an egg in a jungle colour that the player set down, beside a campfire.
		eggPos = base.offset(0, 0, 7);
		eggColour = Birthplace.JUNGLE.colours().getFirst();
		level.setBlockAndUpdate(eggPos, Eggs.BLOCK.get().defaultBlockState());
		level.setBlockAndUpdate(eggPos.east(2), Blocks.CAMPFIRE.defaultBlockState());
		EggBlockEntity egg = EggBlock.eggAt(level, eggPos);
		check(egg != null, "an egg stands in the world");
		if (egg != null) {
			egg.setColour(eggColour);
			egg.setOwner(player.getUUID());
		}
	}

	/** A few ticks later, on the server: nothing has drifted. */
	private void onServer(MinecraftServer server) {
		ServerPlayer player = player(server);
		Dragon plains = dragon(player.level().getEntity(plainsDragon));
		Dragon end = dragon(player.level().getEntity(endDragon));
		check(plains != null && end != null, "both dragons are still there on the server");
		if (plains == null || end == null) {
			return;
		}
		check(plains.getColour() == plainsColour && end.getColour() == endColour, "the dragons keep their colours");
		check(plains.isOwnedBy(player), "the tamed dragon belongs to the player");
		check(plains.isInSittingPose(), "the tamed dragon sits when told to");
		check(!end.isTame(), "the other dragon stays wild");
	}

	/** In the player's own game: the dragons arrived with their colours and are drawn by Dracora's renderer. */
	private void onClient(Minecraft mc, String when) {
		Dragon plains = dragon(mc.level.getEntity(plainsDragon));
		Dragon end = dragon(mc.level.getEntity(endDragon));
		check(plains != null && end != null, "both dragons reach the player's game " + when);
		if (plains == null || end == null) {
			return;
		}
		check(plains.getColour() == plainsColour, String.format("the player's game shows the plains dragon's colour %06X %s, not %06X",
				plainsColour, when, plains.getColour()));
		check(end.getColour() == endColour, String.format("the player's game shows the End dragon's colour %06X %s, not %06X",
				endColour, when, end.getColour()));
		check(plains.isInSittingPose(), "the player's game shows the tamed dragon sitting " + when);
		check(mc.getEntityRenderDispatcher().getRenderer(plains) instanceof DragonRenderer, "dragons are drawn by Dracora's renderer " + when);

		EggBlockEntity egg = EggBlock.eggAt(mc.level, eggPos);
		check(egg != null, "the egg reaches the player's game " + when);
		if (egg == null) {
			return;
		}
		check(egg.colour() == eggColour, String.format("the player's game knows the egg's colour %06X %s, not %06X", eggColour, when, egg.colour()));
		BlockState state = mc.level.getBlockState(eggPos);
		BlockTintSource tint = mc.getBlockColors().getTintSource(state, 0);
		check(tint instanceof EggTint, "the egg block is tinted by Dracora " + when);
		if (tint != null) {
			int drawn = tint.colorInWorld(state, mc.level, eggPos) & 0xFFFFFF;
			check(drawn == eggColour, String.format("the egg is drawn in its colour %06X %s, not %06X", eggColour, when, drawn));
		}
	}

	/** After the world was saved, closed and opened again: colour, owner and sitting came back. */
	private void afterReopen(MinecraftServer server) {
		ServerPlayer player = player(server);
		Dragon plains = dragon(player.level().getEntity(plainsDragon));
		Dragon end = dragon(player.level().getEntity(endDragon));
		check(plains != null && end != null, "both dragons are there after reopening the world");
		if (plains == null || end == null) {
			return;
		}
		check(plains.getColour() == plainsColour, String.format("the plains dragon keeps its colour %06X after reopening, not %06X",
				plainsColour, plains.getColour()));
		check(end.getColour() == endColour, String.format("the End dragon keeps its colour %06X after reopening, not %06X",
				endColour, end.getColour()));
		check(plains.isOwnedBy(player), "the tamed dragon still belongs to the player after reopening");
		check(plains.isOrderedToSit(), "the tamed dragon is still told to sit after reopening");

		EggBlockEntity egg = EggBlock.eggAt(player.level(), eggPos);
		check(egg != null, "the egg is there after reopening the world");
		if (egg != null) {
			check(egg.colour() == eggColour, String.format("the egg keeps its colour %06X after reopening, not %06X", eggColour, egg.colour()));
			check(player.getUUID().equals(egg.owner()), "the egg still remembers who set it down after reopening");
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static Dragon dragon(Entity entity) {
		return entity instanceof Dragon dragon ? dragon : null;
	}

	private void check(boolean condition, String what) {
		if (!condition) {
			Dracora.LOGGER.error("[dracora_gametest] FAIL: {}", what);
			failures.add(what);
		} else {
			Dracora.LOGGER.info("[dracora_gametest] ok: {}", what);
		}
	}
}
