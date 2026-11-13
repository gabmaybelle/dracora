package com.gabbymaybelle.dracora;

import com.gabbymaybelle.dracora.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Vanilla's random spread with its distances scaled by {@link Config#nestSpacing}, so the nests'
 * structure sets keep their usual numbers in data and the setting stretches or shrinks them. It
 * reads the same fields as {@code minecraft:random_spread}; the sets name it as
 * {@code dracora:nest_spread}. At a setting of 0 no chunk is ever a nest's.
 */
public final class NestSpread extends RandomSpreadStructurePlacement {
	public static final MapCodec<NestSpread> CODEC = RecordCodecBuilder.<NestSpread>mapCodec(
			i -> placementCodec(i)
					.and(i.group(
							Codec.intRange(1, 4096).fieldOf("spacing").forGetter(spread -> spread.usualSpacing),
							Codec.intRange(0, 4096).fieldOf("separation").forGetter(spread -> spread.usualSeparation),
							RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(spread -> spread.spreadType)))
					.apply(i, NestSpread::new))
			.validate(spread -> spread.usualSpacing <= spread.usualSeparation
					? DataResult.error(() -> "Spacing has to be larger than separation") : DataResult.success(spread));

	public static final Supplier<MapCodec<? extends StructurePlacement>> TYPE =
			Services.PLATFORM.register(Registries.STRUCTURE_PLACEMENT, Dracora.id("nest_spread"), () -> CODEC);

	private final int usualSpacing;
	private final int usualSeparation;
	private final RandomSpreadType spreadType;

	private NestSpread(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt, Optional<ExclusionZone> exclusionZone,
			int spacing, int separation, RandomSpreadType spreadType) {
		super(locateOffset, method, frequency, salt, exclusionZone, spacing, separation, spreadType);
		this.usualSpacing = spacing;
		this.usualSeparation = separation;
		this.spreadType = spreadType;
	}

	/** The spacing now, by the setting (also what /locate steps by). */
	@Override
	public int spacing() {
		return Config.spacingAt(usualSpacing, Config.nestSpacing);
	}

	@Override
	public int separation() {
		return Config.separationAt(usualSpacing, usualSeparation, Config.nestSpacing);
	}

	@Override
	public RandomSpreadType spreadType() {
		return spreadType;
	}

	/** The chunk in this one's grid cell where a nest may start: vanilla's random spread, at the scaled distances. */
	@Override
	public ChunkPos getPotentialStructureChunk(long seed, int sourceX, int sourceZ) {
		int spacing = spacing();
		int gridX = Math.floorDiv(sourceX, spacing);
		int gridZ = Math.floorDiv(sourceZ, spacing);
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, gridX, gridZ, salt());
		int limit = spacing - separation();
		int spreadX = spreadType.evaluate(random, limit);
		int spreadZ = spreadType.evaluate(random, limit);
		return new ChunkPos(gridX * spacing + spreadX, gridZ * spacing + spreadZ);
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
		if (Config.nestSpacing <= 0) {
			return false;
		}
		ChunkPos chunk = getPotentialStructureChunk(state.getLevelSeed(), sourceX, sourceZ);
		return chunk.x() == sourceX && chunk.z() == sourceZ;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public MapCodec codec() {
		return CODEC;
	}

	/** Touching the class registers the placement type. */
	public static void register() {
	}
}
