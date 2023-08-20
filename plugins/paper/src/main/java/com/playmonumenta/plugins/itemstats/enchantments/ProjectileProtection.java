package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class ProjectileProtection extends Protection {

	@Override
	public String getName() {
		return "Projectile Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PROJECTILE_PROTECTION;
	}

	@Override
	public DamageType getType() {
		return DamageType.PROJECTILE;
	}

	@Override
	public int getEPF() {
		return 2;
	}

}
