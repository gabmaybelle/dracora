package com.gabbymaybelle.dracora.dragon;

import com.gabbymaybelle.dracora.Config;
import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.egg.EggBlock;
import com.gabbymaybelle.dracora.egg.EggBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Dracora dragon. It hatches from an egg as its placer's own and grows through three
 * {@link Stage}s: a hatchling walks, sits and follows; a juvenile also carries its owner and
 * hops; an adult flies with them, never faster than {@link Flight#TOP_SPEED}. Two tamed adults
 * fed raw fish lay an egg in their blended colour ({@link Breeding}).
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

	/** How far a wild parent strolls from its nest, in blocks. */
	public static final int NEST_RADIUS = 6;
	/** How long a parent whose egg is gone flies before it is out of sight and gone, in ticks. */
	public static final int LEAVE_TICKS = 200;
	private static final int NEST_SEARCH_EVERY = 20;
	private static final int NEST_SEARCHES = 5;

	/** In flight: no gravity, steered by where the rider looks, or flown by a wild parent itself. */
	private boolean airborne;
	private int hopRest;

	/** A wild parent watching over a nest's egg. */
	private boolean guarding;
	private @Nullable BlockPos nest;
	private int nestSearches;
	private final Map<UUID, Trust> trust = new HashMap<>();
	/** Ticks left of a scare: the parent stays in the air until it passes. */
	private int scared;
	/** Its egg is gone: it flies off and does not come back. */
	private boolean leaving;
	private int leavingTicks;

	public Dragon(EntityType<? extends Dragon> type, Level level) {
		super(type, level);
		if (!level.isClientSide()) {
			applyFlightSpeed();
		}
	}

	/**
	 * Sets its top flying speed from the server's setting. The flying speed attribute is sent to
	 * players' games, so a rider's game (which moves a ridden dragon) flies it at the same speed.
	 */
	private void applyFlightSpeed() {
		var speed = getAttribute(Attributes.FLYING_SPEED);
		if (speed != null && speed.getBaseValue() != Config.flightSpeedPerTick()) {
			speed.setBaseValue(Config.flightSpeedPerTick());
		}
	}

	/** Whether it can carry its owner now: an adult always, a juvenile unless the setting turns that off. */
	public boolean canCarry() {
		Stage stage = getStage();
		return stage == Stage.ADULT || stage.canCarry() && Config.juvenileRiding;
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
		this.goalSelector.addGoal(1, new ShyFlightGoal(this));
		this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 10.0F, 3.0F));
		this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.8));
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
		if (spawnReason == EntitySpawnReason.STRUCTURE) {
			// Generated with a nest: it finds the egg beside it once the world around it is loaded.
			guarding = true;
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
				if (!canCarry()) {
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
				if (canCarry() && !player.isSecondaryUseActive()) {
					setOrderedToSit(false);
					setInSittingPose(false);
					player.startRiding(this);
				} else {
					setOrderedToSit(!isOrderedToSit());
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (isBaby() && isFood(player.getItemInHand(hand))) {
			// Young dragons grow with time alone: fish neither feeds nor hurries them.
			return InteractionResult.PASS;
		}
		return super.mobInteract(player, hand);
	}

	// A wild parent and its nest

	/** A wild dragon watching over a nest's egg. */
	public boolean isGuarding() {
		return guarding && !isTame();
	}

	/** Where its egg is, once it has found it. */
	public @Nullable BlockPos nest() {
		return nest;
	}

	/** Scared into the air; it comes back down once the scare has passed. */
	public boolean isScared() {
		return scared > 0;
	}

	/** Its egg is gone and so, shortly, is it. */
	public boolean isLeaving() {
		return leaving;
	}

	/** How far it trusts this player. */
	public Trust trustOf(UUID player) {
		return trust.computeIfAbsent(player, id -> new Trust());
	}

	/** Makes this dragon the parent of the egg here: it stays near, the egg is guarded, and an egg with no colour takes its parent's. */
	public void guard(BlockPos eggAt) {
		guarding = true;
		nest = eggAt.immutable();
		setHomeTo(nest, NEST_RADIUS);
		EggBlockEntity egg = EggBlock.eggAt(level(), nest);
		if (egg != null) {
			if (!egg.hasColour()) {
				egg.setColour(getColour());
			}
			egg.setGuarded(true);
		}
	}

	/** Looks for the nest's egg beside it; a parent that finds none has no reason to stay. */
	private void findNest(ServerLevel level) {
		BlockPos here = blockPosition();
		if (!level.isLoaded(here.offset(-NEST_RADIUS, 0, -NEST_RADIUS)) || !level.isLoaded(here.offset(NEST_RADIUS, 0, NEST_RADIUS))) {
			return;
		}
		for (BlockPos pos : BlockPos.betweenClosed(here.offset(-NEST_RADIUS, -3, -NEST_RADIUS), here.offset(NEST_RADIUS, 3, NEST_RADIUS))) {
			EggBlockEntity egg = EggBlock.eggAt(level, pos);
			if (egg != null && egg.guarded()) {
				guard(pos);
				return;
			}
		}
		if (++nestSearches >= NEST_SEARCHES) {
			leave();
		}
	}

	/**
	 * A parent's look around: if its egg is gone it leaves; otherwise each player nearby either
	 * scares it or, staying calm, earns a little of its trust (see {@link #notice}).
	 */
	public void watchNest() {
		if (!(level() instanceof ServerLevel level) || !isGuarding() || nest == null || !level.isLoaded(nest)) {
			return;
		}
		EggBlockEntity egg = EggBlock.eggAt(level, nest);
		if (egg == null || !egg.guarded()) {
			leave();
			return;
		}
		long day = level.getOverworldClockTime() / Stage.STAGE_TICKS;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(Trust.SPRINT_RANGE), p -> p.isAlive() && !p.isSpectator())) {
			notice(player, day);
		}
	}

	/**
	 * What one look at one player does. Sprinting nearby, or coming right up close without
	 * sneaking, scares the parent into the air unless it already trusts them fully. A player who
	 * stays near and calm while it is on the ground earns a visit, one a day; the last one earns
	 * the egg.
	 */
	public void notice(Player player, long day) {
		Trust theirs = trustOf(player.getUUID());
		Trust.Sight sight = Trust.see(distanceTo(player), player.isSprinting(), player.isShiftKeyDown());
		if (sight == Trust.Sight.SCARY) {
			if (!theirs.full()) {
				startle(player);
			}
		} else if (sight == Trust.Sight.CALM && scared == 0 && theirs.calm(Trust.WATCH_EVERY, day)) {
			level().broadcastEntityEvent(this, (byte) 7);
			if (theirs.full()) {
				trustWithEgg(player.getUUID(), true);
			}
		}
	}

	/** Scares the parent into the air for a while; whoever did it loses the visit they were on. */
	public void startle(@Nullable Player by) {
		if (by != null) {
			trustOf(by.getUUID()).startled();
		}
		if (scared == 0) {
			level().broadcastEntityEvent(this, (byte) 6);
		}
		scared = Trust.SCARED_TICKS;
	}

	private void trustWithEgg(UUID player, boolean trusts) {
		EggBlockEntity egg = nest == null ? null : EggBlock.eggAt(level(), nest);
		if (egg != null) {
			egg.setTrusted(player, trusts);
		}
	}

	/** Its egg is gone (taken by someone it trusted, or lost): it flies off for good. */
	private void leave() {
		guarding = false;
		leaving = true;
		nest = null;
		scared = 0;
		clearHome();
	}

	/** Being hurt scares a parent, and a player who hurts it loses a visit they had earned. It never fights back. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isGuarding()) {
			Player by = source.getEntity() instanceof Player player ? player : null;
			if (by != null) {
				trustOf(by.getUUID()).hurt();
				trustWithEgg(by.getUUID(), false);
			}
			startle(by);
		}
		return hurt;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (leaving) {
			if (++leavingTicks > LEAVE_TICKS) {
				discard();
			}
		} else if (isGuarding()) {
			if (scared > 0) {
				scared--;
			}
			if (nest == null) {
				if (tickCount % NEST_SEARCH_EVERY == 0) {
					findNest(level);
				}
			} else if (tickCount % Trust.WATCH_EVERY == 0) {
				watchNest();
			}
		}
	}

	/** Where a parent flies to: up and away from the nearest player when scared, far off when leaving. */
	Vec3 retreat() {
		Vec3 from = nest == null ? position() : Vec3.atBottomCenterOf(nest);
		Player near = level().getNearestPlayer(this, 48.0);
		Vec3 away = near == null ? Vec3.ZERO : new Vec3(getX() - near.getX(), 0.0, getZ() - near.getZ());
		if (away.lengthSqr() < 0.01) {
			float turn = getRandom().nextFloat() * Mth.TWO_PI;
			away = new Vec3(Mth.cos(turn), 0.0, Mth.sin(turn));
		}
		away = away.normalize();
		double along = leaving ? 80.0 : 16.0;
		double up = leaving ? 48.0 : 12.0;
		double x = from.x + away.x * along;
		double z = from.z + away.z * along;
		int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
		return new Vec3(x, Math.max(from.y + up, ground + 8.0), z);
	}

	/** Where a parent comes down again: just above the ground beside its egg. */
	@Nullable Vec3 landing() {
		return nest == null ? null : Vec3.atBottomCenterOf(nest).add(2.2, 3.0, 0.0);
	}

	/** One tick of a wild dragon's own flight: a push toward a point, easing off as it arrives, and a turn to face the way it goes. */
	void flyToward(Vec3 target) {
		Vec3 to = target.subtract(position());
		double distance = to.length();
		if (distance < 0.5) {
			return;
		}
		double push = Flight.push(Flight.WILD_SPEED) * Math.min(1.0, distance / 4.0);
		setDeltaMovement(getDeltaMovement().add(to.scale(push / distance)));
		if (to.horizontalDistanceSqr() > 0.25) {
			float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
			setYRot(Mth.approachDegrees(getYRot(), yaw, 10.0F));
			this.yBodyRot = this.yHeadRot = getYRot();
		}
	}

	void setAirborne(boolean airborne) {
		this.airborne = airborne;
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
		if (guarding || leaving) {
			output.putBoolean("Guarding", guarding);
			output.putBoolean("Leaving", leaving);
			output.storeNullable("Nest", BlockPos.CODEC, nest);
			List<Trust.Saved> earned = new ArrayList<>();
			trust.forEach((player, theirs) -> {
				if (theirs.visits() > 0) {
					earned.add(new Trust.Saved(player, theirs.visits(), theirs.lastDay()));
				}
			});
			output.store("Trust", Trust.Saved.CODEC.listOf(), earned);
		}
	}

	/** The age is vanilla's to save; reading it back goes through {@link #setAge}, which sets the stage. */
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setColour(input.getIntOr("Colour", NO_COLOUR));
		guarding = input.getBooleanOr("Guarding", false);
		leaving = input.getBooleanOr("Leaving", false);
		nest = input.read("Nest", BlockPos.CODEC).orElse(null);
		trust.clear();
		for (Trust.Saved saved : input.read("Trust", Trust.Saved.CODEC.listOf()).orElse(List.of())) {
			trust.put(saved.player(), new Trust(saved.visits(), saved.day()));
		}
		// A saved dragon keeps the speed it was saved with; the server's setting now wins.
		if (!level().isClientSide()) {
			applyFlightSpeed();
		}
	}

	// Breeding

	/** Raw fish (salmon or cod) is what two tamed adults are fed to breed. */
	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SALMON) || stack.is(Items.COD);
	}

	/** Only a tamed adult falls in love; a wild one, or one resting after laying, does not. */
	@Override
	public boolean canFallInLove() {
		return isTame() && getStage() == Stage.ADULT && super.canFallInLove();
	}

	/** Two tamed adults lay an egg rather than bear a baby, so there is no offspring mob. */
	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	/**
	 * The pair lays an egg item in the blend of their colours (or, rarely, a mutation). Whoever sets
	 * it down hatches it and owns the hatchling, as with any egg. Both then rest a Minecraft day.
	 */
	@Override
	public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
		int partnerColour = partner instanceof Dragon dragon ? dragon.getColour() : getColour();
		int colour = Breeding.eggColour(getColour(), partnerColour, getRandom().nextInt(), getRandom().nextInt());
		ItemEntity egg = new ItemEntity(level, getX(), getY(), getZ(), Eggs.coloured(colour));
		egg.setDefaultPickUpDelay();
		finalizeSpawnChildFromBreeding(level, partner, null);
		setAge(Breeding.REST_TICKS);
		partner.setAge(Breeding.REST_TICKS);
		level.addFreshEntity(egg);
	}
}
