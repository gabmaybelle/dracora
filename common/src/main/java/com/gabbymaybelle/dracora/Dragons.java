package com.gabbymaybelle.dracora;

import com.gabbymaybelle.dracora.dragon.Dragon;
import com.gabbymaybelle.dracora.platform.Services;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

/** The dragon, its spawn egg and attributes, registered through the loader's {@link Services#PLATFORM}. */
public final class Dragons {
	public static final Identifier DRAGON_ID = Dracora.id("dragon");
	private static final ResourceKey<Item> SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, Dracora.id("dragon_spawn_egg"));

	/** An adult dragon's size, in blocks. Growth stages will scale it. */
	public static final float ADULT_WIDTH = 2.5F;
	public static final float ADULT_HEIGHT = 2.0F;

	/** Dragons never spawn on their own: they come from nests and eggs, so the spawn list is empty. */
	public static final Supplier<EntityType<Dragon>> DRAGON = Services.PLATFORM.registerMob(new MobSpec<>(
			DRAGON_ID, Dragon::new, MobCategory.CREATURE, ADULT_WIDTH, ADULT_HEIGHT, 1.7F, Dragon::createAttributes,
			SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, Animal::checkAnimalSpawnRules, List.of()));

	public static final Supplier<Item> DRAGON_SPAWN_EGG = Services.PLATFORM.register(Registries.ITEM, SPAWN_EGG_KEY.identifier(),
			() -> new SpawnEggItem(new Item.Properties().setId(SPAWN_EGG_KEY).spawnEgg(DRAGON.get())));

	private Dragons() {
	}

	/** Touching the class registers the dragon; this puts its egg in the creative menu. */
	public static void register() {
		ResourceKey<CreativeModeTab> eggs = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));
		Services.PLATFORM.addToCreativeTab(eggs, DRAGON_SPAWN_EGG);
	}
}
