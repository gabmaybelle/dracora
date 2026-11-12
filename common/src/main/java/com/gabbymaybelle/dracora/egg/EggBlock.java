package com.gabbymaybelle.dracora.egg;

import com.gabbymaybelle.dracora.Dragons;
import com.gabbymaybelle.dracora.dragon.Birthplace;
import com.gabbymaybelle.dracora.dragon.Dragon;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A dragon's egg. It carries the hatchling's colour (an egg with none takes one from the biome
 * it is set down in), remembers who set it down, and hatches for them once it has spent long
 * enough beside something warm (see {@link Incubation}). An egg nobody placed never hatches.
 *
 * <p>It does no work between its warmth checks: one scheduled tick every
 * {@link Incubation#CHECK_EVERY} ticks.
 */
public class EggBlock extends Block implements EntityBlock {
	/** How cracked the egg looks: 0 whole, 1 and 2 on the way to hatching. */
	public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
	private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 14.0);

	public EggBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(HATCH, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HATCH);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EggBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** The egg as it stands in the world. */
	public static @Nullable EggBlockEntity eggAt(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EggBlockEntity egg ? egg : null;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(this)) {
			level.scheduleTick(pos, this, Incubation.CHECK_EVERY);
		}
	}

	/** Set down by a player, the egg is theirs; an egg with no colour yet takes one from this biome. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
		super.setPlacedBy(level, pos, state, by, stack);
		EggBlockEntity egg = eggAt(level, pos);
		if (level.isClientSide() || egg == null) {
			return;
		}
		if (!egg.hasColour()) {
			egg.setColour(Birthplace.of(level.getBiome(pos)).pick(level.getRandom().nextInt()));
		}
		if (by instanceof Player player) {
			egg.setOwner(player.getUUID());
		}
	}

	/** The warmth check: a placed egg beside something warm gathers warmth, cracks, and hatches. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.scheduleTick(pos, this, Incubation.CHECK_EVERY);
		EggBlockEntity egg = eggAt(level, pos);
		if (egg == null || egg.owner() == null || !Incubation.isWarm(level, pos)) {
			return;
		}
		egg.setWarmth(egg.warmth() + Incubation.CHECK_EVERY);
		int stage = Incubation.stage(egg.warmth());
		if (stage >= Incubation.HATCHED) {
			hatch(level, pos, egg, random);
		} else if (stage > state.getValue(HATCH)) {
			level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
			level.setBlock(pos, state.setValue(HATCH, stage), Block.UPDATE_ALL);
		}
	}

	/** The egg breaks open and a hatchling in the egg's colour stands in its place, bonded to whoever set the egg down. */
	private static void hatch(ServerLevel level, BlockPos pos, EggBlockEntity egg, RandomSource random) {
		int colour = egg.hasColour() ? egg.colour() : Birthplace.of(level.getBiome(pos)).pick(random.nextInt());
		UUID owner = egg.owner();
		level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
		level.destroyBlock(pos, false);
		Dragon dragon = Dragons.DRAGON.get().create(level, EntitySpawnReason.BREEDING);
		if (dragon == null) {
			return;
		}
		Vec3 at = Vec3.atBottomCenterOf(pos);
		dragon.snapTo(at.x(), at.y(), at.z(), Mth.wrapDegrees(random.nextFloat() * 360.0F), 0.0F);
		dragon.setColour(colour);
		dragon.startAsHatchling();
		if (owner != null) {
			dragon.bondWith(owner);
		}
		level.addFreshEntity(dragon);
	}

	/** In a player's game: a thin wisp of smoke now and then while the egg is being kept warm. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(6) == 0 && Incubation.isWarm(level, pos)) {
			level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9,
					pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
		}
	}

	/** Picking the block in creative gives an egg of the same colour. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
		EggBlockEntity egg = eggAt(level, pos);
		if (egg != null) {
			stack.applyComponents(egg.collectComponents());
		}
		return stack;
	}
}
