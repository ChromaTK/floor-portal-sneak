package com.playmonumenta.plugins.effects;

import org.jetbrains.annotations.Nullable;

public class ThuribleBonusHealing extends SingleArgumentEffect {
	public static final String effectID = "ThuribleBonusHealing";

	public ThuribleBonusHealing(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public String toString() {
		return String.format("ThuribleBonusHealing duration=%d healing=%f", this.getDuration(), mAmount);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Thurible Healing";
	}
}
