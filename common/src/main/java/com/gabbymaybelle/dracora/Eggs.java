package com.gabbymaybelle.dracora;

import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import com.gabbymaybelle.dracora.platform.Services;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The dragon's egg: its block, item and block entity, registered through the loader's {@link Services#PLATFORM}. */
public final class Eggs {
	public static final Identifier ID = Dracora.id("dragon_egg");
	private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, ID);
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);

	public static final Supplier<Block> BLOCK = Services.PLATFORM.register(Registries.BLOCK, ID, () -> new EggBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.STONE).noOcclusion().setId(BLOCK_KEY)));

	/** The item carries the egg's colour as vanilla's dyed colour (which tints it); the "Dyed" tooltip line is hidden, since nothing dyes an egg. */
	public static final Supplier<Item> ITEM = Services.PLATFORM.register(Registries.ITEM, ID,
			() -> new BlockItem(BLOCK.get(), new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()
					.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true))));

	public static final Supplier<BlockEntityType<?>> BLOCK_ENTITY = Services.PLATFORM.register(Registries.BLOCK_ENTITY_TYPE, ID,
			() -> new BlockEntityType<>(EggBlockEntity::new, Set.of(BLOCK.get())));

	private Eggs() {
	}

	/** An egg item of one colour. */
	public static ItemStack coloured(int colour) {
		ItemStack stack = new ItemStack(ITEM.get());
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(colour & 0xFFFFFF));
		return stack;
	}

	/** Touching the class registers the egg; this puts a plain one (it takes its colour where it is set down) in the creative menu. */
	public static void register() {
		ResourceKey<CreativeModeTab> natural = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("natural_blocks"));
		Services.PLATFORM.addToCreativeTab(natural, ITEM);
	}
}
