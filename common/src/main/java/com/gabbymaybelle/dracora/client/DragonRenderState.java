package com.gabbymaybelle.dracora.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the dragon's model needs each frame, copied from the dragon by {@link DragonRenderer}. */
public class DragonRenderState extends LivingEntityRenderState {
	/** The dragon's RGB colour, which tints the grey texture. */
	public int colour = 0xFFFFFF;
	/** Off the ground: the wings beat instead of folding along the sides. */
	public boolean flying;
	/** Sitting when told to: legs tucked, wings folded. */
	public boolean sitting;
}
