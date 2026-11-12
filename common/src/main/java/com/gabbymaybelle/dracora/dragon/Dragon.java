package com.gabbymaybelle.dracora.dragon;

import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Dracora dragon. It hatches from an egg as its placer's own and grows through three
 * {@link Stage}s: a hatchling walks, sits and follows; a juvenile also carries its owner and
 * hops; an adult flies with them, never faster than {@link Flight#TOP_SPEED}. Breeding comes in
 * a later session.
 *
 * <p>Its colour is one RGB number, picked from its birthplace's palette when it first appears,
 * synced to every player who sees it, and saved with it. The renderer tints the grey texture
 * with it.
 *
 * <p>A ridden dragon is moved by its rider's own game, as vanilla's ridden mobs are, so whether
 * it is in flight ({@link #isAirborne}) is worked out on each side from the rider's keys rather
 * than sent.
 */
public class Dragon extends TamableAnimal {
	private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(Dragon.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(Dragon.class, EntityDataSerializers.INT);
	/** Shown until a colour is picked; never one of the palettes' colours. */
	public static final int NO_COLOUR = 0xFFFFFF;

	/** In flight with a rider: no gravity, steered by where the rider looks. */
	private boolean airborne;
	private int hopRest;

	public Dragon(EntityType<? extends Dragon> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FLYING_SPEED, Flight.TOP_SPEED)
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
		entityData.define(DATA_STAGE, Stage.ADULT.ordinal());
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

	// Growing up

	/** The stage of life this dragon is in. Players' games are told it, since they aren't told the age. */
	public Stage getStage() {
		return Stage.byIndex(this.entityData.get(DATA_STAGE));
	}

	/** Makes a new dragon a fresh hatchling, with both young stages ahead of it. */
	public void startAsHatchling() {
		setAge(Stage.HATCH_AGE);
	}

	/** The age moves on each tick (vanilla's growing up); the stage follows it. */
	@Override
	public void setAge(int age) {
		super.setAge(age);
		if (!level().isClientSide()) {
			Stage stage = Stage.of(age);
			if (stage != getStage()) {
				this.entityData.set(DATA_STAGE, stage.ordinal());
				if (!stage.canCarry()) {
					ejectPassengers();
				}
			}
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_STAGE.equals(accessor)) {
			refreshDimensions();
		}
	}

	/** Its size next to an adult's, for its hitbox and its model alike. */
	@Override
	public float getAgeScale() {
		return getStage().scale();
	}

	// Its owner

	/**
	 * Bonds a hatchling with the player who set its egg down. If they are in this world they tame
	 * it as usual; if they are away it is theirs all the same, and follows them once they are back.
	 */
	public void bondWith(UUID owner) {
		Player player = level().getPlayerByUUID(owner);
		if (player != null) {
			tame(player);
		} else {
			setTame(true, false);
			setOwnerReference(EntityReference.of(owner));
		}
	}

	/**
	 * Its owner's empty hand: a juvenile or an adult takes them on its back; with sneak held, or
	 * for a hatchling, it sits down or stands up instead. Nobody else can do either.
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (isTame() && isOwnedBy(player) && player.getItemInHand(hand).isEmpty()) {
			if (!level().isClientSide()) {
				if (getStage().canCarry() && !player.isSecondaryUseActive()) {
					setOrderedToSit(false);
					setInSittingPose(false);
					player.startRiding(this);
				} else {
					setOrderedToSit(!isOrderedToSit());
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	// Riding and flight

	/** Only its owner steers it. */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player && isOwnedBy(player) ? player : super.getControllingPassenger();
	}

	/** The rider sits on its back, a little below the top of its hitbox. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0.0, dimensions.height() * 0.92, 0.3 * getAgeScale());
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		// Left in the air, it glides down by itself.
		airborne = false;
	}

	/** In flight with its rider, rather than walking, hopping or gliding down. */
	public boolean isAirborne() {
		return airborne;
	}

	/** An adult leaves the ground (or stops falling) and flies; anything younger stays as it is. */
	public void takeOff() {
		if (getStage().canFly() && !airborne) {
			airborne = true;
			if (onGround()) {
				Vec3 movement = getDeltaMovement();
				setDeltaMovement(movement.x, Flight.TAKE_OFF_UP, movement.z);
			}
		}
	}

	/** A juvenile leaps up and forward off the ground; it can't stay up. */
	public void hop() {
		if (hopRest > 0) {
			return;
		}
		hopRest = Flight.HOP_REST;
		float yaw = getYRot() * Mth.DEG_TO_RAD;
		Vec3 movement = getDeltaMovement();
		setDeltaMovement(movement.x - Mth.sin(yaw) * Flight.HOP_FORWARD, Flight.HOP_UP, movement.z + Mth.cos(yaw) * Flight.HOP_FORWARD);
		setOnGround(false);
		this.needsSync = true;
	}

	/** It turns to face where its rider faces; the jump key takes an adult into the air and makes a juvenile hop. */
	@Override
	protected void tickRidden(Player controller, Vec3 riddenInput) {
		super.tickRidden(controller, riddenInput);
		float yaw = getYRot() + Mth.wrapDegrees(controller.getYRot() - getYRot()) * (airborne ? 0.2F : 0.5F);
		setRot(yaw, controller.getXRot() * 0.5F);
		this.yRotO = this.yBodyRot = this.yHeadRot = yaw;
		if (hopRest > 0) {
			hopRest--;
		}
		if (controller.isJumping()) {
			if (getStage().canFly()) {
				takeOff();
			} else if (onGround()) {
				hop();
			}
		} else if (airborne && onGround()) {
			airborne = false;
		}
	}

	/**
	 * On the ground the rider steers as on a horse. In flight, forward follows the rider's line
	 * of sight (look up to climb, down to descend), the jump key rises, and letting go hovers.
	 * The result is never longer than 1, so no mix of keys beats the top speed.
	 */
	@Override
	protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
		float sideways = controller.xxa * 0.5F;
		float forward = controller.zza;
		if (!airborne) {
			return new Vec3(sideways, 0.0, forward <= 0.0F ? forward * 0.25F : forward);
		}
		float pitch = controller.getXRot() * Mth.DEG_TO_RAD;
		double up = 0.0;
		double ahead = 0.0;
		if (forward != 0.0F) {
			double along = forward < 0.0F ? -0.5 : 1.0;
			ahead = Mth.cos(pitch) * along;
			up = -Mth.sin(pitch) * along;
		}
		if (controller.isJumping()) {
			up += 0.6;
		}
		Vec3 input = new Vec3(sideways, up, ahead);
		return input.lengthSqr() > 1.0 ? input.normalize() : input;
	}

	/** Ridden on the ground it trots a little faster than its rider could sprint, well short of a horse's gallop. */
	@Override
	protected float getRiddenSpeed(Player controller) {
		return (float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * Flight.RIDDEN_WALK);
	}

	/** In flight it is pushed along its rider's input and slowed by drag; otherwise it moves as any mob, but never falls faster than a glide. */
	@Override
	public void travel(Vec3 input) {
		if (airborne) {
			float push = (float) Flight.push(getAttributeValue(Attributes.FLYING_SPEED));
			travelFlying(input, push, push, push);
			return;
		}
		super.travel(input);
		Vec3 movement = getDeltaMovement();
		if (!onGround() && !isInLiquid() && movement.y < -Flight.GLIDE_FALL) {
			setDeltaMovement(movement.x, -Flight.GLIDE_FALL, movement.z);
		}
	}

	/** Dragons glide down, so a fall never hurts them. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	/** An adult is a flying mount, so a server that forbids flying doesn't kick its rider. */
	@Override
	public boolean isFlyingVehicle() {
		return getStage().canFly();
	}

	/** Off the ground with air below: the wings beat. */
	public boolean isFlying() {
		return airborne || !onGround() && !isInWater() && !isPassenger() && level().getBlockState(blockPosition().below()).isAir();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Colour", getColour());
	}

	/** The age is vanilla's to save; reading it back goes through {@link #setAge}, which sets the stage. */
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setColour(input.getIntOr("Colour", NO_COLOUR));
	}

	/** Nothing breeds dragons yet, and nothing feeds them: they grow with time alone. */
	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}
}
