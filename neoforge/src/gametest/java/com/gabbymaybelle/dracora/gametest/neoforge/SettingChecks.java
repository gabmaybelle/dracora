package com.gabbymaybelle.dracora.gametest.neoforge;

import com.gabbymaybelle.dracora.Config;
import com.gabbymaybelle.dracora.Dracora;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** The settings: the nest spacing stretches and shrinks both nests' spread, and 0 means no nests. */
final class SettingChecks {
	private static final String TAG = "[dracora_gametest] ";
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
