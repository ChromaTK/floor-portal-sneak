package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class FallFragility extends Protection {

	@Override
	public String getName() {
		return "Fall Fragility";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FALL_FRAGILITY;
	}

	@Override
	public DamageType getType() {
		return DamageType.FALL;
	}

	@Override
	public int getEPF() {
		return -3;
	}

}
