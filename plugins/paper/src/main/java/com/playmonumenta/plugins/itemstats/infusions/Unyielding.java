package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import java.util.Collection;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Unyielding implements Infusion {

	public static final String MODIFIER = "UnyieldingMod";
	public static final double KB_PER_LEVEL = 0.06;

	@Override
	public String getName() {
		return "Unyielding";
	}

	@Override
	public ItemStatUtils.InfusionType getInfusionType() {
		return InfusionType.UNYIELDING;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		AttributeInstance kn = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		if (kn != null) {
			Collection<AttributeModifier> modTemp = kn.getModifiers();
			for (AttributeModifier mod : modTemp) {
				if (mod != null && mod.getName().equals(MODIFIER)) {
					kn.removeModifier(mod);
				}
			}
		}
		double level = plugin.mItemStatManager.getInfusionLevel(player, ItemStatUtils.InfusionType.UNYIELDING);
		if (level > 0) {
			AttributeInstance knockBack = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
			if (knockBack != null) {
				AttributeModifier mod = new AttributeModifier(MODIFIER, getKnockbackResistance(level),
					AttributeModifier.Operation.ADD_SCALAR);
				knockBack.addModifier(mod);
			}
		}
	}

	public static double getKnockbackResistance(double level) {
		return KB_PER_LEVEL * level;
	}

}
