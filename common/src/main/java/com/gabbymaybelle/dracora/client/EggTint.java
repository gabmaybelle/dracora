package com.gabbymaybelle.dracora.client;

import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import java.util.List;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Tints the egg block with the colour its block entity carries, the way a dragon's texture is tinted. */
public final class EggTint implements BlockTintSource {
	/** The egg's one tint layer, for the loaders' block colour registration. */
	public static final List<BlockTintSource> LAYERS = List.of(new EggTint());

	private EggTint() {
	}

	/** With no world to ask (a held or falling block), the egg is drawn untinted. */
	@Override
	public int color(BlockState state) {
		return -1;
	}

	@Override
	public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		EggBlockEntity egg = EggBlock.eggAt(level, pos);
		return egg == null ? -1 : 0xFF000000 | egg.colour();
	}
}
