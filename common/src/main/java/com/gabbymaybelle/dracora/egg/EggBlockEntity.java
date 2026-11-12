package com.gabbymaybelle.dracora.egg;

import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.dragon.Dragon;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What an egg remembers: its colour (the hatchling's), who set it down (the hatchling's owner)
 * and the warmth it has gathered. It never ticks; {@link EggBlock} looks for warmth on a
 * scheduled tick. A nest's egg is guarded by its parent: only the players the parent has come to
 * trust can take it. Players are sent the colour, to tint the egg, and for a guarded egg who may
 * take it, so their own game knows whether it will break.
 *
 * <p>As an item the egg carries its colour in vanilla's dyed-colour component, so the item is
 * tinted with no code of its own; the owner and warmth stay behind when an egg is picked up.
 */
public class EggBlockEntity extends BlockEntity {
	private static final String COLOUR = "Colour";
	private static final String OWNER = "Owner";
	private static final String WARMTH = "Warmth";
	private static final String GUARDED = "Guarded";
	private static final String TRUSTED = "Trusted";

	private int colour = Dragon.NO_COLOUR;
	private @Nullable UUID owner;
	private int warmth;
	private boolean guarded;
	private final Set<UUID> trusted = new HashSet<>();

	public EggBlockEntity(BlockPos pos, BlockState state) {
		super(Eggs.BLOCK_ENTITY.get(), pos, state);
	}

	/** The egg's RGB colour, or {@link Dragon#NO_COLOUR} before it has one. */
	public int colour() {
		return colour;
	}

	public boolean hasColour() {
		return colour != Dragon.NO_COLOUR;
	}

	public void setColour(int colour) {
		this.colour = colour & 0xFFFFFF;
		changed();
	}

	/** The player who set the egg down, or null for an egg nobody placed (it never hatches). */
	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** The warmth gathered so far, in ticks. */
	public int warmth() {
		return warmth;
	}

	public void setWarmth(int warmth) {
		this.warmth = Math.max(0, warmth);
		setChanged();
	}

	/** A nest's egg, watched over by its parent: nobody can take it until the parent trusts them. */
	public boolean guarded() {
		return guarded;
	}

	public void setGuarded(boolean guarded) {
		if (this.guarded != guarded) {
			this.guarded = guarded;
			changed();
		}
	}

	/** Whether this player may take the egg: anyone for an ordinary egg, only those its parent trusts for a nest's. */
	public boolean lets(UUID player) {
		return !guarded || trusted.contains(player);
	}

	/** The parent has come to trust this player, or has stopped trusting them. */
	public void setTrusted(UUID player, boolean trusts) {
		if (trusts ? trusted.add(player) : trusted.remove(player)) {
			changed();
		}
	}

	/** Saves, and tells players' games about the change. */
	private void changed() {
		setChanged();
		Level level = getLevel();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt(COLOUR, colour);
		output.putInt(WARMTH, warmth);
		output.storeNullable(OWNER, UUIDUtil.CODEC, owner);
		if (guarded) {
			output.putBoolean(GUARDED, true);
			output.store(TRUSTED, UUIDUtil.CODEC.listOf(), List.copyOf(trusted));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		colour = input.getIntOr(COLOUR, Dragon.NO_COLOUR) & 0xFFFFFF;
		warmth = input.getIntOr(WARMTH, 0);
		owner = input.read(OWNER, UUIDUtil.CODEC).orElse(null);
		guarded = input.getBooleanOr(GUARDED, false);
		trusted.clear();
		trusted.addAll(input.read(TRUSTED, UUIDUtil.CODEC.listOf()).orElse(List.of()));
		// In a player's game the colour arrives after the egg was drawn: draw it again.
		Level level = getLevel();
		if (level != null && level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Players' games only need the colour and, for a guarded egg, who may take it. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt(COLOUR, colour);
		if (guarded) {
			tag.putBoolean(GUARDED, true);
			tag.store(TRUSTED, UUIDUtil.CODEC.listOf(), List.copyOf(trusted));
		}
		return tag;
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		DyedItemColor dyed = components.get(DataComponents.DYED_COLOR);
		colour = dyed != null ? dyed.rgb() & 0xFFFFFF : Dragon.NO_COLOUR;
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (hasColour()) {
			components.set(DataComponents.DYED_COLOR, new DyedItemColor(colour));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeComponentsFromTag(ValueOutput output) {
		output.discard(COLOUR);
	}
}
