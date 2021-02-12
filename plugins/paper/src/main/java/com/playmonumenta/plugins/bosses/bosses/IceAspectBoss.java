package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.PotionUtils;

public class IceAspectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_iceaspect";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new IceAspectBoss(plugin, boss);
	}

	public IceAspectBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		PotionUtils.applyPotion(mBoss, (LivingEntity) event.getEntity(), new PotionEffect(PotionEffectType.SLOW, 80, 1, false, true));
	}
}
