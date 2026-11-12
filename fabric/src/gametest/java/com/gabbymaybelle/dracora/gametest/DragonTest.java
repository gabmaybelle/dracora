package com.gabbymaybelle.dracora.gametest;

import com.gabbymaybelle.dracora.Dracora;
import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.client.DragonRenderer;
import com.gabbymaybelle.dracora.client.EggTint;
import com.gabbymaybelle.dracora.dragon.Birthplace;
import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.dragon.Flight;
import com.gabbymaybelle.dracora.dragon.Stage;
import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The mod in a real game: two dragons stand in front of the player, one born on the flat world's
 * plains (tamed and sitting) and one on a patch turned into End biome (standing), each wearing
 * its birthplace's colour, with an egg in a jungle colour between them beside a campfire. The
 * player's game gets the colours, the sitting pose, Dracora's renderer and the egg's tint, and
 * all of it comes back after the world is saved, closed and opened again. Then the player rides:
 * they walk, take off, hover, fly and get off a grown dragon with their own keys, and hop a juvenile.
 * Run with {@code ./gradlew :fabric:runClientGameTest}; screenshots land in
 * fabric/build/run/clientGameTest/screenshots.
 */
public class DragonTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();
	private UUID plainsDragon;
	private UUID endDragon;
	private int plainsColour;
	private int endColour;
	private UUID mount;
	private final List<UUID> galleryDragons = new ArrayList<>();
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
			ride(ctx, world.getServer());
			gallery(ctx, world.getServer());
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

	/**
	 * A real ride, with the player's own keys: mounted on an adult of their own they walk it, take
	 * off, hover, fly level at its top speed and get off in the air, and the dragon glides down
	 * unhurt. Then the same dragon as a juvenile hops and comes back down.
	 */
	private void ride(ClientGameTestContext ctx, TestServerContext server) {
		TestInput input = ctx.getInput();
		Vec3 ground = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			Dragon dragon = Dragons.DRAGON.get().spawn(player.level(), player.blockPosition().offset(0, 0, -12), EntitySpawnReason.COMMAND);
			dragon.tame(player);
			mount = dragon.getUUID();
			dragon.mobInteract(player, InteractionHand.MAIN_HAND);
			return dragon.position();
		});
		ctx.waitTicks(10);
		check(ctx.computeOnClient(mc -> mc.player.getVehicle() instanceof Dragon), "the owner's empty hand puts them on their dragon's back, in their own game too");
		// Face north, away from the other dragons, looking level.
		ctx.runOnClient(mc -> {
			mc.player.setYRot(180.0F);
			mc.player.setXRot(0.0F);
		});

		input.holdKey(options -> options.keyUp);
		ctx.waitTicks(40);
		input.releaseKey(options -> options.keyUp);
		Vec3 walked = mountAt(server);
		double walkedFar = walked.subtract(ground).horizontalDistance();
		check(walkedFar > 3.0 && walkedFar < 15.0 && Math.abs(walked.y - ground.y) < 1.0,
				String.format("ridden on the ground, the dragon trots where its rider steers, slower than a horse (%.1f blocks in two seconds)", walkedFar));

		input.holdKey(options -> options.keyJump);
		ctx.waitTicks(40);
		input.releaseKey(options -> options.keyJump);
		Vec3 risen = mountAt(server);
		check(risen.y > ground.y + 4.0, String.format("holding jump takes a grown dragon into the air (it rose %.1f blocks)", risen.y - ground.y));
		check(ctx.computeOnClient(mc -> mc.player.getVehicle() instanceof Dragon dragon && dragon.isAirborne()), "the rider's game has the dragon in flight");

		// The climb coasts to a stop over a few seconds.
		ctx.waitTicks(80);
		Vec3 hoverFrom = mountAt(server);
		ctx.waitTicks(20);
		double drifted = mountAt(server).distanceTo(hoverFrom);
		check(drifted < 0.2, String.format("with the keys let go it hovers (it drifted %.2f blocks in a second)", drifted));

		input.holdKey(options -> options.keyUp);
		ctx.waitTicks(80);
		Vec3 from = mountAt(server);
		ctx.waitTicks(40);
		Vec3 to = mountAt(server);
		double speed = to.subtract(from).horizontalDistance() / 40.0;
		check(speed > Flight.TOP_SPEED * 0.8 && speed < Flight.TOP_SPEED * 1.1,
				String.format("in level flight it settles near its top speed of %.2f blocks a tick (measured %.3f)", Flight.TOP_SPEED, speed));
		check(Math.abs(to.y - from.y) < 1.0, String.format("level flight holds its height (it changed %.2f blocks)", to.y - from.y));
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		ctx.waitTicks(5);
		ctx.takeScreenshot("flying");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		input.releaseKey(options -> options.keyUp);

		double leftAt = mountAt(server).y;
		server.runOnServer(s -> player(s).stopRiding());
		ctx.waitTicks(20);
		double sinking = mountAt(server).y;
		double fallPerTick = (leftAt - sinking) / 20.0;
		check(fallPerTick > 0.05 && fallPerTick <= Flight.GLIDE_FALL + 0.02,
				String.format("left in the air by its rider, the dragon glides down (%.2f blocks a tick)", fallPerTick));
		// The flat world's ground: back where it started.
		server.waitFor(s -> mount(s) != null && Math.abs(mount(s).getY() - ground.y) < 0.5, 1200);
		ctx.waitTicks(10);
		check(server.computeOnServer(s -> mount(s).isAlive() && mount(s).getHealth() == mount(s).getMaxHealth()), "it lands unhurt");
		check(ctx.computeOnClient(mc -> mc.level != null && mc.getConnection() != null), "the player is still in the game after the flight");

		// The same dragon, a juvenile now: it carries its owner and hops, but can't stay up.
		server.runOnServer(s -> {
			mount(s).setAge(-100);
			mount(s).mobInteract(player(s), InteractionHand.MAIN_HAND);
		});
		ctx.waitTicks(20);
		check(ctx.computeOnClient(mc -> mc.player.getVehicle() instanceof Dragon dragon && dragon.getStage() == Stage.JUVENILE),
				"the owner rides a juvenile, and their game knows it is one");
		double floor = mountAt(server).y;
		input.holdKey(options -> options.keyJump);
		double highest = floor;
		for (int tick = 0; tick < 40; tick++) {
			if (tick == 4) {
				input.releaseKey(options -> options.keyJump);
			}
			ctx.waitTick();
			highest = Math.max(highest, mountAt(server).y);
		}
		ctx.waitTicks(40);
		double landed = mountAt(server).y;
		check(highest - floor > 1.5 && highest - floor < 6.0, String.format("the jump key makes a juvenile hop (it peaked %.1f blocks up)", highest - floor));
		check(Math.abs(landed - floor) < 0.5, String.format("a juvenile comes back down after its hop (it ended %.1f blocks up)", landed - floor));
		server.runOnServer(s -> player(s).stopRiding());
	}

	/**
	 * Pictures of every pose, away from everything else: a grown dragon standing, one sitting, a
	 * juvenile, a hatchling, and a grown one in the air, each in a different birthplace's colour.
	 * The checks are that all five stand where they were put in the player's game, in the right
	 * stage; the pictures are for people to look at.
	 */
	private void gallery(ClientGameTestContext ctx, TestServerContext server) {
		int[] spot = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = player.level();
			BlockPos at = player.blockPosition().offset(120, 0, 120);
			int ground = (int) Math.round(mount(s).getY());
			at = new BlockPos(at.getX(), ground, at.getZ());
			galleryDragons.clear();
			pose(level, at, 90.0F, Birthplace.JUNGLE, 0, false, false);
			pose(level, at.offset(9, 0, 0), 90.0F, Birthplace.END, 0, true, false);
			pose(level, at.offset(16, 0, 0), 90.0F, Birthplace.SNOWY, -Stage.STAGE_TICKS / 2, false, false);
			pose(level, at.offset(21, 0, 0), 90.0F, Birthplace.WARM, Stage.HATCH_AGE, false, false);
			pose(level, at.offset(6, 8, -10), 90.0F, Birthplace.WATER, 0, false, true);
			return new int[] { at.getX(), at.getY(), at.getZ() };
		});
		int x = spot[0];
		int y = spot[1];
		int z = spot[2];
		server.runCommand("gamemode spectator @a");
		server.runCommand("time set noon");
		ctx.runOnClient(mc -> hideHud(mc, true));
		shot(ctx, server, "gallery_1_side", x + 10, y + 3, z + 20, x + 10, y + 2, z);
		shot(ctx, server, "gallery_2_standing", x - 6, y + 2, z + 7, x, y + 1, z);
		shot(ctx, server, "gallery_3_standing_front", x - 9, y + 1, z, x, y + 1, z);
		shot(ctx, server, "gallery_4_sitting", x + 4, y + 2, z + 7, x + 9, y + 1, z);
		shot(ctx, server, "gallery_5_young", x + 18, y + 1, z + 5, x + 19, y, z);
		shot(ctx, server, "gallery_6_flying", x - 2, y + 7, z - 2, x + 6, y + 9, z - 10);
		shot(ctx, server, "gallery_7_flying_below", x + 6, y + 1, z - 4, x + 6, y + 9, z - 10);
		shot(ctx, server, "gallery_8_top", x + 4, y + 16, z + 1, x + 4, y, z);
		ctx.runOnClient(mc -> hideHud(mc, false));
		check(ctx.computeOnClient(mc -> galleryDragons.stream().allMatch(id -> mc.level.getEntity(id) instanceof Dragon)), "all five posed dragons reach the player's game");
		check(ctx.computeOnClient(mc -> mc.level.getEntity(galleryDragons.get(3)) instanceof Dragon dragon && dragon.getStage() == Stage.HATCHLING
				&& mc.level.getEntity(galleryDragons.get(2)) instanceof Dragon young && young.getStage() == Stage.JUVENILE), "the player's game knows the hatchling and the juvenile");
		check(ctx.computeOnClient(mc -> mc.level.getEntity(galleryDragons.get(4)) instanceof Dragon dragon && dragon.isFlying()), "the player's game shows the dragon in the air as flying");
	}

	/** One posed dragon: still, facing {@code yaw}, in the first colour of a birthplace, at an age, sitting or held in the air. */
	private void pose(ServerLevel level, BlockPos at, float yaw, Birthplace place, int age, boolean sitting, boolean inAir) {
		Dragon dragon = Dragons.DRAGON.get().create(level, EntitySpawnReason.COMMAND);
		dragon.setNoAi(true);
		dragon.setNoGravity(inAir);
		dragon.setColour(place.colours().getFirst());
		dragon.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
		dragon.setYBodyRot(yaw);
		dragon.setYHeadRot(yaw);
		dragon.setAge(age);
		dragon.setOnGround(!inAir);
		if (sitting) {
			dragon.setInSittingPose(true);
		}
		level.addFreshEntity(dragon);
		galleryDragons.add(dragon.getUUID());
	}

	/** Moves the player (a spectator, so nothing of theirs is in the picture) and takes a picture looking at a point. */
	private void shot(ClientGameTestContext ctx, TestServerContext server, String name, int fromX, int fromY, int fromZ, int atX, int atY, int atZ) {
		server.runCommand(String.format("tp @a %d %d %d facing %d %d %d", fromX, fromY, fromZ, atX, atY, atZ));
		ctx.waitTicks(30);
		ctx.takeScreenshot(name);
	}

	/** Hides or shows the whole HUD, as F1 does, so the pictures show only the world. */
	private static void hideHud(Minecraft mc, boolean hidden) {
		if (mc.gui.hud.isHidden() != hidden) {
			mc.gui.hud.toggle();
		}
	}

	private Dragon mount(MinecraftServer server) {
		return dragon(player(server).level().getEntity(mount));
	}

	private Vec3 mountAt(TestServerContext server) {
		return server.computeOnServer(s -> mount(s).position());
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
