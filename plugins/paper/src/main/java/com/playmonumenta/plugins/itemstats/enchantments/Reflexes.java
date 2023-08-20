package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Reflexes implements Enchantment {

	private static final double AGIL_BONUS_PER_LEVEL = 0.2;
	public static final int MOB_CAP = 4;
	public static final int RADIUS = 8;

	@Override
	public String getName() {
		return "Reflexes";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REFLEXES;
	}

	public static double applyReflexes(DamageEvent event, Plugin plugin, Player player) {
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(player.getLocation(), RADIUS);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		if (mobs.size() >= MOB_CAP) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.REFLEXES) * AGIL_BONUS_PER_LEVEL;
		} else {
			return 0;
		}
	}

}
