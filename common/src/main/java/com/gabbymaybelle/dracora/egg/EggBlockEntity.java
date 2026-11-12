package com.gabbymaybelle.dracora.egg;

import com.gabbymaybelle.dracora.Eggs;
import com.gabbymaybelle.dracora.dragon.Dragon;
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
 * scheduled tick. Only the colour is sent to players, to tint the egg.
 *
 * <p>As an item the egg carries its colour in vanilla's dyed-colour component, so the item is
 * tinted with no code of its own; the owner and warmth stay behind when an egg is picked up.
 */
public class EggBlockEntity extends BlockEntity {
	private static final String COLOUR = "Colour";
	private static final String OWNER = "Owner";
	private static final String WARMTH = "Warmth";

	private int colour = Dragon.NO_COLOUR;
	private @Nullable UUID owner;
	private int warmth;

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

	/** Saves, and tells players' games about the new colour. */
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
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		colour = input.getIntOr(COLOUR, Dragon.NO_COLOUR) & 0xFFFFFF;
		warmth = input.getIntOr(WARMTH, 0);
		owner = input.read(OWNER, UUIDUtil.CODEC).orElse(null);
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

	/** Players' games only need the colour. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt(COLOUR, colour);
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
