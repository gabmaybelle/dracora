package com.gabbymaybelle.dracora.dragon;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * A wild parent's own flight. Scared, it takes to the air, flies up and away from whoever scared
 * it and waits there; once the scare has passed it flies back and comes down beside its egg. A
 * parent whose egg is gone flies far off instead and is not seen again.
 */
final class ShyFlightGoal extends Goal {
	private final Dragon dragon;
	private Vec3 retreat = Vec3.ZERO;

	ShyFlightGoal(Dragon dragon) {
		this.dragon = dragon;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return dragon.isLeaving() || dragon.isGuarding() && dragon.isScared();
	}

	/** It stays in the air until it has come back down by itself. */
	@Override
	public boolean canContinueToUse() {
		return dragon.isAirborne() && (dragon.isLeaving() || dragon.isGuarding());
	}

	@Override
	public void start() {
		dragon.getNavigation().stop();
		dragon.setAirborne(true);
		retreat = dragon.retreat();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (dragon.isLeaving() || dragon.isScared()) {
			dragon.flyToward(retreat);
			return;
		}
		Vec3 landing = dragon.landing();
		if (landing == null) {
			dragon.setAirborne(false);
			return;
		}
		Vec3 to = landing.subtract(dragon.position());
		if (to.horizontalDistanceSqr() < 2.25 && to.y > -4.0) {
			// Above its spot: wings spread, it glides the last few blocks down.
			dragon.setAirborne(false);
		} else {
			dragon.flyToward(landing);
		}
	}

	@Override
	public void stop() {
		dragon.setAirborne(false);
	}
}
