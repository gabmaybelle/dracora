package com.gabbymaybelle.dracora.gametest.neoforge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Dracora's NeoForge game tests: a little mod of its own that only {@code :neoforge:runGameTestServer}
 * loads, handing vanilla's test framework one test per check (the checks are in {@link DragonChecks} and {@link EggChecks}).
 * The server has no window, so these run anywhere.
 */
@Mod(DracoraGameTests.MOD_ID)
public final class DracoraGameTests {
	static final String MOD_ID = "dracora_gametest";
	/** Vanilla's 1x1x1 template, for tests that build nothing. */
	private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");
	/** An empty 9x7x9 room of the mod's own (tools/make-test-room.py): a dragon is big, and everything must stay inside the test's loaded, ticking bounds. */
	private static final Identifier ROOM = id("room");

	/** The test functions by name; the test instance of the same name runs each. */
	private static final Map<String, Consumer<GameTestHelper>> FUNCTIONS = new LinkedHashMap<>();

	static {
		FUNCTIONS.put("loaded", DragonChecks::loaded);
		FUNCTIONS.put("a_dragon_lives", DragonChecks::lives);
		FUNCTIONS.put("a_birthplace_sets_the_colour", DragonChecks::birthplaces);
		FUNCTIONS.put("the_end_colours_its_dragons", DragonChecks::endColours);
		FUNCTIONS.put("its_owner_sits_it_down", DragonChecks::sits);
		FUNCTIONS.put("a_dragon_keeps_its_colour_through_a_save", DragonChecks::saves);
		FUNCTIONS.put("the_egg_is_registered", EggChecks::registered);
		FUNCTIONS.put("campfires_and_lava_warm_an_egg", EggChecks::warmthRule);
		FUNCTIONS.put("an_egg_takes_its_colour_where_it_is_set_down", EggChecks::colour);
		FUNCTIONS.put("a_warm_egg_hatches_for_its_placer", EggChecks::hatches);
		FUNCTIONS.put("a_cold_egg_waits", EggChecks::waits);
		FUNCTIONS.put("an_egg_keeps_its_colour_through_a_save", EggChecks::saves);
	}

	public DracoraGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, DracoraGameTests::registerFunctions);
		modBus.addListener(RegisterGameTestsEvent.class, DracoraGameTests::registerTests);
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Test functions live in a built-in registry, so they go in with the mod's other registrations. */
	private static void registerFunctions(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
			FUNCTIONS.forEach((name, function) -> event.register(Registries.TEST_FUNCTION, id(name), () -> function));
		}
	}

	/** Test instances and their environments are registered as the test world's data loads. */
	private static void registerTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"), new TestEnvironmentDefinition.SetGameRules(GameRuleMap.of()));
		test(event, "loaded", plain, EMPTY, 20);
		test(event, "a_dragon_lives", plain, ROOM, 100);
		test(event, "a_birthplace_sets_the_colour", plain, EMPTY, 20);
		test(event, "the_end_colours_its_dragons", plain, ROOM, 100);
		test(event, "its_owner_sits_it_down", plain, ROOM, 60);
		test(event, "a_dragon_keeps_its_colour_through_a_save", plain, ROOM, 60);
		test(event, "the_egg_is_registered", plain, EMPTY, 20);
		test(event, "campfires_and_lava_warm_an_egg", plain, EMPTY, 20);
		test(event, "an_egg_takes_its_colour_where_it_is_set_down", plain, ROOM, 60);
		test(event, "a_warm_egg_hatches_for_its_placer", plain, ROOM, 60);
		test(event, "a_cold_egg_waits", plain, ROOM, 60);
		test(event, "an_egg_keeps_its_colour_through_a_save", plain, ROOM, 60);
	}

	private static void test(RegisterGameTestsEvent event, String name, Holder<TestEnvironmentDefinition<?>> environment, Identifier structure, int maxTicks) {
		ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
		event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(environment, structure, maxTicks, 0, true)));
	}
}
