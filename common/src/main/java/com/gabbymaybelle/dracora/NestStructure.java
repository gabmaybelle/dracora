package com.gabbymaybelle.dracora;

import com.gabbymaybelle.dracora.platform.Services;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * A nest: vanilla's jigsaw structure, read from the same fields as {@code minecraft:jigsaw}, that
 * keeps off water. Where a nest would land on a pond, a river or the sea, its floor sits below the
 * water line and the bowl floods to the egg, so a spot with water standing anywhere under the nest
 * is refused and the nest goes elsewhere. {@code /locate} asks the same question, so it never
 * points at a refused spot. The structure files name it as {@code dracora:nest}.
 */
public final class NestStructure extends Structure {
	public static final MapCodec<NestStructure> CODEC = JigsawStructure.CODEC.xmap(NestStructure::new, nest -> nest.jigsaw);

	public static final Supplier<StructureType<?>> TYPE =
			Services.PLATFORM.register(Registries.STRUCTURE_TYPE, Dracora.id("nest"), () -> (StructureType<NestStructure>) () -> CODEC);

	/** How far from the nest's middle its rim reaches, in blocks: where water is looked for. */
	public static final int REACH = 4;

	private final JigsawStructure jigsaw;

	private NestStructure(JigsawStructure jigsaw) {
		super(new StructureSettings(jigsaw.biomes(), jigsaw.spawnOverrides(), jigsaw.step(), jigsaw.terrainAdaptation()));
		this.jigsaw = jigsaw;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return jigsaw.findGenerationPoint(context).filter(stub -> isDry(context, stub.position()));
	}

	/** Whether the ground under the nest's middle and the four edges of its rim is open to the sky, with no water standing on it. */
	static boolean isDry(GenerationContext context, BlockPos middle) {
		ChunkGenerator generator = context.chunkGenerator();
		int[][] points = {{0, 0}, {REACH, 0}, {-REACH, 0}, {0, REACH}, {0, -REACH}};
		for (int[] point : points) {
			int x = middle.getX() + point[0];
			int z = middle.getZ() + point[1];
			int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			if (surface != ground) {
				return false;
			}
		}
		return true;
	}

	@Override
	public StructureType<?> type() {
		return TYPE.get();
	}

	/** Touching the class registers the structure type. */
	public static void register() {
	}
}
