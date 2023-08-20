package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ThornsDamage implements Attribute {

	@Override
	public String getName() {
		return "Thorns Damage";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.THORNS;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (StasisListener.isInStasis(player)) {
			return;
		}
		//Only deal damage if damager is alive and damage is not from an ability.
		//Damage will be 0.0 if used in the wrong slot, but attribute will still be called. Cancel the damage effect if this is the case as well.
		if (value > 0
			    && source != null
			    && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)
			    && !event.isBlocked()) {
			DamageUtils.damage(player, source, DamageType.THORNS, value, null, false, true);
		}
	}
}
