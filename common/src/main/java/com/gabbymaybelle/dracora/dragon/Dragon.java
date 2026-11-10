package com.gabbymaybelle.dracora.dragon;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Dracora dragon. For now it walks, looks around, and sits or follows its owner; flight,
 * growth stages, hatching and breeding come in later sessions.
 *
 * <p>Its colour is one RGB number, picked from its birthplace's palette when it first appears,
 * synced to every player who sees it, and saved with it. The renderer tints the grey texture
 * with it.
 */
public class Dragon extends TamableAnimal {
	private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(Dragon.class, EntityDataSerializers.INT);
	/** Shown until a colour is picked; never one of the palettes' colours. */
	public static final int NO_COLOUR = 0xFFFFFF;

	public Dragon(EntityType<? extends Dragon> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FLYING_SPEED, 0.4)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 10.0F, 3.0F));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_COLOUR, NO_COLOUR);
	}

	/** The dragon's RGB colour. */
	public int getColour() {
		return this.entityData.get(DATA_COLOUR);
	}

	public void setColour(int colour) {
		this.entityData.set(DATA_COLOUR, colour & 0xFFFFFF);
	}

	/** Where this dragon was born, by the colour it has (its birthplace's palette), or null before it has one. */
	public @Nullable Birthplace birthplace() {
		int colour = getColour();
		for (Birthplace place : Birthplace.values()) {
			if (place.colours().contains(colour)) {
				return place;
			}
		}
		return null;
	}

	/** A dragon that first appears without a colour takes one from the place it appears in. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
			@Nullable SpawnGroupData groupData) {
		if (getColour() == NO_COLOUR) {
			setColour(Birthplace.of(level.getBiome(blockPosition())).pick(level.getRandom().nextInt()));
		}
		return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
	}

	/** Off the ground with air below: the wings beat. Flight proper (a later session) will sync its own state. */
	public boolean isFlying() {
		return !onGround() && !isInWater() && !isPassenger() && level().getBlockState(blockPosition().below()).isAir();
	}

	/** Its owner, with an empty hand, sits a tamed dragon down or stands it up. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (isTame() && isOwnedBy(player) && player.getItemInHand(hand).isEmpty()) {
			if (!level().isClientSide()) {
				setOrderedToSit(!isOrderedToSit());
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Colour", getColour());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setColour(input.getIntOr("Colour", NO_COLOUR));
	}

	/** Nothing breeds dragons yet; breeding comes in a later session. */
	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}
}
